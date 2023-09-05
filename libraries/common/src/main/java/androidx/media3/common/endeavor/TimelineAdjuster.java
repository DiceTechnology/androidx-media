package androidx.media3.common.endeavor;

// We use it to adjust seek bar position with exclude yospace ads.
public interface TimelineAdjuster {

  /**
   * Scale the position value of current timeline into seek bar.
   * For Yospace SSAI stream we should exclude ads and only show content on seekbar.
   *
   * @param timelineMs The position value of current timeline.
   * @return The position value of current seek bar.
   */
  default long scaleTimelineToSeekbarMs(long timelineMs) {
    return timelineMs;
  }

  /**
   * Scale the position value of current seek bar into timeline.
   * For Yospace SSAI stream we should exclude ads and only show content on seekbar.
   *
   * @param seekbarMs The position value of current seek bar.
   * @return The position value of current timeline.
   */
  default long scaleSeekbarToTimelineMs(long seekbarMs) {
    return seekbarMs;
  }
}
