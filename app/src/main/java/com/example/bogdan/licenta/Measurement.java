package com.example.bogdan.licenta;

import android.content.Context;

public class Measurement /*implements Comparable<Measurement> */{


    public Integer id;
    public Double ref_CoordX;
    public Double ref_CoordY;
    public Integer ref_Orientation;
    public String ref_Cluster;
    public Integer SignalStrength;
    public String BSSID;

    public Measurement() {
        id = null;
        ref_CoordX = null;
        ref_CoordY = null;
        ref_Orientation = null;
        ref_Cluster = null;
        SignalStrength = null;
        BSSID = null;
    }
    public Measurement(Integer signalStr,String BSSID){
        id = null;
        ref_CoordX = null;
        ref_CoordY = null;
        ref_Orientation = null;
        ref_Cluster = null;
        SignalStrength = signalStr;
        this.BSSID = BSSID;
    }

    public Measurement( Double coordX, Double coordY, Integer orientation , String cluster,Integer signalStr,String BSSID ) {
        id = null;
        ref_CoordX = coordX;
        ref_CoordY = coordY;
        ref_Orientation = orientation;
        ref_Cluster = cluster;
        SignalStrength = signalStr;
        this.BSSID = BSSID;
    }

    /*
    @Override
    public int compareTo(Measurement m) {
        if (this.SignalStrength > m.SignalStrength)
            return 1;
        else if (this.SignalStrength < m.SignalStrength)
            return -1;
        else return 0;

    }
    */

    public String getBSSID(){
        return this.BSSID;
    }



    }
