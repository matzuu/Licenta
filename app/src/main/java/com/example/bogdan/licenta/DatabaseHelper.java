package com.example.bogdan.licenta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static java.sql.DriverManager.println;

/**
 * Created by Bogdan on 27-Jun-18.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    //public static final String DATABASE_PATH= "data/data/com.example.bogdan.licenta/databases/";
    public static final String DATABASE_NAME = "SignalDB.db";
    public static final String TABLE_POSITION = "position_table";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "CoordX";
    public static final String COL_3 = "CoordY";
    public static final String COL_4 = "Level";
    public static final String COL_5 = "Orientation";
    public static final String COL_6 = "Cluster";
    public static final String TABLE_MEASUREMENTS = "measurements_table";
    public static final String TABLE_ROUTER = "router_table";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /*String sqlPositionTable = " create table " + TABLE_POSITION + " ( " +
                COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT , " +
                COL_2 + " INTEGER , " +
                COL_3 + " INTEGER , " +
                COL_4 + " INTEGER , " +
                COL_5 + " INTEGER , " +
                COL_6 + " TEXT ) ";*/
        Log.d("INIT", "DATABASE INITIALISE");
        String sqlPositionTable = "CREATE TABLE position_table (CoordX  REAL , CoordY  REAL, Level  INTEGER, Orientation  INTEGER,  Cluster  TEXT ," +
                " PRIMARY KEY ( CoordX, CoordY ,Orientation, Cluster ))";
        String sqlRouterTable = "CREATE TABLE router_table ( MACAddress  TEXT PRIMARY KEY )";
        String sqlMeasurementsTable = "CREATE TABLE measurements_table ( " +
                " ID  INTEGER PRIMARY KEY AUTOINCREMENT ," +
                " ref_CoordX  REAL NOT NULL," +
                " ref_CoordY  REAL NOT NULL," +
                " ref_Orientation  INTEGER NOT NULL," +
                " ref_Cluster  TEXT NOT NULL, " +
                " BSSID  TEXT NOT NULL, " +
                " SignalStrength  INTEGER NOT NULL, " +
                " FOREIGN KEY (ref_CoordX,ref_CoordY,ref_Orientation,ref_Cluster) REFERENCES position_table(CoordX,CoordY,Orientation,Cluster), " +
                " FOREIGN KEY (BSSID) REFERENCES router_table(MACAddress) ) ";
        db.execSQL(sqlPositionTable);
        Log.d("INIT", "DATABASE INITIALISE POS");
        db.execSQL(sqlRouterTable);
        Log.d("INIT", "DATABASE INITIALISE ROUTER");
        db.execSQL(sqlMeasurementsTable);
        Log.d("INIT", "DATABASE INITIALISE measurements");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSITION);
        onCreate(db);
    }

    //INSERT POS
    public long insertPosData(Position pos) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, pos.CoordX);
        contentValues.put(COL_3, pos.CoordY);
        contentValues.put(COL_4, pos.Level);
        contentValues.put(COL_5, pos.Orientation);
        contentValues.put(COL_6, pos.Cluster);
        long rowID = db.insertWithOnConflict(TABLE_POSITION, null, contentValues, CONFLICT_IGNORE);
        return rowID;
    }

    //INSERT ROUTER
    public long insertRouterData(HashSet<String> macAddressSet) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues;
        Log.d("INSEERT", "macAddressSet size: " + macAddressSet.size());
        long rowID = -2;
        db.beginTransaction();
        for (String s : macAddressSet) {
            contentValues = new ContentValues();
            contentValues.put("MACAddress", s);
            Log.d("INSEERT", "In macAddress value \n macAddress: " + s);
            rowID = db.insertWithOnConflict(TABLE_ROUTER, null, contentValues, CONFLICT_IGNORE);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        //Log.d("INSEERT","NR ContentValues: "+ contentValues.size());
        return rowID;
    }

    //INSERT SIGNAL STR
    public long insertMeasurementData(HashSet<Measurement> measurementHashSet) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("INSEERT", "insertMeasurementData SetSize: " + measurementHashSet.size());
        ContentValues contentValues;
        long rowID = -2;

        try {
            db.beginTransaction();
            Integer contor = 0;
            for (Measurement m : measurementHashSet) {
                /*Log.d("INSEERT", "In SigStr value \n Pos_ID: " + m.ref_CoordX + " " + m.ref_CoordY + " " + m.ref_Orientation + " " + m.ref_Cluster
                        + " \n BSSID: " + m.BSSID
                        + " \n SignStr: " + m.SignalStrength);
                        */
                Log.d("INSEERT","insertMeasurementData measurement nr: "+contor);
                contor++;
                contentValues = new ContentValues();
                contentValues.put("ref_CoordX", m.ref_CoordX);
                contentValues.put("ref_CoordY", m.ref_CoordY);
                contentValues.put("ref_Orientation", m.ref_Orientation);
                contentValues.put("ref_Cluster", m.ref_Cluster);
                contentValues.put("BSSID", m.BSSID);
                contentValues.put("SignalStrength", m.SignalStrength);
                rowID = db.insertOrThrow(TABLE_MEASUREMENTS, null, contentValues);
                //rowID = db.insertWithOnConflict(TABLE_MEASUREMENTS, null, contentValues, CONFLICT_IGNORE);
            }
            Log.d("INSEERT", "Insert MeasurementData returned rowID: " + rowID);
            db.setTransactionSuccessful();
            db.endTransaction();
            //Log.d("INSEERT","NR ContentValues: "+ contentValues.size());
        }
        catch (Exception e){
            Log.d("INSEERT","Caught Exception in insertMeasurementData: \n"+ e);
        }

        return rowID;
    }

    /*
    public boolean insertMultipleData(List<Position> plist , List<SignalStr> lsigstr,  List<String> lMacAddr){
        SQLiteDatabase db = this.getWritableDatabase();
        long result = -1;
        db.beginTransaction();
        if ( plist != null )
        try {

            ContentValues contentValues = new ContentValues();
            for (Position pos : plist) {
                contentValues.put(COL_2,pos.CoordX);
                contentValues.put(COL_3,pos.CoordY);
                contentValues.put(COL_4,pos.Level);
                contentValues.put(COL_5,pos.Orientation);
                contentValues.put(COL_6,pos.Cluster);
                result = db.insert(TABLE_POSITION,null ,contentValues);
                if (result < 0)
                    Log.d("Eroare Insert Position","Eroare Insert for position");
            }
            contentValues = new ContentValues();
            for(SignalStr i : lsigstr){
                contentValues.put("SignalStrength",i.SignalStrength);
                contentValues.put("Pos_ID",i.Pos_ID); //DE MODIFICAT
                contentValues.put("BSSID",i.BSSID);
                result = db.insert(TABLE_SIGSTR,null ,contentValues);
                if (result < 0)
                    Log.d("Eroare Insert SigStr","Eroare Insert for sigstr");
            }
            contentValues = new ContentValues();
            for(String i : lMacAddr){
                contentValues.put("MACAddress",i);
                result = db.insert(TABLE_ROUTER,null ,contentValues);
                if (result < 0)
                    Log.d("Eroare Insert SigStr","Eroare Insert for sigstr");
            }
            result = -2;
            db.setTransactionSuccessful();
        } catch (Exception exc ) {
            Log.d("EROARE", "Eroare MultipleInsert Position");
        }
        db.endTransaction();
        if(result == -2)
            return true;
        else
            return false;
    }
    */


    public Cursor getAllData(String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + tableName, null);
        return res;
    }

    public Cursor getAllPosFromCluster(String clusterName) {

        String[] tableColumns = new String[]{
                "rowid",
                "CoordX",
                "CoordY",
                "Orientation",
                "Cluster"

        };
        String whereClause = "Cluster = ?";
        String[] whereArgs = new String[]{clusterName};

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.query(TABLE_POSITION, tableColumns, whereClause, whereArgs, null, null, null);

        return res;
    }


    public Cursor queryPosition(Position p) {

        String[] tableColumns = new String[]{
                "rowid",
                "CoordX",
                "CoordY",
                "Orientation",
                "Cluster"

        };
        String whereClause = "CoordX = ? AND CoordY = ? AND Orientation = ? AND Cluster = ?";
        String[] whereArgs = new String[]{
                Double.toString(p.CoordX),
                Double.toString(p.CoordY),
                Integer.toString(p.Orientation),
                p.Cluster
        };

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.query(TABLE_POSITION, tableColumns, whereClause, whereArgs, null, null, null);
        Log.d("QUERY","Queried "+ res.getCount()+" positions ");

        return res;
    }

    public Cursor queryAllPositionsFromCluster(String clusterName) {

        String[] tableColumns = new String[]{
                "rowid",
                "CoordX",
                "CoordY",
                "Orientation",
                "Cluster"

        };
        String whereClause = "Cluster = ?";
        String[] whereArgs = new String[]{clusterName};

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.query(TABLE_POSITION, tableColumns, whereClause, whereArgs, null, null, null);

        return res;
    }

    public Cursor queryClustersFromBSSID(HashSet<String> SetBSSID) {

        String[] whereArgs = (String[]) SetBSSID.toArray(new String[SetBSSID.size()]);
        String inClause = whereArgs.toString();

        String MY_QUERY = "SELECT Cluster " +
                "FROM " + TABLE_POSITION + " p " +
                "JOIN " + TABLE_MEASUREMENTS + " m " +
                "ON p.CoordX=s.ref_CoordX AND p.CoordY=s.ref_CoordY AND p.Orientation=m.ref_Orientation AND p.Cluster = m.ref_Cluster" +
                "WHERE m.BSSID in " + inClause;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery(MY_QUERY, null);

        return res;
    }

    public Cursor queryAllPositionsFromBSSID(HashSet<String> SetBSSID) {
        String[] whereArgs = (String[]) SetBSSID.toArray(new String[SetBSSID.size()]);
        String inClause = whereArgs.toString();

        String MY_QUERY = "SELECT " + TABLE_POSITION + ".* , " + TABLE_MEASUREMENTS + ".SignalStrength , " + TABLE_MEASUREMENTS + ".BSSID " +
                "FROM " + TABLE_POSITION + " p " +
                "JOIN " + TABLE_MEASUREMENTS + " m " +
                "ON p.CoordX=s.ref_CoordX AND p.CoordY=m.ref_CoordY AND p.Orientation=m.ref_Orientation AND p.Cluster = m.ref_Cluster" +
                "WHERE m.BSSID in " + inClause;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery(MY_QUERY, null);

        return res;
    }

    public Cursor queryAllPositionsAndMeasurementsFromBSSIDandCluster(HashSet<String> SetBSSID, String clusterName) {
        String[] whereArgs = SetBSSID.toArray(new String[SetBSSID.size() + 1]);
        whereArgs[SetBSSID.size()] = clusterName;
        String inClause = whereArgs.toString();

        String MY_QUERY = "SELECT " + TABLE_POSITION + ".* , " + TABLE_MEASUREMENTS + ".ID , " + TABLE_MEASUREMENTS + ".SignalStrength , " + TABLE_MEASUREMENTS + ".BSSID " +
                " FROM " + TABLE_POSITION +
                " JOIN " + TABLE_MEASUREMENTS +
                " ON CoordX = ref_CoordX AND CoordY = ref_CoordY AND Orientation = ref_Orientation AND Cluster = ref_Cluster" +
                " WHERE BSSID in (" + makeQuestionmarks(SetBSSID.size()) + ") AND Cluster = ? " +
                " ORDER BY CoordX ASC , CoordY ASC , Orientation ASC ";


        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery(MY_QUERY, whereArgs);

        return res;
    }

    public Cursor querykNN(HashSet<String> SetBSSID, Integer orientation, String cluster, Integer degreeNo){
        // mut din -179,180 in 1,360
        if (orientation == null || cluster == null || degreeNo == null) {
            Cursor res = null;
            return res;
        }
        Integer minOrient = ((orientation + 179)/ (360/degreeNo)) * (360/degreeNo) - 179;
        Integer maxOrient = minOrient + (360/degreeNo) - 1;



        String[] whereArgs = SetBSSID.toArray(new String[SetBSSID.size() + 1]); //+ degreeCheck +
        whereArgs[SetBSSID.size()] = cluster;
        String inClause = whereArgs.toString();

        String MY_QUERY = "SELECT * "+
                " FROM " + TABLE_MEASUREMENTS +
                " WHERE BSSID in (" + makeQuestionmarks(SetBSSID.size()) + ") AND  ref_Orientation BETWEEN "+ minOrient+" AND "+ maxOrient +" AND ref_Cluster = ? " +
                " ORDER BY ref_CoordX ASC , ref_CoordY ASC , ref_Orientation ASC ";

        Log.d("QUERY","in queryKNN \n orienent="+orientation);
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery(MY_QUERY, whereArgs);

        return res;
    }

    public Cursor querySortedBSSIDOccurrences(HashSet<String> SetBSSID,String cluster, Integer apSize){

        String[] whereArgs = SetBSSID.toArray(new String[SetBSSID.size() + 1]);
        whereArgs[SetBSSID.size()] = cluster;
        String inClause = whereArgs.toString();

        String MY_QUERY = "SELECT MACAddress , COUNT(MACAddress) AS value_Occurrence " +
                " FROM " + TABLE_ROUTER +
                " JOIN " + TABLE_MEASUREMENTS +
                " ON BSSID=MACAddress" +
                " WHERE BSSID in (" + makeQuestionmarks(SetBSSID.size()) + ") AND ref_Cluster = ? " +
                " GROUP BY MACAddress "+
                " ORDER BY value_Occurrence DESC"+
                " LIMIT "+apSize;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery(MY_QUERY, whereArgs);
        return res;

    }

    public Cursor queryAllMeasurementsFromPosition(Position p){
        String[] tableColumns = new String[]{
                "ID",
                "ref_CoordX",
                "ref_CoordY",
                "ref_Orientation",
                "ref_Cluster",
                "BSSID",
                "SignalStrength"
        };
        String whereClause = "ref_CoordX = ? AND ref_CoordY = ? AND ref_Orientation = ? AND ref_Cluster = ?";
        String[] whereArgs = new String[]{p.CoordX.toString(),p.CoordY.toString(),p.Orientation.toString(),p.Cluster};
        Log.d("QUERY", "queryAllMeasurementsFromPosition whereArgs: " + whereArgs[0] + " " + whereArgs[1] +" " + whereArgs[2] +" " + whereArgs[3]);
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.query(TABLE_MEASUREMENTS, tableColumns, whereClause, whereArgs, null, null, null);
        return res;

    }

    public Cursor queryAllMeasurementsFromBSSID(HashSet<String> SetBSSID) {

        String[] tableColumns = new String[]{
                "ID",
                "ref_CoordX",
                "ref_CoordY",
                "ref_Orientation",
                "ref_Cluster",
                "BSSID",
                "SignalStrength"
        };
        String whereClause = "BSSID IN (" + makeQuestionmarks(SetBSSID.size()) + ")";
        String[] whereArgs = SetBSSID.toArray(new String[SetBSSID.size()]);
        Log.d("QUERY", "whereArgs: " + whereArgs);
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.query(TABLE_MEASUREMENTS, tableColumns, whereClause, whereArgs, null, null, null);

        return res;
    }

    private String makeQuestionmarks(int length) {
        if (length < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders in whereClause!");
        } else {
            StringBuilder sb = new StringBuilder(length * 2 - 1);
            sb.append("?");
            for (int i = 1; i < length; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }

    public long getMeasurementCount() {
        return DatabaseUtils.queryNumEntries(this.getWritableDatabase(), TABLE_MEASUREMENTS, null);
    }

    //UPDATE
    public boolean updatePosData(String id, Double coordX, Double coordY, Integer level, Integer orientation, String cluster) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, coordX);
        contentValues.put(COL_3, coordY);
        contentValues.put(COL_4, level);
        contentValues.put(COL_5, orientation);
        contentValues.put(COL_6, cluster);
        db.update(TABLE_POSITION,
                contentValues,
                COL_2 + " = ? AND " + COL_3 + " = ? AND " + COL_6 + " = ?",
                new String[]{coordX.toString(), coordY.toString(), cluster});
        return true;
    }

    //DELETE
    public Integer deleteData(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_POSITION, COL_1 + " = " + id, null);
    }


    public Integer deleteMeasurementAtPosData(Position p) {

        String[] whereArgs = new String[]{p.CoordX.toString(),p.CoordY.toString(),p.Orientation.toString(),p.Cluster};
        Log.d("QUERY", "deleteMeasurementAtPosData whereArgs: " + whereArgs[0] + " " + whereArgs[1] +" " + whereArgs[2] +" " + whereArgs[3]);
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MEASUREMENTS, "ref_CoordX = ? AND ref_CoordY = ? AND ref_Orientation = ? AND ref_Cluster = ? ",whereArgs);

    }

    public Integer deleteCluster(String cluster){
        String[] whereArgs = new String[]{cluster};
        Log.d("QUERY", "deleteMeasurementAtPosData whereArgs: " + whereArgs[0]);
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEASUREMENTS, " ref_Cluster = ? ",whereArgs);
        db.delete(TABLE_POSITION,"Cluster = ? ",whereArgs);
        return 1;
    }
}


