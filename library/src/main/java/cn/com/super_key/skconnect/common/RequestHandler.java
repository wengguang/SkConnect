package cn.com.super_key.skconnect.common;

import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

import static cn.com.super_key.skconnect.common.Utils.checkNotNull;

public abstract class RequestHandler {


  public abstract boolean canHandleRequest(Request data);

  @Nullable public abstract String get(Request request) throws IOException;
  @Nullable public abstract String post(Request request,String str) throws IOException;

  int getRetryCount() {
    return 0;
  }

  boolean shouldRetry(boolean airplaneMode, NetworkInfo info) {
    return false;
  }

  boolean supportsReplay() {
    return false;
  }

}
