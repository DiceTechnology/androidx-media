package androidx.media3.common.endeavor;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Util;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class LimitedSeekRange {

  private final long startTimeMs; // UTC timestamp, millisecond
  private final long endTimeMs; // UTC timestamp, millisecond
  private final boolean seekToStart;
  private final boolean useAsLive;

  private long timestampOffsetMs;
  private long factStartTimeMs;
  private boolean playbackEnded;

  private LimitedSeekRange(long startTimeMs, long endTimeMs, boolean seekToStart, boolean useAsLive) {
    this.startTimeMs = startTimeMs;
    this.endTimeMs = endTimeMs;
    this.seekToStart = seekToStart;
    this.useAsLive = useAsLive;
    timestampOffsetMs = C.TIME_UNSET;
    factStartTimeMs = startTimeMs;
  }

  public long getStartTimeMs() {
    return factStartTimeMs;
  }

  public boolean isSeekToStart() {
    return seekToStart;
  }

  public void onTimelineChanged(Timeline timeline) {
    if (timeline == null || timeline.isEmpty()) {
      return;
    }
    long windowStartTimeMs = timeline.getWindow(timeline.getLastWindowIndex(false),
        new Timeline.Window()).windowStartTimeMs;
    if (windowStartTimeMs != C.TIME_UNSET) {
      if (startTimeMs < windowStartTimeMs) {
        factStartTimeMs = windowStartTimeMs;
      }
      timestampOffsetMs = factStartTimeMs - windowStartTimeMs;
    }
  }

  public void setPlaybackEnded(boolean playbackEnded) {
    this.playbackEnded = playbackEnded;
  }

  private boolean isWaitReady() {
    return timestampOffsetMs == C.TIME_UNSET;
  }

  private long getDurationMs() {
    return useAsLive || isWaitReady() ? C.DCE_UNSET : Math.max(0, endTimeMs - factStartTimeMs);
  }

  public static boolean isUseLiveAsVod(LimitedSeekRange limitedSeekRange) {
    return limitedSeekRange != null && !limitedSeekRange.useAsLive;
  }

  public static boolean isValidTimeStamp(long timeMs) {
    return timeMs > C.DCE_TIME_AS_VOD;
  }

  private static Position toPosition(long adjustedPositionMs, LimitedSeekRange limitedSeekRange) {
    if (limitedSeekRange == null || adjustedPositionMs == C.TIME_UNSET) {
      return Position.legal(adjustedPositionMs);
    }
    if (adjustedPositionMs < 0) {
      return Position.illegal(0);
    }
    long durationMs = limitedSeekRange.getDurationMs();
    if (!limitedSeekRange.useAsLive && (durationMs == 0 || adjustedPositionMs > durationMs)) {
      return Position.illegal(durationMs);
    }
    return Position.legal(adjustedPositionMs);
  }

  private static boolean shouldUseOriginal(long timeMs, LimitedSeekRange limitedSeekRange) {
    return timeMs == C.TIME_UNSET || limitedSeekRange == null || limitedSeekRange.isWaitReady();
  }

  public static Position scaleSeekbarToTimelineMs(long seekbarMs, LimitedSeekRange limitedSeekRange) {
    if (shouldUseOriginal(seekbarMs, limitedSeekRange)) {
      return Position.legal(seekbarMs);
    }
    Position position = toPosition(seekbarMs, limitedSeekRange);
    return position.offset(limitedSeekRange.timestampOffsetMs);
  }

  public static Position scaleTimelineToSeekbarMs(long timelineMs, LimitedSeekRange limitedSeekRange) {
    if (shouldUseOriginal(timelineMs, limitedSeekRange)) {
      return Position.legal(timelineMs);
    }
    long adjustedPositionMs = timelineMs - limitedSeekRange.timestampOffsetMs;
    return toPosition(adjustedPositionMs, limitedSeekRange);
  }

  public static long scaleDurationToSeekbarMs(long durationMs, LimitedSeekRange limitedSeekRange) {
    if (shouldUseOriginal(durationMs, limitedSeekRange)) {
      return durationMs;
    }
    if (limitedSeekRange.useAsLive) {
      return Math.max(0, durationMs - limitedSeekRange.timestampOffsetMs);
    }
    return limitedSeekRange.getDurationMs();
  }

  public static long scaleTimestampToTimelineMs(long timeMs, LimitedSeekRange limitedSeekRange) {
    if (shouldUseOriginal(timeMs, limitedSeekRange)) {
      return timeMs;
    }
    return timeMs + limitedSeekRange.timestampOffsetMs;
  }

  public static long scaleTimestampToSeekbarMs(long timeMs, LimitedSeekRange limitedSeekRange) {
    if (shouldUseOriginal(timeMs, limitedSeekRange)) {
      return timeMs;
    }
    return timeMs - limitedSeekRange.timestampOffsetMs;
  }

  public static @Player.State int adjustPlaybackState(@Player.State int state, LimitedSeekRange limitedSeekRange) {
    if (limitedSeekRange != null && limitedSeekRange.playbackEnded) {
      return Player.STATE_ENDED;
    }
    return state;
  }

  // Generate limited seek range.
  public static LimitedSeekRange from(String startDate, String endDate, boolean seekToStart) {
    try {
      long startTimeMs = TextUtils.isEmpty(startDate) ? C.TIME_UNSET : Util.parseXsDateTime(startDate);
      long endTimeMs = TextUtils.isEmpty(endDate) ? C.TIME_UNSET : Util.parseXsDateTime(endDate);
      return from(startTimeMs, endTimeMs, seekToStart);
    } catch (Exception e) {
      return null;
    }
  }

  // Generate limited seek range.
  public static LimitedSeekRange from(String startDate, String endDate, boolean seekToStart, boolean useAsLive) {
    try {
      long startTimeMs = TextUtils.isEmpty(startDate) ? C.TIME_UNSET : Util.parseXsDateTime(startDate);
      long endTimeMs = TextUtils.isEmpty(endDate) ? C.TIME_UNSET : Util.parseXsDateTime(endDate);
      return from(startTimeMs, endTimeMs, seekToStart, useAsLive);
    } catch (Exception e) {
      return null;
    }
  }

  // Generate limited seek range.
  public static LimitedSeekRange from(long startTimeMs, long endTimeMs, boolean seekToStart) {
    long nowMs = System.currentTimeMillis();
    boolean useAsLive = !isValidTimeStamp(endTimeMs) || endTimeMs > nowMs;
    return from(startTimeMs, endTimeMs, seekToStart, useAsLive);
  }

  // Generate limited seek range.
  public static LimitedSeekRange from(long startTimeMs, long endTimeMs, boolean seekToStart, boolean useAsLive) {
    // Normalize the timestamp.
    boolean noStartTime = false;
    boolean noEndTime = false;
    if (!isValidTimeStamp(startTimeMs)) {
      startTimeMs = C.TIME_UNSET;
      noStartTime = true;
    }
    if (!isValidTimeStamp(endTimeMs)) {
      endTimeMs = C.TIME_UNSET;
      noEndTime = true;
    }

    // Valid the time values.
    if (noStartTime && noEndTime) {
      return null;
    }
    if (useAsLive && noStartTime) {
      return null;
    }
    if (!noEndTime && endTimeMs <= startTimeMs) {
      return null;
    }

    return new LimitedSeekRange(startTimeMs, endTimeMs, seekToStart, useAsLive);
  }

  // Generate limited seek range based on current time, just for testing
  public static LimitedSeekRange mock(int backHours, long durationMs, boolean seekToStart) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.add(Calendar.HOUR_OF_DAY, -backHours);
    long startTimeMs = calendar.getTimeInMillis();
    return from(startTimeMs, startTimeMs + durationMs, seekToStart);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof LimitedSeekRange)) {
      return false;
    }

    LimitedSeekRange other = (LimitedSeekRange) obj;
    return startTimeMs == other.startTimeMs
        && endTimeMs == other.endTimeMs
        && seekToStart == other.seekToStart
        && useAsLive == other.useAsLive
        && timestampOffsetMs == other.timestampOffsetMs
        && factStartTimeMs == other.factStartTimeMs
        && playbackEnded == other.playbackEnded;
  }

  @Override
  public int hashCode() {
    int result = (int) (startTimeMs ^ (startTimeMs >>> 32));
    result = 31 * result + (int) (endTimeMs ^ (endTimeMs >>> 32));
    result = 31 * result + (seekToStart ? 1 : 0);
    result = 31 * result + (useAsLive ? 1 : 0);
    result = 31 * result + (int) (timestampOffsetMs ^ (timestampOffsetMs >>> 32));
    result = 31 * result + (int) (factStartTimeMs ^ (factStartTimeMs >>> 32));
    result = 31 * result + (playbackEnded ? 1 : 0);
    return result;
  }

  @SuppressLint("DefaultLocale")
  @Override
  public String toString() {
    return String.format(
        "LimitedSeekRange{startTime='%tT', factStartTime='%tT', duration=%.1fhr, timestampOffset=%.1fhr, seekToStart=%b, useAsLive=%b, playbackEnded=%b}",
        new Date(startTimeMs),
        new Date(factStartTimeMs),
        useAsLive ? 24f : (endTimeMs - startTimeMs) / 3600_000f,
        timestampOffsetMs / 3600_000f,
        seekToStart,
        useAsLive,
        playbackEnded);
  }

  public static final class Position {

    private final boolean invalid;
    private final long positionMs;

    private Position(boolean invalid, long positionMs) {
      this.invalid = invalid;
      this.positionMs = positionMs;
    }

    public boolean isInvalid() {
      return invalid;
    }

    public long getPositionMs() {
      return positionMs;
    }

    public Position offset(long deltaMs) {
      return positionMs == C.TIME_UNSET ? this : new Position(invalid, positionMs + deltaMs);
    }

    public static Position legal(long positionMs) {
      return new Position(false, positionMs);
    }

    public static Position illegal(long positionMs) {
      return new Position(true, positionMs);
    }
  }
}
