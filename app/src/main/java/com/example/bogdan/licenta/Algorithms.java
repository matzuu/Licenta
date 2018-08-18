package com.example.bogdan.licenta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;

public class Algorithms {









    public static Position algEuclideanDistance1(HashSet<Measurement> currentMeasuredSet,Integer orientation,Context context,DatabaseHelper myDb,String cluster){

        if (currentMeasuredSet.size() < 1){
            return null;
        }
        Log.d("ALG","Starting Algorithm EUCLIDEAN DISTANCE");
        Position expectedPos = new Position();
        HashSet<String> macAddressSet = new HashSet<>();
        //imi iau toaate BSSID-urile din Measurment-ul facut recent.
        for (Measurement m : currentMeasuredSet) {
            macAddressSet.add(m.BSSID);
        }
        //pt fiecare BSSID iau toate masuratorile si dupa toate pozitiile facute cu acele pozitii.
        Cursor res = myDb.queryAllMeasurementsFromBSSID(macAddressSet);
        if (res.getCount() == 0) {
            // show message
            Log.d("ALG","No rows retrieved");
            return null;
        }
        SparseArray<Measurement> measurementSparseArray = new SparseArray<>();
        Measurement m;
        StringBuffer buffer = new StringBuffer();
        while (res.moveToNext()) {
            m = new Measurement();
            m.ref_CoordX = res.getDouble(1);
            m.ref_CoordY = res.getDouble(2);
            m.ref_Orientation = res.getInt(3);
            m.ref_Cluster = res.getString(4);
            m.BSSID = res.getString(5);
            m.SignalStrength = res.getInt(6);

            measurementSparseArray.put(res.getInt(0),m);

            buffer.append("ID :" + res.getInt(0) + "\n");
            buffer.append("ref_CoordX :" + m.ref_CoordX + "\n");
            buffer.append("ref_CoordY :" + m.ref_CoordY + "\n");
            buffer.append("ref_Orientation :" + m.ref_Orientation + "\n");
            buffer.append("ref_Cluster :" + m.ref_Cluster  + "\n");
            buffer.append("BSSID :" + m.BSSID + "\n");
            buffer.append("SignalStr :" + m.SignalStrength + "\n\n");
        }
        Log.d("ALG","Queried Measurement Array: \n" + buffer.toString());

        //Cursor res = myDb.queryAllPositionsFromBSSID(macAddressSet);

        Cursor res2 = myDb.queryAllPositionsAndMeasurementsFromBSSIDandCluster(macAddressSet,cluster);
        return expectedPos;
    }

    public static Position algEuclideanDistance2(HashSet<Measurement> currentMeasuredSet,Integer orientation,Context context,DatabaseHelper myDb,String cluster){

        if (currentMeasuredSet.size() < 1){
            return null;
        }
        Log.d("ALG","Starting Algorithm EUCLIDEAN DISTANCE");
        Position expectedPos = new Position();
        HashSet<String> macAddressSet = new HashSet<>();
        //imi iau toaate BSSID-urile din Measurment-ul facut recent.
        for (Measurement m : currentMeasuredSet) {
            macAddressSet.add(m.BSSID);
        }
        //pt fiecare BSSID iau toate masuratorile si dupa toate pozitiile facute cu acele pozitii.
        Cursor res = myDb.queryAllMeasurementsFromBSSID(macAddressSet);
        if (res.getCount() == 0) {
            // show message
            Log.d("ALG","No rows retrieved");
            return null;
        }
        SparseArray<Measurement> measurementSparseArray = new SparseArray<>();
        Measurement m;
        StringBuffer buffer = new StringBuffer();
        while (res.moveToNext()) {
            m = new Measurement();
            m.ref_CoordX = res.getDouble(1);
            m.ref_CoordY = res.getDouble(2);
            m.ref_Orientation = res.getInt(3);
            m.ref_Cluster = res.getString(4);
            m.BSSID = res.getString(5);
            m.SignalStrength = res.getInt(6);

            measurementSparseArray.put(res.getInt(0),m);

            buffer.append("ID :" + res.getInt(0) + "\n");
            buffer.append("ref_CoordX :" + m.ref_CoordX + "\n");
            buffer.append("ref_CoordY :" + m.ref_CoordY + "\n");
            buffer.append("ref_Orientation :" + m.ref_Orientation + "\n");
            buffer.append("ref_Cluster :" + m.ref_Cluster  + "\n");
            buffer.append("BSSID :" + m.BSSID + "\n");
            buffer.append("SignalStr :" + m.SignalStrength + "\n\n");
        }
        Log.d("ALG","Queried Measurement Array: \n" + buffer.toString());

        //Cursor res = myDb.queryAllPositionsFromBSSID(macAddressSet);


        return expectedPos;
    }

    public static Integer calculateEuclidianDistance(){

        return 0;
    }



    public static Integer radiansToRounded45Degrees(float mOrientation){

        double degreeToReturn;
        degreeToReturn = Math.toDegrees((double)mOrientation); //raw
        //degreeAux = degreeAux + 22.5;

        degreeToReturn = Math.floor((degreeToReturn + 22.5)/45.0)*45;//Impartit pe N , NV , V , SV...

        if (degreeToReturn == -180)
            degreeToReturn = 180;

        return (int)degreeToReturn;
    }

}
