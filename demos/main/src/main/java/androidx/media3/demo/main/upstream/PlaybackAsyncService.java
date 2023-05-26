package androidx.media3.demo.main.upstream;

import androidx.annotation.NonNull;
import androidx.media3.demo.main.upstream.entity.StreamResponse;
import androidx.media3.demo.main.upstream.setting.Media;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class PlaybackAsyncService {

  private static class SingletonHolder {

    private static final PlaybackAsyncService instance = new PlaybackAsyncService();
  }

  public static PlaybackAsyncService getInstance() {
    return SingletonHolder.instance;
  }

  private final OkHttpClient client;
  private final PlaybackAsyncAPI playbackService;

  private PlaybackAsyncService() {
    client = OkHttpClientFactory.create();
    playbackService = createPlaybackEndpoint();
  }

  private PlaybackAsyncAPI createPlaybackEndpoint() {
    return new Retrofit.Builder()
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://www.example.com/")
        .client(client)
        .build()
        .create(PlaybackAsyncAPI.class);
  }

  public Observable<PlaybackResult> getStreamInfo(@NonNull Media media, @NonNull PlaybackResult result) {
    if (result.getCredential() == null || result.getPlayback() == null) {
      return Observable.just(result);
    }
    final Observable<StreamResponse> observable;
    if (media.isLive()) {
      observable = playbackService.getStreamInfo(
          result.getPlayback().getPlayerUrlCallback(),
          result.getCredential().getAuthorisationToken(),
          result.getCredential().getRefreshToken());
    } else {
      observable = playbackService.getVodStreamInfo(
              result.getPlayback().getPlayerUrlCallback(),
              result.getCredential().getAuthorisationToken(),
              result.getCredential().getRefreshToken())
          .map(stream -> stream == null ? null : stream.normalize());
    }
    return observable
        .subscribeOn(Schedulers.io())
        .doOnError(result::setError)
        .map(result::setStream);
  }

  public Observable<PlaybackResult> getPlaybackUrl(@NonNull Media media, @NonNull PlaybackResult result) {
    if (result.getCredential() == null) {
      return Observable.just(result);
    }
    String url = media.getRealm().getPlayback(media.isLive(), media.getId());
    String authorizationToken = "Bearer " + result.getCredential().getAuthorisationToken();
    return playbackService.getPlaybackUrl(url, media.getRealm().getName(), authorizationToken)
        .subscribeOn(Schedulers.io())
        .doOnError(result::setError)
        .map(result::setPlayback);
  }

  public Observable<PlaybackResult> getCredentials(@NonNull Media media) {
    PlaybackResult result = new PlaybackResult();
    return playbackService.getCredentials(media.getRealm().getLogin(), media.getRealm().getName(), media.getRealm())
        .subscribeOn(Schedulers.io())
        .doOnError(result::setError)
        .map(result::setCredential);
  }
}