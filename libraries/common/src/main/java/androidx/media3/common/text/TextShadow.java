package androidx.media3.common.text;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.media3.common.util.Util;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TextShadow implements Serializable {

  private static final String FIELD_COMPONENTS = Util.intToStringMaxRadix(0);

  @NonNull
  private final List<Component> components;

  public TextShadow(@NonNull List<Component> components) {
    this.components = components;
  }

  public List<Component> getComponents() {
    return components;
  }

  public Bundle toBundle() {
    Bundle bundle = new Bundle();
    ArrayList<Bundle> componentBundleList = new ArrayList<>();
    for (Component component : components) {
      componentBundleList.add(component.toBundle());
    }
    bundle.putParcelableArrayList(FIELD_COMPONENTS, componentBundleList);
    return bundle;
  }

  public static TextShadow fromBundle(Bundle bundle) {
    @NonNull ArrayList<Bundle> componentBundleList = bundle.getParcelableArrayList(FIELD_COMPONENTS);
    @NonNull List<Component> components = new ArrayList<>(componentBundleList.size());;
    for (int i = 0; i < componentBundleList.size(); i++) {
      components.add(Component.fromBundle(componentBundleList.get(i)));
    }
    return new TextShadow(components);
  }

  public static class Component implements Serializable {

    private static final String FIELD_DX = Util.intToStringMaxRadix(0);
    private static final String FIELD_DY = Util.intToStringMaxRadix(1);
    private static final String FIELD_RADIUS = Util.intToStringMaxRadix(2);
    private static final String FIELD_COLOR = Util.intToStringMaxRadix(3);

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

    public Bundle toBundle() {
      Bundle bundle = new Bundle();
      bundle.putInt(FIELD_DX, dx);
      bundle.putInt(FIELD_DY, dy);
      bundle.putInt(FIELD_RADIUS, radius);
      bundle.putInt(FIELD_COLOR, color);
      return bundle;
    }

    public static Component fromBundle(Bundle bundle) {
      return new Component(
          bundle.getInt(FIELD_DX),
          bundle.getInt(FIELD_DY),
          bundle.getInt(FIELD_RADIUS),
          bundle.getInt(FIELD_COLOR));
    }
  }
}
