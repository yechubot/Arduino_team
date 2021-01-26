package com.example.cs50.arduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WindowActivity extends AppCompatActivity implements View.OnClickListener {
    //ui
    ToggleButton on_off;
    TextView text_windowStatus;
    Switch sw_off;

    //bluetooth
    BluetoothAdapter bluetoothAdapter;
    static final int REQUEST_ENABLE_BT = 10;
    int pairedDeviceCount = 0;
    Set<BluetoothDevice> deviceSet; //장치 가져올 때
    BluetoothDevice remoteDevice;
    BluetoothSocket socket = null; //통신하기 위한 소켓이 필요
    OutputStream outputStream = null;
    InputStream inputStream = null;
    Thread workerThread = null;
    String strDelimiter = "\n"; // 문자열 끝
    char charDelimiter = '/';
    byte[] readBuffer;
    int readBufferPosition;

    //motor 각도 송신 값
    String str="0";
    int flame;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.window_adjust);
        on_off = findViewById(R.id.btn_on_off);
        text_windowStatus=findViewById(R.id.text_windowStatus);
        sw_off=(Switch)findViewById(R.id.sw_off);
        //자동기능끄기 에 대한 기능
        sendData("9");
        sw_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b==false){//자동기능끄기
                    on_off.setEnabled(true);
                    on_off.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.button_bg));
                    sendData("8");
                }else{
                    on_off.setEnabled(false);
                    on_off.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.button_bg_off));
                    sendData("9");
                }
            }
        });

        //블루투스 바로 연결
        checkBluetooth();

        //버튼 클릭시 리스너 -> 맨 아래 정의되어 있음
        on_off.setOnClickListener(this);

        //화재감지 시 자동 문열림 & 사이렌발생
        if(flame>300){
            if(str=="0"){
                str="1";
                text_windowStatus.setText("창문이 열려있습니다.");
                sendData(str);
            }
            MediaPlayer player=MediaPlayer.create(this, R.raw.siren);
            player.start();
        }
        //테스트 해볼 것: 문이 열려있는 상태일 경우 에러나는지.
    }

    void checkBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //메소드로 어댑터 반환함
        if (bluetoothAdapter == null) {
            showToast("블루투스가 지원되지 않는 기기입니다. 앱을 사용할 수 없습니다.");

        } else {
            //블루투스 지원되는 장치
            if (!bluetoothAdapter.isEnabled()) {
                //안켜진 경우
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                //켜진 경우
                selectDevice();
            }
        }
    }

    //블루투스 장치 선택(페어링 된 장치 중에서)
    void selectDevice() {
        deviceSet = bluetoothAdapter.getBondedDevices();
        //페어링된 목록이 변수에 들어감
        pairedDeviceCount = deviceSet.size(); // 장치 개수 들어감
        if (pairedDeviceCount == 0) {
            //페어링된 장치 0
            showToast("연결된 장치가 없습니다");
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("블루투스 장치 선택")
                    .setIcon(R.drawable.bluetooth);

            //동적 배열
            List<String> listItems = new ArrayList<>();
            for (BluetoothDevice device : deviceSet) {
                listItems.add(device.getName());
            }
            listItems.add("취소");
            final CharSequence items[] = listItems.toArray(new CharSequence[listItems.size()]);
            //동적 배열을 일반배열로 바꿔줌
            // 밑의 메소드는 일반 배열만 가져올 수 있기 때문에
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == pairedDeviceCount) {//취소 눌렀을 때
                        showToast("취소되었습다.");
                    } else {
                        //연결할 장치 선택할 때 장치와 연결을 시도함
                        connectToSelectedDevice(items[which].toString());
                    }
                }
            });
            builder.setCancelable(false); //뒤로가기 버튼 금지
            AlertDialog dialog = builder.create();
            dialog.show();
            // 다이얼로그에서 찾는 작업은 실제로 찾는 게 아님
        }
    }
    //장치와 연결을 시도하는 메소드
    void connectToSelectedDevice(String selectedDeviceName) {
        remoteDevice = getDeviceFromList(selectedDeviceName); // 객체를 돌려줌
        //블루투스를 연결하는 소켓..? hc 06 -  제품 식별자
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try {
            socket = remoteDevice.createRfcommSocketToServiceRecord(uuid); //장치 아이디 보냄 ? 연결?
            socket.connect();// 연결
            showToast("기기와 연결되었습니다.");
            //데이터 실제 송수신은 입출력스트림을 이용해서 이루어짐
            // 소켓이 생성되면 커넥트를 호출하고 두 기기가 연결이 완료 된다 socket.connect()
            outputStream = socket.getOutputStream(); // 보낼거
            inputStream = socket.getInputStream(); //받을거
        } catch (Exception e) {
            showToast("기기와 연결할 수 없습니다.");
        }
    }

    // 데이터 수신 준비 및 처리 메소드
    void beginListenForData() {
        final Handler handler = new Handler();
        readBuffer = new byte[1024]; //수신 처리
        readBufferPosition = 0;// 버퍼 내 수신된 문자 저장 위치
        //문자열 수신 스레드
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    //인터럽트 되지 않는한 계속 실행
                    try {
                        int bytesAvailable = inputStream.available(); //수신 데이터 확인
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable]; //가져온 값을 패킷바이트에 넣음
                            inputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == charDelimiter) {
                                    //마지막이면 \n을 만남
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII"); //아스켓 코드값으로
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //불꽃센서값 수신. 값이 300 이상일 경우, sendData(문열림)
                                            if(data.contains("flame")){
                                                String f = data;
                                                String f2 =  f.substring(5);
                                                flame=Integer.parseInt(f2);
                                            }
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                    //값을 배열에 하나씩 넣음
                                }
                            }
                        }
                    } catch (IOException e) {
                        showToast("데이터 수신 중 오류가 발생했습니다.");
                    }
                }
            }
        });
        workerThread.start();
    }

    //데이터 송신
    void sendData(String msg) {
        msg += strDelimiter; //문자열 종료 표시가 나오면 데이터를 다 받은 것
        try {
            outputStream.write(msg.getBytes());//아두이노로 문자열을 전송함

        } catch (Exception e) {
            showToast("데이터 전송 에러가 발생했습니다.");
        }
    }

    //블루투스 소켓 닫기, 데이터 수신 스레드 종료
    @Override
    protected void onDestroy() {
        try {
            workerThread.interrupt(); // 스레드 종료
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (Exception e) {
            showToast("종료할 수 없습니다.");
        }
        super.onDestroy();
    }

    //페어링된 장치를 이름으로 찾기 -> 실제로 디바이스를 찾는 작업
    BluetoothDevice getDeviceFromList(String name) {
        BluetoothDevice selectedDevice = null; //초기화
        for (BluetoothDevice device : deviceSet) {
            if (name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                //블루투스 장치 활성화 여부
                if (resultCode == RESULT_OK) {
                    //장치 선택
                    selectDevice();
                } else if (resultCode == RESULT_CANCELED) {
                    showToast("활성화된 장치를 찾을 수 없습니다.");
                }
                break;

        }
    }

    @Override
    public void onClick(View v) {
        //진짜 데이터 보내는 거
        if(str=="0"){
            str="1";
            text_windowStatus.setText("창문이 열려있습니다.");
            showToast(str);

        }else if(str=="1"){
            str="0";
            text_windowStatus.setText("창문이 닫혀있습니다.");
            showToast(str);
        }
        sendData(str);
    }

    void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

}

