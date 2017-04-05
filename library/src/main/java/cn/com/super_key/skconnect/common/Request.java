package cn.com.super_key.skconnect.common;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.view.Gravity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import cn.com.super_key.skconnect.common.Utils.Priority;

import static java.util.Collections.unmodifiableList;

public final class Request {

  int id;
  long started;
  public final String stableKey;
  public final Priority priority;
  public final String method;
  public final Uri uri;
  public final String className;

  public void setObj(Object obj) {
    this.obj = obj;
  }

  public  Object obj;


  public Object getObj() {
    return obj;
  }



  private Request(Uri uri,  String stableKey, Priority priority,String method,Object obj,String className) {
    this.uri = uri;
    this.stableKey = stableKey;
    this.priority = priority;
    this.obj = obj;
    this.method = method;
    this.className = className;
  }
  public String getMethod() {
    return method;
  }

  String getName() {
    if (uri != null) {
      return String.valueOf(uri.getPath());
    }
    return "";
  }


  public static final class Builder {
    private Uri uri;
    private String stableKey;
    private Priority priority;
    private Object obj;
    private String method;
    private String className;

    public Builder(@NonNull Uri uri,String method,Object obj,String className) {
      this.obj = obj;
      this.method = method;
      this.className = className;
      setUri(uri);
    }
    private Builder(Request request) {
      uri = request.uri;
      stableKey = request.stableKey;
      priority = request.priority;
      obj = request.obj;
      method = request.method;
      className = request.className;
    }

    boolean hasPriority() {
      return priority != null;
    }

    public Builder setUri(@NonNull Uri uri) {
      if (uri == null) {
        throw new IllegalArgumentException("Image URI may not be null.");
      }
      this.uri = uri;
      return this;
    }

    public Builder stableKey(@Nullable String stableKey) {
      this.stableKey = stableKey;
      return this;
    }

    /** Execute request using the specified priority. */
    public Builder priority(@NonNull Priority priority) {
      if (priority == null) {
        throw new IllegalArgumentException("Priority invalid.");
      }
      if (this.priority != null) {
        throw new IllegalStateException("Priority already set.");
      }
      this.priority = priority;
      return this;
    }

    public String getMethod() {
      return method;
    }

    public Request build() {
      if (priority == null) {
        priority = Priority.NORMAL;
      }
      return new Request(uri,stableKey, priority,method,obj,className);
    }
  }
}
