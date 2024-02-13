package androidx.media3.common.text;

import androidx.annotation.NonNull;
import java.io.Serializable;
import java.util.List;

public class TextShadow implements Serializable {

  @NonNull
  private final List<Component> components;

  public TextShadow(@NonNull List<Component> components) {
    this.components = components;
  }

  public List<Component> getComponents() {
    return components;
  }

  public static class Component implements Serializable {

    public final int dx;
    public final int dy;
    public final int radius;
    public final int color;

    public Component(int dx, int dy, int radius, int color) {
      this.dx = dx;
      this.dy = dy;
      this.radius = radius;
      this.color = color;
    }
  }
}
