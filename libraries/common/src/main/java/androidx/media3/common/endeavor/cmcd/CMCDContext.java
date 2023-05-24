package androidx.media3.common.endeavor.cmcd;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.Player;
import androidx.media3.common.endeavor.cmcd.CMCDType.CMCDKey;
import androidx.media3.common.endeavor.cmcd.CMCDType.CMCDStreamFormat;
import androidx.media3.common.endeavor.cmcd.CMCDType.CMCDStreamType;
import java.util.EnumMap;

public class CMCDContext {

  private double speed;
  private boolean bufferingEnd;
  private CMCDStreamType streamType;

  protected final CMCDConfig config;

  private final Player player;
  private final Handler handler;
  private final Runnable playbackRunnable;
  private final EnumMap<CMCDKey, Object> dataMap;

  public CMCDContext(CMCDConfig config, Player player) {
    this.config = config;

    this.player = player;
    this.handler = new Handler(Looper.getMainLooper());
    this.playbackRunnable = this::updatePlaybackInfo;
    this.dataMap = new EnumMap<>(CMCDKey.class);

    updateSessionId(CMCDType.toUuidString(String.valueOf(player.hashCode())));
    updateVersion(1);
  }

  // Player.EventListener implementation.

  public void onPlaybackStateChanged(int playbackState) {
    if (playbackState != Player.STATE_READY) {
      return;
    }
    bufferingEnd = true;
    updatePlaybackInfo();
  }

  public void onIsPlayingChanged(boolean isPlaying) {
    if (isPlaying) {
      startUpdatePlaybackInfo();
      if (player.getPlaybackState() == Player.STATE_READY) {
        bufferingEnd = true;
      }
    } else {
      bufferingEnd = false;
      updateBufferStarvation(true);
      updatePlaybackRate(0d);
      stopUpdatePlaybackInfo();
    }
  }

  public void onTracksChanged() {
    if (streamType == null) {
      updateStreamType(player.isCurrentMediaItemLive() ? CMCDStreamType.LIVE : CMCDStreamType.VOD);
    }
  }

  // AnalyticsListener implementation.

  public final void onBandwidthSample(long bitrateEstimate) {
    updateMeasuredThroughput(Math.round(bitrateEstimate / 1024f));
  }

  // Public method.

  public void finishBufferStarvation() {
    if (bufferingEnd) {
      updateBufferStarvation(false);
    }
  }

  public void updateMediaInfo(String contentId, CMCDStreamFormat streamFormat) {
    updateContentId(contentId);
    updateStreamFormat(streamFormat);
    updateStreamType(null);
  }

  public void updateTopBitrate(int topBitrate) {
    setPayload(CMCDKey.TOP_BITRATE, topBitrate);
  }

  // Internal method.

  private void startUpdatePlaybackInfo() {
    stopUpdatePlaybackInfo();
    handler.post(playbackRunnable);
  }

  private void stopUpdatePlaybackInfo() {
    handler.removeCallbacks(playbackRunnable);
  }

  private void updatePlaybackInfo() {
    stopUpdatePlaybackInfo();

    updateBufferLength((int) player.getTotalBufferedDuration());
    double speed = player.getPlaybackParameters().speed;
    if (speed != 0 && this.speed != speed) {
      updatePlaybackRate(speed);
    }

    handler.postDelayed(playbackRunnable, 500);
  }

  private void updateBufferLength(int bufferLength) {
    setPayload(CMCDKey.BUFFER_LENGTH, bufferLength);
  }

  private void updateBufferStarvation(boolean buffering) {
    setPayload(CMCDKey.BUFFER_STARVATION, buffering ? "" : null);
  }

  private void updateContentId(String contentId) {
    setPayload(CMCDKey.CONTENT_ID, contentId);
  }

  private void updateMeasuredThroughput(int measuredThroughput) {
    setPayload(CMCDKey.MEASURED_THROUGHPUT, measuredThroughput);
  }

  private void updatePlaybackRate(double playbackRate) {
    this.speed = playbackRate;
    setPayload(CMCDKey.PLAYBACK_RATE, playbackRate);
  }

  private void updateStreamFormat(CMCDStreamFormat streamFormat) {
    setPayload(CMCDKey.STREAM_FORMAT, streamFormat == null ? null : streamFormat.getToken());
  }

  private void updateSessionId(String sessionId) {
    setPayload(CMCDKey.SESSION_ID, sessionId);
  }

  private void updateStreamType(CMCDStreamType streamType) {
    this.streamType = streamType;
    setPayload(CMCDKey.STREAM_TYPE, streamType == null ? null : streamType.getToken());
  }

  private void updateVersion(int version) {
    setPayload(CMCDKey.VERSION, version);
  }

  private synchronized void setPayload(CMCDKey key, Object value) {
    if (config.isActive(key)) {
      dataMap.put(key, value);
    }
  }

  protected synchronized Object getPayload(CMCDKey key) {
    return dataMap.get(key);
  }

  public boolean matchPlayer(Player player) {
    return this.player == player;
  }

  public synchronized void release() {
    stopUpdatePlaybackInfo();
    dataMap.clear();
  }

  public static CMCDCollector createCollector(CMCDContext context) {
    return (context == null ? null : new CMCDCollector(context));
  }
}
