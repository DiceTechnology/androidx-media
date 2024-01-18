package androidx.media3.common.endeavor;

import android.util.Log;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;

public class DebugUtil {

  public static final String TAG = DebugUtil.class.getSimpleName();

  public static final int DEBUG_CMAF_ATOM = 1 << 1;
  public static final int DEBUG_LOW_LATENCY = 1 << 2;
  public static final int DEBUG_SAMPLE_SEGMENT = 1 << 3;

  public static final int DEBUG_SAMPLE_VIDEO_READ = 1 << 8;
  public static final int DEBUG_SAMPLE_VIDEO_WRITE = 1 << 9;
  public static final int DEBUG_SAMPLE_AUDIO_READ = 1 << 10;
  public static final int DEBUG_SAMPLE_AUDIO_WRITE = 1 << 11;
  public static final int DEBUG_SAMPLE_TEXT_READ = 1 << 12;
  public static final int DEBUG_SAMPLE_TEXT_WRITE = 1 << 13;
  public static final int DEBUG_SAMPLE_META_READ = 1 << 14;
  public static final int DEBUG_SAMPLE_META_WRITE = 1 << 15;

  public static final int DEBUG_SAMPLE_VIDEO = DEBUG_SAMPLE_VIDEO_READ | DEBUG_SAMPLE_VIDEO_WRITE;
  public static final int DEBUG_SAMPLE_AUDIO = DEBUG_SAMPLE_AUDIO_READ | DEBUG_SAMPLE_AUDIO_WRITE;
  public static final int DEBUG_SAMPLE_TEXT = DEBUG_SAMPLE_TEXT_READ | DEBUG_SAMPLE_TEXT_WRITE;
  public static final int DEBUG_SAMPLE_META = DEBUG_SAMPLE_META_READ | DEBUG_SAMPLE_META_WRITE;
  public static final int DEBUG_SAMPLE = DEBUG_SAMPLE_VIDEO | DEBUG_SAMPLE_AUDIO
      | DEBUG_SAMPLE_TEXT | DEBUG_SAMPLE_META | DEBUG_SAMPLE_SEGMENT;

  private static int debugFlags;

  public static void setDebugFlags(int flags) {
    debugFlags = flags;
  }

  public static boolean isDebugCmafAtomAllowed() {
    return (debugFlags & DEBUG_CMAF_ATOM) != 0;
  }

  public static boolean isDebugLowLatencyAllowed() {
    return (debugFlags & DEBUG_LOW_LATENCY) != 0;
  }

  public static boolean isDebugSampleSegmentAllowed() {
    return (debugFlags & DEBUG_SAMPLE_SEGMENT) != 0;
  }

  public static boolean isDebugSampleVideoReadAllowed() {
    return (debugFlags & DEBUG_SAMPLE_VIDEO_READ) != 0;
  }

  public static boolean isDebugSampleVideoWriteAllowed() {
    return (debugFlags & DEBUG_SAMPLE_VIDEO_WRITE) != 0;
  }

  public static boolean isDebugSampleVideoAllowed() {
    return isDebugSampleVideoReadAllowed() || isDebugSampleVideoWriteAllowed();
  }

  public static boolean isDebugSampleAudioReadAllowed() {
    return (debugFlags & DEBUG_SAMPLE_AUDIO_READ) != 0;
  }

  public static boolean isDebugSampleAudioWriteAllowed() {
    return (debugFlags & DEBUG_SAMPLE_AUDIO_WRITE) != 0;
  }

  public static boolean isDebugSampleAudioAllowed() {
    return isDebugSampleAudioReadAllowed() || isDebugSampleAudioWriteAllowed();
  }

  public static boolean isDebugSampleTextReadAllowed() {
    return (debugFlags & DEBUG_SAMPLE_TEXT_READ) != 0;
  }

  public static boolean isDebugSampleTextWriteAllowed() {
    return (debugFlags & DEBUG_SAMPLE_TEXT_WRITE) != 0;
  }

  public static boolean isDebugSampleTextAllowed() {
    return isDebugSampleTextReadAllowed() || isDebugSampleTextWriteAllowed();
  }

  public static boolean isDebugSampleMetaReadAllowed() {
    return (debugFlags & DEBUG_SAMPLE_META_READ) != 0;
  }

  public static boolean isDebugSampleMetaWriteAllowed() {
    return (debugFlags & DEBUG_SAMPLE_META_WRITE) != 0;
  }

  public static boolean isDebugSampleMetaAllowed() {
    return isDebugSampleMetaReadAllowed() || isDebugSampleMetaWriteAllowed();
  }

  public static boolean isDebugSampleAllowed() {
    return isDebugSampleSegmentAllowed() || isDebugSampleVideoAllowed()
        || isDebugSampleAudioAllowed() || isDebugSampleTextAllowed()
        || isDebugSampleMetaAllowed();
  }

  public static boolean isDebugSampleReadAllowed(String mimeType) {
    if (mimeType != null) {
      if (MimeTypes.isVideo(mimeType)) {
        return isDebugSampleVideoReadAllowed();
      } else if (MimeTypes.isAudio(mimeType)) {
        return isDebugSampleAudioReadAllowed();
      } else if (MimeTypes.isText(mimeType)) {
        return isDebugSampleTextReadAllowed();
      } else if (isMeta(mimeType)) {
        return isDebugSampleMetaReadAllowed();
      }
    }
    return false;
  }

  public static boolean isDebugSampleReadAllowed(Format format) {
    if (format != null) {
      return isDebugSampleReadAllowed(format.containerMimeType)
          || isDebugSampleReadAllowed(format.sampleMimeType);
    }
    return false;
  }

  public static boolean isDebugSampleWriteAllowed(String mimeType) {
    if (mimeType != null) {
      if (MimeTypes.isVideo(mimeType)) {
        return isDebugSampleVideoWriteAllowed();
      } else if (MimeTypes.isAudio(mimeType)) {
        return isDebugSampleAudioWriteAllowed();
      } else if (MimeTypes.isText(mimeType)) {
        return isDebugSampleTextWriteAllowed();
      } else if (isMeta(mimeType)) {
        return isDebugSampleMetaWriteAllowed();
      }
    }
    return false;
  }

  public static boolean isDebugSampleWriteAllowed(Format format) {
    if (format != null) {
      return isDebugSampleWriteAllowed(format.containerMimeType)
          || isDebugSampleWriteAllowed(format.sampleMimeType);
    }
    return false;
  }

  public static boolean isMeta(String mimeType) {
    return MimeTypes.APPLICATION_ID3.equals(mimeType)
        || MimeTypes.APPLICATION_EMSG.equals(mimeType)
        || MimeTypes.APPLICATION_SCTE35.equals(mimeType);
  }

  public static void i(String msg) {
    Log.i(TAG, msg);
  }

  public static void e(String msg, Throwable tr) {
    Log.e(TAG, msg, tr);
  }
}
