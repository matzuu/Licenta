package com.example.bogdan.licenta;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.WIFI_SERVICE;

public class FragmentMap extends Fragment implements SensorEventListener {
    private static final String TAG = "FragmentMap";


    private Button btnNavFragSearch;
    private Button btnNavSecondActivity;
    private TextView textTitle;
    private ImageView imgView_map;

    private IMainActivity mIMainActivity;
    private String mIncomingMessage = "";
    private DatabaseHelper myDb;
    private Cluster myCl;


    TextView textViewCompass;
    TextView textWifiInfo;
    TextView textWifiNr;
    TextView textViewAlgorithmfeedback;
    EditText editCoordX, editCoordY, editOrientation, editCluster;

    private SensorManager mSensorManager;
    Sensor mAccelerometer;
    Sensor mMagnetometer;
    float currentDegree = 0.0f;
    private boolean finePermission;
    WifiManager mWifiManager;
    BroadcastReceiver mWifiReceiver;
    StringBuffer capturedDatabuffer;
    HashSet<Measurement> capturedMeasurementSet;
    ArrayList<HashSet<Measurement>> liveMeasurementSet;
    Integer nrOfScans;
    Integer liveMeasurementsTotalSize = 3;

    String kNNposResult;


    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myDb = new DatabaseHelper(getContext());

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mIncomingMessage = bundle.getString(getString(R.string.intent_message));
        }


        capturedMeasurementSet = new HashSet<>();
        liveMeasurementSet = new ArrayList<>();

        capturedDatabuffer = new StringBuffer();
        capturedDatabuffer.append("N/A");

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mWifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        mWifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {

                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    if (nrOfScans != null && capturedMeasurementSet != null && liveMeasurementSet != null) {
                        capturedMeasurementSet.addAll(getScanResultInfo());

                        liveMeasurementSet.add(capturedMeasurementSet);
                        nrOfScans++;

                        Log.d("WIFI", "Scanned once");

                        if (nrOfScans < liveMeasurementsTotalSize) {
                            capturedMeasurementSet = new HashSet<>();
                            mWifiManager.startScan();
                        } else {
                            Log.d("WIFI", "Finished Scanning");
                            handleEndOfScanning();
                            algorithmKNN();
                        }
                    }
                }
            }
        }; //END BROADCAST RECEIVER

        //algorithmKNN();

    }

    //FRAGMENT thingies

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);


        //btnNavFragSearch = (Button) view.findViewById(R.id.btnNavFragSearch);
        textTitle = view.findViewById(R.id.textView_Title);
        imgView_map = view.findViewById(R.id.imageView_map);


        if (!mIncomingMessage.equals(""))
            handleReceivedCluster(mIncomingMessage);


        Log.d("WIFI", "onCreateView FragS: started.");
        //String value = getArguments().getString("Key");

        //Log.d("FRAGMENTMAP","Am primit: "+value);


        startWifiScanning();
        return view;
    }

    @Override
    public void onViewCreated(View v,Bundle savedInstanceState) {
        super.onViewCreated(v,savedInstanceState);


        if (myCl.clusterImageUrl != null) {

            try {

                int imgID;
                imgID = Integer.parseInt(myCl.clusterImageUrl);
                imgView_map.setImageResource(imgID);
                //Drawable d = getContext().getResources().getDrawable(imgID);
                //this.getView().setBackground(d);                              cand foloseam backgroud inloc de imageView



            } catch (Exception e) {
                Log.d("FRAGMAP", "Background error: " + e);
            }
        } else {
            //this.getView().setBackgroundResource(R.drawable.questionmark); cand foloseam backgroud inloc de imageView
        }
    }


    public void handleReceivedCluster(String mIncomingMessage)
    {
        textTitle.setText(mIncomingMessage);

        Cursor res = myDb.queryCluster(mIncomingMessage);
        if (res == null || res.getCount() == 0) {
            // show message
            Toast.makeText((MainActivity)getActivity(), "Error querrying Cluster: "+ mIncomingMessage,Toast.LENGTH_LONG).show();
            return;
        }
        Log.d("FRAGMAP","Res.getcount()= "+res.getCount());


        while (res.moveToNext()) {
            myCl = new Cluster(
                    res.getString(res.getColumnIndex("clusterName")),
                    res.getString(res.getColumnIndex("clusterType")),
                    res.getString(res.getColumnIndex("clusterImageUrl")),
                    res.getInt(res.getColumnIndex("startPixX")),
                    res.getInt(res.getColumnIndex("startPixY")),
                    res.getDouble(res.getColumnIndex("distancePx")));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIMainActivity = (MainActivity) getActivity();
    }


    //LOCALIZARE

    public void algorithmKNN(){

        //todo recognition  ActivityRecognitionClient de detectat daca este in miscare/stationar
        Boolean isStill = true;
        if (isStill == true ) {
            //todo sters steagul? getActivity().
            Log.d("THREAD","Starting new thread");
            new FragmentMap.threadKNN().execute(textTitle.getText().toString());
        }
    }

    private class threadKNN extends AsyncTask< String,String,String> {

        protected String doInBackground(String... params) {

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d("kNN", "Created kNN Thread");
            publishProgress("Started");



            Integer k = 5; //don: default 5
            Integer degreeNo = 4; //don: default 4
            Integer liveScans = 5; //don: default 3
            Integer trainingScans = 30; //don default 20
            Integer apSize = 6; //default 6
            Integer degree; //don
            //degree = Algorithms.radiansToRounded90Degrees(mOrientation[0]);
            //degree = Integer.parseInt(editOrientation.getText().toString());

            Log.d("kNN","k="+k+";degreeNo="+degreeNo+";liveMM="+liveScans+";trainMM="+trainingScans+";apSize="+apSize);
            LinkedHashSet<Measurement> offlineScanSet = null;
            publishProgress("Reading");
            if (params[0].compareTo("crawDad")==0){
                offlineScanSet = FileHelper.getOfflineScans(params[0], getContext());
            }

            ArrayList<HashSet<Measurement>> onlineScanList;
            if( liveMeasurementSet != null )
                onlineScanList = liveMeasurementSet;
            else {

                return null;}

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d("kNN", "onlineScanList.size(): " + onlineScanList.size());
            if (onlineScanList != null) {

                HashSet<Measurement> combinedHashSet = new HashSet<>();
                Position[] estimatedPos = null;
                Measurement auxMeasurement;
                int contorTotal = 0;

                    //iau si aplic algoritmul pe *liveMeasurments* din 10, astfel incat sa aplic pe aceleasi masuratori pentru parametrii diferiti
                for (HashSet<Measurement> hs : onlineScanList) {

                    combinedHashSet.addAll(hs);

                    Log.d("kNN", "combinedHashSet.size(): " + combinedHashSet.size()+"\n contorT: "+contorTotal);


                    publishProgress("Working: Calculating kNN");
                    if (params[0].compareTo("crawDad")==0) {
                        estimatedPos = Algorithms.kNN2(offlineScanSet,combinedHashSet,params[0], degreeNo, k, trainingScans, apSize);
                    } else {
                        estimatedPos = Algorithms.kNN(combinedHashSet, params[0], myDb, degreeNo, k, trainingScans, apSize);
                    }
                    publishProgress("Working: Getting next scan");

                    if (estimatedPos != null) {
                        Log.d("kNN", "estimatedPos " + estimatedPos[0].CoordX+","+estimatedPos[0].CoordY + " W: " + estimatedPos[1].CoordX+","+estimatedPos[1].CoordY);

                        publishProgress(estimatedPos[1].CoordX+";"+estimatedPos[1].CoordY);
                        return estimatedPos[1].CoordX+";"+estimatedPos[1].CoordY+";";

                    } else {Log.d("kNN","estimatedPos is NULL!");}
                    {
                        publishProgress("");
                    }
                }
            }


            return null;
        }
        protected void onProgressUpdate(String... progress){
            //////textViewAlgorithmfeedback.setText(progress[0]);
            kNNposResult = progress[0];

        }
        protected void onPostExecute(String result) {
            startWifiScanning();
            handleEndOfKnn(result);
            //showMessage("","Finished!");
        }
    }

    private void handleEndOfKnn(String result) {

        if (result != null || result.equals("")==false){
            //todo posX si posY tot timpul 0 nu citeste patternul
            double posX = 0;
            double posY = 0;
            String aux;
            Matcher mPos = Pattern.compile("-?\\d*\\.\\d*;").matcher(result);
            if (mPos.find()) {
                aux = mPos.group();
                aux = aux.substring(0, aux.length() - 1);
                //Log.d("parseString", "INCERC COORDX " + aux);
                posX = Double.parseDouble(aux);
            }
            if (mPos.find()) {
                aux = mPos.group();
                aux = aux.substring(0, aux.length() - 1);
                posY = Double.parseDouble(aux);
            }

            Log.d("handleEndOfKnn", " estimated positions: "+posX +" "+posY);

            //Drawable myDrawable = imgView_map.getDrawable(); // asa iau poza din image view //
            int imgID = Integer.parseInt(myCl.clusterImageUrl);

            Drawable originalImage = getResources().getDrawable(imgID);



            Drawable imageToChange = getResources().getDrawable(R.drawable.map_todisplay);


            int width = 25;
            int height = 25;
            int[] intarray = new int[width*height];
            for (int y = 0; y< height; y++){
                int outputOffset = y * width;
                for (int x= 0; x<width; x++){

                    intarray[outputOffset + x] = Color.RED;
                }

            }
            int bitmapX = (int)Math.round(myCl.startPixX + (myCl.distancePx * posX));
            int bitmapY = (int)Math.round(myCl.startPixY + (myCl.distancePx * posY));


            Bitmap myMap = ((BitmapDrawable) ResourcesCompat.getDrawable(getContext().getResources(), imgID, null)).getBitmap();

            myMap = myMap.copy( Bitmap.Config.ARGB_8888 , true);
            //probabil nu e mutabila
            myMap.setPixels(intarray,0,width,bitmapX,bitmapY,width,height); //crash!


            imgView_map.setImageBitmap(myMap);
        }


    }

    public void startWifiScanning(){

        Log.d("WIFI","StartingWfifScannig");
        Toast.makeText(getContext(),"Starting wifi scan Nr: "+ liveMeasurementsTotalSize,Toast.LENGTH_SHORT);

        finePermission = false;
            checkPermissions();
            if (finePermission == true) {
                nrOfScans = 0;

                liveMeasurementSet = new ArrayList<>();
                capturedMeasurementSet = new HashSet<>();

                //boolean check1 =((WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE)).startScan();
                boolean check = mWifiManager.startScan();

                Log.d("WIFI","am trecut de startScan");
            } else {
                Log.d("WIFI", "### Missing Permissions: " + finePermission);
            }
    }

    public void handleEndOfScanning(){
        HashSet<String> macAddressSet = new HashSet<>();
        liveMeasurementSet.add(capturedMeasurementSet);

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
        for (ScanResult scanResult : wifiScanList) {
            Measurement measurement = new Measurement();
            measurement.BSSID = scanResult.BSSID;
            measurement.SignalStrength = scanResult.level;
            measurement.ref_Orientation = (int) currentDegree;

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
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
                //Permission Not Granted
                Log.d("WIFI","### Requesting Permission Fine Location");
                ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
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
                    Toast.makeText(getContext(), "FineLocation PERMISSION DENIED", Toast.LENGTH_LONG).show();
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
            currentDegree = -degree;

            //textViewCompass.setText("Azimuth: "+ Integer.toString((int)(Math.toDegrees((double)mOrientation[0]))));
            //Integer degreeToShow;
            //degreeToShow = Algorithms.radiansToRounded90Degrees(mOrientation[0]);
            //textViewCompass.setText("Azimuth: "+ Integer.toString(degreeToShow));
        }
    }

    public void viewData(){

        showMessage("Captured Data", capturedDatabuffer.toString());

    }

    public void showMessage(String title, String Message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        getActivity().registerReceiver(mWifiReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        getActivity().unregisterReceiver(mWifiReceiver);
    }







}
