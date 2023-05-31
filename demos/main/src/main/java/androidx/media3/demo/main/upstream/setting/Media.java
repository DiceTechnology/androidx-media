package androidx.media3.demo.main.upstream.setting;

import androidx.annotation.NonNull;

public class Media {

  public static final String GAME_DOMAIN = "dummy.imggaming.com";

  public static final class Builder {

    private int id;
    private boolean isLive;
    @NonNull private Realm realm = Realm.DCE_SANDBOX;

    public Builder() {
    }

    private Builder(Media media) {
      id = media.getId();
      isLive = media.isLive();
      realm = media.getRealm();
    }

    public Builder setId(int id) {
      this.id = id;
      return this;
    }

    public Builder setLive(boolean live) {
      isLive = live;
      return this;
    }

    public Builder setRealm(@NonNull Realm realm) {
      this.realm = realm;
      return this;
    }

    public Media build() {
      return new Media(
          id,
          isLive,
          realm);
    }
  }

  private final int id;
  private final boolean isLive;
  @NonNull private final Realm realm;

  public Media(
      int id,
      boolean isLive,
      Realm realm) {
    this.id = id;
    this.isLive = isLive;
    this.realm = realm;
  }

  public Builder buildUpon() {
    return new Builder(this);
  }

  public int getId() {
    return id;
  }

  public boolean isLive() {
    return isLive;
  }

  public Realm getRealm() {
    return realm;
  }
}