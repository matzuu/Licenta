package com.example.bogdan.licenta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;

public class Algorithms {









    public static Position kNN(HashSet<Measurement> currentMeasurementSet,Integer orientation,Context context,DatabaseHelper myDb,String cluster){

        if (currentMeasurementSet.size() < 1){
            return null;
        }
        Log.d("ALG","Starting Algorithm EUCLIDEAN DISTANCE");
        Position expectedPos = new Position();


        HashMap<Position,List<Measurement>> posMeasureHashMap = new HashMap<>();
        Position pos = new Position();
        Measurement m;
        List<Measurement> measurementList = new ArrayList<>();
        StringBuffer buffer = new StringBuffer();

        HashSet<String> macAddressSet = new HashSet<>();
        //imi iau toaate BSSID-urile din Measurment-ul facut recent.
        for (Measurement measurement : currentMeasurementSet) {
            macAddressSet.add(measurement.BSSID);
        }
        Cursor res2 = myDb.queryAllPositionsAndMeasurementsFromBSSIDandCluster(macAddressSet,cluster);
        if (res2.getCount() == 0) {
            // show message
            Log.d("ALG","No rows retrieved");
            return null;
        }

        res2.moveToFirst();
        pos = new Position();
        pos.CoordX = res2.getDouble(res2.getColumnIndex ("CoordX" ));
        pos.CoordY = res2.getDouble(res2.getColumnIndex ("CoordY" ));
        pos.Orientation = res2.getInt(res2.getColumnIndex ("Orientation" ));
        pos.Cluster = res2.getString(res2.getColumnIndex ("Cluster" ));
        m = new Measurement();
        m.id = res2.getInt(res2.getColumnIndex ("ID" ));
        m.ref_CoordX = res2.getDouble(res2.getColumnIndex ("CoordX" ));
        m.ref_CoordY = res2.getDouble(res2.getColumnIndex ("CoordY" ));
        m.ref_Orientation = res2.getInt(res2.getColumnIndex ("Orientation" ));
        m.ref_Cluster = res2.getString(res2.getColumnIndex ("Cluster" ));
        m.BSSID = res2.getString(res2.getColumnIndex ("BSSID" ));
        m.SignalStrength = res2.getInt(res2.getColumnIndex ("SignalStrength" ));
        measurementList.add(m);

        while (res2.moveToNext()) {
            if(     pos.CoordX != res2.getDouble(res2.getColumnIndex ("CoordX" )) ||
                    pos.CoordY != res2.getDouble(res2.getColumnIndex ("CoordY" )) ||
                    pos.Orientation != res2.getInt(res2.getColumnIndex ("Orientation" ))
                    ){
                posMeasureHashMap.put(pos,measurementList);
                measurementList = new ArrayList<>();
                pos = new Position();
                pos.CoordX = res2.getDouble(res2.getColumnIndex ("CoordX" ));
                pos.CoordY = res2.getDouble(res2.getColumnIndex ("CoordY" ));
                pos.Orientation = res2.getInt(res2.getColumnIndex ("Orientation" ));
                pos.Cluster = res2.getString(res2.getColumnIndex ("Cluster" ));
            }
            m = new Measurement();
            m.id = res2.getInt(res2.getColumnIndex ("ID" ));
            m.ref_CoordX = res2.getDouble(res2.getColumnIndex ("CoordX" ));
            m.ref_CoordY = res2.getDouble(res2.getColumnIndex ("CoordY" ));
            m.ref_Orientation = res2.getInt(res2.getColumnIndex ("Orientation" ));
            m.ref_Cluster = res2.getString(res2.getColumnIndex ("Cluster" ));
            m.BSSID = res2.getString(res2.getColumnIndex ("BSSID" ));
            m.SignalStrength = res2.getInt(res2.getColumnIndex ("SignalStrength" ));
            measurementList.add(m);

            buffer.append("ref_CoordX :" + m.ref_CoordX + "\n");
            buffer.append("ref_CoordY :" + m.ref_CoordY + "\n");
            buffer.append("ref_Orientation :" + m.ref_Orientation + "\n");
            buffer.append("ref_Cluster :" + m.ref_Cluster  + "\n");
            buffer.append("ID :" +m.id  + "\n");
            buffer.append("BSSID :" + m.BSSID + "\n");
            buffer.append("SignalStr :" + m.SignalStrength + "\n\n");
        }
        posMeasureHashMap.put(pos,measurementList);
        Log.d("ALG","Queried Measurement Map: \n" + buffer.toString());
        Log.d("ALG","PosMeasureHASHMAP.size(): "+posMeasureHashMap.size());
        //poz cititite; trebuie gasita cea mai similara poz;
        Integer k = 3;
        Set<Position> posEuclidianDistance = calculateEuclideanDistance(posMeasureHashMap,currentMeasurementSet,macAddressSet,k);

        return expectedPos;
    }

    public static Set<Position> calculateEuclideanDistance(HashMap<Position,List<Measurement>> posMeasureHASHMAP, HashSet<Measurement> currentMeasurementSet,HashSet<String> macAddressSet, Integer k){


        List<Measurement> measurementList;
        for (Map.Entry<Position,List<Measurement>> entry : posMeasureHASHMAP.entrySet()){

            measurementList = entry.getValue();
            //Collections.sort(measurementList);
            //todo sa sortez dupa BSSID inloc de puterea semnalului?
            Collections.sort(measurementList, new Comparator<Measurement>() {
                @Override
                public int compare(final Measurement object1, final Measurement object2) {
                    return object1.getBSSID().compareTo(object2.getBSSID());
                }
            });
            Log.d("ALG", "Array sortat: \n" );

            Integer[] frecvSignalForBSSID; //= new Integer[100];
            Integer i = 0;
            String auxBSSID = null;
            HashMap<String,Integer[]> strDistributionGivenBSSID = new HashMap<>();

            for (Measurement m : measurementList){

                if(auxBSSID != m.BSSID)
                    frecvSignalForBSSID = new Integer[100];
                    if(auxBSSID == null){
                        auxBSSID = m.BSSID;
                    }
                    else {


                    }





                Log.d("ALG", "Measurmentul: " +i+ " \n SS: " + m.SignalStrength + " \n BSSID: "+ m.BSSID);
                i++;
            }
            //todo vector frecventa

        }

        return null;
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
