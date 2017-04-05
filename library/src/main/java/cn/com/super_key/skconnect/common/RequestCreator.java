/*
 * Copyright (C) 2013 Square, Inc.
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
package cn.com.super_key.skconnect.common;


import android.net.Uri;
import android.support.annotation.NonNull;
import java.util.concurrent.atomic.AtomicInteger;
import cn.com.super_key.skconnect.listener.CallBackListener;
import static cn.com.super_key.skconnect.common.Utils.checkMain;


public class RequestCreator {
  private static final AtomicInteger nextId = new AtomicInteger();

  private final SKConn skconn;
  private  String    method; // get  post
  private Object  postObject;


  private final Request.Builder data;
 // private boolean noFade;

  private int errorResId;
  private Object tag;
  private String className;

  RequestCreator(SKConn skconn, Uri uri, String method,Object postParam,String className) {
    if (skconn.shutdown) {
      throw new IllegalStateException(
              "skconn instance already shut down. Cannot submit new requests.");
    }
    this.skconn = skconn;
    this.method = method;
    postObject = postParam;
    this.className = className;
    this.data = new Request.Builder(uri,method,postParam,className);
  }


  public RequestCreator tag(@NonNull Object tag) {
    if (tag == null) {
      throw new IllegalArgumentException("Tag invalid.");
    }
    if (this.tag != null) {
      throw new IllegalStateException("Tag already set.");
    }
    this.tag = tag;
    return this;
  }
  RequestCreator clearTag() {
    this.tag = null;
    return this;
  }


  Object getTag() {
    return tag;
  }


  public RequestCreator stableKey(@NonNull String stableKey) {
    data.stableKey(stableKey);
    return this;
  }


  public RequestCreator priority(@NonNull Utils.Priority priority) {
    data.priority(priority);
    return this;
  }

  public void into(CallBackListener callback) {
    long started = System.nanoTime();
    checkMain();

    tag = callback;
    Request request = createRequest(started);
    String requestKey =  Utils.createKey(request);



    CallBackAction action =
            new CallBackAction(skconn,request,requestKey, tag, callback);

    skconn.enqueueAndSubmit(action);
  }

  private Request createRequest(long started) {
    int id = nextId.getAndIncrement();


    Request request = data.build();
    request.id = id;
    request.started = started;

    return request;
  }

}
