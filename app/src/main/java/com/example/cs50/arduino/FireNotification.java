package com.example.cs50.arduino;

import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class FireNotification extends AppCompatActivity {
    EditText edt_number;
    Button btn_call;
    SmsManager sms;
    String callNum = "01073770785"; //핸들러, 타이머 클래스 이용

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fire_notification);
        edt_number = (EditText) findViewById(R.id.edt_number);
        btn_call = (Button) findViewById(R.id.btn_call);
        sms = SmsManager.getDefault();
        btn_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sms.sendTextMessage(callNum, null, "불났어요", null, null);
                //AutoPermission을 main에 넣어서 성공함.
//                Uri uri = Uri.parse("tel:"+edt_number.getText().toString());
//                Intent intent = new Intent(Intent.ACTION_CALL, uri);
//                startActivity(intent);


//                if(ActivityCompat.checkSelfPermission(FireNotification.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
//
//                }else{
//                    //바로전화걸기
//                    /*Intent intent=new Intent(Intent.ACTION_CALL);
//                    intent.setData(Uri.parse(phone));*/
//                    //다이얼통해 전화걸기 테스트용//
//                }
            }
        });


    }


}