package cn.com.super_key.skconnect.common;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static cn.com.super_key.skconnect.common.JsonHunter.forRequest;

import static cn.com.super_key.skconnect.common.Utils.getService;
import static cn.com.super_key.skconnect.common.Utils.hasPermission;

class Dispatcher {
  private static final int RETRY_DELAY = 500;
  private static final int AIRPLANE_MODE_ON = 1;
  private static final int AIRPLANE_MODE_OFF = 0;

  static final int REQUEST_SUBMIT = 1;
  static final int REQUEST_CANCEL = 2;
  static final int REQUEST_GCED = 3;
  static final int HUNTER_COMPLETE = 4;
  static final int HUNTER_RETRY = 5;
  static final int HUNTER_DECODE_FAILED = 6;
  static final int HUNTER_DELAY_NEXT_BATCH = 7;
  static final int HUNTER_BATCH_COMPLETE = 8;
  static final int NETWORK_STATE_CHANGE = 9;
  static final int AIRPLANE_MODE_CHANGE = 10;
  static final int TAG_PAUSE = 11;
  static final int TAG_RESUME = 12;
  static final int REQUEST_BATCH_RESUME = 13;

  private static final String DISPATCHER_THREAD_NAME = "Dispatcher";
  private static final int BATCH_DELAY = 200; // ms

  final DispatcherThread dispatcherThread;
  final Context context;
  final ExecutorService service;
  final NetRequest downloader;
  final Map<String, JsonHunter> hunterMap;
  final Map<Object, Action> failedActions;
  final Map<Object, Action> pausedActions;
  final Set<Object> pausedTags;
  final Handler handler;
  final Handler mainThreadHandler;

  final Stats stats;
  final List<JsonHunter> batch;
  final NetworkBroadcastReceiver receiver;
  final boolean scansNetworkChanges;

  boolean airplaneMode;

  Dispatcher(Context context, ExecutorService service, Handler mainThreadHandler,
      NetRequest downloader,  Stats stats) {
    this.dispatcherThread = new DispatcherThread();
    this.dispatcherThread.start();
    Utils.flushStackLocalLeaks(dispatcherThread.getLooper());
    this.context = context;
    this.service = service;
    this.hunterMap = new LinkedHashMap<String, JsonHunter>();
    this.failedActions = new WeakHashMap<Object, Action>();
    this.pausedActions = new WeakHashMap<Object, Action>();
    this.pausedTags = new HashSet<Object>();
    this.handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
    this.downloader = downloader;
    this.mainThreadHandler = mainThreadHandler;
    this.stats = stats;
    this.batch = new ArrayList<JsonHunter>(4);
    this.airplaneMode = Utils.isAirplaneModeOn(this.context);
    this.scansNetworkChanges = hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);
    this.receiver = new NetworkBroadcastReceiver(this);
    receiver.register();
  }

  void shutdown() {
    // Shutdown the thread pool only if it is the one created by Picasso.
    if (service instanceof SKExecutorService) {
      service.shutdown();
    }
    downloader.shutdown();
    dispatcherThread.quit();
    // Unregister network broadcast receiver on the main thread.
    SKConn.HANDLER.post(new Runnable() {
      @Override public void run() {
        receiver.unregister();
      }
    });
  }

  void dispatchSubmit(Action action) {
    handler.sendMessage(handler.obtainMessage(REQUEST_SUBMIT, action));
  }

  void dispatchCancel(Action action) {
    handler.sendMessage(handler.obtainMessage(REQUEST_CANCEL, action));
  }

  void dispatchPauseTag(Object tag)
  {
    handler.sendMessage(handler.obtainMessage(TAG_PAUSE, tag));
  }

  void dispatchResumeTag(Object tag) {
    handler.sendMessage(handler.obtainMessage(TAG_RESUME, tag));
  }

  void dispatchComplete(JsonHunter hunter) {
    handler.sendMessage(handler.obtainMessage(HUNTER_COMPLETE, hunter));
  }

  void dispatchRetry(JsonHunter hunter) {
    handler.sendMessageDelayed(handler.obtainMessage(HUNTER_RETRY, hunter), RETRY_DELAY);
  }

  void dispatchFailed(JsonHunter hunter) {
    handler.sendMessage(handler.obtainMessage(HUNTER_DECODE_FAILED, hunter));
  }

  void dispatchNetworkStateChange(NetworkInfo info) {
    handler.sendMessage(handler.obtainMessage(NETWORK_STATE_CHANGE, info));
  }

  void dispatchAirplaneModeChange(boolean airplaneMode) {
    handler.sendMessage(handler.obtainMessage(AIRPLANE_MODE_CHANGE,
        airplaneMode ? AIRPLANE_MODE_ON : AIRPLANE_MODE_OFF, 0));
  }

  void performSubmit(Action action) {
    performSubmit(action, true);
  }

  void performSubmit(Action action, boolean dismissFailed) {
    if (pausedTags.contains(action.getTag())) {
      pausedActions.put(action.getTarget(), action);
      return;
    }

    JsonHunter hunter = hunterMap.get(action.getKey());
    if (hunter != null) {
      hunter.attach(action);
      return;
    }

    if (service.isShutdown()) {
      return;
    }

    hunter = forRequest(action.getSkconn(), this,  stats, action);
    hunter.setFuture( service.submit(hunter) );
    hunterMap.put(action.getKey(), hunter);
    if (dismissFailed) {
      failedActions.remove(action.getTarget());
    }


  }

  void performCancel(Action action) {
    String key = action.getKey();
    JsonHunter hunter = hunterMap.get(key);
    if (hunter != null) {
      hunter.detach(action);
      if (hunter.cancel()) {
        hunterMap.remove(key);
      }
    }

    if (pausedTags.contains(action.getTag())) {
      pausedActions.remove(action.getTarget());

    }

    failedActions.remove(action.getTarget());

  }

  void performPauseTag(Object tag) {

    if (!pausedTags.add(tag)) {
      return;
    }

    // Go through all active hunters and detach/pause the requests
    // that have the paused tag.
    for (Iterator<JsonHunter> it = hunterMap.values().iterator(); it.hasNext();) {
      JsonHunter hunter = it.next();
   //   boolean loggingEnabled = hunter.gets.loggingEnabled;

      Action single = hunter.getAction();
      List<Action> joined = hunter.getActions();
      boolean hasMultiple = joined != null && !joined.isEmpty();

      // Hunter has no requests, bail early.
      if (single == null && !hasMultiple) {
        continue;
      }

      if (single != null && single.getTag().equals(tag)) {
        hunter.detach(single);
        pausedActions.put(single.getTarget(), single);

      }

      if (hasMultiple) {
        for (int i = joined.size() - 1; i >= 0; i--) {
          Action action = joined.get(i);
          if (!action.getTag().equals(tag)) {
            continue;
          }

          hunter.detach(action);
          pausedActions.put(action.getTarget(), action);
        }
      }
      if (hunter.cancel()) {
        it.remove();
      }
    }
  }

  void performResumeTag(Object tag) {
    // Trying to resume a tag that is not paused.
    if (!pausedTags.remove(tag)) {
      return;
    }

    List<Action> batch = null;
    for (Iterator<Action> i = pausedActions.values().iterator(); i.hasNext();) {
      Action action = i.next();
      if (action.getTag().equals(tag)) {
        if (batch == null) {
          batch = new ArrayList<Action>();
        }
        batch.add(action);
        i.remove();
      }
    }

    if (batch != null) {
      mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(REQUEST_BATCH_RESUME, batch));
    }
  }

  void performRetry(JsonHunter hunter) {
    if (hunter.isCancelled()) return;

    if (service.isShutdown()) {
      performError(hunter, false);
      return;
    }

    NetworkInfo networkInfo = null;
    if (scansNetworkChanges) {
      ConnectivityManager connectivityManager = getService(context, CONNECTIVITY_SERVICE);
      networkInfo = connectivityManager.getActiveNetworkInfo();
    }

    boolean hasConnectivity = networkInfo != null && networkInfo.isConnected();
    boolean shouldRetryHunter = hunter.shouldRetry(airplaneMode, networkInfo);
    boolean supportsReplay = hunter.supportsReplay();

    if (!shouldRetryHunter) {
      // Mark for replay only if we observe network info changes and support replay.
      boolean willReplay = scansNetworkChanges && supportsReplay;
      performError(hunter, willReplay);
      if (willReplay) {
        markForReplay(hunter);
      }
      return;
    }

    // If we don't scan for network changes (missing permission) or if we have connectivity, retry.
    if (!scansNetworkChanges || hasConnectivity) {


      hunter.future = service.submit(hunter);
      return;
    }

    performError(hunter, supportsReplay);

    if (supportsReplay) {
      markForReplay(hunter);
    }
  }

  void performComplete(JsonHunter hunter) {

    hunterMap.remove(hunter.getKey());
    batch(hunter);

  }

  void performBatchComplete() {
    List<JsonHunter> copy = new ArrayList<JsonHunter>(batch);
    batch.clear();
    mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(HUNTER_BATCH_COMPLETE, copy));
    //logBatch(copy);
  }

  void performError(JsonHunter hunter, boolean willReplay) {

    hunterMap.remove(hunter.getKey());
    batch(hunter);
  }

  void performAirplaneModeChange(boolean airplaneMode) {
    this.airplaneMode = airplaneMode;
  }

  void performNetworkStateChange(NetworkInfo info) {
    if (service instanceof SKExecutorService) {
      ((SKExecutorService) service).adjustThreadCount(info);
    }
    if (info != null && info.isConnected()) {
      flushFailedActions();
    }
  }

  private void flushFailedActions() {
    if (!failedActions.isEmpty()) {
      Iterator<Action> iterator = failedActions.values().iterator();
      while (iterator.hasNext()) {
        Action action = iterator.next();
        iterator.remove();
        performSubmit(action, false);
      }
    }
  }

  private void markForReplay(JsonHunter hunter) {
    Action action = hunter.getAction();
    if (action != null) {
      markForReplay(action);
    }
    List<Action> joined = hunter.getActions();
    if (joined != null) {
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0, n = joined.size(); i < n; i++) {
        Action join = joined.get(i);
        markForReplay(join);
      }
    }
  }

  private void markForReplay(Action action) {
    Object target = action.getTarget();
    if (target != null) {
      action.willReplay = true;
      failedActions.put(target, action);
    }
  }

  private void batch(JsonHunter hunter) {
    if (hunter.isCancelled()) {
      return;
    }
    batch.add(hunter);
    if (!handler.hasMessages(HUNTER_DELAY_NEXT_BATCH)) {
      handler.sendEmptyMessageDelayed(HUNTER_DELAY_NEXT_BATCH, BATCH_DELAY);
    }
  }

  private static class DispatcherHandler extends Handler {
    private final Dispatcher dispatcher;

    public DispatcherHandler(Looper looper, Dispatcher dispatcher) {
      super(looper);
      this.dispatcher = dispatcher;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_SUBMIT: {
          Action action = (Action) msg.obj;
          dispatcher.performSubmit(action);
          break;
        }
        case REQUEST_CANCEL: {
          Action action = (Action) msg.obj;
          dispatcher.performCancel(action);
          break;
        }
        case TAG_PAUSE: {
          Object tag = msg.obj;
          dispatcher.performPauseTag(tag);
          break;
        }
        case TAG_RESUME: {
          Object tag = msg.obj;
          dispatcher.performResumeTag(tag);
          break;
        }
        case HUNTER_COMPLETE: {
          JsonHunter hunter = (JsonHunter) msg.obj;
          dispatcher.performComplete(hunter);
          break;
        }
        case HUNTER_RETRY: {
          JsonHunter hunter = (JsonHunter) msg.obj;
          dispatcher.performRetry(hunter);
          break;
        }
        case HUNTER_DECODE_FAILED: {
          JsonHunter hunter = (JsonHunter) msg.obj;
          dispatcher.performError(hunter, false);
          break;
        }
        case HUNTER_DELAY_NEXT_BATCH: {
          dispatcher.performBatchComplete();
          break;
        }
        case NETWORK_STATE_CHANGE: {
          NetworkInfo info = (NetworkInfo) msg.obj;
          dispatcher.performNetworkStateChange(info);
          break;
        }
        case AIRPLANE_MODE_CHANGE: {
          dispatcher.performAirplaneModeChange(msg.arg1 == AIRPLANE_MODE_ON);
          break;
        }
        default:
          SKConn.HANDLER.post(new Runnable() {
            @Override public void run() {
              throw new AssertionError("Unknown handler message received: " + msg.what);
            }
          });
      }
    }
  }

  static class DispatcherThread extends HandlerThread {
    DispatcherThread() {
      super(Utils.THREAD_PREFIX + DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    }
  }

  static class NetworkBroadcastReceiver extends BroadcastReceiver {
    static final String EXTRA_AIRPLANE_STATE = "state";

    private final Dispatcher dispatcher;

    NetworkBroadcastReceiver(Dispatcher dispatcher) {
      this.dispatcher = dispatcher;
    }

    void register() {
      IntentFilter filter = new IntentFilter();
      filter.addAction(ACTION_AIRPLANE_MODE_CHANGED);
      if (dispatcher.scansNetworkChanges) {
        filter.addAction(CONNECTIVITY_ACTION);
      }
      dispatcher.context.registerReceiver(this, filter);
    }

    void unregister() {
      dispatcher.context.unregisterReceiver(this);
    }

    @Override public void onReceive(Context context, Intent intent) {
      // On some versions of Android this may be called with a null Intent,
      // also without extras (getExtras() == null), in such case we use defaults.
      if (intent == null) {
        return;
      }
      final String action = intent.getAction();
      if (ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
        if (!intent.hasExtra(EXTRA_AIRPLANE_STATE)) {
          return; // No airplane state, ignore it. Should we query Utils.isAirplaneModeOn?
        }
        dispatcher.dispatchAirplaneModeChange(intent.getBooleanExtra(EXTRA_AIRPLANE_STATE, false));
      } else if (CONNECTIVITY_ACTION.equals(action)) {
        ConnectivityManager connectivityManager = getService(context, CONNECTIVITY_SERVICE);
        dispatcher.dispatchNetworkStateChange(connectivityManager.getActiveNetworkInfo());
      }
    }
  }
}
