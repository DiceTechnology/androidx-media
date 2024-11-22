package androidx.media3.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;
import androidx.annotation.ColorInt;
import androidx.annotation.Px;

public class PaddingLineBackgroundSpan implements LineBackgroundSpan {

  private final int color;
  private final int horizontalPadding;
  private final float left;
  private final float top;
  private final float right;
  private final float bottom;
  private final float measureScale;
  private final PaddingSpanInfo[] spanInfos;

  public PaddingLineBackgroundSpan(
      @ColorInt int color,
      int horizontalPadding,
      float left,
      float top,
      float right,
      float bottom,
      float measureScale,
      PaddingSpanInfo[] spanInfos) {
    this.color = color;
    this.horizontalPadding = horizontalPadding;
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
    this.measureScale = measureScale;
    this.spanInfos = spanInfos;
  }

  @Override
  public void drawBackground(Canvas canvas, Paint paint, @Px int left, @Px int right, @Px int top,
      @Px int baseline, @Px int bottom, CharSequence text, int start, int end, int lineNumber) {
    drawBackgroundWithPadding(canvas, paint, text, start, end);
  }

  private void drawBackgroundWithPadding(
      Canvas canvas,
      Paint paint,
      CharSequence text,
      int start,
      int end) {
    final int originColor = paint.getColor();
    if (spanInfos.length > 0) {
      float left = this.left;
      int textPos = start;
      for (int index = 0; index < spanInfos.length; index++) {
        PaddingSpanInfo info = spanInfos[index];
        PaddingSpanInfo nextInfo = index < spanInfos.length - 1 ? spanInfos[index + 1] : null;
        if (info.start > text.length()) {
          continue;
        }
        // draw background
        paint.setColor(color);
        float textWidth = measureText(paint, text, textPos, info.start);
        canvas.drawRect(left - horizontalPadding, top, left + textWidth, bottom, paint);

        left = left + textWidth;
        textPos = info.start;
        // draw span
        paint.setColor(info.color);
        textWidth = measureText(paint, text, textPos, info.end);
        canvas.drawRect(left, top, left + textWidth, bottom, paint);

        left = left + textWidth;
        textPos = info.end;

        if (nextInfo == null) {
          // draw background
          paint.setColor(color);
          textWidth = measureText(paint, text, textPos, end);
          canvas.drawRect(left, top, left + textWidth + horizontalPadding, bottom, paint);
        }
      }
    } else {
      paint.setColor(color);
      canvas.drawRect(left - horizontalPadding, top, right + horizontalPadding, bottom, paint);
    }
    paint.setColor(originColor);
  }

  private float measureText(Paint paint, CharSequence text, int start, int end) {
    if (end < start) {
      return 0f;
    }
    return paint.measureText(text, start, end) * measureScale;
  }

  public static class PaddingSpanInfo implements Comparable<PaddingSpanInfo> {

    public final int start;
    public final int end;
    public final int color;

    public PaddingSpanInfo(int start, int end, int color) {
      this.start = start;
      this.end = end;
      this.color = color;
    }

    @Override
    public int compareTo(PaddingSpanInfo info) {
      return start - info.start;
    }
  }
}
