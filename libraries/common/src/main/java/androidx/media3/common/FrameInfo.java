/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.common;

import static androidx.media3.common.util.Assertions.checkArgument;

import androidx.media3.common.util.UnstableApi;

/** Value class specifying information about a decoded video frame. */
@UnstableApi
public class FrameInfo {
  /** The width of the frame, in pixels. */
  public final int width;
  /** The height of the frame, in pixels. */
  public final int height;
  /** The ratio of width over height for each pixel. */
  public final float pixelWidthHeightRatio;
  /**
   * An offset in microseconds that is part of the input timestamps and should be ignored for
   * processing but added back to the output timestamps.
   *
   * <p>The offset stays constant within a stream but changes in between streams to ensure that
   * frame timestamps are always monotonically increasing.
   */
  public final long streamOffsetUs;

  // TODO(b/227624622): Add color space information for HDR.

  /**
   * Creates a new instance.
   *
   * @param width The width of the frame, in pixels.
   * @param height The height of the frame, in pixels.
   * @param pixelWidthHeightRatio The ratio of width over height for each pixel.
   * @param streamOffsetUs An offset in microseconds that is part of the input timestamps and should
   *     be ignored for processing but added back to the output timestamps.
   */
  public FrameInfo(int width, int height, float pixelWidthHeightRatio, long streamOffsetUs) {
    checkArgument(width > 0, "width must be positive, but is: " + width);
    checkArgument(height > 0, "height must be positive, but is: " + height);

    this.width = width;
    this.height = height;
    this.pixelWidthHeightRatio = pixelWidthHeightRatio;
    this.streamOffsetUs = streamOffsetUs;
  }
}
