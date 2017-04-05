package cn.com.super_key.skconnect.common;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import cn.com.super_key.skconnect.common.NetRequest;
import cn.com.super_key.skconnect.common.OkHttp3Downloader;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.GINGERBREAD;
import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static java.lang.String.format;

final class Utils {
  static final String THREAD_PREFIX = "SKconn-";
  static final String THREAD_IDLE_NAME = THREAD_PREFIX + "Idle";
  private static final int KEY_PADDING = 50; // Determined by exact science.
  static final int THREAD_LEAK_CLEANING_MS = 1000;
  static final char KEY_SEPARATOR = '\n';


  static final StringBuilder MAIN_THREAD_KEY_BUILDER = new StringBuilder();

  public enum Priority {
    LOW,
    NORMAL,
    HIGH
  }



  private Utils() {
    // No instances.
  }


  static <T> T checkNotNull(T value, String message) {
    if (value == null) {
      throw new NullPointerException(message);
    }
    return value;
  }

  static void checkNotMain() {
    if (isMain()) {
      throw new IllegalStateException("Method call should not happen from the main thread.");
    }
  }

  static void checkMain() {
    if (!isMain()) {
      throw new IllegalStateException("Method call should happen from the main thread.");
    }
  }

  static boolean isMain() {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }



  static String createKey(Request data) {
    String result = createKey(data, MAIN_THREAD_KEY_BUILDER);
    MAIN_THREAD_KEY_BUILDER.setLength(0);
    return result;
  }

  static String createKey(Request data, StringBuilder builder) {
    if (data.stableKey != null) {
      builder.ensureCapacity(data.stableKey.length() + KEY_PADDING);
      builder.append(data.stableKey);
    } else if (data.uri != null) {
      String path = data.uri.toString();
      builder.ensureCapacity(path.length() + KEY_PADDING);
      builder.append(path);
    } else {
      builder.ensureCapacity(KEY_PADDING);
    }
    builder.append(KEY_SEPARATOR);

    return builder.toString();
  }
  static void closeQuietly(InputStream is) {
    if (is == null)
      return;
    try {
      is.close();
    } catch (IOException ignored) {
    }
  }

  static NetRequest createDefaultDownloader(Context context) {
    if (SDK_INT >= GINGERBREAD) {
      try {
        Class.forName("okhttp3.OkHttpClient");
        return OkHttp3DownloaderCreator.create(context);
      } catch (ClassNotFoundException ignored) {
      }

    }
    return null;
  //  return new UrlConnectionDownloader(context);
  }

  static boolean isAirplaneModeOn(Context context) {
    ContentResolver contentResolver = context.getContentResolver();
    try {
      if (SDK_INT < JELLY_BEAN_MR1) {
        return Settings.System.getInt(contentResolver, Settings.System.AIRPLANE_MODE_ON, 0) != 0;
      }
      return Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    } catch (NullPointerException e) {

      return false;
    } catch (SecurityException e) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  static <T> T getService(Context context, String service) {
    return (T) context.getSystemService(service);
  }

  static boolean hasPermission(Context context, String permission) {
    return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
  }

  static byte[] toByteArray(InputStream input) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024 * 4];
    int n;
    while (-1 != (n = input.read(buffer))) {
      byteArrayOutputStream.write(buffer, 0, n);
    }
    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Prior to Android 5, HandlerThread always keeps a stack local reference to the last message
   * that was sent to it. This method makes sure that stack local reference never stays there
   * for too long by sending new messages to it every second.
   */
  static void flushStackLocalLeaks(Looper looper) {
    Handler handler = new Handler(looper) {
      @Override public void handleMessage(Message msg) {
        sendMessageDelayed(obtainMessage(), THREAD_LEAK_CLEANING_MS);
      }
    };
    handler.sendMessageDelayed(handler.obtainMessage(), THREAD_LEAK_CLEANING_MS);
  }

  @TargetApi(HONEYCOMB)
  private static class ActivityManagerHoneycomb {
    static int getLargeMemoryClass(ActivityManager activityManager) {
      return activityManager.getLargeMemoryClass();
    }
  }

  static class SKThreadFactory implements ThreadFactory {
    @SuppressWarnings("NullableProblems")
    public Thread newThread(Runnable r) {
      return new SKThread(r);
    }
  }

  private static class SKThread extends Thread {
    public SKThread(Runnable r) {
      super(r);
    }

    @Override public void run() {
      Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
      super.run();
    }
  }

  private static class OkHttp3DownloaderCreator {
    static NetRequest create(Context context) {
      return new OkHttp3Downloader(context);
    }
  }
}
