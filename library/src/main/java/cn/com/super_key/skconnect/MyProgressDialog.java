package cn.com.super_key.skconnect;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.com.super_key.skconnect.library.R;


public class MyProgressDialog {


	public static Dialog show(final Context context, CharSequence title,CharSequence message, final boolean cancelable) {
		if (context != null) {
			Dialog dialog=new Dialog(context, R.style.sp_progress_dialog);
			dialog.setCancelable(cancelable);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setContentView(R.layout.myprogress_dialog);
			
			TextView msgText = (TextView)dialog.findViewById(R.id.msgText);
			msgText.setText(message);
			dialog.show();
			
			return dialog;
		}

		return null;
	}

}
