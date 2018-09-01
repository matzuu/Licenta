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

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
    TextView textViewAlgorithmfeedback;
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
    Integer liveMeasurementsTotalSize;
    StringBuffer capturedDatabuffer;
    HashSet<Measurement> capturedMeasurementSet;
    List<HashSet<Measurement>> liveMeasurementSet;



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
        textViewAlgorithmfeedback = findViewById(R.id.textView_algorithmFeedback);
        editCoordX = (EditText) findViewById(R.id.editText_coordX2);
        editCoordY = (EditText) findViewById(R.id.editText_CoordY2);
        editOrientation = (EditText) findViewById(R.id.editText_Orientation2);
        editCluster = (EditText) findViewById(R.id.editText_Cluster2);

        capturedMeasurementSet = new HashSet<>();
        liveMeasurementSet = new ArrayList<>();
        liveMeasurementsTotalSize = 500;

        capturedDatabuffer = new StringBuffer();
        capturedDatabuffer.append("N/A");

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {

                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    if (startTime != null && nrOfScans != null && capturedMeasurementSet != null && liveMeasurementSet != null) {
                        capturedMeasurementSet.addAll(getScanResultInfo());

                        liveMeasurementSet.add(capturedMeasurementSet);

                        timeDifference = SystemClock.elapsedRealtime() - startTime;
                        textWifiInfo.setText("ETA: " + Double.toString(3.25 * liveMeasurementsTotalSize - (timeDifference / 1000.0)));
                        nrOfScans++;
                        //normal mode
                        /*if (nrOfScans < 3) {
                            mWifiManager.startScan();
                        } else {

                            startTime = null;
                            nrOfScans = null;
                            // new method?
                            handleEndOfScanning();
                        }*/
                        //testing mode
                        if(lastPos == null){
                            Log.d("WIFI","LASTPOS == NULL!!");
                        }
                        if(nrOfScans < liveMeasurementsTotalSize) {
                            capturedMeasurementSet = new HashSet<>();
                            mWifiManager.startScan();
                        } else {
                            startTime = null;
                            nrOfScans = null;
                            handleEndOfScanning2();
                        }
                    }
                }
            }
        }; //END BROADCAST RECEIVER

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
                    /*
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            Boolean isStill = true;
                            if (isStill == true && !checkForIncompletedTexts() ) {
                                Log.d("kNN", "Created kNN Thread");
                                textViewAlgorithmfeedback.setText("Started");
                                Integer k = 3;
                                Integer degreeNo = 4; //don
                                Integer liveMeasurements = 4; //don
                                Integer trainingMeasurements = 100; //don
                                Integer apSize = 6; //don
                                Integer degree; //don
                                //degree = Algorithms.radiansToRounded90Degrees(mOrientation[0]);
                                //degree = Integer.parseInt(editOrientation.getText().toString());
                                Log.d("kNN","k="+k+";degreeNo="+degreeNo+";liveMM="+liveMeasurements+";trainMM="+trainingMeasurements+";apSize="+apSize);
                                List<HashSet<Measurement>> onlineScanList = FileHelper.getOnlineScans("android", LocatingActivity.this);
                                Log.d("kNN", "onlineScanList.size(): " + onlineScanList.size());
                                if (onlineScanList != null) {

                                    ArrayList<String> stringToWrite = new ArrayList<>();
                                    Integer contorLiveMM = 0;
                                    HashSet<Measurement> combinedHashSet = new HashSet<>();
                                    LinkedHashMap<Position, BigDecimal> estimatedPos;

                                    //parcurgerea listei cu parametrii X
                                    //iau si aplic algoritmul pe *liveMeasurments* din 10, astfel incat sa aplic pe aceleasi masuratori pentru parametrii diferiti
                                    for (HashSet<Measurement> hs : onlineScanList) {
                                        contorLiveMM++;
                                        if (contorLiveMM > 10){ //sau maximul live scanrilor
                                            contorLiveMM = 1;

                                        }
                                        //Log.d("kNN","contorLiveMM: "+contorLiveMM);
                                        if (contorLiveMM < liveMeasurements) {
                                            combinedHashSet.addAll(hs);
                                        }
                                        if (contorLiveMM == liveMeasurements) { //pentru algoritm simulez numarul de scanari pe care l-as face.
                                            combinedHashSet.addAll(hs);
                                            Log.d("kNN", "combinedHashSet.size(): " + combinedHashSet.size());

                                            textViewAlgorithmfeedback.setText("Entered kNN: Working");
                                            estimatedPos = Algorithms.kNN(combinedHashSet,"Acasa", myDb, degreeNo, k, trainingMeasurements, apSize);
                                            Log.d("kNN","estimatedPos.size()"+ estimatedPos.size());
                                            if (estimatedPos.size()!= 0) {

                                                String s = "cluster=" + editCluster.getText().toString() +
                                                        ";pos=" + editCoordX.getText().toString() + "," + editCoordY.getText().toString() +
                                                        ";degree=" + editOrientation.getText().toString() +
                                                        ";degreeNo=" + degreeNo +
                                                        ";neighbours=" + k +
                                                        ";liveMeasurements=" + liveMeasurements +
                                                        ";trainingMeasurements=" + trainingMeasurements +
                                                        ";apSize=" + apSize;


                                                //String s = "cluster=" + lastPos.Cluster +
                                                        ";pos=" + lastPos.CoordX.toString() + "," + lastPos.CoordY.toString() +
                                                        ";degree=" + lastPos.Orientation.toString() +
                                                        ";degreeNo=" + degreeNo +
                                                        ";neighbours=" + k +
                                                        ";liveMeasurements=" + liveMeasurements +
                                                        ";trainingMeasurements=" + trainingMeasurements +
                                                        ";apSize=" + apSize;


                                                for (LinkedHashMap.Entry<Position, BigDecimal> entry : estimatedPos.entrySet()) {
                                                    s = s + ";expectedPos=" + entry.getKey().CoordX + "," + entry.getKey().CoordY + ";weight=" + entry.getValue().toString();
                                                }
                                                stringToWrite.add(s + "\r\n");


                                                combinedHashSet = new HashSet<>();
                                            }
                                        }
                                    } //end onlinescan for
                                    textViewAlgorithmfeedback.setText("Writing File");
                                    FileHelper.writeFile(stringToWrite, "dateKNNresultsTest.txt", getApplicationContext(), 2);
                                    textViewAlgorithmfeedback.setText("Done!");
                                }// end onlinescan if
                            }
                        }

                    };
                    */

                    //todo recognition  ActivityRecognitionClient de detectat daca este in miscare/stationar
                    Boolean isStill = true;
                    if (isStill == true ) {

                        Log.d("THREAD","Starting new thread");
                        new threadKNN().execute("crawDad");
                        textViewAlgorithmfeedback.setText("DONE!");

                    }



                    //Thread readingThread = new Thread(runnable);
                    //readingThread.start();
                 }
            }
        );
    }

    private class threadKNN extends AsyncTask< String,String,Void> {

        protected Void doInBackground(String... params) {


            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d("kNN", "Created kNN Thread");
            publishProgress("Started");



            Integer k = 1; //don: default 1
            Integer degreeNo = 1; //don: default 1
            Integer liveMeasurements = 1; //don: default 1
            Integer trainingMeasurements = 10; //don
            Integer apSize = 2; //don
            Integer degree; //don
            //degree = Algorithms.radiansToRounded90Degrees(mOrientation[0]);
            //degree = Integer.parseInt(editOrientation.getText().toString());

            Log.d("kNN","k="+k+";degreeNo="+degreeNo+";liveMM="+liveMeasurements+";trainMM="+trainingMeasurements+";apSize="+apSize);

            publishProgress("Reading");
            List<HashSet<Measurement>> onlineScanList = FileHelper.getOnlineScans(params[0], LocatingActivity.this);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d("kNN", "onlineScanList.size(): " + onlineScanList.size());
            if (onlineScanList != null) {

                ArrayList<String> stringToWrite = new ArrayList<>();
                Integer contorLiveMM = 0;
                HashSet<Measurement> combinedHashSet = new HashSet<>();
                Position[] estimatedPos;
                Measurement auxMeasurement;

                for (degreeNo = 1; degreeNo < 4 ; degreeNo++) { //facut pentru a parcurge parametrii
                    Log.d("kNN","degreeNo: "+degreeNo);
                    //parcurgerea listei cu parametrii X
                    //iau si aplic algoritmul pe *liveMeasurments* din 10, astfel incat sa aplic pe aceleasi masuratori pentru parametrii diferiti
                    for (HashSet<Measurement> hs : onlineScanList) {
                        contorLiveMM++;
                        if (contorLiveMM > 10)/*sau maximul live scanrilor*/ {
                            contorLiveMM = 1;
                            combinedHashSet = new HashSet<>();
                        }
                        //Log.d("kNN","contorLiveMM: "+contorLiveMM);
                        if (contorLiveMM < liveMeasurements) {
                            combinedHashSet.addAll(hs);
                        }
                        if (contorLiveMM == liveMeasurements) { //pentru algoritm simulez numarul de scanari pe care l-as face.
                            combinedHashSet.addAll(hs);
                            Log.d("kNN", "combinedHashSet.size(): " + combinedHashSet.size());


                            publishProgress("Working: Calculating kNN");
                            estimatedPos = Algorithms.kNN(combinedHashSet, params[0], myDb, degreeNo, k, trainingMeasurements, apSize);
                            publishProgress("Working: Getting next scan");

                            if (estimatedPos != null) {
                                Log.d("kNN", "estimatedPos " + estimatedPos[0].CoordX+","+estimatedPos[0].CoordY + " " + estimatedPos[1].CoordX+","+estimatedPos[1].CoordY);
                                auxMeasurement = hs.iterator().next();
                                String s = "cluster=" + auxMeasurement.ref_Cluster +
                                        ";pos=" + auxMeasurement.ref_CoordX.toString() + "," + auxMeasurement.ref_CoordY.toString() +
                                        ";degree=" + auxMeasurement.ref_Orientation.toString() +
                                        ";degreeNo=" + degreeNo +
                                        ";neighbours=" + k +
                                        ";liveMeasurements=" + liveMeasurements +
                                        ";trainingMeasurements=" + trainingMeasurements +
                                        ";apSize=" + apSize;

                                s += ";estimatedPositions=" + estimatedPos[0] + ";" + estimatedPos[1];

                            /*
                            for (LinkedHashMap.Entry<Position, BigDecimal> entry : estimatedPos.entrySet()) {
                                DecimalFormat decimalFormat = new DecimalFormat("###.########");
                                s = s + ";" + entry.getKey().CoordX + "," + entry.getKey().CoordY +
                                        ";" + decimalFormat.format(entry.getValue());
                            }*/


                                stringToWrite.add(s + "\r\n");



                            } else {Log.d("kNN","estimatedPos is NULL!");}


                        }
                    } //end onlinescan for
                    publishProgress("Writing File");
                    FileHelper.writeFile(stringToWrite, params[0] + "dateKNNresultsDegreeNr.txt", getApplicationContext(), 2);

                    publishProgress("Done");
                }// end onlinescan if
            }
            return null;
        }
        protected void onProgressUpdate(String... progress){
            textViewAlgorithmfeedback.setText(progress[0]);
        }
        protected void onPostExecute(Void result) {

            showMessage("","Finished!");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    public void startWifiScanning(){
        if(checkForIncompletedTexts()){
            return;
        }
        else {

            finePermission = false;
            checkPermissions();
            if (finePermission == true) {
                Toast.makeText(LocatingActivity.this,"Starting wifi scan Nr: "+ liveMeasurementsTotalSize,Toast.LENGTH_SHORT);
                nrOfScans = 0;

                lastPos = new Position(
                        Double.parseDouble(editCoordX.getText().toString()),
                        Double.parseDouble(editCoordY.getText().toString()),
                        Integer.parseInt(editOrientation.getText().toString()),
                        editCluster.getText().toString());

                startTime = SystemClock.elapsedRealtime();
                liveMeasurementSet = new ArrayList<>();
                capturedMeasurementSet = new HashSet<>();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                ((WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE)).startScan();
                mWifiManager.startScan();
            } else {
                Log.d("WIFI", "### Missing Permissions: " + finePermission);
            }
        }

    }

    public void handleEndOfScanning(){
        HashSet<String> macAddressSet = new HashSet<>();
        liveMeasurementSet.add(capturedMeasurementSet);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        capturedDatabuffer = new StringBuffer();
        capturedDatabuffer.append("Captured Measurments: \n\n ");
        for (Measurement s : capturedMeasurementSet) {
            macAddressSet.add(s.BSSID);
            capturedDatabuffer.append("BSSID :" + s.BSSID + "\n");
            capturedDatabuffer.append("Signal Str :" + s.SignalStrength + "\n\n");

        }
        showMessage("Captured Data", capturedDatabuffer.toString());

    }

    public void handleEndOfScanning2(){

        Log.d("WIFI","Entered handleEndOfScanning2");
        boolean result = FileHelper.writeLiveMeasurements(liveMeasurementSet,lastPos,"dateAndroidOnline.txt",LocatingActivity.this);
        Log.d("WIFI","Wrote successfully: "+result);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        HashSet<String> macAddressSet = new HashSet<>();
        capturedDatabuffer = new StringBuffer();
        capturedDatabuffer.append("Captured list size: "+liveMeasurementSet.size()+"\n\n ");
        for (HashSet<Measurement> hs : liveMeasurementSet){
            capturedDatabuffer.append(hs.size()+"\n");
            /*
            for (Measurement m : hs) {
                macAddressSet.add(m.BSSID);
                capturedDatabuffer.append("BSSID :" + m.BSSID + "\n");
                capturedDatabuffer.append("Signal Str :" + m.SignalStrength + "\n\n");
            }*/
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
                        textWifiInfo.setText("SCAN ETA: Calculating");
                        liveMeasurementsTotalSize = 200; // pentru fiecare locatie imi trebuie 50 de teste * masuratori live de la 1 la 10 => imi trebuie 500 pentru o locatie, repet scanarea de 10 ori
                        liveMeasurementSet = new ArrayList<>();
                        startTime = null;
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
        Log.d("Locating","checkForIncompletedTexts result: "+res);
        if (res){
            Toast.makeText(LocatingActivity.this,"Not all position fields are completed", Toast.LENGTH_SHORT);
        }
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
