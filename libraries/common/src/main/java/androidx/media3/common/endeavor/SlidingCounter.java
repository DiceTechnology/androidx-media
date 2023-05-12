package androidx.media3.common.endeavor;

import java.util.Date;

// Based on androidx.media3.exoplayer.upstream.SlidingPercentile, media3:lib-exoplayer
// Calculate any counter over a sliding window of times.
public class SlidingCounter {

    private static final long EXPIRED_DURATION_MS = 120_000;

    private final int capacity;
    private final long expiredMs;
    private final long[] timesMs;

    private int writePosition;
    private int readPosition;

    public SlidingCounter(int capacity) {
        this(capacity, EXPIRED_DURATION_MS);
    }

    public SlidingCounter(int capacity, long expiredMs) {
        this.capacity = capacity;
        this.expiredMs = expiredMs;
        this.timesMs = new long[capacity];
    }

    /**
     * Adds a new observation, also remove the oldest observation if it is expired.
     *
     * @param timeMs The time of the new observation.
     * @return The counter reaches the maximum capacity or not.
     */
    public boolean add(long timeMs) {
        long expired = timeMs - expiredMs;
        while (readPosition < writePosition && timesMs[readPosition] < expired) {
            readPosition++;
        }
        if (writePosition == capacity) {
            int count = Math.min(capacity - 1, writePosition - readPosition);
            System.arraycopy(timesMs, capacity - count, timesMs, 0, count);
            readPosition = 0;
            writePosition = count;
        }
        timesMs[writePosition++] = timeMs;
        return (writePosition - readPosition >= capacity);
    }

    public void reset() {
        writePosition = 0;
        readPosition = 0;
    }

    public String toText() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("length %d, position %d %d, capacity %d",
                writePosition - readPosition, readPosition, writePosition, capacity));
        if (writePosition > readPosition) {
            long lastMs = timesMs[writePosition - 1];
            builder.append(", lastMs ").append(WebUtil.df.format(new Date(lastMs)));
            for (int i = readPosition; i < writePosition; i++) {
                builder.append(String.format(", %.3f", (timesMs[i] - lastMs) / 1000f));
            }
        }
        return builder.toString();
    }
}
