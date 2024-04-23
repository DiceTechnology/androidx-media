package androidx.media3.demo.main.upstream;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.demo.main.upstream.setting.Media;
import androidx.media3.demo.main.upstream.setting.Realm;
import com.diceplatform.doris.sdk.playback.PlaybackService;
import com.diceplatform.doris.sdk.playback.PlaybackServiceImpl;
import com.diceplatform.doris.sdk.playback.PlaybackServicePolicy;
import com.diceplatform.doris.sdk.playback.internal.HttpResponse.DrmInfo;
import com.diceplatform.doris.sdk.playback.internal.HttpResponse.LiveStreamInfo;
import com.diceplatform.doris.sdk.playback.internal.HttpResponse.PlaybackData;
import com.diceplatform.doris.sdk.playback.internal.HttpResponse.VodStreamInfo;
import io.reactivex.rxjava3.core.Observable;
import java.util.HashMap;

public class PlaybackProvider {

  private static class SingletonHolder {

    private static final PlaybackProvider instance = new PlaybackProvider();
  }

  public static PlaybackProvider getInstance() {
    return SingletonHolder.instance;
  }

  private final PlaybackService playbackService;

  private PlaybackProvider() {
    playbackService = new PlaybackServiceImpl();
  }

  // Asynchronous methods.

  public Observable<MediaItem> getStream(@NonNull Uri uri) {
    Media media = buildRequestMedia(uri);
    if (media == null) {
      return Observable.just(buildResponseMedia(null));
    }
    boolean useDash = "true".equals(uri.getQueryParameter("dash"));
    if (media.isLive()) {
      return playbackService.fetchRealmConfig(media.getRealm().getName(), media.getRealm().getEnv())
          .flatMap(realm -> playbackService.login(media.getRealm().getUsername(), media.getRealm().getPassword(), realm)
              .flatMap(credential -> playbackService.fetchLiveDetail(media.getId(), null, credential, realm)
                  .map(detail -> buildResponseMedia(detail == null ? null : getLiveStream(useDash, detail.stream)))));
    }
    return playbackService.fetchRealmConfig(media.getRealm().getName(), media.getRealm().getEnv())
        .flatMap(realm -> playbackService.login(media.getRealm().getUsername(), media.getRealm().getPassword(), realm)
            .flatMap(credential -> playbackService.fetchVodDetail(media.getId(), null, credential, realm)
                .map(detail -> buildResponseMedia(detail == null ? null : getVodStream(useDash, detail.stream)))));
  }

  // Synchronous methods.

  public MediaItem getStreamSync(@NonNull Uri uri) {
    return getStream(uri).blockingFirst();
  }

  // Internal methods.

  private Media buildRequestMedia(Uri uri) {
    if (uri == null || !Media.GAME_DOMAIN.equals(uri.getHost())) {
      return null;
    }
    Media.Builder builder = new Media.Builder();
    int id = Integer.parseInt(uri.getQueryParameter("id"));
    String realm = uri.getQueryParameter("realm");
    String usr = uri.getQueryParameter("usr");
    String pwd = uri.getQueryParameter("pwd");
    boolean live = "true".equals(uri.getQueryParameter("live"));
    if (id > 0 && !TextUtils.isEmpty(realm) && !TextUtils.isEmpty(usr) && !TextUtils.isEmpty(pwd)) {
      builder.setId(id).setLive(live).setRealm(new Realm(realm).setUsername(usr).setPassword(pwd));
    }
    return builder.build();
  }

  private MediaItem buildResponseMedia(PlaybackData result) {
    MediaItem.Builder builder = new MediaItem.Builder();
    if (!PlaybackData.isEmpty(result)) {
      builder.setUri(result.url);
      if (!DrmInfo.isEmpty(result.drm)) {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + result.drm.jwtToken);
        MediaItem.DrmConfiguration drmConfiguration = new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
            .setLicenseUri(result.drm.url)
            .setLicenseRequestHeaders(requestHeaders)
            .build();
        builder.setDrmConfiguration(drmConfiguration);
      }
    }
    return builder.build();
  }

  private PlaybackData getVodStream(boolean useDash, VodStreamInfo result) {
    VodStreamInfo vod = VodStreamInfo.isEmpty(result) ? null : new VodStreamInfo(
        useDash ? null : result.hls,
        useDash ? null : result.hlsWidevine,
        useDash ? result.dash : null,
        result.annotations, result.watermark, result.skipMarkers);
    Pair<String, PlaybackData> playback = PlaybackServicePolicy.getPreferredPlayback(vod);
    return playback == null ? null : playback.second;
  }

  private PlaybackData getLiveStream(boolean useDash, LiveStreamInfo result) {
    LiveStreamInfo live = LiveStreamInfo.isEmpty(result) ? null : new LiveStreamInfo(
        result.eventId,
        useDash ? null : result.hls,
        useDash ? null : result.hlsWidevine,
        useDash ? result.dash : null);
    Pair<String, PlaybackData> playback = PlaybackServicePolicy.getPreferredPlayback(live);
    return playback == null ? null : playback.second;
  }
}