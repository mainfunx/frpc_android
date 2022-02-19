package com.car.frpc_android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.room.util.StringUtil;

import com.car.frpc_android.database.AppDatabase;
import com.car.frpc_android.database.Config;
import com.car.frpc_android.ui.HomeFragment;
import com.car.frpc_android.ui.MainActivity;
import com.google.gson.Gson;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.List;

import frpclib.Frpclib;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FrpcService extends Service {
    public static final String INTENT_KEY_FILE = "INTENT_KEY_FILE";
    public static final int NOTIFY_ID = 0x1010;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private NotificationManager notificationManager;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        LiveEventBus.get(INTENT_KEY_FILE, String.class).observeStickyForever(keyObserver);

        startForeground(NOTIFY_ID, createForegroundNotification());
    }

    Observer<String> keyObserver = uid -> {
        if (Frpclib.isRunning(uid)) {
            return;
        }


        AppDatabase.getInstance(FrpcService.this)
                .configDao()
                .getConfigByUid(uid)
                .flatMap((Function<Config, SingleSource<String>>) config -> {
                    String error = Frpclib.runContent(config.getUid(), config.getCfg());
                    return Single.just(error);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(String error) {
                        if (!TextUtils.isEmpty(error)) {
                            Toast.makeText(FrpcService.this, error, Toast.LENGTH_SHORT).show();
                            LiveEventBus.get(HomeFragment.EVENT_RUNNING_ERROR, String.class).post(uid);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });



    };



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    private Notification createForegroundNotification() {

        String notificationChannelId = "frpc_android_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = "Frpc Service Notification";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(notificationChannelId, channelName, importance);
            notificationChannel.setDescription("Frpc Foreground Service");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationChannelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Frpc Foreground Service");
        builder.setContentText("Frpc Service is running");
        builder.setWhen(System.currentTimeMillis());
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        compositeDisposable.dispose();
    }


}
