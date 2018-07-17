package com.example.bogdan.licenta;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileHelper {

    final static String path = "D:/Facultate/Licenta";
    final static String filename = "datein.txt";
    final static String TAG = "ERROR";//FileHelper.class.getName();

    public static List<String> ReadFile(Context context){
        String line = null;
        List<String> rezList = new ArrayList<>();
        try {
            //FileInputStream fileInputStream = new FileInputStream (new File(path,filename));
            InputStreamReader inputStreamReader = new InputStreamReader(context.getAssets().open("crawdadOffileTraceTest"));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            //StringBuilder stringBuilder = new StringBuilder();

            while ( (line = bufferedReader.readLine()) != null )
            {
                //stringBuilder.append(line + System.getProperty("line.separator"));
                rezList.add(line);
            }
            //fileInputStream.close();


            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            Log.d("FileNotFound", ex.getMessage());
        }
        catch(IOException ex) {
            Log.d("IOException", ex.getMessage());
        }
        //return line;
        Log.d("READLIST",rezList.toString());
        return rezList;
    }

    public static  String ReadLine( Context context){
        String line = null;

        try {
            FileInputStream fileInputStream = new FileInputStream (new File(path));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();

            while ( (line = bufferedReader.readLine()) != null )
            {
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

    public static boolean saveToFile( String data){
        try {
            new File(path  ).mkdir();
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file,true);
            fileOutputStream.write((data + System.getProperty("line.separator")).getBytes());

            return true;
        }  catch(FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        }  catch(IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return  false;


    }

    public static List<String> CleanString (Context context, List<String> inputList){
        String line;
        List<String> outputList = new ArrayList<>();
        Log.d("READLINE","Size of inputList "+String.valueOf(inputList.size()));
        for (Integer nrLinie = 0;nrLinie < inputList.size();nrLinie++){

            if( String.valueOf(inputList.get(nrLinie).charAt(0)).equals("#")){
                inputList.remove(nrLinie+0);
            }

        }


        List<SignalStr> strList = new ArrayList<>();
        Log.d("READLINE","Size of inputList After "+String.valueOf(inputList.size()));
        Log.d("READLINE", "First row before cleanup: "+inputList.get(0));
        StringBuilder sb;
        for (Integer nrLinie = 0;nrLinie < inputList.size();nrLinie++) {
            //curat string-ul
            sb = new StringBuilder(inputList.get(nrLinie));
            sb.delete(28,52);
            sb.delete(0,20);
            outputList.add(sb.toString());

        }
        Log.d("READLINE", "First row partially cleaned: "+outputList.get(0));

        // \d\.\d,
        // \w{2}:\w{2}:\w{2}:\w{2}:\w{2}:\w{2} Mac address
        // =-\d{2}
        return outputList;
    }

}
