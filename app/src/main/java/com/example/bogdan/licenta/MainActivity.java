package com.example.bogdan.licenta;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    DatabaseHelper myDb;

    Button btnviewAll;
    Button btnRead;
    Button btnWrite;
    Button btnsigStrView;
    Button btnCountSig;
    Button btnRegisterActivity;
    Button btnLocatingActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDb = new DatabaseHelper(this);

        btnviewAll = (Button) findViewById(R.id.button_viewAll);
        btnRead = (Button) findViewById(R.id.button_read);
        btnWrite = (Button) findViewById(R.id.button_write);
        btnsigStrView = (Button) findViewById(R.id.button_SigStrView);
        btnCountSig =  (Button) findViewById(R.id.button_CountSig);
        btnRegisterActivity = (Button) findViewById(R.id.button_toRegisterActivity);
        btnLocatingActivity = (Button) findViewById(R.id.button_toLocatingActivity);


        viewAll();
        ReadingThread();
        viewSigStr();
        CountSig();
        toRegisterActivity();
        toLocatingActivity();

    }

    public void ReadingThread(){
        btnRead.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            Log.d("READ","Precitire");
                            List<String> stringList = FileHelper.ReadFile(MainActivity.this);
                            Log.d("READ","Curatare");
                            stringList = FileHelper.CleanString(stringList);
                            boolean ok;
                            ok = FileHelper.parseString(stringList,myDb);
                            Log.d("READ","FINISH PARSESTRING: "+ ok);

                        }
                    };
                    Thread readingThread = new Thread(runnable);
                    readingThread.start();
                }
            }
        );
    }

    public void viewAll() {
        btnviewAll.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Cursor res = myDb.getAllData("position_table");
                        //position_table
                        //posRouter_table
                        //router_table
                        if (res.getCount() == 0) {
                            // show message
                            showMessage("Error", "Nothing found");
                            return;
                        }

                        StringBuffer buffer = new StringBuffer();
                        while (res.moveToNext()) {
                            //buffer.append("Id :" + res.getString(0) + "\n");
                            buffer.append("CoordX :" + res.getString(0) + "\n");
                            buffer.append("CoordY :" + res.getString(1) + "\n");
                            buffer.append("Level :" + res.getString(2) + "\n");
                            buffer.append("Orientation :" + res.getString(3) + "\n");
                            buffer.append("Cluster :" + res.getString(4) + "\n\n");
                        }

                        // Show all data
                        showMessage("Data", buffer.toString());
                    }
                }
        );
    }

    public void viewSigStr(){
        btnsigStrView.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Cursor res = myDb.getAllData("posRouter_table");
                    //position_table
                    //posRouter_table
                    //router_table
                    if (res.getCount() == 0) {
                        // show message
                        showMessage("Error", "Nothing found");
                        return;
                    }
                    Log.d("TEEST","VIEW SIG STR DATA");
                    StringBuffer buffer = new StringBuffer();
                    while (res.moveToNext()) {
                        //buffer.append("Id :" + res.getString(0) + "\n");
                        buffer.append("ID :" + res.getString(0) + "\n");
                        buffer.append("POS_ID :" + res.getString(1) + "\n");
                        buffer.append("BSSID :" + res.getString(2) + "\n");
                        buffer.append("Signal Str :" + res.getString(3) + "\n\n");
                    }

                    // Show all data
                    showMessage("Data", buffer.toString());
                }
            }
        );
    }

    public void CountSig(){
        btnCountSig.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long ret = myDb.getSigCount();
                        StringBuffer buffer = new StringBuffer();
                        buffer.append("Count Sig Str : " + ret + " \n");


                        // Show all data
                        showMessage("Count", buffer.toString());
                    }
                }
        );
    }

    public void showMessage(String title, String Message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    private void toRegisterActivity(){

        btnRegisterActivity.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this,RegisterActivity.class));
                    }
                }
        );

    }

    private void toLocatingActivity(){

        btnLocatingActivity.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this,LocatingActivity.class));
                    }
                }
        );

    }


}


