package com.example.cs50.arduino;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "Alarm";

    public AlarmReceiver() {};

    NotificationManager manager;
    NotificationCompat.Builder builder;

    private static String CH_ID = "channel3";
    private static String CH_NAME = "channel3";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + "알람");
        builder = null;
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //채널
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                    new NotificationChannel(CH_ID, CH_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            );
            builder = new NotificationCompat.Builder(context, CH_ID);
        } else {
            builder = new NotificationCompat.Builder(context);
        }

        Intent intentActivity = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 101, intentActivity, PendingIntent.FLAG_UPDATE_CURRENT);
        //알림 콘텐츠
        builder.setContentTitle("산책 알리미")
                .setContentText("산책할 시간이예요")
                .setSmallIcon(R.drawable.footprint)
                .setAutoCancel(true) // 알림 터치하여 삭제
                .setContentIntent(pendingIntent);//탭하여 보이는 인텐트

        Notification notification = builder.build();
        manager.notify(1, notification);
    }
}
