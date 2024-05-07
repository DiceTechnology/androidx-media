package androidx.media3.demo.main.upstream.setting;

import android.text.TextUtils;

public class Realm {

  public static final Realm DCE_SANDBOX = new Realm("dce.sandbox");

  private final transient String name;
  private final transient String env;

  private String username = "";
  private String password = "";

  public Realm(String name) {
    this(name, null);
  }

  public Realm(String name, String env) {
    this.name = name;
    this.env = TextUtils.isEmpty(env) ? "production" : env;
  }

  public String getName() {
    return name;
  }

  public String getEnv() {
    return env;
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
}