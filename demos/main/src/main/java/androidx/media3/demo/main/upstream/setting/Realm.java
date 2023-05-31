package androidx.media3.demo.main.upstream.setting;

import com.google.gson.annotations.SerializedName;

public class Realm {

  public static final Realm DCE_SANDBOX = new Realm("dce.sandbox");

  private final transient String name;

  @SerializedName("id")
  private String username = "";
  @SerializedName("secret")
  private String password = "";
  private transient String login = "https://dce-frontoffice.imggaming.com/api/v2/login";
  private transient String live = "https://dce-frontoffice.imggaming.com/api/v2/stream?displayGeoblockedLive=false&propertyId=0&sportId=0&tournamentId=0&eventId=%ID%";
  private transient String vod = "https://dce-frontoffice.imggaming.com/api/v3/stream/vod/%ID%";

  public Realm(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getUsername() {
    return username;
  }

  public Realm setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public Realm setPassword(String password) {
    this.password = password;
    return this;
  }

  public String getLogin() {
    return login;
  }

  public Realm setLogin(String login) {
    this.login = login;
    return this;
  }

  public String getPlayback(boolean isLive, int id) {
    return (isLive ? live : vod).replaceAll("%ID%", String.valueOf(id));
  }

  public Realm setLive(String live) {
    this.live = live;
    return this;
  }

  public Realm setVod(String vod) {
    this.vod = vod;
    return this;
  }
}