package androidx.media3.demo.main.upstream;

import androidx.media3.demo.main.upstream.entity.CredentialResponse;
import androidx.media3.demo.main.upstream.entity.PlaybackResponse;
import androidx.media3.demo.main.upstream.entity.StreamResponse;
import java.io.IOException;

public class PlaybackResult {

  private CredentialResponse credential;
  private PlaybackResponse playback;
  private StreamResponse stream;
  private Throwable error;

  public CredentialResponse getCredential() {
    return credential;
  }

  public PlaybackResult setCredential(CredentialResponse credential) {
    if (credential != null && !credential.isEmpty()) {
      this.error = null;
      this.credential = credential;
    }
    return this;
  }

  public PlaybackResponse getPlayback() {
    return playback;
  }

  public PlaybackResult setPlayback(PlaybackResponse playback) {
    if (playback != null && !playback.isEmpty()) {
      this.error = null;
      this.playback = playback;
    }
    return this;
  }

  public StreamResponse getStream() {
    return stream;
  }

  public PlaybackResult setStream(StreamResponse stream) {
    if (stream != null) {
      this.error = null;
      this.stream = stream;
    }
    return this;
  }

  public Throwable getError() {
    return error;
  }

  public void setError(Throwable error) {
    this.error = error;
  }

  public void setFailCode(int code) {
    this.error = new IOException("fail " + code);
  }
}