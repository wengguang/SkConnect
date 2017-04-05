package cn.com.super_key.skconnect.common;

import android.widget.ImageView;

import java.lang.annotation.Target;

import cn.com.super_key.skconnect.listener.CallBackListener;

/**
 * Created by wwg on 2017/2/16.
 */

public class CallBackAction extends Action<CallBackListener> {

    CallBackAction(SKConn skconn, Request request, String key, Object tag, CallBackListener target) {
        super(skconn, request, key, tag, target);
    }

    @Override
    public void complete(Object result) {

        if (result == null) {
            throw new AssertionError(
                    String.format("Attempted to complete action with no result!\n%s", this));
        }
        if (target.get() != null) {
            target.get().complete(result);
        }
    }

    @Override
    public void error(String error) {

        if (target.get() != null) {
            target.get().fail(error);
        }

    }
}
