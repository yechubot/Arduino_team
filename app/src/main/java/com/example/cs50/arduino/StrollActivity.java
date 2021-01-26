package com.example.cs50.arduino;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class StrollActivity extends AppCompatActivity {
    //log tag
    private static final String TAG = "stroll";
    String chID = "stroll";

    //ui
    Button btn_save,off;
    TimePicker timePicker;
    int hour, min;
    TextView stroll_time;

    //db
    MyHelper helper;
    SQLiteDatabase db;
    Cursor cursor;
    int count;
    String set_time;

    //noti
    NotificationManager notificationManager;
    AlarmManager alarmManager;
    GregorianCalendar mCalendar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.stroll_notify);

        //db
        helper = new MyHelper(getApplicationContext());

        //notification set up
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mCalendar = new GregorianCalendar();

        //변수 연결
        btn_save = findViewById(R.id.btn_save);
        stroll_time = findViewById(R.id.stroll_time);
        off = findViewById(R.id.off);
        stroll_time.setText("산책알림 설정 시간 : 설정 안됨");

        //db에 알림 시간 설정한거 있으면 읽어오기
        db = helper.getReadableDatabase();
        String[] projection = {StrollDB.StrollEntry.COL_NAME_NOTIFICATION_TIME};
        cursor = db.query(
                StrollDB.StrollEntry.TBL_NAME,
                projection,
                null,
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            count = cursor.getCount();
            set_time = cursor.getString(0);
            if (count > 0) stroll_time.setText("산책알림 설정 시간 : " + set_time);
        }
        //시간 가져오기
        timePicker = findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true); //24시간제

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hour = timePicker.getCurrentHour();
                min = timePicker.getCurrentMinute();
                String time24 = hour + " 시 " + min + " 분";
                //알림 설정 메소드
                setAlarm();

                //db에 시간 넣음
                db = helper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(StrollDB.StrollEntry.COL_NAME_NOTIFICATION_TIME, time24);
                db.insert(StrollDB.StrollEntry.TBL_NAME, null, values);
                Toast.makeText(getApplicationContext(), "산책 알림이 " + time24 + " 에 설정되었습니다. ", Toast.LENGTH_SHORT).show();
                stroll_time.setText("산책알림 설정 시간 : " + time24);
            }
        });

/*        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "반복알림을 끕니다.", Toast.LENGTH_SHORT).show();
                //offAlarm();

            }
        });*/
    }

//    private void offAlarm() {
//        alarmManager = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
//        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
//        alarmManager.cancel(pendingIntent);
//    }

    private void setAlarm() {
        Intent rIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, rIntent, 0);
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
        cal.set(Calendar.MINUTE, timePicker.getCurrentMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Log.d(TAG, "cal: " + cal.getTime());
        long aTime = System.currentTimeMillis();
        long bTime = cal.getTimeInMillis();

        //하루 시간
        long interval = 1000 * 60 * 60 * 24;

        //설정시간이 현재보다 작으면 다음날 울림
        while (aTime > bTime) {
            bTime += interval;
        }
        //매일 반복
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, bTime, interval, pendingIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.close();
    }

}
