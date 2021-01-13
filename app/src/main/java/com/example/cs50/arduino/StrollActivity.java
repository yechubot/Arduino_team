package com.example.cs50.arduino;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class StrollActivity extends AppCompatActivity {
    String chID = "stroll";
    //ui
    Button btn_save;
    TimePicker timePicker;
    int hour, min;
    TextView stroll_time;
    String am_pm;

    NotificationCompat.Builder builder;

    //db
    MyHelper helper;
    SQLiteDatabase db;
    Cursor cursor;
    int count;
    String set_time;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stroll_notify);
        //db
        helper = new MyHelper(getApplicationContext());
        db = helper.getWritableDatabase();
/*
        //notification set up
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        GregorianCalendar mCalendar = new GregorianCalendar();
*/

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
            else stroll_time.setText("산책알림 설정 시간 : "+set_time);
        }
        //시간 가져오기
        timePicker = findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true); //24시간제

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hour = timePicker.getCurrentHour();
                min = timePicker.getCurrentMinute();
                String time24 = hour + " 시 " + min +" 분";

                //db에 시간 넣음
                ContentValues values = new ContentValues();
                values.put(StrollDB.StrollEntry.COL_NAME_NOTIFICATION_TIME, time24);
                db.insert(StrollDB.StrollEntry.TBL_NAME, null, values);
                setAlarm(hour, min);

                Toast.makeText(getApplicationContext(), "산책 알림이 " + time24 + " 에 설정되었습니다. ", Toast.LENGTH_SHORT).show();
                stroll_time.setText("산책알림 설정 시간 : " + time24);


            }
        });

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, chID)
                .setSmallIcon(R.drawable.stroll_circle)
                .setContentTitle("산책 알리미")
                .setContentText("산책할 시간이예요")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    private void setAlarm(int hour, int min) {
        Intent rIntent = new Intent(StrollActivity.this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(StrollActivity.this, 0, rIntent, 0);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.close();
    }
}
