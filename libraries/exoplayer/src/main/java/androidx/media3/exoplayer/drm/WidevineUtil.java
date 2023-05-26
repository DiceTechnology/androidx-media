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
package androidx.media3.exoplayer.drm;

import android.util.Base64;
import android.util.JsonWriter;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Utility methods for Widevine. */
@UnstableApi
public final class WidevineUtil {

  /** Widevine specific key status field name for the remaining license duration, in seconds. */
  public static final String PROPERTY_LICENSE_DURATION_REMAINING = "LicenseDurationRemaining";
  /** Widevine specific key status field name for the remaining playback duration, in seconds. */
  public static final String PROPERTY_PLAYBACK_DURATION_REMAINING = "PlaybackDurationRemaining";

  public static final String AUTHORIZATION = "Authorization";
  public static final String X_DRM_INFO = "X-DRM-INFO";

  private WidevineUtil() {}

  /**
   * Returns license and playback durations remaining in seconds.
   *
   * @param drmSession The drm session to query.
   * @return A {@link Pair} consisting of the remaining license and playback durations in seconds,
   *     or null if called before the session has been opened or after it's been released.
   */
  @Nullable
  public static Pair<Long, Long> getLicenseDurationRemainingSec(DrmSession drmSession) {
    Map<String, String> keyStatus = drmSession.queryKeyStatus();
    if (keyStatus == null) {
      return null;
    }
    return new Pair<>(
        getDurationRemainingSec(keyStatus, PROPERTY_LICENSE_DURATION_REMAINING),
        getDurationRemainingSec(keyStatus, PROPERTY_PLAYBACK_DURATION_REMAINING));
  }

  private static long getDurationRemainingSec(Map<String, String> keyStatus, String property) {
    if (keyStatus != null) {
      try {
        String value = keyStatus.get(property);
        if (value != null) {
          return Long.parseLong(value);
        }
      } catch (NumberFormatException e) {
        // do nothing.
      }
    }
    return C.TIME_UNSET;
  }

  public static String generateXDrmInfo(UUID uuid, UUID[] keyIds) {
    if (!C.WIDEVINE_UUID.equals(uuid) || keyIds == null || keyIds.length < 1) {
      return null;
    }

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
      writer.beginObject();
      writer.name("system").value("com.widevine.alpha");
      writer.name("key_ids").beginArray();
      for (int i = 0; i < keyIds.length; i++) {
        writer.value(keyIds[i].toString().replaceAll("-", ""));
      }
      writer.endArray();
      writer.endObject();
      writer.close();
      byte[] bytes = out.toByteArray();
      return new String(Base64.encode(bytes, Base64.NO_WRAP));
    } catch (IOException e) {
      Log.e("WidevineUtil", "generateXDrmInfo fail", e);
    }
    return null;
  }

  public static String createXDrmInfoHeader(String systemId, List kIds) {
    JSONObject object = new JSONObject();
    try {
      object.put("system", systemId == null ? getSystem(C.WIDEVINE_UUID) : systemId);
      if (kIds != null && kIds.size() > 0) {
        object.put("key_ids", new JSONArray(kIds));
      }
      return Base64.encodeToString(object.toString().getBytes(), Base64.NO_WRAP);
    } catch (JSONException e) {
      Log.e("WidevineUtil", "createXDrmInfoHeader error", e);
    }
    return null;
  }

  public static String getSystem(UUID drmSchemeUuid) {
    if (C.WIDEVINE_UUID.equals(drmSchemeUuid)) {
      return "com.widevine.alpha";
    } else {
      return "unknown";
    }
  }
}
