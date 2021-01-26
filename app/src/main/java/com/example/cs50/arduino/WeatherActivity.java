package com.example.cs50.arduino;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "날씨";
    //ui
    TextView[] tv_weathers= new TextView[5];
    int[] weatherID={R.id.temperature, R.id.sunlight, R.id.humidity, R.id.flame, R.id.drop};
    //firebase
    FirebaseDatabase mDatabase;
    DatabaseReference mReference;

    String[] values={"temperature", "sunlight", "humidity", "flame", "water"};
    String[] val_name={"온도", "조도", "습도", "화재", "물방울"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather);

        // text view
        for(int i=0; i<tv_weathers.length; i++){
            tv_weathers[i]=(TextView)findViewById(weatherID[i]);
        }

        mDatabase = FirebaseDatabase.getInstance();
        for(int i=0; i<values.length; i++){
            final int index = i;
            mReference = mDatabase.getReference("values").child(values[i]);
            ValueEventListener weather_Listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String value = dataSnapshot.getValue(String.class);
                    //처음에 없으면 null로 세팅되서
                    if(val_name[index]==null){
                        tv_weathers[index].setText("불러오는중");

                    }
                    tv_weathers[index].setText(val_name[index]+" : " + value);

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w(TAG, "onCancelled: ",databaseError.toException() );
                }
            };
            mReference.addValueEventListener(weather_Listener);
        }

    }

}