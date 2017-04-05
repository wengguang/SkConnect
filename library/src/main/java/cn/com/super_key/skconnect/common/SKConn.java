package cn.com.super_key.skconnect.common;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;


import cn.com.super_key.skconnect.codec.ChannelInboundHandler;
import cn.com.super_key.skconnect.codec.ChannelOutboundHandler;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static cn.com.super_key.skconnect.common.Dispatcher.HUNTER_BATCH_COMPLETE;
import static cn.com.super_key.skconnect.common.Dispatcher.REQUEST_BATCH_RESUME;
import static cn.com.super_key.skconnect.common.Dispatcher.REQUEST_GCED;
import static cn.com.super_key.skconnect.common.Utils.THREAD_LEAK_CLEANING_MS;
import static cn.com.super_key.skconnect.common.Utils.checkMain;
import static cn.com.super_key.skconnect.common.Action.RequestWeakReference;




public class SKConn {

  static final String TAG = "SKConn";
  static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
        case HUNTER_BATCH_COMPLETE: {
          @SuppressWarnings("unchecked")
          List<JsonHunter> batch = (List<JsonHunter>) msg.obj;
          //noinspection ForLoopReplaceableByForEach
          for (int i = 0, n = batch.size(); i < n; i++) {
            JsonHunter hunter = batch.get(i);
            hunter.getSkconn().complete(hunter);
          }
          break;
        }
        case REQUEST_GCED: {
          Action action = (Action) msg.obj;
          action.skconn.cancelExistingRequest(action.getTarget());
          break;
        }
        case REQUEST_BATCH_RESUME:
          @SuppressWarnings("unchecked")
          List<Action> batch = (List<Action>) msg.obj;
          //noinspection ForLoopReplaceableByForEach
          for (int i = 0, n = batch.size(); i < n; i++) {
            Action action = batch.get(i);
            action.skconn.resumeAction(action);
          }
          break;
        default:
          throw new AssertionError("Unknown handler message received: " + msg.what);
      }
    }
  };

  static volatile SKConn singleton = null;
  final List<RequestHandler> requestHandlers;
  final Context context;
  final Dispatcher dispatcher;
  final Stats stats;
  final Map<Object, Action> targetToAction;
  final ReferenceQueue<Object> referenceQueue;
  final CleanupThread cleanupThread;


  private List<ChannelInboundHandler>  inboundHandlerList_;
  private List<ChannelOutboundHandler>  outboundHandlerList_;

  boolean shutdown;


  List<RequestHandler> getRequestHandlers() {
    return requestHandlers;
  }


    SKConn(Context context, Dispatcher dispatcher,
           List<RequestHandler> extraRequestHandlers, Stats stats) {
    this.context = context;
    this.dispatcher = dispatcher;

    int builtInHandlers = 7; // Adjust this as internal handlers are added or removed.
    int extraCount = (extraRequestHandlers != null ? extraRequestHandlers.size() : 0);
    List<RequestHandler> allRequestHandlers =
        new ArrayList<RequestHandler>(builtInHandlers + extraCount);

    allRequestHandlers.add(new NetworkRequestHandler(dispatcher.downloader, stats));
    requestHandlers = Collections.unmodifiableList(allRequestHandlers);

    this.stats = stats;
    this.targetToAction = new WeakHashMap<Object, Action>();


      this.referenceQueue = new ReferenceQueue<Object>();
      this.cleanupThread = new CleanupThread(referenceQueue, HANDLER);
      this.cleanupThread.start();

      inboundHandlerList_ = new ArrayList();
      outboundHandlerList_= new ArrayList();

  }

  public SKConn addLastInbound(ChannelInboundHandler inbound){

    inboundHandlerList_.add(inbound );
    return this;
  }
  public SKConn addLastOutbound(ChannelOutboundHandler outbound){
    outboundHandlerList_.add(outbound);
    return this;
  }
  public SKConn remomveInbound(){
    inboundHandlerList_.clear();
    return this;
}
  public SKConn removeOutBount(){
    outboundHandlerList_.clear();
    return this;
  }

  public void enCodces(JsonHunter ctx){
    for(ChannelInboundHandler handler:inboundHandlerList_){
      try {
        handler.channelRead(ctx,ctx.getRequestData());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  public void deCodces(JsonHunter ctx){

   for(ChannelOutboundHandler handler:outboundHandlerList_ ) {
     try {
       handler.channelRead(ctx,ctx.getResult());
     } catch (Exception e) {
       e.printStackTrace();
     }
   }

  }
  public void cancelTag(@NonNull Object tag) {
    checkMain();
    if (tag == null) {
      throw new IllegalArgumentException("Cannot cancel requests with null tag.");
    }
    List<Action> actions = new ArrayList<Action>(targetToAction.values());
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, n = actions.size(); i < n; i++) {
      Action action = actions.get(i);
      if (tag.equals(action.getTag())) {
        cancelExistingRequest(action.getTarget());
      }
    }
  }

  public void pauseTag(@NonNull Object tag) {
    if (tag == null) {
      throw new IllegalArgumentException("tag == null");
    }
    dispatcher.dispatchPauseTag(tag);
  }


  public void resumeTag(@NonNull Object tag) {
    if (tag == null) {
      throw new IllegalArgumentException("tag == null");
    }
    dispatcher.dispatchResumeTag(tag);
  }


    public RequestCreator get(@Nullable Uri uri)
    {
        return new RequestCreator(this, uri,"get",null,null);
    }

    public RequestCreator get(@Nullable String url)
    {
        return new RequestCreator(this, Uri.parse(url),"get",null,null);
    }
    public RequestCreator get(@Nullable String url,@Nullable String className)
    {
      return new RequestCreator(this, Uri.parse(url),"get",null,className);
    }

    public RequestCreator post(@Nullable Uri uri,Object postParam,String className) {
        return new RequestCreator(this, uri, "post",postParam,className);
    }
    public RequestCreator post(@Nullable String url,Object postParam,String className) {

      return post(Uri.parse(url),postParam,className);
    }

    public RequestCreator post(@Nullable String url,Object postParam) {

        return post(Uri.parse(url),postParam,null);
    }

  /** Stops this instance from accepting further requests. */
  public void shutdown() {
    if (this == singleton) {
      throw new UnsupportedOperationException("Default singleton instance cannot be shutdown.");
    }
    if (shutdown) {
      return;
    }
    stats.shutdown();
    dispatcher.shutdown();
    shutdown = true;
  }





  void enqueueAndSubmit(Action action) {
    Object target = action.getTarget();
    if (target != null && targetToAction.get(target) != action) {
      // This will also check we are on the main thread.
      cancelExistingRequest(target);
      targetToAction.put(target, action);
    }
    submit(action);
  }

  void submit(Action action) {
    dispatcher.dispatchSubmit(action);
  }

  public  String getErrorInfoFromException(Exception e) {
    try {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      return "\r\n" + sw.toString() + "\r\n";
    } catch (Exception e2) {
      return "bad getErrorInfoFromException";
    }
  }

  void complete(JsonHunter hunter) {

        Action single = hunter.getAction();
        List<Action> joined = hunter.getActions();
        boolean hasMultiple = joined != null && !joined.isEmpty();
        boolean shouldDeliver = single != null || hasMultiple;

        if (!shouldDeliver) {
            return;
        }

        Uri uri = hunter.getData().uri;
        Exception exception = hunter.getException();
        String error = getErrorInfoFromException(exception);

        Object result = hunter.getResult();
        if (single != null) {
            deliverAction(result, single,error);
        }

        if (hasMultiple) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = joined.size(); i < n; i++) {
                Action join = joined.get(i);
                deliverAction(result,  join,error);
            }
        }

    }


  void resumeAction(Action action) {
      enqueueAndSubmit(action);
  }

  private void deliverAction(Object result, Action action,String error) {
    if (action.isCancelled()) {
      return;
    }
    if (!action.willReplay()) {
      targetToAction.remove(action.getTarget());
    }
    if (result != null) {

      action.complete(result);

    } else {
      action.error(error);
    }
  }

  private void cancelExistingRequest(Object target) {
    checkMain();
    Action action = targetToAction.remove(target);
    if (action != null) {
      action.cancel();
      dispatcher.dispatchCancel(action);
    }
  }


  public static SKConn with(@NonNull Context context) {
    if (context == null) {
      throw new IllegalArgumentException("context == null");
    }
    if (singleton == null) {
      synchronized (SKConn.class) {
        if (singleton == null) {
          singleton = new Builder(context).build();
        }
      }
    }
    return singleton;
  }


  @SuppressWarnings("UnusedDeclaration") // Public API.
  public static class Builder {
    private final Context context;
    private NetRequest downloader;
    private ExecutorService service;
    private List<RequestHandler> requestHandlers;

    public Builder(@NonNull Context context) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      this.context = context.getApplicationContext();
    }


    public Builder downloader(@NonNull NetRequest downloader) {
      if (downloader == null) {
        throw new IllegalArgumentException("Downloader must not be null.");
      }
      if (this.downloader != null) {
        throw new IllegalStateException("Downloader already set.");
      }
      this.downloader = downloader;
      return this;
    }


    public Builder executor(@NonNull ExecutorService executorService) {
      if (executorService == null) {
        throw new IllegalArgumentException("Executor service must not be null.");
      }
      if (this.service != null) {
        throw new IllegalStateException("Executor service already set.");
      }
      this.service = executorService;
      return this;
    }

    public Builder addRequestHandler(@NonNull RequestHandler requestHandler) {
      if (requestHandler == null) {
        throw new IllegalArgumentException("RequestHandler must not be null.");
      }
      if (requestHandlers == null) {
        requestHandlers = new ArrayList<RequestHandler>();
      }
      if (requestHandlers.contains(requestHandler)) {
        throw new IllegalStateException("RequestHandler already registered.");
      }
      requestHandlers.add(requestHandler);
      return this;
    }

    public SKConn build() {
      Context context = this.context;

      if (downloader == null) {
        downloader = Utils.createDefaultDownloader(context);
      }
      if (service == null) {
        service = new SKExecutorService();
      }

      Stats stats = new Stats();

      Dispatcher dispatcher = new Dispatcher(context, service, HANDLER, downloader, stats);

      return new SKConn(context, dispatcher, requestHandlers, stats);
    }
  }

  private static class CleanupThread extends Thread {
    private final ReferenceQueue<Object> referenceQueue;
    private final Handler handler;

    CleanupThread(ReferenceQueue<Object> referenceQueue, Handler handler) {
      this.referenceQueue = referenceQueue;
      this.handler = handler;
      setDaemon(true);
      setName( "skconn-refQueue");
    }

    @Override public void run() {
      Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
      while (true) {
        try {
          // Prior to Android 5.0, even when there is no local variable, the result from
          // remove() & obtainMessage() is kept as a stack local variable.
          // We're forcing this reference to be cleared and replaced by looping every second
          // when there is nothing to do.
          // This behavior has been tested and reproduced with heap dumps.
          RequestWeakReference<?> remove =
                  (RequestWeakReference<?>) referenceQueue.remove(THREAD_LEAK_CLEANING_MS);
          Message message = handler.obtainMessage();
          if (remove != null) {
            message.what = REQUEST_GCED;
            message.obj = remove.action;
            handler.sendMessage(message);
          } else {
            message.recycle();
          }
        } catch (InterruptedException e) {
          break;
        } catch (final Exception e) {
          handler.post(new Runnable() {
            @Override public void run() {
              throw new RuntimeException(e);
            }
          });
          break;
        }
      }
    }

    void shutdown() {
      interrupt();
    }
  }

}
