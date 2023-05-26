package androidx.media3.demo.main.upstream;

import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.demo.main.upstream.entity.ProtocolData;
import androidx.media3.demo.main.upstream.setting.Media;
import androidx.media3.demo.main.upstream.setting.Realm;
import io.reactivex.Observable;
import java.util.HashMap;

public class PlaybackProvider {

  private static class SingletonHolder {

    private static final PlaybackProvider instance = new PlaybackProvider();
  }

  public static PlaybackProvider getInstance() {
    return SingletonHolder.instance;
  }

  private PlaybackProvider() {
  }

  // Asynchronous methods.

  public Observable<MediaItem> getLiveStreamInfo(@NonNull Uri uri) {
    Media media = buildRequestMedia(uri);
    return media != null
        ? PlaybackAsyncService.getInstance().getCredentials(media)
        .flatMap(result -> PlaybackAsyncService.getInstance().getPlaybackUrl(media, result))
        .flatMap(result -> PlaybackAsyncService.getInstance().getStreamInfo(media, result))
        .map(result -> buildResponseMedia(uri, result))
        : Observable.just(new MediaItem.Builder().build());
  }

  // Synchronous methods.

  public MediaItem getLiveStreamInfoSync(@NonNull Uri uri) {
    Media media = buildRequestMedia(uri);
    if (media != null) {
      PlaybackResult result = PlaybackSyncService.getInstance().getCredentials(media);
      result = PlaybackSyncService.getInstance().getPlaybackUrl(media, result);
      return buildResponseMedia(uri, PlaybackSyncService.getInstance().getStreamInfo(media, result));
    }
    return new MediaItem.Builder().build();
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

  private MediaItem buildResponseMedia(Uri uri, PlaybackResult result) {
    boolean forceDash = "true".equals(uri.getQueryParameter("dash"));
    ProtocolData data = ProtocolData.buildFrom(forceDash, result);
    MediaItem.Builder builder = new MediaItem.Builder();
    if (!ProtocolData.isEmpty(data)) {
      builder.setUri(data.getUrl());
      if (ProtocolData.isDrm(data)) {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + data.getDrm().getJwtToken());
        MediaItem.DrmConfiguration drmConfiguration = new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
            .setLicenseUri(data.getDrm().getUrl())
            .setLicenseRequestHeaders(requestHeaders)
            .build();
        builder.setDrmConfiguration(drmConfiguration);
      }
    }
    return builder.build();
  }
}