
package cn.com.super_key.skconnect.common;

import android.net.NetworkInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import cn.com.super_key.skconnect.common.Utils.Priority;

import okhttp3.Cache;

public  class JsonHunter implements Runnable {
  private static final Object DECODE_LOCK = new Object();

  private static final ThreadLocal<StringBuilder> NAME_BUILDER = new ThreadLocal<StringBuilder>() {
    @Override protected StringBuilder initialValue() {
      return new StringBuilder(Utils.THREAD_PREFIX);
    }
  };

  private static final AtomicInteger SEQUENCE_GENERATOR = new AtomicInteger();

  private static final RequestHandler ERRORING_HANDLER = new RequestHandler() {
    @Override public boolean canHandleRequest(Request data) {
      return true;
    }

    @Nullable
    @Override
    public String get(Request request) throws IOException {
      throw new IllegalStateException("Unrecognized type of request: " + request);
    }
    @Nullable
    @Override
    public String post(Request request, String str) throws IOException {
      throw new IllegalStateException("Unrecognized type of request: " + request);
    }

  };

  final int sequence;
  final SKConn skconn;
  final Dispatcher dispatcher;
  final Stats stats;
  final String key;
  final Request data;


  final RequestHandler requestHandler;

  Action action;
  List<Action> actions;


  Object result = null;
  Future<?> future;
  int retryCount;
  Utils.Priority priority;
  Exception exception;


  public String getClassName(){
    return data.className;
  }



  public void setResult(Object result) {
    this.result = result;
  }

  public Object getResult() {
    return result;
  }

  public void setFuture(Future<?> future) {
    this.future = future;
  }


  JsonHunter(SKConn skconn, Dispatcher dispatcher, Stats stats, Action action,
             RequestHandler requestHandler) {
    this.sequence = SEQUENCE_GENERATOR.incrementAndGet();
    this.skconn = skconn;
    this.dispatcher = dispatcher;
    this.stats = stats;
    this.action = action;
    this.key = action.getKey();
    this.data = action.getRequest();
    this.priority = action.getPriority();
    this.requestHandler = requestHandler;
    this.retryCount = requestHandler.getRetryCount();
  }
  public SKConn getSkconn() {
    return skconn;
  }
  public int getSequence() {
    return sequence;
  }

  @Override public void run() {
    try {
      updateThreadName(data);

      result = hunt();

      if (result == null) {
        dispatcher.dispatchFailed(this);
      } else {
        dispatcher.dispatchComplete(this);
      }
    } catch (NetRequest.ResponseException e) {
      if ( e.responseCode != 504) {
        exception = e;
      }
      dispatcher.dispatchFailed(this);
    } catch (NetworkRequestHandler.ContentLengthException e) {
      exception = e;
      dispatcher.dispatchRetry(this);
    } catch (OutOfMemoryError e) {
      StringWriter writer = new StringWriter();
      stats.createSnapshot().dump(new PrintWriter(writer));
      exception = new RuntimeException(writer.toString(), e);
      dispatcher.dispatchFailed(this);
    } catch (Exception e) {
      exception = e;
      dispatcher.dispatchFailed(this);
    } finally {
      Thread.currentThread().setName(Utils.THREAD_IDLE_NAME);
    }
  }

  Object hunt() throws IOException {

    String method = data.getMethod();
    getSkconn().enCodces(this);
   // RequestHandler.Result result1;
    if(method.equalsIgnoreCase("post")){
      String str =(String)data.getObj();
      result = requestHandler.post(data,str);
    }else{

      result = requestHandler.get(data);
    }

   // result = result1.getResult();
    getSkconn().deCodces(this);


    return getResult();
  }

  void attach(Action action) {
    Request request = action.request;

    if (this.action == null) {
      this.action = action;
      return;
    }

    if (actions == null) {
      actions = new ArrayList<Action>(3);
    }

    actions.add(action);

    Priority actionPriority = action.getPriority();
    if (actionPriority.ordinal() > priority.ordinal()) {
      priority = actionPriority;
    }
  }

  void detach(Action action) {
    boolean detached = false;
    if (this.action == action) {
      this.action = null;
      detached = true;
    } else if (actions != null) {
      detached = actions.remove(action);
    }
    if (detached && action.getPriority() == priority) {
      priority = computeNewPriority();
    }

  }

  private Priority computeNewPriority() {
    Priority newPriority = Priority.LOW;

    boolean hasMultiple = actions != null && !actions.isEmpty();
    boolean hasAny = action != null || hasMultiple;

    // Hunter has no requests, low priority.
    if (!hasAny) {
      return newPriority;
    }

    if (action != null) {
      newPriority = action.getPriority();
    }

    if (hasMultiple) {
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0, n = actions.size(); i < n; i++) {
        Priority actionPriority = actions.get(i).getPriority();
        if (actionPriority.ordinal() > newPriority.ordinal()) {
          newPriority = actionPriority;
        }
      }
    }

    return newPriority;
  }

  boolean cancel() {
    return action == null
        && (actions == null || actions.isEmpty())
        && future != null
        && future.cancel(false);
  }

  boolean isCancelled() {
    return future != null && future.isCancelled();
  }

  boolean shouldRetry(boolean airplaneMode, NetworkInfo info) {
    boolean hasRetries = retryCount > 0;
    if (!hasRetries) {
      return false;
    }
    retryCount--;
    return requestHandler.shouldRetry(airplaneMode, info);
  }

  boolean supportsReplay() {
    return requestHandler.supportsReplay();
  }



  String getKey() {
    return key;
  }


  public Request getData() {
    return data;
  }

  Action getAction() {
    return action;
  }

  SKConn getPicasso() {
    return skconn;
  }

  List<Action> getActions() {
    return actions;
  }

  Exception getException() {
    return exception;
  }


  Priority getPriority() {
    return priority;
  }

  static void updateThreadName(Request data) {
    String name = data.getName();

    StringBuilder builder = NAME_BUILDER.get();
    builder.ensureCapacity(Utils.THREAD_PREFIX.length() + name.length());
    builder.replace(Utils.THREAD_PREFIX.length(), builder.length(), name);

    Thread.currentThread().setName(builder.toString());
  }

  static JsonHunter forRequest(SKConn skconn, Dispatcher dispatcher,  Stats stats,
                               Action action) {

    Request request = action.getRequest();
    List<RequestHandler> requestHandlers = skconn.getRequestHandlers();


    for (int i = 0, count = requestHandlers.size(); i < count; i++) {
      RequestHandler requestHandler = requestHandlers.get(i);
      if (requestHandler.canHandleRequest(request)) {
        return new JsonHunter(skconn, dispatcher,  stats, action, requestHandler);
      }
    }


    return new JsonHunter(skconn, dispatcher, stats, action, ERRORING_HANDLER);
  }

}

