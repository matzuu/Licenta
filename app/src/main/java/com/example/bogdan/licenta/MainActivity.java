package com.example.bogdan.licenta;

import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    DatabaseHelper myDb;
    EditText editCoordX, editCoordY, editLevel, editPosID,editOrientation,editCluster;
    Button btnAddData;
    Button btnviewAll;
    Button btnDelete;
    Button btnviewUpdate;
    Button btnRead;
    Button btnWrite;
    Button btnsigStrView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDb = new DatabaseHelper(this);

        editCoordX = (EditText) findViewById(R.id.editText_CoordX);
        editCoordY = (EditText) findViewById(R.id.editText_CoordY);
        editLevel = (EditText) findViewById(R.id.editText_Level);
        editOrientation = (EditText) findViewById(R.id.editText_Orientation);
        editCluster = (EditText) findViewById(R.id.editText_Cluster);
        editPosID = (EditText) findViewById(R.id.editText_PosID);
        btnAddData = (Button) findViewById(R.id.button_add);
        btnviewAll = (Button) findViewById(R.id.button_viewAll);
        btnviewUpdate = (Button) findViewById(R.id.button_update);
        btnDelete = (Button) findViewById(R.id.button_delete);
        btnRead = (Button) findViewById(R.id.button_read);
        btnWrite = (Button) findViewById(R.id.button_write);
        btnsigStrView = (Button) findViewById(R.id.button_SigStrView);

        AddData();
        viewAll();
        UpdateData();
        DeleteData();
        ReadData();
        TestData();
        viewSigStr();

    }
    public void TestData(){
        btnWrite.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("TEEST", "TEEEEEEEEESSSSSSTTT");

                        String toTest ="0.0,1.3,;";
                        Matcher m;
                        Double coordx = 1.1;
                        Double coordy = 2.2;

                        m = Pattern.compile("\\d\\.\\d").matcher(toTest);
                        if(m.find())
                            coordx = Double.parseDouble(m.group());
                        if(m.find())
                            coordy = Double.parseDouble(m.group());
                        Log.d("TEEST","coordonata lui x" +coordx.toString()+" coord lui y " + coordy);

                    }
                }
        );
    }



    public void ReadData(){
        btnRead.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("READ","Precitire");
                    List<String> stringList = FileHelper.ReadFile(MainActivity.this);
                    stringList = FileHelper.CleanString(MainActivity.this,stringList);

                    //
                    Matcher mPos;
                    Position pos = new Position();
                    Double coordx = null;
                    Double coordy = null;
                    Matcher mMacAddress;
                    HashSet<String> macAddressSet = new HashSet<>();
                    String macAddressAux;
                    Matcher mSigStr;
                    HashSet<SignalStr> signalStrHashSet = new HashSet<>();
                    SignalStr sigStr = new SignalStr();
                    List<String> auxL = new ArrayList<>();
                    String aux = "";
                    Long auxl;

                    Matcher mMacAddressSig;

                    long lastPosID = -2;

                    for (String line : stringList) {
                        Log.d("CleanString", "\n linia: " + line);
                        mPos = Pattern.compile("\\d\\.\\d,").matcher(line);
                        coordx = null;
                        coordy = null;
                        if (mPos.find()) {
                            aux = mPos.group();
                            aux = aux.substring(0, aux.length() - 1);
                            Log.d("CleanString", "INCERC COORDX " + aux);
                            coordx = Double.parseDouble(aux);
                        }


                        if (mPos.find()) {
                            aux = mPos.group();
                            aux = aux.substring(0, aux.length() - 1);
                            coordy = Double.parseDouble(aux);
                        }

                        //ma asigur ca pe fiecare linie gasesc doua Double de pozitie.. in caz contrar ignor linia;
                        if (coordx != null && coordy != null) {
                            if (coordx != pos.CoordX || coordy != pos.CoordY) {
                                pos.CoordX = coordy;
                                pos.CoordY = coordy;
                                pos.Level = 0;
                                pos.Orientation = 0;
                                pos.Cluster = "test";
                                auxl = myDb.insertPosData(pos);
                                if (auxl >= 0 )
                                    lastPosID = auxl;
                            }
                            //gasesc toate intensitatile de semnal asociate cu o adresa mac
                            mMacAddressSig = Pattern.compile("\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}=-\\d{2}").matcher(line);
                            while (mMacAddressSig.find()) {
                                auxL.add(mMacAddressSig.group());
                            }

                            for (String s : auxL) {
                                Log.d("CleanString","Stringul s: "+s);
                                mMacAddress = Pattern.compile("\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}").matcher(s);
                                mSigStr = Pattern.compile("-\\d{2}").matcher(s);
                                if (mMacAddress.find()) {
                                    macAddressAux = mMacAddress.group();
                                    macAddressSet.add(macAddressAux);
                                    Log.d("CleanString","marimea mac: "+macAddressSet.size());
                                    if (mSigStr.find()) {
                                        sigStr = new SignalStr();
                                        sigStr.SignalStrength = Integer.parseInt(mSigStr.group());
                                        sigStr.Pos_ID = lastPosID;
                                        sigStr.Router_Address = macAddressAux;
                                        signalStrHashSet.add(sigStr);
                                        Log.d("CleanString","marimea signalStrset: "+signalStrHashSet.size());
                                    }
                                }
                            }//end aux for
                        }
                    }//end for
                    Log.d("CleanString","sizeOfmacAddressSet: "+macAddressSet.size()+"sizeOfsignalStrHashSet "+ signalStrHashSet.size());
                    myDb.insertRouterData(macAddressSet);
                    myDb.insertSignalStrData(signalStrHashSet);

                    //Pozitie
                    //Router Sgl Str

                }
            }
        );
    }

    public void DeleteData() {
        btnDelete.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Integer deletedRows = myDb.deleteData(editPosID.getText().toString());
                        if (deletedRows > 0)
                            Toast.makeText(MainActivity.this, "Data Deleted", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(MainActivity.this, "Data not Deleted", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    public void UpdateData() {
        btnviewUpdate.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isUpdate = myDb.updateData(editPosID.getText().toString(),
                                Double.parseDouble(editCoordX.getText().toString()),
                                Double.parseDouble(editCoordY.getText().toString()),
                                Integer.parseInt(editLevel.getText().toString()),
                                Integer.parseInt(editOrientation.getText().toString()),
                                editCluster.getText().toString()
                                );
                        if (isUpdate == true)
                            Toast.makeText(MainActivity.this, "Data Update", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(MainActivity.this, "Data not Updated", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    public void AddData() {
        btnAddData.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Position p = new Position(
                                Double.parseDouble(editCoordX.getText().toString()),
                                Double.parseDouble(editCoordY.getText().toString()),
                                Integer.parseInt(editLevel.getText().toString()),
                                Integer.parseInt(editOrientation.getText().toString()),
                                editCluster.getText().toString());
                        //List<Position> plist = new ArrayList<Position>();
                        long lastPosID = myDb.insertPosData(p);

                        //------SignalStr


                        //------MacAddr


                        //boolean isInserted = myDb.insertMultipleData(lastPosID,lsigstr,lmacAddr)
                        /*
                        boolean isInserted = myDb.insertPosData(
                                Integer.parseInt(editCoordX.getText().toString()),
                                Integer.parseInt(editCoordY.getText().toString()),
                                Integer.parseInt(editLevel.getText().toString()),
                                Integer.parseInt(editOrientation.getText().toString()),
                                editCluster.getText().toString()
                        );*/
                        if (lastPosID >= 0)
                            Toast.makeText(MainActivity.this, "Data Inserted , lastId: "+lastPosID, Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(MainActivity.this, "Data not Inserted", Toast.LENGTH_LONG).show();
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
                        buffer.append("Router_address :" + res.getString(2) + "\n");
                        buffer.append("Signal Str :" + res.getString(3) + "\n\n");
                    }

                    // Show all data
                    showMessage("Data", buffer.toString());
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




}


