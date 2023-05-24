package androidx.media3.common.endeavor.cmcd;

import android.net.Uri;
import androidx.media3.common.endeavor.DebugBase;
import androidx.media3.common.endeavor.WebUtil;
import androidx.media3.common.endeavor.cmcd.CMCDType.CMCDKey;
import androidx.media3.common.endeavor.cmcd.CMCDType.CMCDObjectType;
import androidx.media3.common.util.Log;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class CMCDCollector {

  private final CMCDContext context;
  private final EnumMap<CMCDKey, Object> dataMap;

  public CMCDCollector(CMCDContext context) {
    this.context = context;
    this.dataMap = new EnumMap<>(CMCDKey.class);
  }

  // Public method.

  public void updateEncodedBitrate(int encodedBitrate) {
    if (encodedBitrate > 0) {
      setPayload(CMCDKey.ENCODED_BITRATE, encodedBitrate);
    }
  }

  public void updateObjectDuration(int objectDuration) {
    setPayload(CMCDKey.OBJECT_DURATION, objectDuration);
  }

  public void updateNextObjectRequest(String nextObjectRequest) {
    setPayload(CMCDKey.NEXT_OBJECT_REQUEST, nextObjectRequest);
  }

  public void updateNextRangeRequest(String nextRangeRequest) {
    setPayload(CMCDKey.NEXT_RANGE_REQUEST, nextRangeRequest);
  }

  public void updateObjectType(CMCDObjectType objectType) {
    setPayload(CMCDKey.OBJECT_TYPE, objectType == null ? null : objectType.getToken());
  }

  public void updateRequestedThroughput(int requestedThroughput) {
    setPayload(CMCDKey.REQUESTED_MAXIMUM_THROUGHPUT, requestedThroughput);
  }

  public void updateStartup(boolean startup) {
    setPayload(CMCDKey.STARTUP, startup ? "" : null);
  }

  public void updateTopBitrate(int topBitrate) {
    context.updateTopBitrate(topBitrate);
  }

  public Map<String, String> buildHeaders(Uri dataSpecUri) {
    boolean isAudioVideoType = CMCDObjectType.isAudioVideo(getPayload(CMCDKey.OBJECT_TYPE));
    Map<String, String> headers = new HashMap<>();
    for (CMCDKey key : CMCDKey.values()) {
      buildPayload(isAudioVideoType, key, headers);
    }

    if (DebugBase.debug_cmcd) {
      StringBuilder message = new StringBuilder("CMCDHeader");
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        message.append("; ").append(entry);
      }
      message.append("; dataSpecUrl ").append(dataSpecUri);
      Log.i(WebUtil.DEBUG, message.toString());
    }

    return headers;
  }

  // Internal method.

  private void buildPayload(boolean isAudioVideoType, CMCDKey key, Map<String, String> headers) {
    // Find payload from collector and context.
    Object payload = getAdjustedPayload(isAudioVideoType, key);
    if (payload == null) {
      return;
    }

    // Convert payload value.
    String strValue = null;
    switch (key) {
      case ENCODED_BITRATE:
      case OBJECT_DURATION:
      case TOP_BITRATE:
        strValue = CMCDType.intToString((int) payload);
        break;
      case BUFFER_LENGTH:
      case DEADLINE:
      case MEASURED_THROUGHPUT:
      case REQUESTED_MAXIMUM_THROUGHPUT:
        strValue = CMCDType.intRoundToString((int) payload);
        break;
      case BUFFER_STARVATION:
        strValue = (String) payload;
        context.finishBufferStarvation();
        break;
      case CONTENT_ID:
      case NEXT_RANGE_REQUEST:
      case SESSION_ID:
        strValue = CMCDType.toQuoteString((String) payload);
        break;
      case NEXT_OBJECT_REQUEST:
        strValue = CMCDType.toEncodeString((String) payload);
        break;
      case OBJECT_TYPE:
      case STREAM_FORMAT:
      case STREAM_TYPE:
      case STARTUP:
        strValue = (String) payload;
        break;
      case PLAYBACK_RATE:
        strValue = CMCDType.speedToString((double) payload);
        break;
      case VERSION:
        strValue = CMCDType.versionToString((int) payload);
        break;
    }
    if (strValue == null) {
      return;
    }

    // Put the payload pair to header map.
    String payloadName = key.getKey();
    String headerName = key.getHeader().getKey();
    String headerValue = headers.containsKey(headerName) ? headers.get(headerName) + "," + payloadName : payloadName;
    if (strValue.length() > 0) {
      headerValue += "=" + strValue;
    }
    headers.put(headerName, headerValue);
  }

  private Object getAdjustedPayload(boolean isAudioVideoType, CMCDKey key) {
    if (!isAudioVideoType) {
      switch (key) {
        case BUFFER_LENGTH:
        case BUFFER_STARVATION:
        case DEADLINE:
          return null;
      }
    }
    boolean isDeadLine = (key == CMCDKey.DEADLINE);
    Object payload = getPayload(isDeadLine ? CMCDKey.BUFFER_LENGTH : key);
    if (payload == null) {
      return null;
    }
    if (isDeadLine) {
      Object obj = getPayload(CMCDKey.PLAYBACK_RATE);
      double speed = (obj == null ? 0 : (double) obj);
      if (speed == 0) {
        return null;
      }
      payload = (int) Math.round(((int) payload) / speed);
    }
    return payload;
  }

  protected synchronized Object getPayload(CMCDKey key) {
    Object payload = dataMap.get(key);
    if (payload == null) {
      payload = context.getPayload(key);
    }
    return payload;
  }

  private synchronized void setPayload(CMCDKey key, Object value) {
    if (context.config.isActive(key)) {
      dataMap.put(key, value);
    }
  }

  public boolean isActiveNextPayload() {
    return context.config.isActive(CMCDKey.NEXT_OBJECT_REQUEST) || context.config.isActive(CMCDKey.NEXT_RANGE_REQUEST);
  }

  public synchronized void release() {
    dataMap.clear();
  }

  public static CMCDCollector createCollector(CMCDCollector cmcdCollector) {
    return (cmcdCollector == null ? null : CMCDContext.createCollector(cmcdCollector.context));
  }
}
