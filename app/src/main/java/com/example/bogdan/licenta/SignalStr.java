package com.example.bogdan.licenta;

import android.content.Context;

public class SignalStr {

    public Long Pos_ID;
    public Integer SignalStrength;
    public String Router_Address;

    public SignalStr() {
        Pos_ID = null;
        SignalStrength = null;
        Router_Address = null;
    }
    public SignalStr(Long posID,Integer signalStr,String routerAddr){
        Pos_ID = posID;
        SignalStrength = signalStr;
        Router_Address = routerAddr;
    }


    }
