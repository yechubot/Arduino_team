package com.example.cs50.arduino;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    //ui
    LinearLayout weather, stroll, window;
    String date_time;
    TextView today_date_time;
    TextView today_weather;
    TextView today_suggest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy년 MM월 dd일");
        date_time = format.format(calendar.getTime());
        //원 3개
        weather = findViewById(R.id.weather);
        stroll = findViewById(R.id.stroll);
        window = findViewById(R.id.window);

        //날씨 원 text
        today_date_time = findViewById(R.id.today_date_time);
        today_weather = findViewById(R.id.today_weather);
        today_suggest = findViewById(R.id.today_suggest);
        //현재 날짜
        today_date_time.setText(date_time);

        weather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WeatherActivity.class);
                startActivity(intent);
            }
        });
        stroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), StrollActivity.class);
                startActivity(intent);
            }
        });
        window.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WindowActivity.class);
                startActivity(intent);
            }
        });

    }


}