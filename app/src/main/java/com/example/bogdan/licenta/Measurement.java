package com.example.bogdan.licenta;

import android.content.Context;

public class Measurement {


    public Double ref_CoordX;
    public Double ref_CoordY;
    public Integer ref_Orientation;
    public String ref_Cluster;
    public Integer SignalStrength;
    public String BSSID;

    public Measurement() {
        ref_CoordX = null;
        ref_CoordY = null;
        ref_Orientation = null;
        ref_Cluster = null;
        SignalStrength = null;
        BSSID = null;
    }
    public Measurement(Integer signalStr,String BSSID){
        SignalStrength = signalStr;
        this.BSSID = BSSID;
    }

    public Measurement( Double coordX, Double coordY, Integer orientation , String cluster,Integer signalStr,String BSSID ) {
        ref_CoordX = coordX;
        ref_CoordY = coordY;
        ref_Orientation = orientation;
        ref_Cluster = cluster;
        SignalStrength = signalStr;
        this.BSSID = BSSID;
    }


    }
