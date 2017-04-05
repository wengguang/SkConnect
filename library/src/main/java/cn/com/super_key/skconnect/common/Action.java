package cn.com.super_key.skconnect.common;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import cn.com.super_key.skconnect.common.Utils.Priority;

abstract class Action<T> {

  static class RequestWeakReference<M> extends WeakReference<M> {
    final Action action;

    public RequestWeakReference(Action action, M referent, ReferenceQueue<? super M> q) {
      super(referent, q);
      this.action = action;
    }
  }
  final SKConn skconn;
  final Request request;
  final String key;
  final Object tag;


  final WeakReference<T> target;

  boolean willReplay;
  boolean cancelled;

  Action(SKConn skconn, Request request,  String key, Object tag, T target) {
    this.skconn = skconn;
    this.request = request;

    this.key = key;
    this.tag = (tag != null ? tag : this);

    this.target =
            target == null ? null : new RequestWeakReference<T>(this, target, skconn.referenceQueue);
  }

  abstract  public void complete(Object result);
  abstract public void error(String error);


  void cancel() {
    cancelled = true;
  }
  T getTarget(){  return target == null ? null : target.get();}

  Request getRequest() {
    return request;
  }

  String getKey() {
    return key;
  }

  boolean isCancelled() {
    return cancelled;
  }

  boolean willReplay() {
    return willReplay;
  }

  public SKConn getSkconn() {
    return skconn;
  }

  Priority getPriority() {
    return request.priority;
  }

  Object getTag() {
    return tag;
  }
}
