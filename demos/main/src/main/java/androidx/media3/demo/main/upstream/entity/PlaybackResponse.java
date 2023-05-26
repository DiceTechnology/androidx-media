package androidx.media3.demo.main.upstream.entity;

import android.text.TextUtils;
import com.google.gson.annotations.SerializedName;

public class PlaybackResponse {

  @SerializedName("eventId")
  private int eventId;
  @SerializedName("videoId")
  private int videoId;
  @SerializedName("playerUrlCallback")
  private String playerUrlCallback;

  public int getVideoId() {
    return videoId;
  }

  public String getPlayerUrlCallback() {
    return playerUrlCallback;
  }

  public boolean isEmpty() {
    return TextUtils.isEmpty(playerUrlCallback);
  }
}