package androidx.media3.common.endeavor;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;

import android.os.AsyncTask;
import androidx.media3.common.C;
import androidx.media3.common.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WebUtil {

  private static final String TAG = "WebUtil";
  public static final String DEBUG = "====DEBUG====";
  public static final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
  public static final SimpleDateFormat df2 = new SimpleDateFormat("mm:ss.SSS", Locale.US);
  public static final SimpleDateFormat df3 = new SimpleDateFormat("HHmmss.SSS", Locale.US);

  public static final long TIMEOUT_MS = 6000;
  public static final long MANIFEST_LOAD_UNTIL_MS = 36000;

  public static final float LOW_LATENCY_MIN_PLAYBACK_SPEED = 0.90f;
  public static final float LOW_LATENCY_MAX_PLAYBACK_SPEED = 1.15f;

  public static String emptyIfNull(String str) {
    return str == null ? "" : str;
  }

  public static String nullIfEmpty(String str) {
    return empty(str) ? null : str;
  }

  public static boolean empty(String str) {
    return (str == null || str.length() == 0);
  }

  public static int pos(String key, String[] arr) {
    if (empty(key) || arr == null || arr.length == 0) {
      return -1;
    }
    for (int i = 0; i < arr.length; i++) {
      if (key.equals(arr[i])) {
        return i;
      }
    }
    return -1;
  }

  public static String time(long time) {
    return df3.format(new Date(time > 0 ? time : System.currentTimeMillis()));
  }

  private static String filter(long val) {
    if (val == Long.MIN_VALUE) {
      return "vmin";
    } else if (val == C.TIME_UNSET) {
      return "unset";
    } else if (val == -1) {
      return "-1";
    }
    return null;
  }

  public static String stime0(long ms) {
    String ret = filter(ms);
    if (ret != null) {
      return ret;
    } else if (ms == 0) {
      return "0";
    } else if (ms < -1 && ms > -86400000) {
      return "-" + stime(-ms);
    }
    return stime(ms);
  }

  public static String stime(long ms) {
    String ret = filter(ms);
    if (ret != null) {
      return ret;
    } else if (ms == 0) {
      return df.format(new Date(System.currentTimeMillis()));
    } else if (ms < 60000 && ms > -60000) {
      return df2.format(new Date(ms));
    }
    return df.format(new Date(ms));
  }

  public static String us2s(long us) {
    String ret = filter(us);
    return (ret == null ? String.valueOf(us / 1000000f) : ret);
  }

  public static String ms2s(long ms) {
    String ret = filter(ms);
    return (ret == null ? String.valueOf(ms / 1000f) : ret);
  }

  public static byte[] in2bytes(InputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int len = -1;

    byte[] buffer = new byte[4096];
    while ((len = is.read(buffer)) != -1) {
      baos.write(buffer, 0, len);
    }

    is.close();
    return baos.toByteArray();
  }

  public static String loadToken(String token) {
    String authorization = token;
    if (!empty(token) && token.startsWith("http")) {
      AsyncLoader loader = asyncPost(token, null);
      try {
        authorization = loader.get(5, TimeUnit.SECONDS);
        Log.d(TAG, "drmSession loadToken, [" + token + "] - " + authorization);
      } catch (Exception e) {
        Log.e(TAG, "drmSession loadToken fail, [" + token + "]  - " + e);
      }
    }
    return authorization;
  }

  public static AsyncLoader asyncPost(String url, String body) {
    AsyncLoader loader = new AsyncLoader();
    loader.execute(url, body);
    return loader;
  }

  public static String post(String url, byte[] bytes) {
    return post(false, url, null, bytes);
  }

  public static String post(boolean debug, String url, String contentType, byte[] bytes) {
    String result = "";
    HttpURLConnection hc = null;
    try {
      long time = System.currentTimeMillis();
      hc = (HttpURLConnection) new URL(url).openConnection();
      hc.setConnectTimeout(4000);
      hc.setReadTimeout(4000);
      hc.setRequestProperty("Content-Type", empty(contentType) ? "text/plain" : contentType);
      if (bytes != null && bytes.length > 0) {
        hc.setDoOutput(true);
        hc.getOutputStream().write(bytes);
      }
      int code = hc.getResponseCode();
      if (HTTP_OK != code && HTTP_NO_CONTENT != code) {
        Log.w(TAG, "post fail (" + code + "), " + url);
      } else {
        result = new String(in2bytes(hc.getInputStream()));
        long spend = System.currentTimeMillis() - time;
        if (debug) {
          Log.d(TAG, "post, " + spend + " ms, [" + result + "], " + url);
        }
      }
      closeIn(hc.getInputStream());
    } catch (Exception e) {
      consumeErr(hc);
      Log.e(TAG, "post error, " + url, e);
    }
    return result;
  }

  public static void head(String url, Map<String, String> headers) {
    if (headers == null || headers.size() < 1) {
      return;
    }
    HttpURLConnection hc = null;
    try {
      long time = System.currentTimeMillis();
      hc = (HttpURLConnection) new URL(url).openConnection();
      hc.setRequestMethod("HEAD");
      hc.setConnectTimeout(1000);
      hc.setReadTimeout(1000);
      for (String key : headers.keySet()) {
        headers.put(key, hc.getHeaderField(key));
      }
      closeIn(hc.getInputStream());
    } catch (Exception e) {
      consumeErr(hc);
      Log.e(TAG, "head error, " + url, e);
    }
  }

  public static void consumeErr(HttpURLConnection hc) {
    if (hc == null) {
      return;
    }
    InputStream in = hc.getErrorStream();
    consume(in);
    closeIn(in);
  }

  public static void consume(InputStream in) {
    if (in == null) {
      return;
    }
    try {
      byte[] buffer = new byte[4096];
      while (-1 != in.read(buffer)) {
      }
    } catch (IOException e) {
    }
  }

  public static void closeIn(InputStream in) {
    try {
      if (in != null) {
        in.close();
      }
    } catch (IOException e) {
      Log.e(TAG, "fail to close inputstream", e);
    }
  }

  public static void closeOut(OutputStream out) {
    try {
      if (out != null) {
        out.close();
      }
    } catch (IOException e) {
      Log.e(TAG, "fail to close outputstream", e);
    }
  }

  public static final class AsyncLoader extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... strs) {
      String body = (strs.length > 1 ? strs[1] : null);
      byte[] bytes = (body == null ? null : body.getBytes());
      String contentType = (strs.length > 2 ? strs[2] : null);
      return post(false, strs[0], contentType, bytes);
    }
  }
}
