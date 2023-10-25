package androidx.media3.exoplayer.endeavor;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.endeavor.SlidingCounter;
import androidx.media3.common.endeavor.TrackSwitcher;
import androidx.media3.common.endeavor.WebUtil;
import androidx.media3.common.util.Log;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TrackCollector {

  private final SlidingCounter counter;
  private final TrackSwitcher switcher;
  private final TrackSelector callback;
  private final Map<Format, Long> blacklistUntilTimes = new HashMap<>();

  private boolean noGroupConstraint = true;
  private boolean videoUseFixedSelection;
  private ExoTrackSelection videoTrackSelection; // only used during reselect tracks
  private Format videoSelectedFormat; // only used to init the selected index.
  private String preferredAudioName;
  private String preferredTextName;

  private MappingTrackSelector.MappedTrackInfo mappedTrackInfo;

  public TrackCollector(TrackSelector callback) {
    this.counter = new SlidingCounter(10, 60000);
    this.switcher = (DebugUtil.debug_switch_ms > 0 ? new TrackSwitcher(DebugUtil.debug_switch_ms) : null);
    this.callback = callback;
  }

  // May update status for the blacklistUntilTimes, videoTrackSelection, preferredAudioName and so on.
  public void onMappedTrackInfoChanged(
      MappingTrackSelector.MappedTrackInfo mappedTrackInfo,
      DefaultTrackSelector.Parameters parameters,
      ExoTrackSelection.Definition[] overrideDefinitions) {
    this.noGroupConstraint = true;
    this.videoUseFixedSelection = false;
    this.videoTrackSelection  = null;
    this.mappedTrackInfo = mappedTrackInfo;

    for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
      if (C.TRACK_TYPE_VIDEO == mappedTrackInfo.getRendererType(i)) {
        // Found group constraint or not.
        TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
        for (int j = 0; j < trackGroups.length; j++) {
          TrackGroup track = trackGroups.get(j);
          for (int k = 0; k < track.length; k++) {
            Format format = track.getFormat(k);
            if (!WebUtil.empty(getAudioGroupId(format)) || !WebUtil.empty(getSubtitleGroupId(format))) {
              noGroupConstraint = false;
              break;
            }
          }
        }
        // Found video use fixed selection or not.
        videoUseFixedSelection = (overrideDefinitions[i] != null && overrideDefinitions[i].tracks.length == 1);
      } else if (C.TRACK_TYPE_AUDIO == mappedTrackInfo.getRendererType(i)) {
        // Init the preferred audio name.
        TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
        if (overrideDefinitions[i] != null) {
          Format format = overrideDefinitions[i].group.getFormat(overrideDefinitions[i].tracks[0]);
          preferredAudioName = getFormatName(format);
        } else if (preferredAudioName == null) {
          preferredAudioName = getParametersPreferred(trackGroups, parameters.preferredAudioLanguages);
          if (preferredAudioName == null) {
            preferredAudioName = getDefaultPreferred(trackGroups);
          }
        }
      } else if (C.TRACK_TYPE_TEXT == mappedTrackInfo.getRendererType(i)) {
        // Init the preferred text name.
        if (overrideDefinitions[i] != null) {
          Format format = overrideDefinitions[i].group.getFormat(overrideDefinitions[i].tracks[0]);
          preferredTextName = getFormatName(format);
        } else if (preferredTextName == null) {
          TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
          preferredTextName = getParametersPreferred(trackGroups, parameters.preferredTextLanguages);
          if (preferredTextName == null) {
            preferredTextName = "";
          }
        }
      }
    }

    // Clean the expired blacklist records.
    long nowMs = SystemClock.elapsedRealtime();
    Iterator<Long> iterator = blacklistUntilTimes.values().iterator();
    while (iterator.hasNext()) {
      Long exist = iterator.next();
      if (exist <= nowMs) {
        iterator.remove();
      }
    }
  }

  // Exist group constraint or not.
  public boolean isNoGroupConstraint() {
    return noGroupConstraint;
  }

  public TrackSwitcher getTrackSwitcher() {
    return switcher;
  }

  // Set the video selection.
  public void setVideoTrackSelection(ExoTrackSelection videoTrackSelection) {
    this.videoTrackSelection = videoTrackSelection;
    setSwitcherTrackCount(videoTrackSelection);
  }

  // Get the selected format of video selection, to init the selected index.
  public Format getVideoSelectedFormat() {
    return videoSelectedFormat;
  }

  // Get the preferred audio name.
  public String getPreferredAudioName() {
    return preferredAudioName;
  }

  // Get the preferred text name.
  public String getPreferredTextName() {
    return preferredTextName;
  }

  // May update status for the videoTrackSelection, preferredAudioName and so on.
  public void onTrackSelectionsChanged(ExoTrackSelection[] rendererTrackSelections) {
    if (!noGroupConstraint) {
      for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
        if (rendererTrackSelections[i] == null) {
          continue;
        }
        if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
          // May update the video track selections as the previous generated.
          if (videoTrackSelection != null) {
            rendererTrackSelections[i] = videoTrackSelection;
          }
          setSwitcherTrackCount(rendererTrackSelections[i]);
        } else if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
          // Update the preferred audio name to current selected.
          preferredAudioName = getFormatName(rendererTrackSelections[i].getSelectedFormat());
        } else if (mappedTrackInfo.getRendererType(i) == C.TRACK_TYPE_TEXT) {
          // Update the preferred text name to current selected.
          preferredTextName = getFormatName(rendererTrackSelections[i].getSelectedFormat());
        }
        // Set the callback for all renderer selections.
        rendererTrackSelections[i].setTrackCollector(this);
      }
    }

    videoTrackSelection = null;
    videoSelectedFormat = null;
  }

  private void setSwitcherTrackCount(ExoTrackSelection trackSelection) {
    if (switcher != null && trackSelection != null) {
      switcher.setTrackCount(trackSelection.length());
    }
  }

  // May need to reselect tracks because the audio group constraint is changed.
  public void onAdaptiveTrackChanged(Format prevSelected, Format newSelected) {
    if (noGroupConstraint) {
      return;
    }

    // Check the group id constraint is changed or not, only for video track.
    if (!getAudioGroupId(prevSelected).equals(getAudioGroupId(newSelected))
        || !getSubtitleGroupId(prevSelected).equals(getSubtitleGroupId(newSelected))) {
      videoSelectedFormat = newSelected;
      reselectTracks();
    }
  }

  // Reselect the video / audio tracks.
  private void reselectTracks() {
    // Add dead loop protection.
    if (counter.add(System.currentTimeMillis())) {
      Log.i(WebUtil.DEBUG, "reselectTracks ignored, too frequent for more than 9 calls duration one minute.");
      return;
    }

    Log.i(WebUtil.DEBUG, "reselectTracks for group constraint");
    callback.invalidate();
  }

  // Notify the blacklist event to update the blacklist list for global.
  @SuppressLint("DefaultLocale")
  public void setBlacklist(Format format, long blackedTimeMs) {
    if (noGroupConstraint || format == null) {
      return;
    }
    blacklistUntilTimes.put(format, blackedTimeMs);

    int trackType = getTrackType(format);
    if (trackType == C.TRACK_TYPE_VIDEO) {
      if (videoUseFixedSelection) {
        reselectTracks();
      }
      Log.i(WebUtil.DEBUG, String.format("track blacklist [%d, %s], timeMs %d", format.bitrate, getAudioGroupId(format), blackedTimeMs));
    } else {
      if (trackType == C.TRACK_TYPE_AUDIO) {
        reselectTracks();
      }
      Log.i(WebUtil.DEBUG, String.format("track blacklist [%s, %s], timeMs %d", getFormatName(format), getFormatGroupId(format), blackedTimeMs));
    }
  }

  // Find the track type for the specified format.
  private int getTrackType(Format target) {
    if (mappedTrackInfo == null) {
      return C.TRACK_TYPE_UNKNOWN;
    }
    for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
      TrackGroupArray trackGroup = mappedTrackInfo.getTrackGroups(i);
      for (int j = 0; j < trackGroup.length; j++) {
        TrackGroup track = trackGroup.get(j);
        for (int k = 0; k < track.length; k++) {
          Format format = track.getFormat(k);
          if (format == target) {
            return mappedTrackInfo.getRendererType(i);
          }
        }
      }
    }
    return C.TRACK_TYPE_UNKNOWN;
  }

  // Override the isBlacklisted() to apply group constraint.
  public boolean isBlacklisted(Format format, long nowMs) {
    // Check this format is blacklisted by self or not.
    if (isSelfBlacklisted(format, nowMs)) {
      return true;
    }
    // Need to check group constraint of this format or not.
    String audioGroupId = getAudioGroupId(format);
    String subtitleGroupId = getSubtitleGroupId(format);
    if (WebUtil.empty(audioGroupId) && WebUtil.empty(subtitleGroupId)) {
      return false;
    }
    // Check this format is blacklisted by audio group or not.
    if (isGroupBlacklisted(WebUtil.emptyIfNull(preferredAudioName), audioGroupId, C.TRACK_TYPE_AUDIO, nowMs)) {
      return true;
    }
    // Check this format is blacklisted by subtitle group or not.
    return isGroupBlacklisted(WebUtil.emptyIfNull(preferredTextName), subtitleGroupId, C.TRACK_TYPE_TEXT, nowMs);
  }

  // Check this format is blacklisted by self or not.
  private boolean isSelfBlacklisted(Format format, long nowMs) {
    Long exist = (blacklistUntilTimes.get(format));
    return (exist != null && exist > nowMs);
  }

  // Check this format is blacklisted by the specified group constrain or not.
  private boolean isGroupBlacklisted(String name, String groupId, int trackType, long nowMs) {
    boolean nameIsEmpty = WebUtil.empty(name);
    boolean groupIdIsEmpty = WebUtil.empty(groupId);
    if (nameIsEmpty && groupIdIsEmpty) {
      return false;
    }

    boolean groupBlacklisted = false;
    TrackGroupArray trackGroup = getTrackGroups(trackType);
    for (int i = 0; i < trackGroup.length; i++) {
      TrackGroup track = trackGroup.get(i);
      for (int j = 0; j < track.length; j++) {
        Format format = track.getFormat(j);
        String formatName = getFormatName(format);
        String formatGroupId = getFormatGroupId(format);
        if ((nameIsEmpty || formatName.equals(name)) && (groupIdIsEmpty || formatGroupId.equals(groupId))) {
          if (!isSelfBlacklisted(format, nowMs)) {
            return false;
          }
          groupBlacklisted = true;
        }
      }
    }
    return groupBlacklisted;
  }

  // Find the trackGroups mapped to the renderer at the specified index.
  private TrackGroupArray getTrackGroups(int trackType) {
    if (mappedTrackInfo == null) {
      return new TrackGroupArray();
    }
    for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
      if (trackType == mappedTrackInfo.getRendererType(i)) {
        return mappedTrackInfo.getTrackGroups(i);
      }
    }
    return new TrackGroupArray();
  }

  private String getParametersPreferred(
      TrackGroupArray trackGroups,
      ImmutableList<String> preferredLanguages) {
    if (preferredLanguages.size() == 0) {
      return null;
    }

    int bestLanguageScore = 0;
    int bestLanguageIndex = Integer.MAX_VALUE;
    String preferredName = null;
    for (int i = 0; i < trackGroups.length; i++) {
      TrackGroup trackGroup = trackGroups.get(i);
      if (trackGroup.length == 0) {
        continue;
      }
      Format format = trackGroup.getFormat(0);
      for (int j = 0; j < preferredLanguages.size(); j++) {
        int score = DefaultTrackSelector.getFormatLanguageScore(
                format,
                preferredLanguages.get(j),
                /* allowUndeterminedFormatLanguage= */ false);
        if (score > 0) {
          if (j < bestLanguageIndex || (j == bestLanguageIndex && score > bestLanguageScore)) {
            bestLanguageIndex = j;
            bestLanguageScore = score;
            preferredName = getFormatName(format);
          }
          break;
        }
      }
    }
    return preferredName;
  }

  private String getDefaultPreferred(TrackGroupArray trackGroups) {
    String preferredName = "";
    for (int i = 0; i < trackGroups.length; i++) {
      TrackGroup trackGroup = trackGroups.get(i);
      for (int j = 0; j < trackGroup.length; j++) {
        Format format = trackGroup.getFormat(j);
        if ((format.selectionFlags & C.SELECTION_FLAG_DEFAULT) != 0) {
          preferredName = getFormatName(format);
          break;
        }
      }
    }
    return preferredName;
  }

  private static int getMetadataEntrySize(Format format) {
    return (format == null || format.metadata == null ? 0 : format.metadata.length());
  }

  public static String getFormatName(Format format) {
    int size = getMetadataEntrySize(format);
    for (int i = 0; i < size; i++) {
      Metadata.Entry entry = format.metadata.get(i);
      if (!WebUtil.empty(entry.getTrackName())) {
        return entry.getTrackName();
      }
    }
    return "";
  }

  public static String getFormatGroupId(Format format) {
    int size = getMetadataEntrySize(format);
    for (int i = 0; i < size; i++) {
      Metadata.Entry entry = format.metadata.get(i);
      if (!WebUtil.empty(entry.getTrackGroupId())) {
        return entry.getTrackGroupId();
      }
    }
    return "";
  }

  public static String getAudioGroupId(Format format) {
    int size = getMetadataEntrySize(format);
    for (int i = 0; i < size; i++) {
      Metadata.Entry entry = format.metadata.get(i);
      if (!WebUtil.empty(entry.getAudioGroupId())) {
        return entry.getAudioGroupId();
      }
    }
    return "";
  }

  public static String getSubtitleGroupId(Format format) {
    int size = getMetadataEntrySize(format);
    for (int i = 0; i < size; i++) {
      Metadata.Entry entry = format.metadata.get(i);
      if (!WebUtil.empty(entry.getSubtitleGroupId())) {
        return entry.getSubtitleGroupId();
      }
    }
    return "";
  }

  public static String getCaptionGroupId(Format format) {
    int size = getMetadataEntrySize(format);
    for (int i = 0; i < size; i++) {
      Metadata.Entry entry = format.metadata.get(i);
      if (!WebUtil.empty(entry.getCaptionGroupId())) {
        return entry.getCaptionGroupId();
      }
    }
    return "";
  }
}
