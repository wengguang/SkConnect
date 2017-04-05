package cn.com.super_key.skconnect.listener;

public interface CallBackListener {
	public void complete(Object  obj);
	public void fail(String errorMsg);
}
