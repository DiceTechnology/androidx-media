package androidx.media3.demo.main;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.reactivex.rxjava3.core.Observable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

public final class FetchUtil {

  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final MediaType MEDIA_TYPE_JSON = MediaType.get(CONTENT_TYPE_JSON);
  public static final long DEFAULT_HTTP_TIMEOUT_MS = 10_000;

  private static final OkHttpClient client = client();

  private FetchUtil() {
    // prevent instantiation.
  }

  public interface RequestBuilder {

    /**
     * Build the request data, e.g. url, headers, post body, body type.
     *
     * @return The request data.
     * @throws Throwable The error for unexpected params.
     */
    @NotNull
    RequestData build() throws Throwable;
  }

  public static final class RequestData {

    public final String url;
    public final Map<String, String> headers;
    public final String body;
    public final MediaType bodyType;

    private RequestData(String url, Map<String, String> headers, String body, MediaType bodyType) {
      this.url = url;
      this.headers = headers;
      this.body = body;
      this.bodyType = bodyType;
    }

    public static RequestData from(String url) {
      return from(url, null, null, null);
    }

    public static RequestData from(String url, Map<String, String> headers) {
      return from(url, headers, null, null);
    }

    public static RequestData from(String url, Map<String, String> headers, String body) {
      return from(url, headers, body, null);
    }

    public static RequestData from(String url, Map<String, String> headers, String body, MediaType bodyType) {
      return new RequestData(url, headers, body, bodyType == null ? MEDIA_TYPE_JSON : bodyType);
    }
  }

  public static OkHttpClient client() {
    return new OkHttpClient().newBuilder()
        .connectTimeout(DEFAULT_HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(DEFAULT_HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(DEFAULT_HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build();
  }

  public static Observable<String> fetch(String url) {
    return fetch(() -> RequestData.from(url));
  }

  public static Observable<String> fetch(RequestBuilder requestBuilder) {
    return Observable.create(emitter -> {
      final RequestData requestData;
      try {
        requestData = requestBuilder.build();
      } catch (Throwable error) {
        emitter.onError(error);
        return;
      }

      Request.Builder newRequestBuilder = new Request.Builder().url(requestData.url);
      if (requestData.headers != null) {
        for (Map.Entry<String, String> entry : requestData.headers.entrySet()) {
          newRequestBuilder.header(entry.getKey(), entry.getValue());
        }
      }
      if (requestData.body != null) {
        newRequestBuilder.post(RequestBody.create(requestData.body, requestData.bodyType));
      }

      client.newCall(newRequestBuilder.build()).enqueue(new okhttp3.Callback() {
        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
          emitter.onError(e);
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) {
          try (ResponseBody responseBody = response.body()) {
            if (!response.isSuccessful() || responseBody == null) {
              throw new IOException("Unexpected response: " + response);
            }
            emitter.onNext(responseBody.string());
            emitter.onComplete();
          } catch (Throwable error) {
            emitter.onError(error);
          }
        }
      });
    });
  }

  public static String generateJson(Object obj) {
    return new GsonBuilder().setPrettyPrinting().create().toJson(obj);
  }

  public static String prettyJson(String json) {
    return generateJson(JsonParser.parseString(json));
  }
}