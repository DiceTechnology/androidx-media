/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.session.ext;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.common.util.RepeatModeUtil;
import androidx.media3.session.R;

/** Provides a custom action for toggling repeat modes. */
public final class RepeatModeActionProvider implements MediaSessionConnector.CustomActionProvider {

  /** The default repeat toggle modes. */
  public static final @RepeatModeUtil.RepeatToggleModes int DEFAULT_REPEAT_TOGGLE_MODES =
      RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE | RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL;

  private static final String ACTION_REPEAT_MODE = "ACTION_EXO_REPEAT_MODE";

  private final @RepeatModeUtil.RepeatToggleModes int repeatToggleModes;
  private final CharSequence repeatAllDescription;
  private final CharSequence repeatOneDescription;
  private final CharSequence repeatOffDescription;

  /**
   * Creates a new instance.
   *
   * <p>Equivalent to {@code RepeatModeActionProvider(context, DEFAULT_REPEAT_TOGGLE_MODES)}.
   *
   * @param context The context.
   */
  public RepeatModeActionProvider(Context context) {
    this(context, DEFAULT_REPEAT_TOGGLE_MODES);
  }

  /**
   * Creates a new instance enabling the given repeat toggle modes.
   *
   * @param context The context.
   * @param repeatToggleModes The toggle modes to enable.
   */
  public RepeatModeActionProvider(
      Context context, @RepeatModeUtil.RepeatToggleModes int repeatToggleModes) {
    this.repeatToggleModes = repeatToggleModes;
    repeatAllDescription = "Repeat all";
    repeatOneDescription = "Repeat one";
    repeatOffDescription = "Repeat none";
  }

  @Override
  public void onCustomAction(Player player, String action, @Nullable Bundle extras) {
    int mode = player.getRepeatMode();
    int proposedMode = RepeatModeUtil.getNextRepeatMode(mode, repeatToggleModes);
    if (mode != proposedMode) {
      player.setRepeatMode(proposedMode);
    }
  }

  @Override
  public PlaybackStateCompat.CustomAction getCustomAction(Player player) {
    CharSequence actionLabel;
    int iconResourceId;
    switch (player.getRepeatMode()) {
      case Player.REPEAT_MODE_ONE:
        actionLabel = repeatOneDescription;
        iconResourceId = R.drawable.media3_ext_action_repeat_one;
        break;
      case Player.REPEAT_MODE_ALL:
        actionLabel = repeatAllDescription;
        iconResourceId = R.drawable.media3_ext_action_repeat_all;
        break;
      case Player.REPEAT_MODE_OFF:
      default:
        actionLabel = repeatOffDescription;
        iconResourceId = R.drawable.media3_ext_action_repeat_off;
        break;
    }
    PlaybackStateCompat.CustomAction.Builder repeatBuilder =
        new PlaybackStateCompat.CustomAction.Builder(
            ACTION_REPEAT_MODE, actionLabel, iconResourceId);
    return repeatBuilder.build();
  }
}
