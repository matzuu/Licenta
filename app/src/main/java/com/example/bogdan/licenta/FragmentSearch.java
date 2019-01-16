package com.example.bogdan.licenta;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class FragmentSearch extends Fragment {
    private static final String TAG = "FragmentSearch";

    private Button btnNavFragMap;
    private Button btnNavSecondActivity;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_search, container, false);
        btnNavFragMap = (Button) view.findViewById(R.id.btnNavFragMap);

        ListView mListView = (ListView) view.findViewById(R.id.listView);
        Log.d(TAG, "onCreateView FragS: started.");

        btnNavFragMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ((MainActivity)getActivity()).setViewPager(1);
            }
        });

        //test

        Cluster c1 = new Cluster("test1","asdf");
        Cluster c2 = new Cluster("test2", "Museum");
        Cluster c3 = new Cluster("test3", "School");
        Cluster c4 = new Cluster("test4", "Shopping");
        Cluster c5 = new Cluster("test5", "School");

        ArrayList<Cluster> clusterArrayList = new ArrayList<>();
        clusterArrayList.add(c1);
        clusterArrayList.add(c2);
        clusterArrayList.add(c3);
        clusterArrayList.add(c4);
        clusterArrayList.add(c5);
        clusterArrayList.add(c1);
        clusterArrayList.add(c2);
        clusterArrayList.add(c3);
        clusterArrayList.add(c4);
        clusterArrayList.add(c5);
        clusterArrayList.add(c1);
        clusterArrayList.add(c2);
        clusterArrayList.add(c3);
        clusterArrayList.add(c4);
        clusterArrayList.add(c5);

        ClusterListAdapter adapter = new ClusterListAdapter(getActivity(),R.layout.adapter_view_layout,clusterArrayList);
        mListView.setAdapter(adapter);


        return view;
    }
}
