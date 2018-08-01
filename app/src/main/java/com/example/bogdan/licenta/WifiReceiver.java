package com.example.bogdan.licenta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class WifiReceiver extends BroadcastReceiver {

    TextView textWifiInfo;
    TextView textWifiNr;


    Context context;
    Long startTime;
    Long timeDifference;
    Integer nrOfScans = 0;
    List<SignalStr> capturedSigList = new ArrayList<>();

    public WifiReceiver(Context contextpar) {
        this.context = contextpar;
        textWifiInfo = ((SensorActivity)context).findViewById(R.id.textView_wifiInfo);
        textWifiNr = ((SensorActivity)context).findViewById(R.id.textView_wifiNr);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        /*
        capturedSigList.addAll(getScanResultInfo());
        timeDifference = SystemClock.elapsedRealtime() - startTime;
        textWifiInfo.setText("Seconds elapsed: "+Double.toString(timeDifference /1000.0));

        nrOfScans++;
        if (nrOfScans < 10){
            mWifiManager.startScan();
        }
        else{
            nrOfScans = 0;
            StringBuffer buffer = new StringBuffer();
            for (SignalStr s : capturedSigList) {

                buffer.append("POS_ID :" + s.Pos_ID  + "\n");
                buffer.append("Router_address :" + s.Router_Address + "\n");
                buffer.append("Signal Str :" +  s.SignalStrength + "\n\n");
            }
            showMessage("Data", buffer.toString());

        }
        */
    }



}


