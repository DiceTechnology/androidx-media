package androidx.media3.common.text;

import android.text.TextPaint;
import android.text.style.CharacterStyle;
import androidx.annotation.Nullable;

/**
 * A span that contains the text shadow but does not modifies the text as we can't apply multiple
 * shadows with span, instead the {@link TextShadow} will be applied in the {@link SubtitlePainter}.
 */
public class TextShadowSpan extends CharacterStyle {

  @Nullable
  private TextShadow textShadow;

  public TextShadowSpan(@Nullable TextShadow textShadow) {
    super();
    this.textShadow = textShadow;
  }

  @Nullable
  public TextShadow getTextShadow() {
    return textShadow;
  }

  @Override
  public void updateDrawState(TextPaint tp) {
    // Do nothing.
  }
}
