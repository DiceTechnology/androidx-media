package androidx.media3.common.endeavor;

import android.os.SystemClock;
import androidx.media3.common.C;
import androidx.media3.common.util.Log;

public class TrackSwitcher {

  private final long keepDuration; // ms

  private int trackCount; // adaptive video track count
  private int lastIndex = C.INDEX_UNSET; // current track index
  private long nextTime; // ms

  public TrackSwitcher(long keepDuration) {
    this.keepDuration = keepDuration;
  }

  // Set current adaptive video track count.
  public void setTrackCount(int trackCount) {
    if (this.trackCount != trackCount) {
      Log.d(WebUtil.DEBUG, "Switcher set track count, " + this.trackCount + " -> " + trackCount);
      // Reset selected index.
      this.trackCount = trackCount;
      setIndex(C.INDEX_UNSET);
    }
  }

  // Random switch track index, for each selected track we will keep it for the specified duration.
  public int random(int indexFromBandwidth, int indexPlaying) {
    if (indexFromBandwidth > lastIndex || SystemClock.elapsedRealtime() > nextTime) {
      int count = 0;
      int[] arr = new int[trackCount];
      for (int i = 0; i < trackCount; i++) {
        if (i != lastIndex && i >= indexFromBandwidth && i != indexPlaying) {
          arr[count++] = i;
        }
      }
      int index = (count == 0 ? Math.min(trackCount - 1, (trackCount + 1) / 2) : arr[(int) (Math.random() * count)]);
      Log.d(WebUtil.DEBUG,
          String.format(
              "Switcher trackChanged!!!, %d -> %d, index [bandwidth %d, playing %d], count %d, total %d",
              lastIndex, index, indexFromBandwidth, indexPlaying, count, trackCount));
      setIndex(index);
    }
    return lastIndex;
  }

  public void setIndex(int index) {
    boolean valid = (index >= 0 && index < trackCount);
    lastIndex = (valid ? index : C.INDEX_UNSET);
    nextTime = (valid ? SystemClock.elapsedRealtime() + keepDuration : 0);
  }
}
