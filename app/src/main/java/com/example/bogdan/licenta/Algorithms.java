package com.example.bogdan.licenta;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.HashSet;

public class Algorithms {






    public static Position algEuclideanDistance(HashSet<SignalStr> capturedSigSet,Integer Orientation,Context context,DatabaseHelper myDb){

        Log.d("ALG","Starting Algorithm EUCLIDEAN DISTANCE");
        Position expectedPos = new Position();
        HashSet<String> macAddressSet = new HashSet<>();

        for (SignalStr s : capturedSigSet) {
            macAddressSet.add(s.BSSID);
        }



        Cursor res = myDb.queryAllPositionsFromBSSID(macAddressSet);

        if (res.getCount() == 0) {
            // show message
            Log.d("ALG","No row retrieved");
        }
        Integer sigStr;
        while (res.moveToNext()) {

            res.getString(res.getColumnIndex("SignalStrength"));
            

        }





        return expectedPos;
    }



}
