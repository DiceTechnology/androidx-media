package androidx.media3.demo.main.upstream;

import androidx.annotation.NonNull;
import androidx.media3.demo.main.upstream.entity.CredentialResponse;
import androidx.media3.demo.main.upstream.entity.PlaybackResponse;
import androidx.media3.demo.main.upstream.entity.StreamResponse;
import androidx.media3.demo.main.upstream.entity.StreamVodResponse;
import androidx.media3.demo.main.upstream.setting.Media;
import androidx.media3.demo.main.upstream.setting.Realm;
import com.google.gson.Gson;
import java.io.StringReader;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PlaybackSyncService {

  private static class SingletonHolder {

    private static final PlaybackSyncService instance = new PlaybackSyncService();
  }

  public static PlaybackSyncService getInstance() {
    return SingletonHolder.instance;
  }

  private final OkHttpClient client;

  private PlaybackSyncService() {
    client = OkHttpClientFactory.create();
  }

  public PlaybackResult getStreamInfo(@NonNull Media media, @NonNull PlaybackResult result) {
    if (result.getCredential() == null || result.getPlayback() == null) {
      return result;
    }

    Request.Builder builder = new Request.Builder()
        .url(result.getPlayback().getPlayerUrlCallback())
        .method("GET", null)
        .addHeader("x-auth-token", result.getCredential().getAuthorisationToken())
        .addHeader("x-refresh-token", result.getCredential().getRefreshToken());
    Request request = builder.build();
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        ResponseBody responseBody = response.body();
        String body = (responseBody == null ? "" : responseBody.string());
        return media.isLive()
            ? result.setStream(new Gson().fromJson(new StringReader(body), StreamResponse.class).setBody(body))
            : result.setStream(new Gson().fromJson(new StringReader(body), StreamVodResponse.class).normalize().setBody(body));
      } else {
        result.setFailCode(response.code());
      }
    } catch (Exception e) {
      result.setError(e);
    }
    return result;
  }

  public PlaybackResult getPlaybackUrl(@NonNull Media media, @NonNull PlaybackResult result) {
    if (result.getCredential() == null) {
      return result;
    }
    Request.Builder builder = new Request.Builder()
        .url(media.getRealm().getPlayback(media.isLive(), media.getId()))
        .method("GET", null)
        .addHeader("authorization", "Bearer " + result.getCredential().getAuthorisationToken());
    Request request = addDefaultHeaders(media.getRealm(), builder).build();
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        result.setPlayback(new Gson().fromJson(response.body().charStream(), PlaybackResponse.class));
      } else {
        result.setFailCode(response.code());
      }
    } catch (Exception e) {
      result.setError(e);
    }
    return result;
  }

  public PlaybackResult getCredentials(@NonNull Media media) {
    PlaybackResult result = new PlaybackResult();
    MediaType mediaType = MediaType.parse("application/json");
    String json = new Gson().toJson(media.getRealm(), Realm.class);
    Request.Builder builder = new Request.Builder()
        .url(media.getRealm().getLogin())
        .method("POST", RequestBody.create(json, mediaType));
    Request request = addDefaultHeaders(media.getRealm(), builder).build();
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        result.setCredential(new Gson().fromJson(response.body().charStream(), CredentialResponse.class));
      } else {
        result.setFailCode(response.code());
      }
    } catch (Exception e) {
      result.setError(e);
    }
    return result;
  }

  // Internal methods.

  private Request.Builder addDefaultHeaders(Realm realm, Request.Builder builder) {
    return builder
        .addHeader("app", "dice")
        .addHeader("content-type", "application/json")
        .addHeader("realm", realm.getName())
        .addHeader("x-api-key", "857a1e5d-e35e-4fdf-805b-a87b6f8364bf")
        .addHeader("x-app-var", "5.10.3");
  }
}