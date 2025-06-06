/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.media3.extractor.text.webvtt;

import androidx.media3.common.C;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.extractor.text.Subtitle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A representation of a WebVTT subtitle. */
/* package */ final class WebvttSubtitle implements Subtitle {

  private final List<WebvttCueInfo> cueInfos;
  private final long[] cueTimesUs;
  private final long[] sortedCueTimesUs;

  /** Constructs a new WebvttSubtitle from a list of {@link WebvttCueInfo}s. */
  public WebvttSubtitle(List<WebvttCueInfo> cueInfos) {
    this.cueInfos = Collections.unmodifiableList(new ArrayList<>(cueInfos));
    cueTimesUs = new long[2 * cueInfos.size()];
    for (int cueIndex = 0; cueIndex < cueInfos.size(); cueIndex++) {
      WebvttCueInfo cueInfo = cueInfos.get(cueIndex);
      int arrayIndex = cueIndex * 2;
      cueTimesUs[arrayIndex] = cueInfo.startTimeUs;
      cueTimesUs[arrayIndex + 1] = cueInfo.endTimeUs;
    }
    sortedCueTimesUs = Arrays.copyOf(cueTimesUs, cueTimesUs.length);
    Arrays.sort(sortedCueTimesUs);
  }

  @Override
  public int getNextEventTimeIndex(long timeUs) {
    int index = Util.binarySearchCeil(sortedCueTimesUs, timeUs, false, false);
    return index < sortedCueTimesUs.length ? index : C.INDEX_UNSET;
  }

  @Override
  public int getEventTimeCount() {
    return sortedCueTimesUs.length;
  }

  @Override
  public long getEventTime(int index) {
    Assertions.checkArgument(index >= 0);
    Assertions.checkArgument(index < sortedCueTimesUs.length);
    return sortedCueTimesUs[index];
  }

  @Override
  public List<Cue> getCues(long timeUs) {
    List<Cue> currentCues = new ArrayList<>();
    List<WebvttCueInfo> cuesWithUnsetLine = new ArrayList<>();
    for (int i = 0; i < cueInfos.size(); i++) {
      if ((cueTimesUs[i * 2] <= timeUs) && (timeUs < cueTimesUs[i * 2 + 1])) {
        WebvttCueInfo cueInfo = cueInfos.get(i);
        if (cueInfo.cue.line == Cue.DIMEN_UNSET) {
          cuesWithUnsetLine.add(cueInfo);
        } else {
          currentCues.add(cueInfo.cue);
        }
      }
    }
    // Steps 4 - 10 of https://www.w3.org/TR/webvtt1/#cue-computed-line
    // (steps 1 - 3 are handled by WebvttCueParser#computeLine(float, int))
    Collections.sort(cuesWithUnsetLine, (c1, c2) -> Long.compare(c1.startTimeUs, c2.startTimeUs));
    for (int i = 0; i < cuesWithUnsetLine.size(); i++) {
      Cue cue = cuesWithUnsetLine.get(i).cue;
      /* [DORIS-2466] Insert cues with unset lines to the next available lines in the "currentCues"
      starting from line number -2 to avoid cropped cues on the bottom. */
      currentCues.add(
          cue.buildUpon()
              .setLine(getNextAvailableLine(currentCues), Cue.LINE_TYPE_NUMBER)
              .build()
      );
    }
    return currentCues;
  }

  private float getNextAvailableLine(List<Cue> currentCues) {
    /* Starting from -2 to avoid cropped cues on the bottom. */
    for (int i = -2; i >= -100; i--) {
      if (isLineAvailable(currentCues, i)) {
        return i;
      }
    }
    return -2;
  }

  private boolean isLineAvailable(List<Cue> currentCues, float line) {
    for (Cue currentCue : currentCues) {
      if (currentCue.lineType == Cue.LINE_TYPE_NUMBER && currentCue.line == line) {
        return false;
      }
    }
    return true;
  }
}
