package com.example.bogdan.licenta;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class RegisterActivity extends AppCompatActivity implements SensorEventListener {

    Button btnMainActivity;
    Button btnStartScanning;
    Button btnDeletePos;
    Button btnViewMeasurements;
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
    StringBuffer capturedDatabuffer;
    HashSet<Measurement> capturedMeasurementSet;


    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        myDb = new DatabaseHelper(this);

        btnMainActivity = (Button) findViewById(R.id.button_ToMainActivity);
        btnStartScanning = (Button) findViewById(R.id.button_startScanning);
        btnDeletePos = findViewById(R.id.button_deletePos);
        btnViewMeasurements = findViewById(R.id.button_viewMeasurementsAtPos);
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

        capturedDatabuffer = new StringBuffer();
        capturedDatabuffer.append("N/A");
        capturedMeasurementSet = new HashSet<>();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);



        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new BroadcastReceiver() {



            @Override
            public void onReceive(Context c, Intent intent) {

                if(startTime != null && nrOfScans!= null && lastPos != null) {
                    capturedMeasurementSet.addAll(getScanResultInfo());
                    timeDifference = SystemClock.elapsedRealtime() - startTime;
                    textWifiInfo.setText("Seconds elapsed: "+Double.toString(timeDifference /1000.0));
                    nrOfScans++;

                    if (nrOfScans <= 100) {
                        mWifiManager.startScan();
                    } else {
                        nrOfScans = null;
                        startTime = null;
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        if (lastPos!= null && lastPos.CoordX != null && lastPos.CoordY!=null && lastPos.Orientation != null && lastPos.Cluster != null){
                            handleEndOfScanning();
                        }
                    }
                }
            }
        };


        startWifiScan();
        toMainActivity();
        deletePos();
        viewMeasurementsOfPos();

    }

    @Override
    protected void onResume() {
        super.onResume();

        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        registerReceiver(mWifiReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        unregisterReceiver(mWifiReceiver);
    }
    // ASYNC TASK ///////////////////

    private class InsertDataTask extends AsyncTask<HashSet<Measurement>,String,List<String>> {

        protected List<String> doInBackground(HashSet<Measurement>... vMeasurements) {

            int count = vMeasurements.length;
            List<String> resultStr = new ArrayList<>();
            int contor;
            for (int i = 0; i < count; i++) {
                contor = 0;
                capturedMeasurementSet = vMeasurements[i];
                HashSet<String> macAddressSet = new HashSet<>();
                for (Measurement m : capturedMeasurementSet) {
                    macAddressSet.add(m.BSSID);
                    m.ref_CoordX = lastPos.CoordX;
                    m.ref_CoordY = lastPos.CoordY;
                    m.ref_Orientation = lastPos.Orientation;
                    m.ref_Cluster = lastPos.Cluster;

                    resultStr.add("POS_KEY :" +
                            m.ref_CoordX + " " +
                            m.ref_CoordY + " " +
                            m.ref_Orientation + " " +
                            m.ref_Cluster + " \n" +
                            "BSSID :" + m.BSSID + "\n " +
                            "Signal Str :" + m.SignalStrength + "\n\n");

                    contor++;
                    publishProgress("Measurement " + contor + " / " + capturedMeasurementSet.size());

                }
                myDb.insertRouterData(macAddressSet);
                myDb.insertMeasurementData(capturedMeasurementSet);
            }
            return resultStr;
        }
        protected void onProgressUpdate(String... progress){
            Log.d("Thread",progress[0]);
        }
        protected void onPostExecute(List<String> result) {
            StringBuffer buffer = new StringBuffer();
            for(String s:result){
                buffer.append(s);
            }
            showMessage("Captured Data: " + result.size(), buffer.toString());

        }
    }

    // WIFI ////////////
    public void handleEndOfScanning() {

        new InsertDataTask().execute(capturedMeasurementSet);


    }

    public void startWifiScan(){
        btnStartScanning.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(editCoordX.getText().toString() == null || editCoordY.getText().toString() == null || editLevel.getText().toString() == null || editOrientation.getText().toString() == null){
                            Toast.makeText(RegisterActivity.this, "All position fields must be completed", Toast.LENGTH_LONG).show();
                            return;
                        }
                        finePermission = false;
                        checkPermissions();
                        if ( finePermission == true){
                            nrOfScans = 0;
                            lastPos = new Position(
                                    Double.parseDouble(editCoordX.getText().toString()),
                                    Double.parseDouble(editCoordY.getText().toString()),
                                    Integer.parseInt(editLevel.getText().toString()),
                                    Integer.parseInt(editOrientation.getText().toString()),
                                    editCluster.getText().toString());
                            Long lastPosID = myDb.insertPosData(lastPos);

                            if (lastPosID >= 0){
                                Toast.makeText(RegisterActivity.this, "Position Inserted , lastId: "+lastPosID, Toast.LENGTH_LONG).show();
                                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                startTime = SystemClock.elapsedRealtime();
                                ((WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE)).startScan();
                                mWifiManager.startScan();

                                }

                            else {
                                Cursor res = myDb.queryPosition(lastPos);
                                if (res.getCount() == 0)
                                    Toast.makeText(RegisterActivity.this, "Position not Inserted", Toast.LENGTH_LONG).show();

                                else {

                                    Integer colIndex = res.getColumnIndex("rowid");
                                    if (res.moveToFirst()) {
                                        lastPosID =(long)res.getInt(colIndex);
                                        Toast.makeText(RegisterActivity.this, "Position already Inserted", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Position not found", Toast.LENGTH_LONG).show();
                                    }
                                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                    startTime = SystemClock.elapsedRealtime();
                                    ((WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE)).startScan();
                                    mWifiManager.startScan();
                                }
                            }
                        }
                        else {
                            Log.d("WIFI","### Missing Permissions: "+finePermission);
                        }
                    }
                }
        );
    }

    public HashSet<Measurement> getScanResultInfo(){
        int level;
        HashSet<Measurement> retList = new HashSet<>();
        //textWifiInfo.setText("");
        List<ScanResult> wifiScanList = mWifiManager.getScanResults();
        Log.d("WIFI","nrOfScans: "+nrOfScans);
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
            if (ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
                //Permission Not Granted
                Log.d("WIFI","### Requesting Permission Fine Location");
                ActivityCompat.requestPermissions(RegisterActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
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
                    Toast.makeText(RegisterActivity.this, "FineLocation PERMISSION DENIED", Toast.LENGTH_LONG).show();
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
            Integer degreeToInsert;
            degreeToInsert = Algorithms.radiansToRounded45Degrees(mOrientation[0]);
            editOrientation.setText(Integer.toString(degreeToInsert));
        }
    }

    public void deletePos(){
        btnDeletePos.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (editCoordX.getText().toString() == null || editCoordY.getText().toString() == null || editLevel.getText().toString() == null || editOrientation.getText().toString() == null) {
                            Toast.makeText(RegisterActivity.this, "All position fields must be completed", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Integer rowsDeleted = myDb.deleteMeasurementAtPosData(editCoordX.getText().toString(),editCoordY.getText().toString(),editLevel.getText().toString(),editOrientation.getText().toString() );
                        Toast.makeText(RegisterActivity.this, "Deleted "+rowsDeleted+" measurements", Toast.LENGTH_LONG).show();
                        return;
                    }
                });
    }

    public void viewMeasurementsOfPos(){
        btnViewMeasurements.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (editCoordX.getText().toString() == null || editCoordY.getText().toString() == null || editLevel.getText().toString() == null || editOrientation.getText().toString() == null) {
                            Toast.makeText(RegisterActivity.this, "All position fields must be completed", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Cursor res = myDb.queryAllMeasurementsFromPosition(editCoordX.getText().toString(),editCoordY.getText().toString(),editLevel.getText().toString(),editOrientation.getText().toString());
                        if (res == null || res.getCount() == 0) {
                            // show message
                            showMessage("Error", "Nothing found");
                            return;
                        }

                        StringBuffer buffer = new StringBuffer();
                        while (res.moveToNext()) {
                            //buffer.append("Id :" + res.getString(0) + "\n");
                            buffer.append("CoordX :" + res.getString(0) + "\n");
                            buffer.append("CoordY :" + res.getString(1) + "\n");
                            buffer.append("Orientation :" + res.getString(2) + "\n");
                            buffer.append("Cluster :" + res.getString(3) + "\n");
                            buffer.append("BSSID :" + res.getString(4) + "\n");
                            buffer.append("SigStr :" + res.getString(5) + "\n\n");
                        }

                        // Show all data
                        showMessage("Measurements: ", buffer.toString());
                    }
                }
        );


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





