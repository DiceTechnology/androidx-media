package androidx.media3.exoplayer.endeavor;

import androidx.annotation.NonNull;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.endeavor.WebUtil;
import androidx.media3.common.endeavor.cmcd.CMCDConfig;
import androidx.media3.common.endeavor.cmcd.CMCDContext;
import androidx.media3.common.endeavor.cmcd.CMCDType.CMCDKey;
import androidx.media3.common.util.Log;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.analytics.PlayerId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CMCDManager {

  private static final class CMCDListener implements Player.Listener, AnalyticsListener {

    private final CMCDContext context;

    public CMCDListener(CMCDContext context) {
      this.context = context;
    }

    // Player.EventListener implementation.

    @Override
    public void onPlaybackStateChanged(@Player.State int playbackState) {
      context.onPlaybackStateChanged(playbackState);
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
      context.onIsPlayingChanged(isPlaying);
    }

    @Override
    public void onTracksChanged(Tracks tracks) {
      context.onTracksChanged();
    }

    // AnalyticsListener implementation.

    @Override
    public void onBandwidthEstimate(@NonNull EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
      context.onBandwidthSample(bitrateEstimate);
    }
  }

  private static final class PlayerHolder {

    private final PlayerId playerId;
    private final ExoPlayer player;
    private final List<CMCDListener> listeners;

    public PlayerHolder(PlayerId playerId, ExoPlayer player) {
      this.playerId = playerId;
      this.player = player;
      this.listeners = new ArrayList<>();
    }
  }

  private static class SingletonHolder {

    private static final CMCDManager instance = new CMCDManager();
  }

  /**
   * Have CMCD be default off, to protect privacy and information leakage.
   * Be able to activate it easily through single API call:
   * 1. CMCDManager.getInstance().setAllActivations(true);
   * 2. CMCDManager.getInstance().setActivation(CMCDKey.NEXT_OBJECT_REQUEST, true);
   */
  private boolean enabled;
  private final CMCDConfig config;
  private final Map<PlayerId, PlayerHolder> players;

  private CMCDManager() {
    config = new CMCDConfig();
    players = new HashMap<>();
  }

  public static CMCDManager getInstance() {
    return SingletonHolder.instance;
  }

  public void setAllActivations(boolean activation) {
    for (CMCDKey key : CMCDKey.values()) {
      config.setActivation(key, activation);
    }
    enabled = activation;
  }

  public void setActivation(CMCDKey key, boolean activation) {
    config.setActivation(key, activation);

    // Check at least one CMCD key is enable or all disable.
    boolean cmcdEnabled = false;
    if (activation) {
      cmcdEnabled = true;
    } else {
      for (CMCDKey cmcdKey : CMCDKey.values()) {
        if (config.isActive(cmcdKey)) {
          cmcdEnabled = true;
          break;
        }
      }
    }
    enabled = cmcdEnabled;
  }

  public synchronized CMCDContext createContext(PlayerId playerId) {
    CMCDContext context = null;
    if (enabled && playerId != null) {
      PlayerHolder holder = players.get(playerId);
      if (holder != null) {
        context = new CMCDContext(config, holder.player);
        CMCDListener listener = new CMCDListener(context);
        holder.player.addListener(listener);
        holder.player.addAnalyticsListener(listener);
        holder.listeners.add(listener);
      }
    }
    return context;
  }

  public synchronized void releaseContext(CMCDContext context) {
    if (!enabled) {
      return;
    }
    context.release();
    Log.i(WebUtil.DEBUG, "CMCDManager#releaseContext " + context.hashCode() + ", context released");
  }

  public synchronized void addPlayer(PlayerId playerId, ExoPlayer player) {
    if (!enabled) {
      return;
    }
    players.put(playerId, new PlayerHolder(playerId, player));
    Log.i(WebUtil.DEBUG, "CMCDManager#addPlayer " + player.hashCode() + ", id " + playerId.hashCode());
  }

  public synchronized void releasePlayer(Player player) {
    if (!enabled) {
      return;
    }
    boolean released = false;
    for (PlayerHolder holder : players.values()) {
      if (holder.player == player) {
        released = true;
        for (CMCDListener listener : holder.listeners) {
          holder.player.removeListener(listener);
          holder.player.removeAnalyticsListener(listener);
        }
        players.remove(holder.playerId);
        break;
      }
    }
    Log.i(WebUtil.DEBUG, "CMCDManager#releasePlayer " + player.hashCode() + ", player " + (released ? "released" : "not found"));
  }
}
