package com.example.bogdan.licenta;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class SensorActivity extends AppCompatActivity implements SensorEventListener {

    Button btnMainActivity;
    Button btnGetWifiInfo;
    TextView textViewCompass;
    TextView textViewCompass2;
    TextView textViewCompass3;
    TextView textWifiInfo;
    TextView textWifiNr;
    ImageView imgViewCompass;
    EditText editCoordX, editCoordY, editLevel, editPosID,editOrientation,editCluster;

    private SensorManager mSensorManager;
    Sensor mAccelerometer;
    Sensor mMagnetometer;
    float currentDegree = 0.0f;
    private boolean finePermission;
    WifiManager mWifiManager;
    BroadcastReceiver mWifiReceiver;
    Long startTime;
    Long timeDifference;
    DatabaseHelper myDb;
    Long lastPosID;
    Integer nrOfScans;


    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        myDb = new DatabaseHelper(this);

        btnMainActivity = (Button) findViewById(R.id.button_ToMainActivity);
        btnGetWifiInfo = (Button) findViewById(R.id.button_GetWifiInfo);
        textViewCompass = findViewById(R.id.textView_CompassDegrees);
        textViewCompass2 = findViewById(R.id.textView_CompassDegrees2);
        textViewCompass3 = findViewById(R.id.textView_CompassDegrees3);
        imgViewCompass = findViewById(R.id.imageView_Compass);
        textWifiInfo = findViewById(R.id.textView_wifiInfo);
        textWifiNr = findViewById(R.id.textView_wifiNr);
        editCoordX = (EditText) findViewById(R.id.editText_CoordX);
        editCoordY = (EditText) findViewById(R.id.editText_CoordY);
        editLevel = (EditText) findViewById(R.id.editText_Level);
        editOrientation = (EditText) findViewById(R.id.editText_Orientation);
        editCluster = (EditText) findViewById(R.id.editText_Cluster);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new BroadcastReceiver() {

            HashSet<SignalStr> capturedSigSet = new HashSet<>();

            @Override
            public void onReceive(Context c, Intent intent) {
                capturedSigSet.addAll(getScanResultInfo());
                if(startTime != null) {
                    timeDifference = SystemClock.elapsedRealtime() - startTime;
                    textWifiInfo.setText("Seconds elapsed: "+Double.toString(timeDifference /1000.0));
                    nrOfScans++;
                }
                if (nrOfScans < 10){
                    mWifiManager.startScan();
                }
                else{
                    nrOfScans = 0;
                    //2 new method?
                    HashSet<String> macAddressSet = new HashSet<>();

                    StringBuffer buffer = new StringBuffer();
                    for (SignalStr s : capturedSigSet) {
                        macAddressSet.add(s.Router_Address);
                        s.Pos_ID =lastPosID;

                        buffer.append("POS_ID :" + s.Pos_ID  + "\n");
                        buffer.append("Router_address :" + s.Router_Address + "\n");
                        buffer.append("Signal Str :" +  s.SignalStrength + "\n\n");
                    }
                    showMessage("Captured Data", buffer.toString());

                    myDb.insertRouterData(macAddressSet);
                    myDb.insertSignalStrData(capturedSigSet);
                }
            }
        };


        getWifiInfo();
        toMainActivity();

    }

    @Override
    protected void onResume() {
        super.onResume();

        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        registerReceiver(mWifiReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        unregisterReceiver(mWifiReceiver);
    }

    // WIFI ////////////

    public void getWifiInfo(){
        btnGetWifiInfo.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finePermission = false;
                        checkPermissions();
                        if ( finePermission == true){
                            nrOfScans = 0;
                            Position p = new Position(
                                    Double.parseDouble(editCoordX.getText().toString()),
                                    Double.parseDouble(editCoordY.getText().toString()),
                                    Integer.parseInt(editLevel.getText().toString()),
                                    Integer.parseInt(editOrientation.getText().toString()),
                                    editCluster.getText().toString());
                            lastPosID = myDb.insertPosData(p);
                            if (lastPosID >= 0){
                                Toast.makeText(SensorActivity.this, "Position Inserted , lastId: "+lastPosID, Toast.LENGTH_LONG).show();

                                startTime = SystemClock.elapsedRealtime();
                                ((WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE)).startScan();
                                mWifiManager.startScan();

                            }

                            else
                                Toast.makeText(SensorActivity.this, "Position not Inserted", Toast.LENGTH_LONG).show();
                        }
                        else {
                            Log.d("WIFI","### Missing Permissions: "+finePermission);
                        }
                    }
                }
        );
    }

    public HashSet<SignalStr> getScanResultInfo(){
        int level;
        HashSet<SignalStr> retList = new HashSet<>();
        //textWifiInfo.setText("");
        List<ScanResult> wifiScanList = mWifiManager.getScanResults();
        Log.d("WIFI","initializat ScanResult List: "+ wifiScanList.size());
        textWifiNr.setText("Nr of detected APs: "+ wifiScanList.size());
        for (ScanResult scanResult : wifiScanList) {
            SignalStr sigStr = new SignalStr();
            sigStr.Router_Address = scanResult.BSSID;
            sigStr.SignalStrength = scanResult.level;


            //sigStr.Pos_ID /////////// De adaugat


            retList.add(sigStr);
            level = WifiManager.calculateSignalLevel(scanResult.level, 5);
            Log.d("WIFI","Level is " + level + " out of 5 " + scanResult.level + " on " + scanResult.BSSID + "  ");
            //textWifiInfo.append(scanResult.SSID  +" "+ scanResult.BSSID+" "+ scanResult.level+"\n\n");
        }

        return retList;
    }

    public void checkPermissions(){
        try {
            //Fine Location
            if (ContextCompat.checkSelfPermission(SensorActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
                //Permission Not Granted
                Log.d("WIFI","### Requesting Permission Fine Location");
                ActivityCompat.requestPermissions(SensorActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
            else{
                Log.d("WIFI","### Fine Location Permission already granted ");
                finePermission = true;
            }
        } catch (Exception e){
            Log.d("WIFI","### EXCEPTIE getWifiInfo: "+ e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            //FineLocation
            case 1:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted keep going status
                    Log.d("RequestPermission", "FineLocation PERMISSION GRANTED");
                    finePermission = true;
                }
                else {
                    Log.d("RequestPermission", "FineLocation PERMISSION DENIED");
                    finePermission = false;
                    Toast.makeText(SensorActivity.this, "FineLocation PERMISSION DENIED", Toast.LENGTH_LONG).show();
                }
        }
    }

    public static String getMacAddrBytes() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    // SENSOR ///////////////////

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            // Log.i("OrientationTestActivity", String.format("Orientation: %f, %f, %f",mOrientation[0], mOrientation[1], mOrientation[2]));


            //Fac lucruri cu orientarea
            float degree = Math.round(Math.toDegrees((double)mOrientation[0]));
            RotateAnimation ra = new RotateAnimation(
                    currentDegree,
                    -degree,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);
            ra.setDuration(200);
            ra.setFillAfter(true);
            imgViewCompass.startAnimation(ra);
            currentDegree = -degree;

            textViewCompass.setText("Azimuth: "+ Integer.toString((int)Math.toDegrees((double)mOrientation[0])));
            textViewCompass2.setText("Pitch: "+Integer.toString((int)Math.toDegrees((double)mOrientation[1])));
            textViewCompass3.setText("Roll: "+Integer.toString((int)Math.toDegrees((double)mOrientation[2])));

            editOrientation.setText(Integer.toString((int)Math.toDegrees((double)mOrientation[0])));
        }
    }

    public void showMessage(String title, String Message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }


    private void toMainActivity(){

        btnMainActivity.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            }
        );
    }


}





