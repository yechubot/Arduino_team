package com.example.cs50.arduino;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class FireService extends Service {
    NotificationManagerCompat notificationManager;
    final long[] VIBRATE_PATTERN = {500, 1000, 300, 1000, 500, 1000, 300, 1000};

    public FireService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        /* Notification notification = new NotificationCompat.Builder(this, NotiChannel.CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_fire)
                .setTicker("화재감지경보")
                .setContentTitle("화재감지경보")
                .setContentText("화재가 감지되어 자동으로 창문열기 실행됨")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.RED)
                .setVibrate(VIBRATE_PATTERN)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(1, notification); */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaPlayer player=MediaPlayer.create(this, R.raw.siren);
        player.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    void startForegroundService() {
        Intent notiIntent = new Intent(this, MainActivity.class); //띄울 액티비티는 클래스로 적어줘야.
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, notiIntent, 0);
        //RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String channelID = "FireChannel";
            NotificationChannel channel = new NotificationChannel(channelID, "화재채널", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, channelID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        builder.setSmallIcon(R.drawable.ic_fire)
                .setTicker("화재감지경보")
                .setContentTitle("화재감지경보")
                .setContentText("화재가 감지되어 자동으로 창문열기 실행됨")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.RED)
                .setVibrate(VIBRATE_PATTERN)
                .setAutoCancel(true)
                .setContentIntent(pIntent)
                .build();
        startForeground(1,builder.build());

    }

}