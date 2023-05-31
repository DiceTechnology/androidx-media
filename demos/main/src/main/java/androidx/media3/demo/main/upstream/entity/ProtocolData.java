package androidx.media3.demo.main.upstream.entity;

import android.text.TextUtils;
import androidx.media3.demo.main.upstream.PlaybackResult;
import com.google.gson.annotations.SerializedName;

public class ProtocolData {

  @SerializedName("drm")
  private DrmData drm;
  @SerializedName("url")
  private String url;
  @SerializedName("subtitles")
  private SubtitleData[] subtitles;

  public DrmData getDrm() {
    return drm;
  }

  public ProtocolData setDrm(DrmData drm) {
    this.drm = drm;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public ProtocolData setUrl(String url) {
    this.url = url;
    return this;
  }

  public SubtitleData[] getSubtitles() {
    return subtitles;
  }

  public ProtocolData setSubtitles(SubtitleData[] subtitles) {
    this.subtitles = subtitles;
    return this;
  }

  public static ProtocolData buildFrom(boolean forceDash, PlaybackResult result) {
    StreamResponse stream = result == null ? null : result.getStream();
    if (stream == null) {
      return null;
    }
    ProtocolData dash = stream.getDash();
    ProtocolData hls = stream.getHlsWidevine() != null ? stream.getHlsWidevine() : stream.getHls();
    if (forceDash && dash != null) {
      return dash;
    }
    return hls != null ? hls : dash;
  }

  public static boolean isDrm(ProtocolData data) {
    return !isEmpty(data) && data.getDrm() != null;
  }

  public static boolean isEmpty(ProtocolData data) {
    return data == null || TextUtils.isEmpty(data.getUrl());
  }
}