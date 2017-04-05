package cn.com.super_key.skconnect.baseactivity;



import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.widget.Toast;

import cn.com.super_key.skconnect.MyProgressDialog;


public class BaseActivity extends Activity implements InfoProgress {

	private Dialog progressDialog = null;
	private int times = 0;

	boolean showProgressAble =true;

	public boolean isShowProgressAble() {
		return showProgressAble;
	}

	public void setShowProgressAble(boolean showProgressAble) {
		this.showProgressAble = showProgressAble;
	}
	//public String getSDPath() {
		
	//	String str = Environment.getExternalStorageDirectory().getPath();
	//	return str +"/Soarup/www5/sbb/";
	//}
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (isShowProgressAble()) {
			dismissProgress();
		}
	}

	@Override
	public void showProgress() {
		if (isShowProgressAble()) {
			showProgress("正在加载", "拼命加载互联网内容");
		}
	}

	@Override
	public void showProgress(String title, String message) {
		if (isShowProgressAble()) {
			synchronized (this) {
				if (times == 0) {
					progressDialog = MyProgressDialog.show(this,  title, message , true);
				}
				times++;
			}
		}
		
	}
	@Override
	public void dismissProgress() {
		if (isShowProgressAble()) {
			synchronized (this) {
				if (times == 1) {
					try {
						progressDialog.dismiss();
						times--;
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					times--;
				}
			}
		}
	}

	@Override
	public void showTip(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void runOnactivity(Runnable r) {
		this.runOnUiThread(r);
	}

	public void selfFinish(){
		this.finish();
		//this.overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
	}

}
