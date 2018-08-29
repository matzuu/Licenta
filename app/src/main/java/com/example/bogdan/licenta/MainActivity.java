package com.example.bogdan.licenta;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import static com.example.bogdan.licenta.DatabaseHelper.TABLE_MEASUREMENTS;

public class MainActivity extends AppCompatActivity {
    DatabaseHelper myDb;

    Button btnViewAllPosFromCluster;
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

        btnViewAllPosFromCluster = (Button) findViewById(R.id.button_viewAll);
        btnRead = (Button) findViewById(R.id.button_read);
        btnWrite = (Button) findViewById(R.id.button_write);
        btnsigStrView = (Button) findViewById(R.id.button_SigStrView);
        btnCountSig = (Button) findViewById(R.id.button_CountSig);
        btnRegisterActivity = (Button) findViewById(R.id.button_toRegisterActivity);
        btnLocatingActivity = (Button) findViewById(R.id.button_toLocatingActivity);


        viewAllPosFromCluster();
        ReadingThread();
        WritingThread();
        viewSigStrCount();
        CountSig();
        toRegisterActivity();
        toLocatingActivity();

    }

    public void ReadingThread() {
        btnRead.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d("READ", "Precitire");
                        List<String> stringList = FileHelper.readAssetsFile(MainActivity.this);
                        Log.d("READ", "Sterg Comment: "+stringList.size());
                        stringList = FileHelper.removeComments(stringList);
                        boolean ok;
                        Log.d("READ","Curat Stringul: "+stringList.size());
                        ok = FileHelper.parseString(stringList, myDb);
                        Log.d("READ", "FINISH PARSESTRING: " + ok);

                    }
                };
                Thread readingThread = new Thread(runnable);
                readingThread.start();
                }
            }
        );
    }

    public void WritingThread() {
        btnWrite.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d("WRITEFILE", "Prescriere");
                        Cursor res2 = myDb.getAllData(TABLE_MEASUREMENTS);
                        if (res2 == null || res2.getCount() == 0) {
                            // show message
                            showMessage("Error", "Nothing found");
                            return;
                        }
                        Log.d("WRITEFILE","Res2.getcount()= "+res2.getCount());

                        List<String> stringsToWrite = new ArrayList<>();
                        stringsToWrite.add(new String("# dateOutAndroid \r\n"));
                        String s;
                        while (res2.moveToNext()) {
                            s = "ID=" + res2.getInt(res2.getColumnIndex("ID")) + ";" +
                                "cluster=" + res2.getString(res2.getColumnIndex("ref_Cluster")) + ";"+
                                "pos=" + res2.getDouble(res2.getColumnIndex("ref_CoordX")) + "," + res2.getDouble(res2.getColumnIndex("ref_CoordY")) + ";"+
                                "degree=" + res2.getInt(res2.getColumnIndex("ref_Orientation")) + ";"+
                                res2.getString(res2.getColumnIndex("BSSID")) + "=" + res2.getInt(res2.getColumnIndex("SignalStrength"))+"\r\n";
                            stringsToWrite.add(s);
                        }
                        boolean ok;
                        ok = FileHelper.writeFile(stringsToWrite,"dateAndroidOut.txt", getApplicationContext(),1);
                        Log.d("WRITEFILE", "writeFile result: " + ok);
                    }
                };
                Thread readingThread = new Thread(runnable);
                readingThread.start();
                }
            }
        );
    }

    public void viewAllPosFromCluster() {
        btnViewAllPosFromCluster.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                Cursor res = myDb.queryAllPositionsFromCluster("Acasa");
                //position_table
                //posRouter_table
                //router_table
                if (res == null || res.getCount() == 0) {
                    // show message
                    showMessage("Error", "Nothing found");
                    return;
                }

                StringBuffer buffer = new StringBuffer();
                while (res.moveToNext()) {
                    //buffer.append("Id :" + res.getString(0) + "\n");
                    buffer.append("CoordX :" + res.getString(0) + "\n");
                    buffer.append("CoordY :" + res.getString(1) + "\n");
                    buffer.append("Orientation :" + res.getString(3) + "\n");
                    buffer.append("Cluster :" + res.getString(4) + "\n\n");
                }

                // Show all data
                showMessage("Data", buffer.toString());
                }
            }
    );
    }

    public void viewSigStrCount() {
        btnsigStrView.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                Cursor res = myDb.getAllData(TABLE_MEASUREMENTS);
                //position_table
                //posRouter_table
                //router_table
                if (res.getCount() == 0) {
                    // show message
                    showMessage("Error", "Nothing found");
                    return;
                }
                Log.d("TEEST", "VIEW MEASUREMENTS DATA");
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

    public void CountSig() {
        btnCountSig.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                long ret = myDb.getMeasurementCount();
                StringBuffer buffer = new StringBuffer();
                buffer.append("Count MEASUREMENTS : " + ret + " \n");


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

    private void toRegisterActivity() {

        btnRegisterActivity.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
                }
            }
        );

    }

    private void toLocatingActivity() {

        btnLocatingActivity.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LocatingActivity.class));
                }
            }
    );

    }


}


