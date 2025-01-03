package androidx.media3.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

public class PaddingLineBackgroundSpan implements LineBackgroundSpan {

  private final int lineBackgroundColor;
  private final int horizontalPadding;
  private final float left;
  private final float top;
  private final float right;
  private final float bottom;
  private final float measureScale;
  @Nullable
  private final BackgroundSpanInfo[] spanInfos;

  public PaddingLineBackgroundSpan(
      @ColorInt int lineBackgroundColor,
      int horizontalPadding,
      float left,
      float top,
      float right,
      float bottom,
      float measureScale,
      @Nullable BackgroundSpanInfo[] spanInfos) {
    this.lineBackgroundColor = lineBackgroundColor;
    this.horizontalPadding = horizontalPadding;
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
    this.measureScale = measureScale;
    this.spanInfos = spanInfos;
  }

  @Override
  public void drawBackground(
      Canvas canvas,
      Paint paint,
      @Px int left,
      @Px int right,
      @Px int top,
      @Px int baseline,
      @Px int bottom,
      CharSequence text,
      int start,
      int end,
      int lineNumber) {
    drawBackgroundWithPadding(canvas, paint, text, start, end);
  }

  private void drawBackgroundWithPadding(
      Canvas canvas,
      Paint paint,
      CharSequence text,
      int start,
      int end) {
    final int originColor = paint.getColor();
    if (spanInfos != null && spanInfos.length > 0) {
      float left = this.left;
      float right;
      int textPos = start;
      for (int index = 0; index < spanInfos.length; index++) {
        BackgroundSpanInfo info = spanInfos[index];
        if (info.start > end) {
          continue;
        }
        // draw line background
        if (info.start > textPos && info.start < end) {
          paint.setColor(lineBackgroundColor);
          // draw left padding
          if (index == 0) {
            canvas.drawRect(this.left - horizontalPadding, top, this.left, bottom, paint);
          }

          float textWidth = measureText(paint, text, textPos, info.start);
          textPos = info.start;
          right = left + textWidth;
          canvas.drawRect(left, top, right, bottom, paint);
          left = right;
        }
        // draw span background
        if (info.end <= end) {
          paint.setColor(info.color);
          // draw left padding
          if (info.start == start) {
            canvas.drawRect(this.left - horizontalPadding, top, this.left, bottom, paint);
          }

          float textWidth = measureText(paint, text, textPos, info.end);
          textPos = info.end;
          right = left + textWidth;
          canvas.drawRect(left, top, right, bottom, paint);
          left = right;
        }

        BackgroundSpanInfo nextInfo = index < spanInfos.length - 1 ? spanInfos[index + 1] : null;
        if (nextInfo == null) {
          // draw line background and right padding
          paint.setColor(info.end == end ? info.color : lineBackgroundColor);
          right = this.right;
          canvas.drawRect(left, top, right + horizontalPadding, bottom, paint);
          left = right;
        }
      }
    } else {
      paint.setColor(lineBackgroundColor);
      canvas.drawRect(left - horizontalPadding, top, right + horizontalPadding, bottom, paint);
    }
    paint.setColor(originColor);
  }

  private float measureText(Paint paint, CharSequence text, int start, int end) {
    if (start < 0 || end < start || end > text.length()) {
      return 0f;
    }
    return paint.measureText(text, start, end) * measureScale;
  }

  public static class BackgroundSpanInfo implements Comparable<BackgroundSpanInfo> {

    public final int start;
    public final int end;
    public final int color;

    public BackgroundSpanInfo(int start, int end, int color) {
      this.start = start;
      this.end = end;
      this.color = color;
    }

    @Override
    public int compareTo(BackgroundSpanInfo info) {
      return start - info.start;
    }
  }
}
