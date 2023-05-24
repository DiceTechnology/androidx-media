package androidx.media3.common.endeavor.cmcd;

import android.annotation.SuppressLint;
import android.net.Uri;
import androidx.media3.common.C;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

public class CMCDType {

  private static final int MIN_REQUESTED_THROUGHPUT = 4500; // kbps
  private static final int MAX_REQUESTED_THROUGHPUT = 30_000; // kbps

  private CMCDType() {
    // Prevents instantiation.
  }

  /**
   * The media type of the current object being requested.
   */
  public enum CMCDObjectType {
    /**
     * Text file, such as a manifest or playlist
     */
    MANIFEST("m"),
    /**
     * Audio only segment
     */
    AUDIO_ONLY("a"),
    /**
     * Video only segment
     */
    VIDEO_ONLY("v"),
    /**
     * Muxed audio and video segment
     */
    MUXED_AUDIO_VIDEO("av"),
    /**
     * Init segment
     */
    INIT_SEGMENT("i"),
    /**
     * Caption or subtitle
     */
    CAPTION_OR_SUBTITLE("c"),
    /**
     * ISOBMFF timed text track
     */
    TIMED_TEXT_TRACK("tt"),
    /**
     * Cryptographic key, license or certificate
     */
    KEY("k"),
    /**
     * Other
     */
    OTHER("o");

    private final String token;

    CMCDObjectType(String token) {
      this.token = token;
    }

    public String getToken() {
      return token;
    }

    public static CMCDObjectType from(String token) {
      if (token != null) {
        for (CMCDObjectType objectType : CMCDObjectType.values()) {
          if (objectType.getToken().equals(token)) {
            return objectType;
          }
        }
      }
      return null;
    }

    public static CMCDObjectType from(int trackType) {
      switch (trackType) {
        case C.TRACK_TYPE_AUDIO:
          return CMCDObjectType.AUDIO_ONLY;
        case C.TRACK_TYPE_DEFAULT:
        case C.TRACK_TYPE_VIDEO:
          return CMCDObjectType.VIDEO_ONLY;
        case C.TRACK_TYPE_TEXT:
          return CMCDObjectType.CAPTION_OR_SUBTITLE;
        default:
          return CMCDObjectType.OTHER;
      }
    }

    public static CMCDObjectType from(CMCDCollector collector) {
      return collector == null ? null : from((String) collector.getPayload(CMCDKey.OBJECT_TYPE));
    }

    public static boolean isAudioVideo(Object token) {
      if (token != null) {
        for (CMCDObjectType objectType : CMCDObjectType.values()) {
          if (objectType.getToken().equals(token)) {
            return objectType == AUDIO_ONLY || objectType == VIDEO_ONLY || objectType == MUXED_AUDIO_VIDEO;
          }
        }
      }
      return false;
    }
  }

  /**
   * The stream type used by the current session.
   */
  public enum CMCDStreamType {
    /**
     * All segments are available – e.g., VOD
     */
    VOD("v"),
    /**
     * Segments become available over time – e.g., LIVE
     */
    LIVE("l");

    private final String token;

    CMCDStreamType(String token) {
      this.token = token;
    }

    public String getToken() {
      return token;
    }

    public static CMCDStreamType from(String token) {
      if (token != null) {
        for (CMCDStreamType streamType : CMCDStreamType.values()) {
          if (streamType.getToken().equals(token)) {
            return streamType;
          }
        }
      }
      return null;
    }
  }

  /**
   * The stream format which defines the current request.
   */
  public enum CMCDStreamFormat {
    MPEG_DASH("d"),
    HLS("h"),
    SMOOTH_STREAMING("s"),
    OTHER("o");

    private final String token;

    CMCDStreamFormat(String token) {
      this.token = token;
    }

    public String getToken() {
      return token;
    }

    public static CMCDStreamFormat from(String token) {
      if (token != null) {
        for (CMCDStreamFormat streamFormat : CMCDStreamFormat.values()) {
          if (streamFormat.getToken().equals(token)) {
            return streamFormat;
          }
        }
      }
      return null;
    }
  }

  /**
   * Four headers are defined to transmit the data.
   */
  public enum CMCDHeader {
    /**
     * Keys whose values vary with each request.
     */
    REQUEST("CMCD-Request"),
    /**
     * Keys whose values vary with the object being requested.
     */
    OBJECT("CMCD-Object"),
    /**
     * Keys whose values do not vary with every request or object.
     */
    STATUS("CMCD-Status"),
    /**
     * Keys whose values are expected to be invariant over the life of the session.
     */
    SESSION("CMCD-Session");

    private final String key;

    CMCDHeader(String key) {
      this.key = key;
    }

    public final String getKey() {
      return this.key;
    }
  }

  /**
   * Payload keys associated with Version 1 of Common Media Client Data specification.
   */
  public enum CMCDKey {
    /**
     * The encoded bitrate of the audio or video object being requested. This may not be known
     * precisely by the player; however, it MAY be estimated based upon playlist/manifest
     * declarations. If the playlist declares both peak and average bitrate values, the peak value
     * should be transmitted.
     */
    ENCODED_BITRATE("br", CMCDHeader.OBJECT),
    /**
     * The buffer length associated with the media object being requested. This value MUST be
     * rounded to the nearest 100 ms. This key SHOULD only be sent with an object type of ‘a’, ‘v’
     * or ‘av’.
     */
    BUFFER_LENGTH("bl", CMCDHeader.REQUEST),
    /**
     * Key is included without a value if the buffer was starved at some point between the prior
     * request and this object request, resulting in the player being in a rebuffering state and the
     * video or audio playback being stalled. This key MUST NOT be sent if the buffer was not
     * starved since the prior request. If the object type ‘ot’ key is sent along with this key,
     * then the ‘bs’ key refers to the buffer associated with the particular object type. If no
     * object type is communicated, then the buffer state applies to the current session.
     */
    BUFFER_STARVATION("bs", CMCDHeader.STATUS),
    /**
     * A unique string identifying the current content. Maximum length is 64 characters. This value
     * is consistent across multiple different sessions and devices and is defined and updated at
     * the discretion of the service provider.
     */
    CONTENT_ID("cid", CMCDHeader.SESSION),
    /**
     * The playback duration in milliseconds of the object being requested. If a partial segment is
     * being requested, then this value MUST indicate the playback duration of that part and not
     * that of its parent segment. This value can be an approximation of the estimated duration if
     * the explicit value is not known.
     */
    OBJECT_DURATION("d", CMCDHeader.OBJECT),
    /**
     * Deadline from the request time until the first sample of this Segment/Object needs to be
     * available in order to not create a buffer underrun or any other playback problems. This value
     * MUST be rounded to the nearest 100ms. For a playback rate of 1, this may be equivalent to the
     * player’s remaining buffer length.
     */
    DEADLINE("dl", CMCDHeader.REQUEST),
    /**
     * The throughput between client and server, as measured by the client and MUST be rounded to
     * the nearest 100 kbps. This value, however derived, SHOULD be the value that the client is
     * using to make its next Adaptive Bitrate switching decision. If the client is connected to
     * multiple servers concurrently, it must take care to report only the throughput measured
     * against the receiving server. If the client has multiple concurrent connections to the
     * server, then the intent is that this value communicates the aggregate throughput the client
     * sees across all those connections.
     */
    MEASURED_THROUGHPUT("mtp", CMCDHeader.REQUEST),
    /**
     * Relative path of the next object to be requested. This can be used to trigger pre-fetching by
     * the CDN. This MUST be a path relative to the current request. This string MUST be URLEncoded.
     * The client SHOULD NOT depend upon any pre-fetch action being taken - it is merely a request
     * for such a pre-fetch to take place.
     */
    NEXT_OBJECT_REQUEST("nor", CMCDHeader.REQUEST),
    /**
     * If the next request will be a partial object request, then this string denotes the byte range
     * to be requested. If the ‘nor’ field is not set, then the object is assumed to match the
     * object currently being requested. The client SHOULD NOT depend upon any pre-fetch action
     * being taken – it is merely a request for such a pre-fetch to take place. Formatting is
     * similar to the HTTP Range header, except that the unit MUST be ‘byte’, the ‘Range:’ prefix is
     * NOT required and specifying multiple ranges is NOT allowed. Valid combinations are:
     * "<range-start>-" "<range-start>-<range-end>" "-<suffix-length>"
     */
    NEXT_RANGE_REQUEST("nrr", CMCDHeader.REQUEST),
    /**
     * The media type of the current object being requested. See [CMCDObjectType].
     */
    OBJECT_TYPE("ot", CMCDHeader.OBJECT),
    /**
     * Current playback rate. 1 if real-time, 2 if double speed, 0 if not playing. SHOULD only be
     * sent if not equal to 1.
     */
    PLAYBACK_RATE("pr", CMCDHeader.SESSION),
    /**
     * The requested maximum throughput that the client considers sufficient for delivery of the
     * asset. Values MUST be rounded to the nearest 100kbps. For example, a client would indicate
     * that the current segment, encoded at 2Mbps, is to be delivered at no more than 10Mbps, by
     * using rtp=10000. Note: This can benefit clients by preventing buffer saturation through
     * over-delivery and can also deliver a community benefit through fair-share delivery. The
     * concept is that each client receives the throughput necessary for great performance, but no
     * more. The CDN may not support the rtp feature.
     */
    REQUESTED_MAXIMUM_THROUGHPUT("rtp", CMCDHeader.STATUS),
    /**
     * The streaming format that defines the current request. If the streaming format being
     * requested is unknown, then this key MUST NOT be used. See [CMCDStreamFormat].
     */
    STREAM_FORMAT("sf", CMCDHeader.SESSION),
    /**
     * A GUID identifying the current playback session. A playback session typically ties together
     * segments belonging to a single media asset. Maximum length is 64 characters. It is
     * RECOMMENDED to conform to the UUID specification.
     */
    SESSION_ID("sid", CMCDHeader.SESSION),
    /**
     * Current stream type. See [CMCDStreamType].
     */
    STREAM_TYPE("st", CMCDHeader.SESSION),
    /**
     * Key is included without a value if the object is needed urgently due to startup, seeking or
     * recovery after a buffer-empty event. The media SHOULD not be rendering when this request is
     * made. This key MUST not be sent if it is FALSE.
     */
    STARTUP("su", CMCDHeader.REQUEST),
    /**
     * The highest bitrate rendition in the manifest or playlist that the client is allowed to play,
     * given current codec, licensing and sizing constraints.
     */
    TOP_BITRATE("tb", CMCDHeader.OBJECT),
    /**
     * The version of this specification used for interpreting the defined key names and values. If
     * this key is omitted, the client and server MUST interpret the values as being defined by
     * version 1. Client SHOULD omit this field if the version is 1.
     */
    VERSION("v", CMCDHeader.SESSION);

    private final String key;
    private final CMCDHeader header;

    CMCDKey(String key, CMCDHeader header) {
      this.key = key;
      this.header = header;
    }

    public final String getKey() {
      return this.key;
    }

    public CMCDHeader getHeader() {
      return header;
    }
  }

  public static int calcRequestedThroughput(int bitrate) {
    int max = Math.min(MAX_REQUESTED_THROUGHPUT, bitrate * 4);
    return Math.max(MIN_REQUESTED_THROUGHPUT, max);
  }

  public static String toUuidString(String payload) {
    return UUID.nameUUIDFromBytes(payload.getBytes()).toString();
  }

  public static String toQuoteString(String payload) {
    return "\"" + payload + "\"";
  }

  public static String toEncodeString(String payload) {
    try {
      return toQuoteString(URLEncoder.encode(payload, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }

  public static String intToString(int payload) {
    return String.valueOf(payload);
  }

  public static String intRoundToString(int payload) {
    return intToString(Math.round(payload / 100f) * 100);
  }

  @SuppressLint("DefaultLocale")
  public static String speedToString(double payload) {
    String speed = String.format("%.2f", payload);
    return "1.00".equals(speed) ? null : speed;
  }

  public static String versionToString(int payload) {
    return payload == 1 ? null : intToString(payload);
  }

  public static String buildNextObject(Uri baseUri, Uri targetUri) {
    // find common path
    String[] target = targetUri.getPathSegments().toArray(new String[0]);
    String[] base = baseUri.getPathSegments().toArray(new String[0]);
    int commonIndex = 0;
    int min = Math.min(target.length, base.length);
    for (; commonIndex < min; commonIndex++) {
      if (!target[commonIndex].equalsIgnoreCase(base[commonIndex])) {
        break;
      }
    }

    if (target.length == base.length && commonIndex == base.length) {
      return null;
    }

    int basePathIndex = Math.max(0, base.length - 1);
    int commonPathIndex = Math.max(0, Math.min(commonIndex, target.length - 1));
    StringBuilder relative = new StringBuilder();
    if (commonPathIndex >= basePathIndex) {
      relative.append(".");
    } else {
      // determine how many path segments we have to backtrack
      int back = basePathIndex - commonPathIndex;
      for (int i = 0; i < back; i++) {
        relative.append(i == 0 ? ".." : "/..");
      }
      commonPathIndex = commonIndex;
    }
    for (int i = commonPathIndex; i < target.length; i++) {
      relative.append('/').append(target[i]);
    }
    return relative.toString();
  }

  public static String buildNextRange(long position, long length) {
    if (position == 0 && length == C.LENGTH_UNSET) {
      return null;
    }
    StringBuilder rangeValue = new StringBuilder();
    rangeValue.append(position);
    rangeValue.append("-");
    if (length != C.LENGTH_UNSET) {
      rangeValue.append(position + length - 1);
    }
    return rangeValue.toString();
  }
}
