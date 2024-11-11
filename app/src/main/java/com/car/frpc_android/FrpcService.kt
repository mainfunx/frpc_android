package com.car.frpc_android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.car.frpc_android.database.AppDatabase
import com.car.frpc_android.database.Config
import com.car.frpc_android.ui.HomeFragment
import com.car.frpc_android.ui.MainActivity
import com.jeremyliao.liveeventbus.LiveEventBus
import frpclib.Frpclib
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import java.io.File

class FrpcService : Service() {
    private val compositeDisposable = CompositeDisposable()
    private var notificationManager: NotificationManager? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        LiveEventBus.get(INTENT_KEY_FILE, String::class.java).observeStickyForever(keyObserver)

        startForeground(NOTIFY_ID, createForegroundNotification())
    }

    var keyObserver: Observer<String> = Observer { uid: String ->
        if (Frpclib.isRunning(uid)) {
            return@Observer
        }
        AppDatabase.getInstance(this@FrpcService)
            .configDao()
            .getConfigByUid(uid)
            .flatMap { config: Config ->
                val dir = File(cacheDir, "config")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "frpc_${config.uid}.toml")
                file.writeText(config.cfg)
                val error = Frpclib.runClientWithUid(config.uid, file.absolutePath, true, true)
                Single.just(error)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<String> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onSuccess(error: String) {
                    Log.d(TAG, error)
                    if (!TextUtils.isEmpty(error)) {
                        Toast.makeText(this@FrpcService, error, Toast.LENGTH_SHORT).show()
                        LiveEventBus.get(HomeFragment.EVENT_RUNNING_ERROR, String::class.java)
                            .post(uid)
                    }
                }

                override fun onError(e: Throwable) {
                }
            })
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }


    private fun createForegroundNotification(): Notification {
        val notificationChannelId = "frpc_android_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Frpc Service Notification"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel =
                NotificationChannel(notificationChannelId, channelName, importance)
            notificationChannel.description = "Frpc Foreground Service"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            if (notificationManager != null) {
                notificationManager!!.createNotificationChannel(notificationChannel)
            }
        }
        val builder = NotificationCompat.Builder(this, notificationChannelId)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("Frpc Foreground Service")
        builder.setContentText("Frpc Service is running")
        builder.setWhen(System.currentTimeMillis())
        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        builder.setContentIntent(pendingIntent)

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        compositeDisposable.dispose()
    }


    companion object {
        const val INTENT_KEY_FILE: String = "INTENT_KEY_FILE"
        const val NOTIFY_ID: Int = 0x1010
        const val TAG = "FrpcService"
    }
}
