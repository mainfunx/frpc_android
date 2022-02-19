package com.car.frpc_android;

import android.app.ActivityManager;
import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import frpclib.Frpclib;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class CommonUtils {


    public static Completable waitService(String serviceName, Context context) {
        return Completable.fromObservable(Observable.interval(0, 1, TimeUnit.SECONDS)
                .takeUntil(aLong -> {
                    return isServiceRunning(serviceName, context);
                })
        );
    }

    public static boolean isServiceRunning(String serviceName, Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = am.getRunningServices(Integer.MAX_VALUE); //获取运行的服务,参数表示最多返回的数量
        for (ActivityManager.RunningServiceInfo runningServiceInfo : runningServices) {
            String className = runningServiceInfo.service.getClassName();
            if (className.equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    public static Observable<String> getStringFromRaw(Context context, int rawName) {
        return Observable.create(emitter -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(rawName)));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            reader.close();
            emitter.onNext(result.toString());
            emitter.onComplete();
        });

    }


}
