package com.example.bogdan.licenta;

import android.database.Cursor;
import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.math.BigDecimal.ROUND_HALF_EVEN;

public class Algorithms {









    public static Position[] kNN(HashSet<Measurement> liveMeasurementsSet, String cluster, DatabaseHelper myDb, Integer degreeNo, Integer k, Integer trainingMeasurements, Integer apSize){

        if (liveMeasurementsSet.size() < 1){
            return null;
        }
        Log.d("ALG","Entering Algorithm kNN");

        HashMap<Position,List<Measurement>> posMeasureHashMap = new HashMap<>();
        Position pos = new Position();
        Measurement m;
        List<Measurement> measurementList = new ArrayList<>();
        Integer orientation = null;

        HashSet<String> macAddressSet = new HashSet<>();
        //imi iau toaate BSSID-urile din Measurment-ul facut recent.

        for (Measurement measurement : liveMeasurementsSet) {
            macAddressSet.add(measurement.BSSID);
            orientation= measurement.ref_Orientation;
        }

        Log.d("ALG","mm orientation: "+orientation);
        //iau primele apSize BSSID-uri sortate dupa nr de inregistrari
        Cursor res=myDb.querySortedBSSIDOccurrences(macAddressSet,cluster,apSize);
        if (res.getCount() == 0) {
            // show message
            Log.d("ALG","No rows retrieved");
            return null;
        }
        Map<String,Integer> macAddressFrecvMap = new HashMap<>();
        macAddressSet = new HashSet<>();
        while (res.moveToNext()) {
            macAddressSet.add(res.getString(res.getColumnIndex("MACAddress")));
            macAddressFrecvMap.put(res.getString(res.getColumnIndex("MACAddress")),0);
            //Log.d("QUERY","returned BSSID: "+res.getString(res.getColumnIndex("MACAddress")) );
        }
        //Log.d("ALG","Intermediate number of BSSID: "+macAddressSet.size());
        //Cursor res2 = myDb.queryAllPositionsAndMeasurementsFromBSSIDandCluster(macAddressSet,cluster); veche , fara orientari

        //verifcare ptr nr trainingMeasurements o fac dupa parcurgerea cursorului.

        Cursor res2=myDb.querykNN(macAddressSet,orientation,cluster,degreeNo);
        if (res2 == null) {
            Log.d("ALG","No rows retrieved");
            return null;
        }
        if (res2.getCount() == 0) {
            // show message
            Log.d("ALG","No rows retrieved");
            return null;
        }
        Integer nrOfMeasurements = 1;
        Integer curPosCount = 1;
        if(res2.moveToFirst()) {

            pos = new Position();
            pos.CoordX = res2.getDouble(res2.getColumnIndex("ref_CoordX"));
            pos.CoordY = res2.getDouble(res2.getColumnIndex("ref_CoordY"));
            pos.Orientation = res2.getInt(res2.getColumnIndex("ref_Orientation"));
            pos.Cluster = res2.getString(res2.getColumnIndex("ref_Cluster"));
            m = new Measurement();
            m.id = res2.getInt(res2.getColumnIndex("ID"));
            m.ref_CoordX = res2.getDouble(res2.getColumnIndex("ref_CoordX"));
            m.ref_CoordY = res2.getDouble(res2.getColumnIndex("ref_CoordY"));
            m.ref_Orientation = res2.getInt(res2.getColumnIndex("ref_Orientation"));
            m.ref_Cluster = res2.getString(res2.getColumnIndex("ref_Cluster"));
            m.BSSID = res2.getString(res2.getColumnIndex("BSSID"));
            m.SignalStrength = res2.getInt(res2.getColumnIndex("SignalStrength"));
            measurementList.add(m);

            //updatez vectoru care limiteaza nr masuratorilor la trainingMeasurements
            macAddressFrecvMap.put(m.BSSID,macAddressFrecvMap.get(m.BSSID)+1);
        }
        while (res2.moveToNext()) {

            //verific daca citesc alta pozitie
            if(     (pos.CoordX != res2.getDouble(res2.getColumnIndex ("ref_CoordX" )) ||
                    pos.CoordY != res2.getDouble(res2.getColumnIndex ("ref_CoordY" )) ||
                    pos.Orientation != res2.getInt(res2.getColumnIndex ("ref_Orientation" )))
                    ){
                //Log.d("kNN","pos="+pos.CoordX+","+pos.CoordY+";Orient="+pos.Orientation+";measureList.size()="+measurementList.size());
                //adaug poz de dinainte si measureliste-ul
                posMeasureHashMap.put(pos,measurementList);
                //ma pregatesc ptr urmatoarea poz
                pos = new Position();
                pos.CoordX = res2.getDouble(res2.getColumnIndex ("ref_CoordX" ));
                pos.CoordY = res2.getDouble(res2.getColumnIndex ("ref_CoordY" ));
                pos.Orientation = res2.getInt(res2.getColumnIndex ("ref_Orientation" ));
                pos.Cluster = res2.getString(res2.getColumnIndex ("ref_Cluster" ));
                measurementList = new ArrayList<>();

                //resetez vectorul cu BSSID
                for (Map.Entry<String, Integer> entry : macAddressFrecvMap.entrySet())
                {
                    entry.setValue(0);
                }

            }
            nrOfMeasurements++;
            m = new Measurement();
            m.BSSID = res2.getString(res2.getColumnIndex("BSSID"));
            //verific daca pentru pozitia curenta am mai mult de  trainingMeasurements   masuratori facute cu un anumit BSSID (vectorul e resetat la poz noua)
            if(macAddressFrecvMap.get(m.BSSID) < trainingMeasurements ){ //trebuie < strict, nu <=

                m.id = res2.getInt(res2.getColumnIndex("ID"));
                m.ref_CoordX = res2.getDouble(res2.getColumnIndex("ref_CoordX"));
                m.ref_CoordY = res2.getDouble(res2.getColumnIndex("ref_CoordY"));
                m.ref_Orientation = res2.getInt(res2.getColumnIndex("ref_Orientation"));
                m.ref_Cluster = res2.getString(res2.getColumnIndex("ref_Cluster"));
                m.SignalStrength = res2.getInt(res2.getColumnIndex("SignalStrength"));

                measurementList.add(m);
                macAddressFrecvMap.put(m.BSSID,macAddressFrecvMap.get(m.BSSID)+1);
            }
        }//Am terminat de citit din cursor

        posMeasureHashMap.put(pos,measurementList);
        //Log.d("ALG","Queried Measurement Map: \n" + buffer.toString()); //spam
        Log.d("ALG","Finished cursor read: \n PosMeasHASHMAP.size(): "+posMeasureHashMap.size()+"\n Total mm queried: "+nrOfMeasurements);
        //poz cititite; trebuie gasita cea mai similara poz;

        //HashMap<Position,BigDecimal> posEuclidianDistance = calculateFreqDistance(posMeasureHashMap,liveMeasurementsSet);
        HashMap<Position,Double> posEuclidianDistance = calculateEuclidDistance(posMeasureHashMap,liveMeasurementsSet);
        Log.d("ALG","Exited calculateED \n Sorting the map");
        LinkedHashMap<Position,Double> sortedMap = sortByDoubleValue(posEuclidianDistance,0); // 1 for desc
        LinkedHashMap<Position,Double> returnedMap = new LinkedHashMap<>();
        Position retPos = new Position(0.0,0.0,0,"resultKNN");
        Position retWeightedPos = new Position(0.0,0.0,0,"resultKNN");
        Integer contor = 0;
        Double totalWeight = 0.0;



        for (LinkedHashMap.Entry<Position,Double> entry : sortedMap.entrySet()) {
            returnedMap.put(entry.getKey(),entry.getValue());
            retPos.CoordX +=entry.getKey().CoordX;
            retPos.CoordY +=entry.getKey().CoordY;

            totalWeight +=entry.getValue();
            contor++;
            if(contor >= k )//de inlocuit cu k
                break;
        }
        retPos.CoordX = retPos.CoordX/k;
        retPos.CoordY = retPos.CoordY/k;

        contor = 0;
        if (sortedMap.size()>1 && k > 1) {
            for (LinkedHashMap.Entry<Position, Double> entry : sortedMap.entrySet()) {
                retWeightedPos.CoordX += (1 - (entry.getValue() / totalWeight)) * entry.getKey().CoordX;
                retWeightedPos.CoordY += (1 - (entry.getValue() / totalWeight)) * entry.getKey().CoordY;
                contor++;
                if (contor >= k)//de inlocuit cu k
                    break;
            }
        } else {
            retWeightedPos.CoordX = retPos.CoordX;
            retWeightedPos.CoordY = retPos.CoordY;

        }





        //return returnedMap;
        Position[] retArr = new Position[2];
        retArr[0] = retPos;
        retArr[1] = retWeightedPos;

        Log.d("ALG","Exiting kNN");
        return retArr;
    }


    public static HashMap<Position,Double> calculateEuclidDistance(HashMap<Position,List<Measurement>> posMeasureHASHMAP, HashSet<Measurement> liveMeasurementSet){

        Position pos;
        HashMap<Position,Double> positionLikelihoodMap = new HashMap<>();
        //setul e folosit doar aici
        //Sortez dupa BSSID
        Measurement[] liveMeasurmentArr =  liveMeasurementSet.toArray(new Measurement[liveMeasurementSet.size()]);
        Arrays.sort(liveMeasurmentArr,new Comparator<Measurement>() {
            @Override
            public int compare(final Measurement object1, final Measurement object2) {
                return object1.getBSSID().compareTo(object2.getBSSID());
            }
        });
        //folosit doar la log.d
        /*
        for (Measurement m:liveMeasurmentArr){
            Log.d("ED","X="+m.ref_CoordX+";Y="+m.ref_CoordY+";deg="+m.ref_Orientation+";BSSID="+m.BSSID+";SS="+m.SignalStrength);
        }
        */
        //Log.d("Euclid Distance","currentMeasurementArr.size() - "+ currentMeasurementArr.length);
        List<Measurement> dbMeasurementList;
        Integer[] frecvSignalForBSSID;
        String currentBSSID = null;
        Integer contorM = 0;

        for (Map.Entry<Position,List<Measurement>> entry : posMeasureHASHMAP.entrySet()){
            //Log.d("Euclid Distance","entry-for: Position - " + entry.getKey().toString());

            dbMeasurementList = entry.getValue();
            //Collections.sort(measurementList);
            Collections.sort(dbMeasurementList, new Comparator<Measurement>() {
                @Override
                public int compare(final Measurement object1, final Measurement object2) {
                    return object1.getBSSID().compareTo(object2.getBSSID());
                }
            });
            //Log.d("Euclid Distance", "Array sortat: \n" );

            /*frecvSignalForBSSID = new Integer[100];
            Arrays.fill(frecvSignalForBSSID, 0);*/

            List<Double> probOfSSsForThisLiveBSSID = new ArrayList<>();
            List<Double> probOfSSsForThisDbBSSID = new ArrayList<>();
            contorM = 0;

            //HashMap<String,BigDecimal> probabilityOfBSSID = new HashMap<>();
            HashMap<String,Double> distanceOfBSSID = new HashMap<>();

            //Log.d("Euclid Distance","measurementList.size() - "+ measurementList.size());
            HashMap<String,Double> liveMeans = new HashMap<>();
            HashMap<String,Double> dbMeans = new HashMap<>();

            for (Measurement m : dbMeasurementList) {

                if(currentBSSID != null) {
                    if (currentBSSID.compareTo(m.BSSID) != 0) {

                        //Log.d("ALG2", "currentBSSID: " + currentBSSID);
                        //Log.d("ALG2", "m.BSSID: " + m.BSSID);
                        //lista si vectorul sunt sortate alfabetic dupa BSSID
                        //Cand termin cu un BSSID , inainte sa trec la urmatorul , iau masuratorile din currentMeasureArr cu acelasi BSSID si verific care e probabilitatea sa sa apara, data fiind vectorul de frecventa
                        if (contorM < liveMeasurmentArr.length) {
                            while (currentBSSID.compareTo(liveMeasurmentArr[contorM].BSSID) >= 0) {
                                //Log.d("ALG2", "currentMeasurementArr[contorM].BSSID " + currentMeasurementArr[contorM].BSSID);
                                if (currentBSSID.compareTo(liveMeasurmentArr[contorM].BSSID) == 0) {
                                    //Iau probabilitatea ca semnalul din currentMeasurement sa fie in vectorul de frecventa atunci cand stiu ca am terminat cu un BSSID
                                    //in currentMeasurment sunt BSSID in fucntie de cate ori scanez
                                    /*
                                    Log.d("ALG2", "contorM: "+contorM);
                                    Log.d("ALG2", "currentMeasurment SignalStr: "+currentMeasurementArr[contorM].SignalStrength);
                                    Log.d("ALG2", "frecv SS pt BSSID-ul curent: " + (double) frecvSignalForBSSID[-currentMeasurementArr[contorM].SignalStrength]);
                                    Log.d("ALG2", "din Totalul de frecv : " + (double) sumOfElements(frecvSignalForBSSID));
                                    */

                                    probOfSSsForThisLiveBSSID.add((double) liveMeasurmentArr[contorM].SignalStrength);
                                    //todo probOfSSsForThisBSSID.add((double) frecvSignalForBSSID[-liveMeasurmentArr[contorM].SignalStrength] / (double) sumOfElements(frecvSignalForBSSID));
                                }
                                contorM++;
                                if (contorM >= liveMeasurmentArr.length)
                                    break;
                            }
                            //Varianta cu media pentru probabilitatea unei singure adrese
                            if(probOfSSsForThisLiveBSSID.size()!=0 && probOfSSsForThisDbBSSID.size()!= 0) {

                                Double liveMeanForThisBSSID = 0.0;
                                for (Double d : probOfSSsForThisLiveBSSID)
                                    liveMeanForThisBSSID += d;
                                liveMeanForThisBSSID = liveMeanForThisBSSID / probOfSSsForThisLiveBSSID.size();

                                Double dbMeanForThisBSSID = 0.0;
                                for (Double d : probOfSSsForThisDbBSSID)
                                    dbMeanForThisBSSID += d;
                                dbMeanForThisBSSID = dbMeanForThisBSSID / probOfSSsForThisDbBSSID.size();

                                //Log.d("ED", "probDbBSSID.size()" + probOfSSsForThisDbBSSID.size() + "  probLiveBSSID.size()" + probOfSSsForThisLiveBSSID.size());
                                //Log.d("ED", "dbMean: " + dbMeanForThisBSSID + "   dbLive: " + liveMeanForThisBSSID);
                                distanceOfBSSID.put(currentBSSID, Math.abs(dbMeanForThisBSSID - liveMeanForThisBSSID));
                                //////////probabilityOfBSSID.put(currentBSSID, liveMeanForThisBSSID.divide(new BigDecimal(probOfSSsForThisLiveBSSID.size()), 16, ROUND_HALF_EVEN));
                                //Log.d("Euclid Distance", "probb Of BSSID: " +currentBSSID+ " = " + sum.divide(new BigDecimal(probOfSSsForThisBSSID.size()),16,ROUND_HALF_EVEN));


                                //Am terminat cu acest BSSID. Golesc listele


                                probOfSSsForThisLiveBSSID = new ArrayList<>();
                                probOfSSsForThisDbBSSID = new ArrayList<>();
                                currentBSSID = m.BSSID;
                            /*frecvSignalForBSSID = new Integer[100];
                            Arrays.fill(frecvSignalForBSSID, 0);*/
                            }

                        }
                    }//ies din if-ul cand s-a schimbat BSSID-ul
                } else {
                    currentBSSID = m.BSSID;
                    //frecvSignalForBSSID = new Integer[100];

                }
                probOfSSsForThisDbBSSID.add((double)m.SignalStrength);
                //frecvSignalForBSSID[-m.SignalStrength]++; ptr prob doar

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
            //Log.d("Euclid Distance","distanceOfBSSID.size(): "+distanceOfBSSID.size());
            if(distanceOfBSSID.size()>0) { //evit impartirea la 0, nu am gasit nimic pentru o anumita pozitie
                double mean = 0.0;
                for (Map.Entry<String, Double> entry2 : distanceOfBSSID.entrySet()) {
                    //Log.d("Euclid Distance", "BSSID: " + entry2.getKey() + " - Distance: " + entry2.getValue().toString());
                    mean += entry2.getValue();
                }
                mean = mean/distanceOfBSSID.size();
                //Log.d("Euclid Distance","### Pos: "+ pos+" - meanProbability: "+mean.toString());
                positionLikelihoodMap.put(pos, mean);
            }
        }//out of pos for

        return positionLikelihoodMap;
    }




    public static HashMap<Position,BigDecimal> calculateFreqDistance(HashMap<Position,List<Measurement>> posMeasureHASHMAP, HashSet<Measurement> liveMeasurementSet){



        Position pos;
        HashMap<Position,BigDecimal> positionLikelihoodMap = new HashMap<>();
        //setul e folosit doar aici
        //Sortez dupa BSSID
        Measurement[] liveMeasurmentArr =  liveMeasurementSet.toArray(new Measurement[liveMeasurementSet.size()]);
        Arrays.sort(liveMeasurmentArr,new Comparator<Measurement>() {
            @Override
            public int compare(final Measurement object1, final Measurement object2) {
                return object1.getBSSID().compareTo(object2.getBSSID());
            }
        });
        //folosit doar la log.d
        for (Measurement m:liveMeasurmentArr){
            Log.d("ED","X="+m.ref_CoordX+";Y="+m.ref_CoordY+";deg="+m.ref_Orientation+";BSSID="+m.BSSID+";SS="+m.SignalStrength);
        }
        List<Measurement> dbMeasurementList;
        for (Map.Entry<Position,List<Measurement>> entry : posMeasureHASHMAP.entrySet()){
            //Log.d("Euclid Distance","In for: Position - " + entry.getKey().toString());


            dbMeasurementList = entry.getValue();
            //Collections.sort(measurementList);
            Collections.sort(dbMeasurementList, new Comparator<Measurement>() {
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


            for (Measurement m : dbMeasurementList) {

                if(currentBSSID != null) {
                    if (currentBSSID.compareTo(m.BSSID) != 0) {

                        //Log.d("ALG2", "currentBSSID: " + currentBSSID);
                        //Log.d("ALG2", "m.BSSID: " + m.BSSID);
                        //lista si vectorul sunt sortate alfabetic dupa BSSID
                        //Cand termin cu un BSSID , inainte sa trec la urmatorul , iau masuratorile din currentMeasureArr cu acelasi BSSID si verific care e probabilitatea sa sa apara, data fiind vectorul de frecventa
                        if (contorM < liveMeasurmentArr.length) { //daca e mai mare atunci inseamna ca nu mai am ce sa compar
                            while (currentBSSID.compareTo(liveMeasurmentArr[contorM].BSSID) >= 0) {
                                //Log.d("ALG2", "currentMeasurementArr[contorM].BSSID " + currentMeasurementArr[contorM].BSSID);
                                if (currentBSSID.compareTo(liveMeasurmentArr[contorM].BSSID) == 0) {
                                    //Iau probabilitatea ca semnalul din currentMeasurement sa fie in vectorul de frecventa atunci cand stiu ca am terminat cu un BSSID
                                    //in currentMeasurment sunt BSSID in fucntie de cate ori scanez
                                    /*
                                    Log.d("ALG2", "contorM: "+contorM);
                                    Log.d("ALG2", "currentMeasurment SignalStr: "+currentMeasurementArr[contorM].SignalStrength);
                                    Log.d("ALG2", "frecv SS pt BSSID-ul curent: " + (double) frecvSignalForBSSID[-currentMeasurementArr[contorM].SignalStrength]);
                                    Log.d("ALG2", "din Totalul de frecv : " + (double) sumOfElements(frecvSignalForBSSID));
                                    */
                                    probOfSSsForThisBSSID.add((double) frecvSignalForBSSID[-liveMeasurmentArr[contorM].SignalStrength] / (double) sumOfElements(frecvSignalForBSSID));
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
                                if (contorM >= liveMeasurmentArr.length)
                                    break;
                            }
                            //Varianta cu media pentru probabilitatea unei singure adrese
                            BigDecimal sum = new BigDecimal(0.0);
                            for (Double d : probOfSSsForThisBSSID)
                                sum = sum.add(new BigDecimal(d));

                            //Am nevoie de map? oricum nu o sa mai folosesc CurrentBSSID dupa
                            probabilityOfBSSID.put(currentBSSID, sum.divide(new BigDecimal(probOfSSsForThisBSSID.size()), 16, ROUND_HALF_EVEN));
                            //Log.d("Euclid Distance", "probb Of BSSID: " +currentBSSID+ " = " + sum.divide(new BigDecimal(probOfSSsForThisBSSID.size()),16,ROUND_HALF_EVEN));

                            //Am terminat cu acest BSSID. Golesc listele
                            probOfSSsForThisBSSID = new ArrayList<>();
                            currentBSSID = m.BSSID;
                            frecvSignalForBSSID = new Integer[100];
                            Arrays.fill(frecvSignalForBSSID, 0);
                        }
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

    private static LinkedHashMap<Position, BigDecimal> sortByBigDecimalValue(Map<Position, BigDecimal> unsortMap, final int type) {

        //type = 0 => ASC type == 1 => DESC
        Set<Map.Entry<Position, BigDecimal>> set = unsortMap.entrySet();
        List<Map.Entry<Position, BigDecimal>> list = new ArrayList<>(set);
        Collections.sort( list, new Comparator<Map.Entry<Position, BigDecimal>>()
        {
            public int compare( Map.Entry<Position, BigDecimal> o1, Map.Entry<Position, BigDecimal> o2 )
            {
                if ( type == 1 )
                    return (o2.getValue()).compareTo( o1.getValue() );
                else
                    return (o1.getValue()).compareTo( o2.getValue() );
            }
        } );

        LinkedHashMap<Position,BigDecimal> sortedMap = new LinkedHashMap<>();
        for(Map.Entry<Position, BigDecimal> entry:list){
            sortedMap.put(entry.getKey(),entry.getValue());
            //Log.d("ALG",entry.getKey().toString()+" ==== "+entry.getValue());
        }


        return sortedMap;
    }

    private static LinkedHashMap<Position, Double> sortByDoubleValue(Map<Position, Double> unsortMap, final int type) {

        //type = 0 => ASC type == 1 => DESC
        Set<Map.Entry<Position, Double>> set = unsortMap.entrySet();
        List<Map.Entry<Position, Double>> list = new ArrayList<>(set);
        Collections.sort( list, new Comparator<Map.Entry<Position, Double>>()
        {
            public int compare( Map.Entry<Position, Double> o1, Map.Entry<Position, Double> o2 )
            {
                if ( type == 1 )
                    return (o2.getValue()).compareTo( o1.getValue() );
                else
                    return (o1.getValue()).compareTo( o2.getValue() );
            }
        } );

        LinkedHashMap<Position,Double> sortedMap = new LinkedHashMap<>();
        for(Map.Entry<Position, Double> entry:list){
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
