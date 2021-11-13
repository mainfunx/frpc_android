package com.car.frpc_android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

import androidx.annotation.Nullable;
import frpclib.Frpclib;

public class FrpcService extends Service {
    private static final String TAG = FrpcService.class.getName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "service create with pid " + android.os.Process.myPid());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String stringExtra = intent.getStringExtra(getResources().getString(R.string.intent_key_file));
        if (TextUtils.isEmpty(stringExtra)) {
            android.os.Process.killProcess(android.os.Process.myPid());
            return START_STICKY;
        }
        startProxy(stringExtra);
        return START_STICKY;
    }


    private void startProxy(String path) {

        new Thread() {
            @Override
            public void run() {
                Log.i(TAG, "service running with file " + new File(path).getName());
                try {
                    Frpclib.run(path);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "service running error " + e.toString());
                }
            }

        }.start();
    }

    /**
     * 由于{@link Frpclib  没有停止的native底层方法，所以采取 @link AndroidManifest 开启进程方式，关闭直接杀死进程}
     *
     * **/
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "service destroy kill pid " + android.os.Process.myPid());
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
