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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

public class LocatingActivity extends AppCompatActivity implements SensorEventListener {

    Button btnMainActivity;
    Button btnSearchkNN;
    Button btnStartScan;
    Button btnViewCapturedData;
    TextView textViewCompass;
    TextView textViewCompass2;
    TextView textViewCompass3;
    TextView textWifiInfo;
    TextView textWifiNr;
    ImageView imgViewCompass;
    EditText editCoordX, editCoordY,editOrientation,editCluster;

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
    Position lastPos;
    Integer nrOfScans;
    StringBuffer capturedDatabuffer;
    StringBuffer exportedDataBuffer;
    HashSet<Measurement> capturedMeasurementSet;



    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locating);

        myDb = new DatabaseHelper(this);

        btnMainActivity = (Button) findViewById(R.id.button_ToMainActivity2);
        btnSearchkNN = (Button) findViewById(R.id.button_SearchKNN);
        btnStartScan = findViewById(R.id.button_scanMeasurements);
        btnViewCapturedData = findViewById(R.id.button_viewCapturedData);
        textViewCompass = findViewById(R.id.textView_CompassDegrees);
        textViewCompass2 = findViewById(R.id.textView_CompassDegrees2);
        textViewCompass3 = findViewById(R.id.textView_CompassDegrees3);
        imgViewCompass = findViewById(R.id.imageView_Compass);
        textWifiInfo = findViewById(R.id.textView_wifiInfo);
        textWifiNr = findViewById(R.id.textView_wifiNr);
        editCoordX = (EditText) findViewById(R.id.editText_coordX2);
        editCoordY = (EditText) findViewById(R.id.editText_CoordY2);
        editOrientation = (EditText) findViewById(R.id.editText_Orientation2);
        editCluster = (EditText) findViewById(R.id.editText_Cluster2);

        capturedMeasurementSet = new HashSet<>();

        capturedDatabuffer = new StringBuffer();
        capturedDatabuffer.append("N/A");
        exportedDataBuffer = new StringBuffer();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {

                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    if (startTime != null && nrOfScans != null) {
                        capturedMeasurementSet.addAll(getScanResultInfo());
                        timeDifference = SystemClock.elapsedRealtime() - startTime;
                        textWifiInfo.setText("Seconds elapsed: " + Double.toString(timeDifference / 1000.0));
                        nrOfScans++;
                        if (nrOfScans < 3) {
                            mWifiManager.startScan();
                        } else {
                            startTime = null;
                            // new method?
                            handleEndOfScanning();
                        }
                    }
                }
            }
        };


        algorithmKNN();
        startScan();
        viewData();
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






    public void algorithmKNN(){
        btnSearchkNN.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Boolean isStill = true;
                        //todo recognition  ActivityRecognitionClient de detectat daca Still / Not Walking
                        if (isStill == true) {
                            LinkedHashMap<Position, BigDecimal> estimatedPos;
                            StringBuffer strBuffer = new StringBuffer();
                            List<String> stringsToWrite = new ArrayList<>();

                            Integer k=3;
                            Integer degreeNo=4;
                            Integer liveMeasurements = 3; // TODO: SCHIMBAT DE CATE ORI SCANEZ
                            Integer trainingMeasurements = 100;
                            Integer apSize=10;

                            Integer degree = Algorithms.radiansToRounded90Degrees(mOrientation[0]);

                            estimatedPos = Algorithms.kNN(capturedMeasurementSet, degree, "Acasa", myDb,degreeNo,k,trainingMeasurements,apSize);
                            if (estimatedPos != null) {

                                String s = "cluster=" + editCluster.getText().toString() +
                                        ";pos=" + editCoordX.getText().toString() + "," + editCoordY.getText().toString() +
                                        ";degree=" + editOrientation.getText().toString() +
                                        ";degreeNo="+degreeNo+
                                        ";neighbours="+k+
                                        ";liveMeasurements="+liveMeasurements +
                                        ";trainingMeasurements"+trainingMeasurements+
                                        ";apSize"+apSize;

                                for (LinkedHashMap.Entry<Position, BigDecimal> entry : estimatedPos.entrySet()) {
                                    strBuffer.append("Pos: " + entry.getKey().toString() + "\n Probability: " + entry.getValue().toString());
                                    s= s+";expectedPos="+entry.getKey().CoordX+","+entry.getKey().CoordY+";weight="+entry.getValue().toString();
                                }

                                stringsToWrite.add(s+"\r\n");
                                FileHelper.writeFile(stringsToWrite, "dateKNNresults.txt", getApplicationContext(),2);
                                showMessage("kNN Result", strBuffer.toString());

                            }
                            Log.d("LocatingAct", "Back in locationAct from kNN");

                        }
                    }
                }
        );
    }

    public void startWifiScanning(){
        if(checkForIncompletedTexts()){
            Toast.makeText(LocatingActivity.this, "All position fields must be completed", Toast.LENGTH_LONG).show();
            return;
        }
        else {
            finePermission = false;
            checkPermissions();
            if (finePermission == true) {
                nrOfScans = 0;
                startTime = SystemClock.elapsedRealtime();
                capturedMeasurementSet = new HashSet<>();
                ((WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE)).startScan();
                mWifiManager.startScan();
            } else {
                Log.d("WIFI", "### Missing Permissions: " + finePermission);
            }
        }

    }

    public void handleEndOfScanning(){
        HashSet<String> macAddressSet = new HashSet<>();

        capturedDatabuffer = new StringBuffer();
        capturedDatabuffer.append("Captured Measurments: \n\n ");
        for (Measurement s : capturedMeasurementSet) {
            macAddressSet.add(s.BSSID);
            capturedDatabuffer.append("BSSID :" + s.BSSID + "\n");
            capturedDatabuffer.append("Signal Str :" + s.SignalStrength + "\n\n");

        }
        showMessage("Captured Data", capturedDatabuffer.toString());
    }

    public HashSet<Measurement> getScanResultInfo(){
        int level;
        HashSet<Measurement> retList = new HashSet<>();
        //textWifiInfo.setText("");
        List<ScanResult> wifiScanList = mWifiManager.getScanResults();
        Log.d("WIFI","initializat ScanResult List: "+ wifiScanList.size());
        textWifiNr.setText("Nr of detected APs: "+ wifiScanList.size());
        for (ScanResult scanResult : wifiScanList) {
            Measurement measurement = new Measurement();
            measurement.BSSID = scanResult.BSSID;
            measurement.SignalStrength = scanResult.level;


            //measurement.Pos_ID /////////// De adaugat


            retList.add(measurement);
            level = WifiManager.calculateSignalLevel(scanResult.level, 5);
            Log.d("WIFI","Level is " + level + " out of 5 " + scanResult.level + " on " + scanResult.BSSID + "  ");
            //textWifiInfo.append(scanResult.SSID  +" "+ scanResult.BSSID+" "+ scanResult.level+"\n\n");
        }

        return retList;
    }

    public void checkPermissions(){
        try {
            //Fine Location
            if (ContextCompat.checkSelfPermission(LocatingActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
                //Permission Not Granted
                Log.d("WIFI","### Requesting Permission Fine Location");
                ActivityCompat.requestPermissions(LocatingActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
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
                    Toast.makeText(LocatingActivity.this, "FineLocation PERMISSION DENIED", Toast.LENGTH_LONG).show();
                }
        }
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

            //textViewCompass.setText("Azimuth: "+ Integer.toString((int)(Math.toDegrees((double)mOrientation[0]))));
            Integer degreeToShow;
            degreeToShow = Algorithms.radiansToRounded90Degrees(mOrientation[0]);
            textViewCompass.setText("Azimuth: "+ Integer.toString(degreeToShow));
        }
    }

    public void startScan(){
        btnStartScan.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        capturedMeasurementSet = new HashSet<>();
                        startWifiScanning();

                    }
                }
        );
    }
    public void viewData(){
        btnViewCapturedData.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMessage("Captured Data", capturedDatabuffer.toString());
                    }
                }
        );
    }

    public boolean checkForIncompletedTexts (){
        boolean res = editCoordX.getText().toString() == null || editCoordY.getText().toString() == null || editOrientation.getText().toString() == null || editCluster.getText().toString() == null ||
                editCoordX.getText().toString().compareTo("")==0 || editCoordY.getText().toString().compareTo("")==0 || editOrientation.getText().toString().compareTo("")==0 || editCluster.getText().toString().compareTo("")==0;
        Log.d("Locating","checkForCompletedTexts result: "+res);
        return res;
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
