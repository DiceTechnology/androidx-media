package androidx.media3.demo.main.upstream.entity;

import com.google.gson.annotations.SerializedName;

public class SubtitleData {

  @SerializedName("format")
  private String format;
  @SerializedName("language")
  private String language;
  @SerializedName("url")
  private String url;

  public String getFormat() {
    return format;
  }

  public String getLanguage() {
    return language;
  }

  public String getUrl() {
    return url;
  }
}