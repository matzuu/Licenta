package com.example.bogdan.licenta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static java.math.BigDecimal.ROUND_HALF_EVEN;

public class Algorithms {









    public static LinkedHashMap<Position,BigDecimal> kNN(HashSet<Measurement> liveMeasurementsSet, Integer orientation, String cluster, DatabaseHelper myDb, Integer degreeNo, Integer k, Integer trainingMeasurements, Integer apSize){

        if (liveMeasurementsSet.size() < 1){
            return null;
        }
        Log.d("ALG","Starting Algorithm kNN");

        HashMap<Position,List<Measurement>> posMeasureHashMap = new HashMap<>();
        Position pos = new Position();
        Measurement m;
        List<Measurement> measurementList = new ArrayList<>();
        //StringBuffer buffer = new StringBuffer();
        Integer nrOfMeasurements = 0;
        Integer curPosCount = 0;

        HashSet<String> macAddressSet = new HashSet<>();
        //imi iau toaate BSSID-urile din Measurment-ul facut recent.
        for (Measurement measurement : liveMeasurementsSet) {
            macAddressSet.add(measurement.BSSID);
            Log.d("ALG","live BSSID");
        }

        Cursor res=myDb.querySortedBSSIDOccurrences(macAddressSet,cluster,apSize);
        if (res.getCount() == 0) {
            // show message
            Log.d("ALG","No rows retrieved");
            return null;
        }

        macAddressSet = new HashSet<>();
        while (res.moveToNext()) {
            macAddressSet.add(res.getString(res.getColumnIndex("MACAddress")));
            Log.d("QUERY","returned BSSID: "+res.getString(res.getColumnIndex("MACAddress")) );
        }
        Log.d("ALG","Intermediate number of BSSID: "+macAddressSet.size());
        //Cursor res2 = myDb.queryAllPositionsAndMeasurementsFromBSSIDandCluster(macAddressSet,cluster); veche , fara orientari
        Cursor res2=myDb.querykNN(macAddressSet,orientation,cluster,degreeNo);
        if (res2.getCount() == 0) {
            // show message
            Log.d("ALG","No rows retrieved");
            return null;
        }

        if(res2.moveToFirst()) {
            nrOfMeasurements++;
            curPosCount++;
            pos = new Position();
            pos.CoordX = res2.getDouble(res2.getColumnIndex("CoordX"));
            pos.CoordY = res2.getDouble(res2.getColumnIndex("CoordY"));
            pos.Orientation = res2.getInt(res2.getColumnIndex("Orientation"));
            pos.Cluster = res2.getString(res2.getColumnIndex("Cluster"));
            m = new Measurement();
            m.id = res2.getInt(res2.getColumnIndex("ID"));
            m.ref_CoordX = res2.getDouble(res2.getColumnIndex("CoordX"));
            m.ref_CoordY = res2.getDouble(res2.getColumnIndex("CoordY"));
            m.ref_Orientation = res2.getInt(res2.getColumnIndex("Orientation"));
            m.ref_Cluster = res2.getString(res2.getColumnIndex("Cluster"));
            m.BSSID = res2.getString(res2.getColumnIndex("BSSID"));
            m.SignalStrength = res2.getInt(res2.getColumnIndex("SignalStrength"));
            measurementList.add(m);
        }
        while (res2.moveToNext()) { //verific daca citesc alta pozitie
            if(     (pos.CoordX != res2.getDouble(res2.getColumnIndex ("CoordX" )) ||
                    pos.CoordY != res2.getDouble(res2.getColumnIndex ("CoordY" )) ||
                    pos.Orientation != res2.getInt(res2.getColumnIndex ("Orientation" )))
                    ){

                //adaug poz de dinainte si measureliste-ul
                posMeasureHashMap.put(pos,measurementList);
                //ma pregatesc ptr urmatoarea poz
                measurementList = new ArrayList<>();
                pos = new Position();
                pos.CoordX = res2.getDouble(res2.getColumnIndex ("CoordX" ));
                pos.CoordY = res2.getDouble(res2.getColumnIndex ("CoordY" ));
                pos.Orientation = res2.getInt(res2.getColumnIndex ("Orientation" ));
                pos.Cluster = res2.getString(res2.getColumnIndex ("Cluster" ));
                //buffer.append("ref_CoordX :" + pos.CoordX + "  ref_CoordY :" + pos.CoordY + "  ref_Orientation :" + pos.Orientation + "  ref_Cluster :" + pos.Cluster  + "\n");
                curPosCount = 1;
            }
            //pentru analiza: verific sa nu am nr de masuratori mai mare decat nr datelor de antrenrare(trainingMeasurements)
            if(curPosCount<trainingMeasurements) {
                nrOfMeasurements++;
                m = new Measurement();
                m.id = res2.getInt(res2.getColumnIndex("ID"));
                m.ref_CoordX = res2.getDouble(res2.getColumnIndex("CoordX"));
                m.ref_CoordY = res2.getDouble(res2.getColumnIndex("CoordY"));
                m.ref_Orientation = res2.getInt(res2.getColumnIndex("Orientation"));
                m.ref_Cluster = res2.getString(res2.getColumnIndex("Cluster"));
                m.BSSID = res2.getString(res2.getColumnIndex("BSSID"));
                m.SignalStrength = res2.getInt(res2.getColumnIndex("SignalStrength"));
                measurementList.add(m);
            }

            /*
            buffer.append("ID :" +m.id  + "\n");
            buffer.append("BSSID :" + m.BSSID + "\n");
            buffer.append("SignalStr :" + m.SignalStrength + "\n\n");
            */
        }
        posMeasureHashMap.put(pos,measurementList);
        //Log.d("ALG","Queried Measurement Map: \n" + buffer.toString()); //spam
        Log.d("ALG","\nPosMeasureHASHMAP.size()/nrOfMeasurements: "+posMeasureHashMap.size()+"\n nrOfMeasurements: "+nrOfMeasurements);
        //poz cititite; trebuie gasita cea mai similara poz;

        HashMap<Position,BigDecimal> posEuclidianDistance = calculateEuclideanDistance(posMeasureHashMap,liveMeasurementsSet);
        Log.d("ALG","Exited calculateED");
        Log.d("ALG","Sorting the map");
        LinkedHashMap<Position,BigDecimal> sortedMap = sortByValue(posEuclidianDistance);
        LinkedHashMap<Position,BigDecimal> returnedMap = new LinkedHashMap<>();
        Integer contor = 0;
        for (LinkedHashMap.Entry<Position,BigDecimal> entry : sortedMap.entrySet()) {
            returnedMap.put(entry.getKey(),entry.getValue());
            contor++;
            Log.d("ALG3","Contor: "+contor);
            if(contor >= k )//de inlocuit cu k
                break;
        }
        //Position expectedPos = new Position();
        Log.d("ALG","Exiting kNN");
        return returnedMap;
    }

    public static HashMap<Position,BigDecimal> calculateEuclideanDistance(HashMap<Position,List<Measurement>> posMeasureHASHMAP, HashSet<Measurement> currentMeasurementSet){


        List<Measurement> measurementList;
        Position pos;
        HashMap<Position,BigDecimal> positionLikelihoodMap = new HashMap<>();
        //the set is only used here
        Measurement[] currentMeasurementArr =  currentMeasurementSet.toArray(new Measurement[currentMeasurementSet.size()]);
        Arrays.sort(currentMeasurementArr,new Comparator<Measurement>() {
            @Override
            public int compare(final Measurement object1, final Measurement object2) {
                return object1.getBSSID().compareTo(object2.getBSSID());
            }
        });

        /*List currentMeasurementList = new ArrayList(currentMeasurementSet);

        Collections.sort(currentMeasurementList, new Comparator<Measurement>() {
            @Override
            public int compare(final Measurement object1, final Measurement object2) {
                return object1.getBSSID().compareTo(object2.getBSSID());
            }
        });
        */
        //Log.d("Euclid Distance","currentMeasurementArr.size() - "+ currentMeasurementArr.length);

        for (Map.Entry<Position,List<Measurement>> entry : posMeasureHASHMAP.entrySet()){
            //Log.d("Euclid Distance","In for: Position - " + entry.getKey().toString());


            measurementList = entry.getValue();
            //Collections.sort(measurementList);
            //todo sa sortez dupa BSSID inloc de puterea semnalului?
            Collections.sort(measurementList, new Comparator<Measurement>() {
                @Override
                public int compare(final Measurement object1, final Measurement object2) {
                    return object1.getBSSID().compareTo(object2.getBSSID());
                }
            });
            //Log.d("Euclid Distance", "Array sortat: \n" );

            Integer[] frecvSignalForBSSID = new Integer[100];
            Arrays.fill(frecvSignalForBSSID, 0);
            Integer i = 0;
            Integer contorM = 0;
            List<Double> probOfSSsForThisBSSID = new ArrayList<>();

            String currentBSSID = null;
            HashMap<String,BigDecimal> probabilityOfBSSID = new HashMap<>();
            //Log.d("Euclid Distance","measurementList.size() - "+ measurementList.size());


            for (Measurement m : measurementList) {

                if(currentBSSID != null) {
                    if (currentBSSID.compareTo(m.BSSID) != 0) {

                        //Log.d("ALG2", "currentBSSID: " + currentBSSID);
                        //Log.d("ALG2", "m.BSSID: " + m.BSSID);
                        //lista si vectorul sunt sortate alfabetic dupa BSSID
                        //Cand termin cu un BSSID , inainte sa trec la urmatorul , iau masuratorile din currentMeasureArr cu acelasi BSSID si verific care e probabilitatea sa sa apara, data fiind vectorul de frecventa
                        if (contorM < currentMeasurementArr.length)
                            while (currentBSSID.compareTo(currentMeasurementArr[contorM].BSSID) >= 0) {
                                //Log.d("ALG2", "currentMeasurementArr[contorM].BSSID " + currentMeasurementArr[contorM].BSSID);
                                if (currentBSSID.compareTo(currentMeasurementArr[contorM].BSSID) == 0) {
                                    //Iau probabilitatea ca semnalul din currentMeasurement sa fie in vectorul de frecventa atunci cand stiu ca am terminat cu un BSSID
                                    //in currentMeasurment sunt BSSID in fucntie de cate ori scanez
                                    /*
                                    Log.d("ALG2", "contorM: "+contorM);
                                    Log.d("ALG2", "currentMeasurment SignalStr: "+currentMeasurementArr[contorM].SignalStrength);
                                    Log.d("ALG2", "frecv SS pt BSSID-ul curent: " + (double) frecvSignalForBSSID[-currentMeasurementArr[contorM].SignalStrength]);
                                    Log.d("ALG2", "din Totalul de frecv : " + (double) sumOfElements(frecvSignalForBSSID));
                                    */
                                    probOfSSsForThisBSSID.add((double) frecvSignalForBSSID[-currentMeasurementArr[contorM].SignalStrength] / (double) sumOfElements(frecvSignalForBSSID));
                                /*
                                Cand vreau sa imi calculez cele mai apropiate pozitii am pentru fiecare adresa MAC un vector de frecventa de lung 100 in care mi-am pus intensitatile din baza de date (De exemplu daca am 30 de intensitati cu valoarea -50 atunci elementul de la indicele 50 va avea valoarea 30 daca am 10 cu valoarea -55 atunci elementul de la indicele 55 are val 10 etc.)
                                Cand fac live o masuratoare si am o intensitate X pentru ADDR1 ma uit la vectorul de frecventa asociat adresei si imi iau ca probabilitate nr de intensitati X / nr total de intensitati (Fiecare intensitate corespunde unei singure adrese)(de exemplu X = 50 si am 20 de intensitati cu valoarea 50 din 100 => P este 1/5)
                                Problema este cand am mai multe probabilitati de combinat.
                                Pentru o poz am doua adrese Addr1 si Addr2
                                //Fie ca probabilitatea la Addr1 iese 1/3 si la Addr2 1/10 daca fac media => 13/60 daca fac P(1/3) * P(1/10) = 1/30
                                //Fie ca probabilitatea la Addr1 iese 1/5 si la Addr2 1/6  daca fac media => 11/60 daca fac P(1/5) * P(1/6) = 1/30
                                //Deci intr-o metoda au acceasi probabilitate si in alta e favorizata cealalta
                                 */
                                }
                                contorM++;
                                if (contorM >= currentMeasurementArr.length)
                                    break;
                            }
                        //Varianta cu media pentru probabilitatea unei singure adrese
                        BigDecimal sum = new BigDecimal(0.0);
                        for (Double d : probOfSSsForThisBSSID)
                            sum = sum.add(new BigDecimal(d));

                        //Am nevoie de map? oricum nu o sa mai folosesc CurrentBSSID dupa
                        probabilityOfBSSID.put(currentBSSID, sum.divide(new BigDecimal(probOfSSsForThisBSSID.size()),16,ROUND_HALF_EVEN));
                        //Log.d("Euclid Distance", "probb Of BSSID: " +currentBSSID+ " = " + sum.divide(new BigDecimal(probOfSSsForThisBSSID.size()),16,ROUND_HALF_EVEN));

                        //Am terminat cu acest BSSID. Golesc listele
                        probOfSSsForThisBSSID = new ArrayList<>();
                        currentBSSID = m.BSSID;
                        frecvSignalForBSSID = new Integer[100];
                        Arrays.fill(frecvSignalForBSSID, 0);



                    }//ies din if-ul cand s-a schimbat BSSID-ul
                } else {
                    currentBSSID = m.BSSID;
                    //frecvSignalForBSSID = new Integer[100];

                }
                frecvSignalForBSSID[-m.SignalStrength]++;
            }//out of Measure for
            pos = entry.getKey();

            //am nevoie de Map? oricum nu mai folosesc string-ul BSSID
            //daca folosesc prod pot sa ajung la nr de peste 10*E-144;
            /*
            BigDecimal prod = new BigDecimal(1.0);
            for (Map.Entry<String,BigDecimal> entry2 : probabilityOfBSSID.entrySet()) {
                Log.d("ALG2","BSSID: "+entry2.getKey() +" - probOfBSSID: "+entry2.getValue().toString());
                prod = prod.multiply(entry2.getValue());
            }

            Log.d("ALG2","\nPos Probability : \n" +pos.toString()+" \n Prod: "+prod);
            positionLikelihoodMap.put(pos,prod);
            */
            BigDecimal mean = new BigDecimal(0.0);
            for (Map.Entry<String,BigDecimal> entry2 : probabilityOfBSSID.entrySet()) {
                //Log.d("Euclid Distance","BSSID: "+entry2.getKey() +" - probOfBSSID: "+entry2.getValue().toString());
                mean = mean.add(entry2.getValue());
            }
            mean = mean.divide(new BigDecimal(probabilityOfBSSID.size()),16,ROUND_HALF_EVEN);
            //Log.d("Euclid Distance","### Pos: "+ pos+" - meanProbability: "+mean.toString());
            positionLikelihoodMap.put(pos,mean);
        }//out of pos for

        return positionLikelihoodMap;
    }

    private static LinkedHashMap<Position, BigDecimal> sortByValue(Map<Position, BigDecimal> unsortMap) {

        Set<Map.Entry<Position, BigDecimal>> set = unsortMap.entrySet();
        List<Map.Entry<Position, BigDecimal>> list = new ArrayList<>(set);
        Collections.sort( list, new Comparator<Map.Entry<Position, BigDecimal>>()
        {
            public int compare( Map.Entry<Position, BigDecimal> o1, Map.Entry<Position, BigDecimal> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        } );

        LinkedHashMap<Position,BigDecimal> sortedMap = new LinkedHashMap<>();
        for(Map.Entry<Position, BigDecimal> entry:list){
            sortedMap.put(entry.getKey(),entry.getValue());
            //Log.d("ALG",entry.getKey().toString()+" ==== "+entry.getValue());
        }


        return sortedMap;
    }



    public static Integer radiansToRounded45Degrees(float mOrientation){

        double degreeToReturn;
        degreeToReturn = Math.toDegrees((double)mOrientation); //raw
        //degreeAux = degreeAux + 22.5;

        degreeToReturn = Math.floor((degreeToReturn + 22.5)/45.0)*45;//Impartit pe N , NV , V , SV...

        if (degreeToReturn == -180)
            degreeToReturn = 180;

        return (int)degreeToReturn;
    }

    public static Integer radiansToRounded90Degrees(float mOrientation){

        double degreeToReturn;
        degreeToReturn = Math.toDegrees((double)mOrientation); //raw
        //degreeAux = degreeAux + 22.5;

        degreeToReturn = Math.floor((degreeToReturn + 45.0)/90.0)*90;//Impartit pe N , NV , V , SV...

        if (degreeToReturn == -180)
            degreeToReturn = 180;

        return (int)degreeToReturn;
    }
    public static Integer sumOfElements(Integer[] arr){
        Integer sum = 0;
        for (Integer i = 0;i<arr.length;i++)
            sum = sum + arr[i];
        return sum;
    }





}
