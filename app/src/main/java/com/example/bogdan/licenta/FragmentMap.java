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

public class FragmentMap extends Fragment {
    private static final String TAG = "FragmentMap";

    private Button btnNavFragSearch;
    private Button btnNavSecondActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_map, container, false);
        btnNavFragSearch = (Button) view.findViewById(R.id.btnNavFragSearch);
        Log.d(TAG, "onCreateView FragS: started.");

        btnNavFragSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ((MainActivity)getActivity()).setViewPager(0);
            }
        });
        return view;
    }
}
