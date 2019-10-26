package com.lmgy.bgupdate;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author lmgy
 * @date 2019/10/26
 */
public class DownloadService extends Service {

    /**
     * 保存文件路径
     */
    private String filePath;
    /**
     * 订阅器
     */
    private Subscription subscription;
    /**
     * MyBinder
     */
    private static MyBinder myBinder;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        //获取通知服务
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        myBinder = new MyBinder();
        return myBinder;
    }

    class MyBinder extends Binder {

        //下载进度
        private int progress;
        //用于第一次显示
        private boolean isShow = false;

        /**
         * 开始下载
         *
         * @param url      下载链接
         * @param filePath 存储路径
         */
        public void startDownload(String url, String filePath) {
            DownloadService.this.filePath = filePath;
            if (subscription == null) {
                // 新线程，由 observeOn() 指定 , 对象变化
                //下载完毕 , 存储到手机根目录
                subscription = NetWork.getApi((section, total, done) -> {
                    int percent = (int) (section / total * 100);
                    if (!isShow) {
                        //开始下载有进度即显示通知栏
                        showNotification(percent, false);
                        isShow = true;
                    } else if (percent % 5 == 0 && progress != percent) {
                        //每百分之五和旧进度与新进度不相等时,才更新一次通知栏 , 防止更新频繁
                        progress = percent;
                        if (percent == 100) {
                            //下载完毕时 , 存储文件需要时间 , 修改进度条状态
                            showNotification(percent, true);
                        } else {
                            showNotification(percent, false);
                        }

                    }
                }).downloadFile(url)   // IO 线程，由 subscribeOn() 指定 , 下载文件
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.newThread())
                        .map(DownloadService.this::writeResponseBodyToDisk)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<Boolean>() {// Android 主线程，由 observeOn() 指定
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                updateFailHint();
                            }

                            @Override
                            public void onNext(Boolean isSuccess) {
                                //true - 下载完成并存储在手机根目录 , false - 失败
                                if (isSuccess) {    //存储完成 , 开始安装
                                    startInstallApk();
                                } else {        //存储失败 , 提示错误
                                    updateFailHint();
                                }
                            }
                        });
            }
        }

        /**
         * 取消下载
         */
        public void cancelDownload() {
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
                subscription = null;
                mNotifyBuilder = null;
                isShow = false;
                notificationManager.cancelAll();
            }
        }

    }

    /**
     * 显示通知
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification(int progress, boolean isSuccess) {
        //创建通知详细信息
        if (mNotifyBuilder == null) {
            Intent intentClick = new Intent(this, NotificationBroadcastReceiver.class);
            intentClick.setAction("cancelDownload");
            PendingIntent cancelIntent = PendingIntent.getBroadcast(this, 0, intentClick, PendingIntent.FLAG_ONE_SHOT);

            mNotifyBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_file_download_black_24dp)
                    .setContentTitle("正在下载...")
                    .setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .addAction(R.drawable.ic_cancel_black_24dp, "取消", cancelIntent);
        }
        if (isSuccess) {
            mNotifyBuilder.setContentTitle("等待安装...");
            mNotifyBuilder.setProgress(100, progress, true);
        } else {
            mNotifyBuilder.setProgress(100, progress, false);
        }
        //显示通知
        notificationManager.notify(0, mNotifyBuilder.build());
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (myBinder != null) {
            myBinder.cancelDownload();
        }
        super.onTaskRemoved(rootIntent);
        this.stopSelf();
    }

    /**
     * 接受通知
     */
    public static class NotificationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("cancelDownload".equals(action)) {
                //处理点击事件
                myBinder.cancelDownload();//取消下载
            }
        }
    }


    /**
     * 保存下载文件
     */
    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            File filepath = new File(filePath);
            if (!filepath.getParentFile().exists()) {
                filepath.getParentFile().mkdirs();
            }
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] fileReader = new byte[4096];
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(filepath);
                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                }
                outputStream.flush();
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 下载完毕 , 安装新的Apk
     */
    private void startInstallApk() {
        myBinder.cancelDownload();
        //apk文件的本地路径
        File apkFile = new File(filePath);
        //会根据用户的数据类型打开android系统相应的Activity。
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //设置intent的数据类型是应用程序application
        //判读版本是否在7.0以上
        if (Build.VERSION.SDK_INT >= 24) {
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            //为这个新apk开启一个新的activity栈
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.parse("file://" + apkFile.toString()), "application/vnd.android.package-archive");
        }
        //开始安装
        startActivity(intent);
        //关闭旧版本的应用程序的进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 更新失败提示
     */
    private void updateFailHint() {
        Toast.makeText(this, "更新失败 , 请重试", Toast.LENGTH_SHORT).show();
        myBinder.cancelDownload();
    }

}
