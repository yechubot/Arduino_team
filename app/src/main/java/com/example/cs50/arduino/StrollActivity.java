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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class StrollActivity extends AppCompatActivity {
    //log tag
    private static final String TAG = "stroll";

    String chID = "stroll";
    //ui
    Button btn_save;
    TimePicker timePicker;
    int hour, min;
    TextView stroll_time;
    String am_pm;

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
            if (count == 0) stroll_time.setText("산책알림 설정 시간 : 설정 안됨");
            else stroll_time.setText("산책알림 설정 시간 : " + set_time);
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

                //db에 시간 넣음
                db = helper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(StrollDB.StrollEntry.COL_NAME_NOTIFICATION_TIME, time24);
                db.insert(StrollDB.StrollEntry.TBL_NAME, null, values);
                //알림 메소드 호출
                setAlarm(hour, min);

                Toast.makeText(getApplicationContext(), "산책 알림이 " + time24 + " 에 설정되었습니다. ", Toast.LENGTH_SHORT).show();
                stroll_time.setText("산책알림 설정 시간 : " + time24);
            }
        });
    }

    private void setAlarm(int hour, int min) {
        Intent rIntent = new Intent(StrollActivity.this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(StrollActivity.this, 0, rIntent, 0);
        String from = hour +":"+min;
        Log.d(TAG, "setAlarm: "+from);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Date datetime = null;

        try {
            datetime = dateFormat.parse(from);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(datetime);

        //해당시간에 매일 알림
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pendingIntent);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.close();
    }
}
