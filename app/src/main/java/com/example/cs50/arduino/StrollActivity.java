package com.example.cs50.arduino;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class StrollActivity extends AppCompatActivity {
    //log tag
    private static final String TAG = "stroll";

    //ui
    Button btn_save,off;
    TimePicker timePicker;
    int hour, min;
    TextView stroll_time;

    //firebase
    FirebaseDatabase mDatabase;
    DatabaseReference mReference;

    //noti
    NotificationManager notificationManager;
    AlarmManager alarmManager;
    GregorianCalendar mCalendar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.stroll_notify);


        mDatabase = FirebaseDatabase.getInstance();
        mReference = mDatabase.getReference("strollTime");

        //notification set up
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mCalendar = new GregorianCalendar();

        //변수 연결
        btn_save = findViewById(R.id.btn_save);
        stroll_time = findViewById(R.id.stroll_time);
        off = findViewById(R.id.off);
        stroll_time.setText("산책알림 설정 시간 : 설정 안됨");

        //firebase에서 읽어오기
        ValueEventListener timeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
               String value = dataSnapshot.getValue(String.class);
               stroll_time.setText("산책알림 설정 시간 : "+value);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "onCancelled: ",databaseError.toException() );
            }
        };
        mReference.addValueEventListener(timeListener);

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

                //firebase db에 넣기
                writeTime(time24);

                Toast.makeText(getApplicationContext(), "산책 알림이 " + time24 + " 에 설정되었습니다. ", Toast.LENGTH_SHORT).show();
                stroll_time.setText("산책알림 설정 시간 : " + time24);
            }
        });

    }

    //firebase db에 시간 넣기
    private void writeTime(String time){
        mReference.setValue(time);
        Log.d(TAG, "firebase db write time : "+ time );

    }

    private void setAlarm() {//타임피커에서 정한대로 알람 설정함
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


}
