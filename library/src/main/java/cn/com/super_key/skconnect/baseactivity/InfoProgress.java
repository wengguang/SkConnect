package cn.com.super_key.skconnect.baseactivity;

public interface  InfoProgress {
	
	abstract public void showProgress(String title, String message);
	abstract public void showProgress();
	abstract public void dismissProgress();
	abstract public void showTip(String message);
	abstract public void runOnactivity(Runnable r);

}
