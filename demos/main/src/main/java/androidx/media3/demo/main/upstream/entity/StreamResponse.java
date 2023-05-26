package androidx.media3.demo.main.upstream.entity;

import com.google.gson.annotations.SerializedName;

public class StreamResponse {

  @SerializedName("hls")
  private ProtocolData hls;
  @SerializedName("hlsWidevine")
  private ProtocolData hlsWidevine;
  @SerializedName("dash")
  private ProtocolData dash;

  private transient String body;

  public ProtocolData getHls() {
    return hls;
  }

  public StreamResponse setHls(ProtocolData hls) {
    this.hls = hls;
    return this;
  }

  public ProtocolData getHlsWidevine() {
    return hlsWidevine;
  }

  public StreamResponse setHlsWidevine(ProtocolData hlsWidevine) {
    this.hlsWidevine = hlsWidevine;
    return this;
  }

  public ProtocolData getDash() {
    return dash;
  }

  public StreamResponse setDash(ProtocolData dash) {
    this.dash = dash;
    return this;
  }

  public String getBody() {
    return body;
  }

  public StreamResponse setBody(String body) {
    this.body = body;
    return this;
  }
}