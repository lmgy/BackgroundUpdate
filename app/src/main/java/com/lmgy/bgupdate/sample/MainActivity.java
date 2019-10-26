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

    private String url = "https://test-10061090.cos.ap-shanghai.myqcloud.com/app-release.apk";
    private String filePath = Environment.getExternalStorageDirectory() + "/new.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.bt_notification).setOnClickListener(view -> BgUpdate.updateFile(this, url, filePath));
    }

    @Override
    protected void onDestroy() {
        BgUpdate.closeService(this);
        super.onDestroy();
    }
}
