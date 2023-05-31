package androidx.media3.demo.main.upstream.entity;

import com.google.gson.annotations.SerializedName;

public class StreamVodResponse {

  @SerializedName("hls")
  private ProtocolData[] hls;
  @SerializedName("hlsWidevine")
  private ProtocolData[] hlsWidevine;
  @SerializedName("dash")
  private ProtocolData[] dash;

  public StreamResponse normalize() {
    return new StreamResponse()
        .setHls(hls == null || hls.length < 1 ? null : hls[0])
        .setHlsWidevine(hlsWidevine == null || hlsWidevine.length < 1 ? null : hlsWidevine[0])
        .setDash(dash == null || dash.length < 1 ? null : dash[0]);
  }
}