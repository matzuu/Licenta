package com.example.bogdan.licenta;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
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

import java.util.HashSet;
import java.util.List;

public class LocatingActivity extends AppCompatActivity implements SensorEventListener {

    Button btnMainActivity;
    Button btnSearchED;
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
    Position lastPos;
    Integer nrOfScans;
    HashSet<SignalStr> capturedSigSet = new HashSet<>();


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
        btnSearchED = (Button) findViewById(R.id.button_SearchED);
        textViewCompass = findViewById(R.id.textView_CompassDegrees);
        textViewCompass2 = findViewById(R.id.textView_CompassDegrees2);
        textViewCompass3 = findViewById(R.id.textView_CompassDegrees3);
        imgViewCompass = findViewById(R.id.imageView_Compass);
        textWifiInfo = findViewById(R.id.textView_wifiInfo);
        textWifiNr = findViewById(R.id.textView_wifiNr);

        capturedSigSet = new HashSet<>();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new BroadcastReceiver() {



            @Override
            public void onReceive(Context c, Intent intent) {

                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    capturedSigSet.addAll(getScanResultInfo());
                    if (startTime != null) {
                        timeDifference = SystemClock.elapsedRealtime() - startTime;
                        textWifiInfo.setText("Seconds elapsed: " + Double.toString(timeDifference / 1000.0));
                        nrOfScans++;
                    }
                    if (nrOfScans != null) {
                        if (nrOfScans < 10) {
                            mWifiManager.startScan();
                        } else {
                            nrOfScans = 0;
                            // new method?
                            HashSet<String> macAddressSet = new HashSet<>();

                            StringBuffer buffer = new StringBuffer();
                            for (SignalStr s : capturedSigSet) {
                                macAddressSet.add(s.BSSID);
                                /*
                                s.ref_CoordX = lastPos.CoordX;
                                s.ref_CoordY = lastPos.CoordY;
                                s.ref_Orientation = lastPos.Orientation;
                                s.ref_Cluster = lastPos.Cluster;

                                buffer.append("POS_KEY :" + s.ref_CoordX + " " + s.ref_CoordY + " " + s.ref_Orientation + " " + s.ref_Cluster + " " + "\n");
                                */
                                buffer.append("BSSID :" + s.BSSID + "\n");
                                buffer.append("Signal Str :" + s.SignalStrength + "\n\n");
                            }
                            showMessage("Captured Data", buffer.toString());


                        }
                    }
                }
            }
        };


        algorithmED();
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

    public void algorithmED(){
        btnSearchED.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        initializeWifiScanner();
                        Boolean isStill = true;
                        //todo recognition  ActivityRecognitionClient cu Still
                        if (isStill == true){
                            Position pos;

                            double degree = ((int)(Math.toDegrees((double)mOrientation[0])+ 22.5)/45)*45;
                            pos = Algorithms.algEuclideanDistance(capturedSigSet,(int)degree,getApplicationContext(),myDb);
                        }

                    }
                }
        );
    }

    public void initializeWifiScanner(){
        finePermission = false;
        checkPermissions();
        if ( finePermission == true){
            nrOfScans = 0;
            startTime = SystemClock.elapsedRealtime();

            ((WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE)).startScan();
            mWifiManager.startScan();
        }
        else {
            Log.d("WIFI","### Missing Permissions: "+finePermission);
        }

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
            sigStr.BSSID = scanResult.BSSID;
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
            double degreeToShow;
            degreeToShow = Math.toDegrees((double)mOrientation[0]); //raw
            degreeToShow = ((int)(degreeToShow + 22.5)/45)*45;//Impartit pe N , NV , V , SV...
            textViewCompass.setText("Azimuth: "+ Integer.toString((int)degreeToShow));
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
