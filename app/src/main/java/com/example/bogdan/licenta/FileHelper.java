package com.example.bogdan.licenta;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileHelper extends Thread{

    final static String path = "D:/Facultate/Licenta";
    final static String filename = "datein.txt";
    final static String TAG = "ERROR";//FileHelper.class.getName();

    public void run(){


    }



    public static List<String> readFile(Context context){
        String line;
        List<String> rezList = new ArrayList<>();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(context.getAssets().open("crawdadOfflineTraceTest"));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while ( (line = bufferedReader.readLine()) != null )
            {
                rezList.add(line);
            }
            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            Log.d("FileNotFound", ex.getMessage());
        }
        catch(IOException ex) {
            Log.d("IOException", ex.getMessage());
        }
        return rezList;
    }

    public static  String ReadLine( Context context){
        String line = null;
        Log.d("READLINE","Pana in try");
        try {
            FileInputStream fileInputStream = new FileInputStream (new File(path));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            Log.d("READLINE","Pana in while");
            ////
            Integer deTest = 0;

            while ( (line = bufferedReader.readLine()) != null )
            {
                deTest++;
                Log.d("READLINE","Line no: "+ deTest);
                stringBuilder.append(line + System.getProperty("line.separator"));
            }
            fileInputStream.close();
            line = stringBuilder.toString();

            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        }
        catch(IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return line;
    }

    public static List<String> removeComments(List<String> inputList){
        //String line;
        String aux;
        List<String> outputList = new ArrayList<>();
        Log.d("READLINE","Size of inputList "+String.valueOf(inputList.size()));
        for (Integer nrLinie = inputList.size()-1 ;nrLinie >= 0; nrLinie--){
            aux = String.valueOf(inputList.get(nrLinie));
            if (aux.length() == 0 ){
                inputList.remove(nrLinie+0);
            }
            else if( aux.charAt(0) == '#')
            {
                inputList.remove(nrLinie+0);
            }
        }

        //List<Measurement> measurementsList = new ArrayList<>();
        Log.d("READLINE","Size of inputList After "+String.valueOf(inputList.size()));

        /* Redundat, oricum folosesc regex mai tarziu
        Log.d("READLINE", "First row before cleanup: "+inputList.get(0));
        StringBuilder sb;
        for (Integer nrLinie = 0;nrLinie < inputList.size();nrLinie++) {
            //curat string-ul
            sb = new StringBuilder(inputList.get(nrLinie));
            sb.delete(28,52);
            sb.delete(0,40);
            outputList.add(sb.toString());

        }
        Log.d("READLINE", "First row partially cleaned: "+outputList.get(0));
        */

        // \d\.\d,
        // \w{2}:\w{2}:\w{2}:\w{2}:\w{2}:\w{2} Mac address
        // =-\d{2}
        return inputList;
    }

    public static boolean parseString (List<String> stringList,DatabaseHelper myDb) {
        Matcher mPos;
        Position pos = new Position();
        Double coordx;
        Double coordy;
        Double orientation;
        Matcher mMacAddress;
        HashSet<String> macAddressSet = new HashSet<>();
        String macAddressAux;
        Matcher mMeasurement;
        HashSet<Measurement> measurementHashSet = new HashSet<>();
        Measurement measurement = new Measurement();
        List<String> auxLine = new ArrayList<>();
        String aux = "";
        Long auxl;
        boolean retValue = false;

        Matcher mMacAddressSig;

        long lastPosID = -2;
        Integer noLine = 0;
        for (String line : stringList) {
            noLine++;

            Log.d("parseString", "\n linia: "+ noLine +" \n " + line + " \n");
            Log.d("parseString", "marimea mac: " + macAddressSet.size());
            mPos = Pattern.compile("\\d\\.\\d,").matcher(line);
            coordx = null;
            coordy = null;
            if (mPos.find()) {
                aux = mPos.group();
                aux = aux.substring(0, aux.length() - 1);
                Log.d("parseString", "INCERC COORDX " + aux);
                coordx = Double.parseDouble(aux);
            }
            if (mPos.find()) {
                aux = mPos.group();
                aux = aux.substring(0, aux.length() - 1);
                coordy = Double.parseDouble(aux);
            }

            mPos = Pattern.compile("degree=\\d.\\d").matcher(line);
            orientation=null;
            if (mPos.find()){
                aux = mPos.group();
                aux = aux.substring(7,aux.length());
                orientation = Double.parseDouble(aux);
                orientation =  Math.floor((orientation)/45.0)*45;
                orientation = orientation - 180; // pentru a muta din intrevalul 0:359.9 in -179.9:180
                if (orientation == -180)
                    orientation = 180.0;
            }


            //ma asigur ca pe fiecare linie gasesc doua Double de pozitie.. in caz contrar ignor linia;
            if (coordx != null && coordy != null) {
                if (coordx != pos.CoordX || coordy != pos.CoordY) {
                    pos.CoordX = coordy;
                    pos.CoordY = coordy;
                    pos.Level = 0;
                    pos.Orientation = orientation.intValue();
                    pos.Cluster = "crawDad";
                    Log.d("parseString","inserting position: "+pos.toString());
                    lastPosID = myDb.insertPosData(pos);

                }
                //gasesc toate intensitatile de semnal asociate cu o adresa mac
                auxLine = new ArrayList<>();
                mMacAddressSig = Pattern.compile("\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}=-\\d{2}").matcher(line);
                while (mMacAddressSig.find()) {
                    auxLine.add(mMacAddressSig.group());
                }

                for (String s : auxLine) {
                    Log.d("parseString", "Stringul s: " + s);
                    mMacAddress = Pattern.compile("\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}:\\w{2}").matcher(s);
                    mMeasurement = Pattern.compile("-\\d{2}").matcher(s);
                    if (mMacAddress.find()) {
                        macAddressAux = mMacAddress.group();
                        macAddressSet.add(macAddressAux);

                        if (mMeasurement.find()) {
                            measurement = new Measurement();
                            measurement.SignalStrength = Integer.parseInt(mMeasurement.group());
                            measurement.ref_CoordX = pos.CoordX;
                            measurement.ref_CoordY = pos.CoordY;
                            measurement.ref_Orientation=pos.Orientation;
                            measurement.ref_Cluster = pos.Cluster;
                            measurement.BSSID = macAddressAux;
                            measurementHashSet.add(measurement);
                            Log.d("parseString", "marimea MeasurementSet: " + measurementHashSet.size());
                        }
                    }
                }//end aux for
            }
        }//end for
        Log.d("parseString", "sizeOfmacAddressSet: " + macAddressSet.size() + " \n sizeOfMeasurementHashSet " + measurementHashSet.size());
        myDb.insertRouterData(macAddressSet);
        myDb.insertMeasurementData(measurementHashSet);

        //Pozitie
        //Router Sgl Str
        retValue = true;
        return retValue;
    }

    public static boolean writeFile(List<String> stringList,String filename,Context context,Integer mode){

        Log.d("WRITE","trying to write file: "+filename+"  in mode: "+mode);
        switch(mode){
            case 1: mode = Context.MODE_PRIVATE;
                break;
            case 2: mode = Context.MODE_APPEND;
        }

        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(filename, mode));

            for(String s : stringList) {
                outputStreamWriter.append(s);
            }
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
            return false;
        }
        return true;
    }



}
