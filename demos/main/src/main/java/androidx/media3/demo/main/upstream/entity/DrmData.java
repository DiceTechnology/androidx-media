package androidx.media3.demo.main.upstream.entity;

import com.google.gson.annotations.SerializedName;

public class DrmData {

  @SerializedName("jwtToken")
  private String jwtToken;
  @SerializedName("url")
  private String url;

  public String getJwtToken() {
    return jwtToken;
  }

  public DrmData setJwtToken(String jwtToken) {
    this.jwtToken = jwtToken;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public DrmData setUrl(String url) {
    this.url = url;
    return this;
  }
}