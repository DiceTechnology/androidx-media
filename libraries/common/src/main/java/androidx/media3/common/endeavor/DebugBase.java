package androidx.media3.common.endeavor;

import androidx.media3.common.util.Log;

public class DebugBase {

  private static final String TAG = "====DebugBase====";

  public static boolean enable = false; // BuildConfig.DEBUG;
  public static boolean debug_track = false;
  public static boolean debug_cmcd = true;
  public static boolean debug_ad = false;
  public static boolean debug_dash = false;
  public static boolean debug_lowlatency = false;

  // Internal Params
  public static final long debug_switch_ms = 0; // 0, 45000

  protected StringBuilder builder;

  public DebugBase(boolean flag, int sz) {
    if (flag) {
      builder = new StringBuilder(sz < 10 ? 10 : sz);
    }
  }

  public boolean enable() {
    return (builder != null);
  }

  public DebugBase line(String str) {
    if (enable()) {
      builder.append('\n').append(str);
    }
    return this;
  }

  public DebugBase clock(long time) {
    if (enable()) {
      if (time < 0) {
        time = 0;
        builder.append('*');
      }
      builder.append(time).append('(').append(WebUtil.time(time)).append(')');
    }
    return this;
  }

  public DebugBase dur(long dur) {
    if (enable()) {
      if (dur < 0) {
        dur = 0;
        builder.append('*');
      }
      builder.append(dur);
    }
    return this;
  }

  public DebugBase add(Object obj) {
    if (enable()) {
      builder.append(obj);
    }
    return this;
  }

  public void reset() {
    if (enable()) {
      builder.setLength(0);
    }
  }

  public void d(String tag) {
    if (enable()) {
      tag = (WebUtil.empty(tag) ? TAG : tag);
      int size = builder.length(), limit = 4000;
      for (int i = 0; i < size; ) {
        int next = (size - i < limit ? size : builder.lastIndexOf("\n", i + limit));
        if (next <= i) {
          next = size;
        }
        Log.i(tag, builder.substring(i, Math.min(next, size)));
        i = next;
      }
      reset();
    }
  }

  public void dd(String tag, Object obj) {
    add(obj);
    d(tag);
  }
}
