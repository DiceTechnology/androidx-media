package androidx.media3.demo.main.upstream;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class OkHttpClientFactory {

  private OkHttpClientFactory() {
  }

  public static OkHttpClient create() {
    long timeoutMs = 6_000;
    return new OkHttpClient().newBuilder()
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build();
  }
}