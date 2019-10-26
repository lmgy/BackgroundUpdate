package com.lmgy.bgupdate.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.lmgy.bgupdate.BgUpdate;

/**
 * @author lmgy
 * @date 2019/10/26
 */
public class MainActivity extends AppCompatActivity {

    private String url = "http://183.56.150.169/imtt.dd.qq.com/16891/50172C52EBCCD8F9B0AD2B576DB7BA16.apk?mkey=58fedb8402e16784&f=9602&c=0&fsname=cn.flyexp_2.0.1_6.apk&csr=1bbd&p=.apk";
    private String filePath = Environment.getExternalStorageDirectory() + "/xiaohui.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.bt_dialog).setOnClickListener(view -> BgUpdate.updateForDialog(this, url, filePath));
        findViewById(R.id.bt_notification).setOnClickListener(view -> BgUpdate.updateForNotification(this, url, filePath));
    }
}
