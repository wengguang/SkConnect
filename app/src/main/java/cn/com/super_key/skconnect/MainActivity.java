package cn.com.super_key.skconnect;

import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import cn.com.super_key.skconnect.common.SKConn;
import cn.com.super_key.skconnect.jsonparse.InJson;
import cn.com.super_key.skconnect.jsonparse.OutJson;
import cn.com.super_key.skconnect.listener.CallBackListener;
import cn.com.super_key.skconnect.modal.FightStatus;
import cn.com.super_key.skconnect.modal.Ret;

public class MainActivity extends AppCompatActivity {

    private Button but;

    private Dialog progressDialog = null;

    public void showProgress(String message) {
           progressDialog = MyProgressDialog.show(this,  message , true);
    }
    public void dismissProgress() {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        but = (Button) findViewById(R.id.button);
        SKConn.with(MainActivity.this).addLastInbound(new InJson());
        SKConn.with(MainActivity.this).addLastOutbound(new OutJson());

        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showProgress("");

                String url = "http://wifi.hainanairlines.com/index.php?m=Api&c=CallApi&a=index";
                FightStatus fstatus = new FightStatus();

                fstatus.setStatus(0);

                SKConn.with(MainActivity.this).post(url,fstatus,"Ret").into(new CallBackListener() {
                    @Override
                    public void complete(Object obj) {
                        dismissProgress();
                        Ret str = (Ret) obj;
                        Log.i("onClick", "type=" + str.getResponseType());

                        int i = 0;
                        i++;
                        i = 0;
                    }

                    @Override
                    public void fail(String errorMsg) {
                        dismissProgress();
                        Log.i("onClick", "error=" + errorMsg);
                        int i = 0;
                        i++;
                        i = 0;
                    }
                });
            }
        });
    }
}
