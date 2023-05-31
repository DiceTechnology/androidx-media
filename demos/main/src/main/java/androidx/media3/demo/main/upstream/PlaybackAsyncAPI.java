package androidx.media3.demo.main.upstream;

import androidx.media3.demo.main.upstream.entity.CredentialResponse;
import androidx.media3.demo.main.upstream.entity.PlaybackResponse;
import androidx.media3.demo.main.upstream.entity.StreamResponse;
import androidx.media3.demo.main.upstream.entity.StreamVodResponse;
import androidx.media3.demo.main.upstream.setting.Realm;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface PlaybackAsyncAPI {

  @Headers({
      "app: dice",
      "content-type: application/json",
      "x-api-key: 857a1e5d-e35e-4fdf-805b-a87b6f8364bf",
      "x-app-var: 5.10.3"
  })
  @POST
  Observable<CredentialResponse> getCredentials(
      @Url String url,
      @Header("realm") String realm,
      @Body Realm body
  );

  @Headers({
      "app: dice",
      "content-type: application/json",
      "x-api-key: 857a1e5d-e35e-4fdf-805b-a87b6f8364bf",
      "x-app-var: 5.10.3"
  })
  @GET
  Observable<PlaybackResponse> getPlaybackUrl(
      @Url String url,
      @Header("realm") String realm,
      @Header("authorization") String authorizationToken
  );

  @GET
  Observable<StreamResponse> getStreamInfo(
      @Url String url,
      @Header("x-auth-token") String authorizationToken,
      @Header("x-refresh-token") String refreshToken
  );

  @GET
  Observable<StreamVodResponse> getVodStreamInfo(
      @Url String url,
      @Header("x-auth-token") String authorizationToken,
      @Header("x-refresh-token") String refreshToken
  );
}