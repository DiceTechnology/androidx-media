package androidx.media3.common.text;

import android.os.Bundle;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import androidx.annotation.Nullable;
import androidx.media3.common.util.Util;

/**
 * A span that contains the text shadow but does not modifies the text as we can't apply multiple
 * shadows with span, instead the {@link TextShadow} will be applied in the {@link SubtitlePainter}.
 */
public class TextShadowSpan extends CharacterStyle {

  private static final String FIELD_TEXT_SHADOW = Util.intToStringMaxRadix(0);

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

  public Bundle toBundle() {
    Bundle bundle = new Bundle();
    if (textShadow != null) {
      bundle.putBundle(FIELD_TEXT_SHADOW, textShadow.toBundle());
    }
    return bundle;
  }

  public static TextShadowSpan fromBundle(Bundle bundle) {
    @Nullable Bundle textShadowBundle = bundle.getBundle(FIELD_TEXT_SHADOW);
    @Nullable TextShadow textShadow = textShadowBundle == null ? null : TextShadow.fromBundle(textShadowBundle);
    return new TextShadowSpan(textShadow);
  }
}
