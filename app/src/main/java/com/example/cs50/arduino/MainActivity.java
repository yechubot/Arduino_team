package com.example.cs50.arduino;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AutoPermissionsListener, View.OnClickListener {

    private static final String TAG = "main";
    //ui
    LinearLayout weather, stroll, window;
    String date_time;
    TextView today_date_time;
    TextView des, windowText;
    ImageView loc;
    ImageView weather_icon;
    Switch sw_off;
    Button test;

    String strDelimiter = "\n"; // 문자열 끝
    String str = "0";

    //화재감지 전화
    TextView fire_status;

    //location
    LocationManager locManager;
    Location location;
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean isGetLoc = false;
    double lat;
    double lon;

    //최소 gps 정보 업뎃 거리 1000 미터
    private static final long MIN_DISTANCE_TO_UPDATE = 1000;
    //최소 업뎃 시간 1분
    private static final long MIN_TIME_TO_UPDATE = 1000 * 60;

    //firebase
    FirebaseDatabase mDatabase;
    DatabaseReference mReference;

    //화재감지 notification
    ImageView iv_fire;
    SmsManager sms;
    String callNum = "01073770785";

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
    char charDelimiter = '/'; // 아두이노에서 끝에 날라옴

    byte[] readBuffer;
    int readBufferPosition;

    String flameVal, waterVal, temperatureVal, humidityVal, sunlightVal;
    String stroll_in_date = "", stroll_out_date = "";
    int strollCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //퍼미션을 모두 체크
        AutoPermissions.Companion.loadAllPermissions(this, 101);

        //오늘 날짜
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일");
        date_time = simpleDateFormat.format(calendar.getTime());

        //원 3개
        weather = findViewById(R.id.weather);
        stroll = findViewById(R.id.stroll);
        window = findViewById(R.id.window);

        //테스트
        test = findViewById(R.id.test);
        //맨 처음 counts 노드는 있어야 한다. db에서 안지우면 되긴 하는데 ..
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                strollCount++;
                getCounts(mDatabase.getReference("stroll_in_out").child("stroll"));

                //앱 종료후 값이 초기화됨
              // connectFirebase("stroll_in_out", "stroll", String.valueOf(strollCount));

            }
        });

        //처음에 자동환기스위치 켜져있으니까 회색에다가 클릭 안됨
        window.setEnabled(false);

        //스위치
        sw_off = findViewById(R.id.sw_off);
        windowText = findViewById(R.id.windowText);

        //앱 켜자마자 전송
        sendData("9");

        sw_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.window_circle);
                Drawable drawable1 = ContextCompat.getDrawable(getApplicationContext(), R.drawable.disabled);
                if (b == false) {//자동기능끄기
                    window.setEnabled(true);
                    window.setBackground(drawable);
                    sendData("8");
                } else {
                    window.setEnabled(false);
                    window.setBackground(drawable1);
                    sendData("9");
                }
            }
        });

        //화재감지 연결
        iv_fire = (ImageView) findViewById(R.id.iv_fire);
        fire_status = (TextView) findViewById(R.id.fire_status);

        //블루투스 바로 연결
        checkBluetooth();

        //위치 가져오기
        get_Location();

        //날씨 원 text
        today_date_time = findViewById(R.id.today_date_time);
        loc = findViewById(R.id.loc);
        weather_icon = findViewById(R.id.weather_icon);
        des = findViewById(R.id.des);

        //현재 날짜
        today_date_time.setText(date_time);

        //날씨 동그라미 클릭
        weather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WeatherActivity.class);
                startActivity(intent);
            }
        });

        //산책 동그라미 클릭
        stroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), StrollActivity.class);
                startActivity(intent);
            }
        });

        //새 액티비티 안뜸
        /*window.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WindowActivity.class);
                startActivity(intent);
            }
        });*/
        // 창 리스너 설정 -> 맨 밑에 코드 있음
        window.setOnClickListener(this);

        //화재감지 아이콘 클릭
        iv_fire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendOnChannel1(v);
                fire_status.setTextColor(Color.RED);
                fire_status.setText("화재감지됨!!(여기를 눌러 종료)");

                //화재 notification 날림
                Intent intent = new Intent(getApplicationContext(), FireService.class);

                if (Build.VERSION.SDK_INT >= 26) { //oreo이상은 startForegroundService로 호출
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }

                //보호자에게 화재 알림 SMS 날림
                sms = SmsManager.getDefault();
                sms.sendTextMessage(callNum, null, "이젠실버타운 105호 화재 감지되었습니다!!", null, null);
            }
        });
        fire_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fire_status.setTextColor(Color.parseColor("#ffffff"));
                fire_status.setText("화재감지 센서 상태: 연결되지 않음");
                Intent intent = new Intent(getApplicationContext(), FireService.class);
                stopService(intent);
            }
        });
    }

    //데이터 송신
    void sendData(String msg) {
        msg += strDelimiter; //문자열 종료 표시가 나오면 데이터를 다 받은 것
        try {
            if (outputStream == null) {
                try {
                    outputStream = socket.getOutputStream();

                } catch (IOException e) {
                    Log.d(TAG, "체크 에러 : " + e.getMessage());
                }
            }

            outputStream.write(msg.getBytes());//아두이노로 문자열을 전송함


        } catch (Exception e) {
            showToast("데이터 전송 에러가 발생했습니다.");
            Log.d(TAG, "sendData: " + e.getMessage());
        }
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
            inputStream = socket.getInputStream(); //받을거

            //데이터 수신
            beginListenForData();
            //  Log.d(TAG, "트라이 메소드 : 됐니");
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
                            //Log.d(TAG, "어베일러블하니 ");
                            Log.d(TAG, "어베일러블 이후 바이트 어베일러블 " + bytesAvailable);

                            byte[] packetBytes = new byte[bytesAvailable]; //가져온 값을 패킷바이트에 넣음
                            inputStream.read(packetBytes);

                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                //Log.d(TAG, "바이트: "+String.valueOf(b));

                                if (b == charDelimiter) {
                                    //Log.d(TAG, "run: 됐니  ");
                                    //마지막이면 \n을 만남
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII"); //아스켓 코드값으로
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {

                                            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMddhhmm");
                                            Date date = new Date();
                                            String strollTime = dataFormat.format(date);
                                            // Log.d(TAG, "data: " + data);

                                            if (data.contains("flame")) {
                                                String f = data;
                                                flameVal = f.substring(5);

                                            } else if (data.contains("water")) {
                                                String w = data;
                                                waterVal = w.substring(5);

                                            } else if (data.contains("temp")) {
                                                String t = data;
                                                temperatureVal = t.substring(4);

                                            } else if (data.contains("humi")) {
                                                String h = data;
                                                humidityVal = h.substring(4);

                                            } else if (data.contains("lig")) {
                                                //Log.d(TAG, "light data: "+data);
                                                String l = data;
                                                sunlightVal = l.substring(3);

                                            } else if (data.contains("doorin")) {
                                                strollCount++;
                                                // Log.d(TAG, "data: " + data);
                                                stroll_in_date = strollTime;

                                            } else if (data.contains("doorout")) {
                                                // Log.d(TAG, "data: " + data);
                                                stroll_out_date = strollTime;

                                            }

                                            if (flameVal != null) {

                                                if (Integer.parseInt(flameVal) > 300) {
                                                    if (str == "0") {
                                                        str = "1";
                                                        windowText.setText("창문 열림");
                                                        //sendData(str);
                                                    }
                                                    MediaPlayer player = MediaPlayer.create(getApplicationContext(), R.raw.siren);
                                                    player.start();
                                                }
                                            }


                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
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

    @Override
    protected void onPause() {
        //센서 firebase 쓰기
        connectFirebase(waterVal, flameVal, temperatureVal, humidityVal, sunlightVal);
        //in, out fireabse에 쓰기
        connectFirebase("stroll_in_out", "in", stroll_in_date);
        connectFirebase("stroll_in_out", "out", stroll_out_date);
         //connectFirebase("stroll_in_out", "stroll", String.valueOf(strollCount));
        super.onPause();
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

    //블루투스로 받은값이 매개변수로 들어갑니다.
    public void connectFirebase(String waterVal, String flameVal, String temperatureVal, String humidityVal, String sunlightVal) {
        //fireabse 실시간 db관리 객체를 얻습니다.
        mDatabase = FirebaseDatabase.getInstance();

        //저장시킬 노드 참조객체를 가져오는데 그 이름은 value 입니다.
        mReference = mDatabase.getReference("values");//안쓰면 최상위 노드

        //firebase 실시간 db에 저장합니다.
        //values 라는 이름 밑에 차일드 노드가 5개 아래의 이름으로 만들어집니다.

        mReference.child("water").setValue(waterVal);
        mReference.child("flame").setValue(flameVal);
        mReference.child("temperature").setValue(temperatureVal);
        mReference.child("humidity").setValue(humidityVal);
        mReference.child("sunlight").setValue(sunlightVal);
    }

    // stroll in & out 메소드
    public void connectFirebase(String node, String child_node, String val) {
        mDatabase = FirebaseDatabase.getInstance();
        mReference = mDatabase.getReference(node);//stroll_in_out
        mReference.child(child_node).setValue(val);// 들어가는 child node 이름에 따라 값 저장됨
    }

    public Location get_Location() {
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            isGPSEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.getMessage();
        }

        if (!isGPSEnabled && !isNetworkEnabled) {
            showToast("GSP 또는 네트워크가 연결되지 않음");
        } else {
            this.isGetLoc = true;
            if (isNetworkEnabled) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                        //requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1); //위치권한 탐색 허용 관련 내용
                    }
                    return null;
                }
                locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_TO_UPDATE, MIN_DISTANCE_TO_UPDATE, locationListener);
                Log.d(TAG, "network enabled ");
            } else {
                showToast("네트워크 위치에 접근할 수 없습니다"); // 와이파이만 사용하는 공기계 네트워크 접근할 수 없음
            }

            if (locManager != null) {
                location = locManager.getLastKnownLocation(locManager.NETWORK_PROVIDER);
                Log.d(TAG, "network location manager not null");
                if (location != null) {
                    lat = location.getLatitude();
                    lon = location.getLongitude();
                    Log.d(TAG, "network location not null");
                }
            }
        }

        if (isGPSEnabled) {
            Log.d(TAG, "gps enabled ");
            location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                locManager.requestLocationUpdates(locManager.GPS_PROVIDER, MIN_TIME_TO_UPDATE, MIN_DISTANCE_TO_UPDATE, locationListener);

                if (locManager != null) {
                    location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Log.d(TAG, "gps location manger not null");

                    if (location != null) {
                        lat = location.getLatitude();
                        lon = location.getLongitude();
                        Log.d(TAG, "gps location not null ");
                    }
                }
            }
        }

        return location;
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            //날씨 호출 메소드
            get_weather(location.getLatitude(), location.getLongitude());
            Log.d(TAG, "onLocationChanged:location:  " + location);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }
    };

    public void get_weather(double lat, double lon) {
        //open weather API id -> description
        final int weather_id[] = {201, 200, 202, 210, 211, 212, 221, 230, 231, 232,
                300, 301, 302, 310, 311, 312, 313, 314, 321, 500,
                501, 502, 503, 504, 511, 520, 521, 522, 531, 600,
                601, 602, 611, 612, 615, 616, 620, 621, 622, 701,
                711, 721, 731, 741, 751, 761, 762, 771, 781, 800,
                801, 802, 803, 804, 900, 901, 902, 903, 904, 905,
                906, 951, 952, 953, 954, 955, 956, 957, 958, 959,
                960, 961, 962};

        final String weather_des[] = {"가벼운 비 동 천둥구름", "비 동반 천둥구름", "폭우 동반 천둥구름", "약한 천둥구름",
                "천둥구름", "강한 천둥구름", "불규칙적 천둥구름", "약한 연무를 동반한 천둥구름", "연무를 동반한 천둥구름",
                "강한 안개비 동반 천둥구름", "가벼운 안개비", "안개비", "강한 안개비", "가벼운 적은비", "적은비",
                "강한 적은비", "소나기와 안개비", "강한 소나기와 안개비", "소나기", "악한 비", "중간 비", "강한 비",
                "매우 강한 비", "극심한 비", "우박", "약한 소나기 비", "소나기 비", "강한 소나기 비", "불규칙적 소나기 비",
                "가벼운 눈", "눈", "강한 눈", "진눈깨비", "소나기 진눈깨비", "약한 비와 눈", "비와 눈", "약한 소나기 눈",
                "소나기 눈", "강한 소나기 눈", "박무", "연기", "연무", "모래 먼지", "안개", "모래", "먼지", "화산재", "돌풍",
                "토네이도", "구름 한 점 없는 맑은 하늘", "약간의 구름이 낀 하늘", "드문드문 구름이 낀 하늘", "구름이 거의 없는 하늘",
                "구름으로 뒤덮인 흐린 하늘", "토네이도", "태풍", "허리케인", "한랭", "고온", "바람부는", "우박", "바람이 거의 없음",
                "약한 바람", "부드러운 바람", "중간 세기 바람", "신선한 바람", "센 바람", "돌풍에 가까운 센 바람", "돌풍",
                "심각한 돌풍", "폭풍", "강한 폭풍", "허리케인"};

        //  Log.d(TAG, "get_weather: 메소드 실행");
        //잘 들어옴
        String url = "http://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&units=metric&appid=b422bc7295c0ae11f8756e015fe316f9";
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    JSONObject main_object = response.getJSONObject("main");
                    Log.d(TAG, "main_object : " + main_object);

                    //"weather"부분 가져옴
                    JSONArray array = response.getJSONArray("weather");
                    JSONObject object = array.getJSONObject(0);

                    //city name 영문이라 생략
                    //String city = response.getString("name");
                    //온도 부분 - 일단 안씀
                    //String main = String.valueOf(main_object.getDouble("temp"));

                    //id를 가져옴
                    String id = object.getString("id");
                    //Log.d(TAG, "id: "+id);
                    String id_description = null;

                    for (int i = 0; i < weather_des.length; i++) {
                        if (Integer.parseInt(id) == weather_id[i]) {
                            int index = i;
                            id_description = weather_des[index];
                        }
                    }
                    //icon
                    String icon = object.getString("icon");
                    String iconUrl = "http://openweathermap.org/img/w/" + icon + ".png";
                    Glide.with(getApplicationContext()).load(iconUrl).into(weather_icon);

                    des.setText(id_description);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "err : " + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jor);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AutoPermissions.Companion.parsePermissions(this, requestCode, permissions, this);
    }

    @Override
    public void onDenied(int i, String[] strings) {

    }

    @Override
    public void onGranted(int i, String[] strings) {

    }

    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        //진짜 데이터 보내는 거
        if (str == "0") {
            str = "1";
            windowText.setText("환기 켜기");
            //  showToast(str);

        } else if (str == "1") {
            str = "0";
            windowText.setText("환기 끄기");
            //  showToast(str);

        }
        sendData(str);
    }

    //firebase 테스트 (stroll count 호출시 사용 )
    private void getCounts(DatabaseReference reference) {
        reference.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String strollCount = currentData.getValue(String.class);
                if (strollCount == null) {
                    return Transaction.success(currentData);
                }
                if (Integer.parseInt(strollCount) >= 0) {
                    int counts = Integer.parseInt(strollCount);
                    Log.d(TAG, "doTransaction() returned: " + counts);
                    //테스트
                    counts++;
                    strollCount = String.valueOf(counts);
                }
                currentData.setValue(strollCount);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                //Log.d(TAG, "onComplete: " + error);
            }
        });
    }
}