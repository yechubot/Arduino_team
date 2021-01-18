package com.example.cs50.arduino;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class NotiChannel extends Application {
    public static final String CHANNEL_1_ID = "channel1";
    public static final String CHANNEL_2_ID = "channel2";

    @Override
    public void onCreate() {
        super.onCreate();
        createNofiticationChannel();
    }

    void createNofiticationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_1_ID, "channel 1", NotificationManager.IMPORTANCE_HIGH);
            channel1.setDescription("Channel1");

            NotificationChannel channel2 = new NotificationChannel(
                    CHANNEL_2_ID, "channel 2", NotificationManager.IMPORTANCE_LOW);
            channel2.setDescription("Channel2");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel1);
            manager.createNotificationChannel(channel2);
        }
    }
}
