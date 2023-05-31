package androidx.media3.demo.main.upstream.entity;

import android.text.TextUtils;
import com.google.gson.annotations.SerializedName;

public class CredentialResponse {

  @SerializedName("authorisationToken")
  private String authorisationToken;
  @SerializedName("refreshToken")
  private String refreshToken;

  public String getAuthorisationToken() {
    return authorisationToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public boolean isEmpty() {
    return TextUtils.isEmpty(authorisationToken) || TextUtils.isEmpty(refreshToken);
  }
}