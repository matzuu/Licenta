package com.example.bogdan.licenta;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.example.bogdan.licenta.DatabaseHelper.TABLE_MEASUREMENTS;

public class FragmentMap extends Fragment {
    private static final String TAG = "FragmentMap";


    private Button btnNavFragSearch;
    private Button btnNavSecondActivity;
    private TextView textTitle;
    private ImageView imgView_map;

    private IMainActivity mIMainActivity;
    private String mIncomingMessage = "";
    private DatabaseHelper myDb;
    private Cluster myCl;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myDb = new DatabaseHelper(getContext());

        Bundle bundle = this.getArguments();
        if(bundle != null){
            mIncomingMessage = bundle.getString(getString(R.string.intent_message));
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_map, container, false);


        //btnNavFragSearch = (Button) view.findViewById(R.id.btnNavFragSearch);
        textTitle = view.findViewById(R.id.textView_Title);
        imgView_map = view.findViewById(R.id.imageView_map);




        if(!mIncomingMessage.equals(""))
            handleReceivedCluster(mIncomingMessage);





        Log.d(TAG, "onCreateView FragS: started.");
        //String value = getArguments().getString("Key");

        //Log.d("FRAGMENTMAP","Am primit: "+value);




        return view;
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

        int imgID = Integer.parseInt(myCl.clusterImageUrl);
        imgView_map.setImageResource(imgID);

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIMainActivity = (MainActivity) getActivity();
    }
}
