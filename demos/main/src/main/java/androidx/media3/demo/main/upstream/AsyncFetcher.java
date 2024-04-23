package androidx.media3.demo.main.upstream;

import android.text.TextUtils;
import com.diceplatform.doris.sdk.playback.internal.HttpService;
import io.reactivex.rxjava3.core.Observable;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

public final class AsyncFetcher<T> {

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

  public interface ResponseParser<T> {

    /**
     * Parse response to specified type instance.
     *
     * @param reader The reader from response body.
     * @return The specified type instance or null/error for unexpected response.
     * @throws Throwable The parse error.
     */
    T parse(@NotNull Reader reader) throws Throwable;
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

    public static RequestData from(String url, Map<String, String> headers) {
      return from(url, headers, null, null);
    }

    public static RequestData from(String url, Map<String, String> headers, String body) {
      return from(url, headers, body, null);
    }

    public static RequestData from(String url, Map<String, String> headers, String body,
        MediaType bodyType) {
      if (TextUtils.isEmpty(body)) {
        return new RequestData(url, headers, null, null);
      }
      return new RequestData(url, headers, body,
          bodyType == null ? HttpService.MEDIA_TYPE_JSON : bodyType);
    }
  }

  public Observable<T> newCall(
      RequestBuilder requestBuilder,
      ResponseParser<T> responseParser,
      OkHttpClient client) {
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
            T result = responseParser.parse(responseBody.charStream());
            emitter.onNext(result);
            emitter.onComplete();
          } catch (Throwable error) {
            emitter.onError(error);
          }
        }
      });
    });
  }
}