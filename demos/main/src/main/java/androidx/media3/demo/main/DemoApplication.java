package androidx.media3.demo.main;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.multidex.MultiDexApplication;

public final class DemoApplication extends MultiDexApplication {

  @Override
  public void onCreate() {
    super.onCreate();
    registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
      private int started = 0;
      private int resumed = 0;
      private int stopped = 0;

      private boolean isAppBackFromBackground() {
        return started == stopped + 1 && stopped > 0 && resumed == started;
      }

      private boolean isAppEnterBackground() {
        return stopped == started;
      }

      @Override
      public void onActivityStarted(@NonNull Activity activity) {
        ++started;
      }

      @OptIn(markerClass = UnstableApi.class)
      @Override
      public void onActivityResumed(@NonNull Activity activity) {
        ++resumed;
        if (isAppBackFromBackground()) {
          DemoDownloadService.sendRefreshForeground(activity, DemoDownloadService.class);
        }
      }

      @Override
      public void onActivityStopped(@NonNull Activity activity) {
        ++stopped;
        resumed = started;
        if (isAppEnterBackground()) {
          // app enter background
        }
      }

      @Override
      public void onActivityCreated(@NonNull Activity activity,
          @Nullable Bundle savedInstanceState) {
      }

      @Override
      public void onActivityPaused(@NonNull Activity activity) {
      }

      @Override
      public void onActivitySaveInstanceState(@NonNull Activity activity,
          @NonNull Bundle outState) {
      }

      @Override
      public void onActivityDestroyed(@NonNull Activity activity) {
      }
    });
  }
}
