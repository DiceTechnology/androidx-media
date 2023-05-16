package androidx.media3.exoplayer.endeavor;

import android.util.Pair;
import java.util.Arrays;

public class LiveEdgeManger {

  private final int length = 16;
  private final long limitUs = 300_000;
  private final long[] data;

  private boolean inited;
  private int pos;
  private long applyOffsetUs;
  private long totalOffsetUs;

  public LiveEdgeManger() {
    data = new long[length];
    Arrays.fill(data, Long.MIN_VALUE);
    applyOffsetUs = Long.MIN_VALUE;
  }

  /**
   * Receive the offset date between local clock time and playlist end time, calculate the average offset,
   * and then judge whether to use the new offset data through the threshold value.
   *
   * @param offsetUs The offset data between local clock time and playlist end time.
   * @return The pair of {changed or not, offset data}
   */
  public Pair<Boolean, Long> report(long offsetUs) {
    if (!inited) {
      inited = (data[pos] != Long.MIN_VALUE);
      if (applyOffsetUs == Long.MIN_VALUE) {
        applyOffsetUs = offsetUs;
      }
    }

    totalOffsetUs += offsetUs - (inited ? data[pos] : 0);
    data[pos++] = offsetUs;
    if (pos >= length) {
      pos = 0;
    }

    // Before inited we use the first offset as average offset to avoid frequent adjustments.
    long avgUs = (inited ? totalOffsetUs / length : applyOffsetUs);
    if (Math.abs(applyOffsetUs - avgUs) < limitUs) {
      return new Pair<>(false, applyOffsetUs);
    }
    applyOffsetUs = avgUs;
    return new Pair<>(true, applyOffsetUs);
  }
}
