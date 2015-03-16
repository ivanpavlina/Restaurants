package com.exitcode.restaurants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class DatabaseAdapter {

    SQLHelper sqlHelper;
    Context context;

    public DatabaseAdapter(Context context) {
        this.context = context;
        sqlHelper = new SQLHelper(context);
    }

    public long insertRow(String name, String address, double latitude, double longitude) {

        SQLiteDatabase db = sqlHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(SQLHelper.NAME, name);
        contentValues.put(SQLHelper.ADDRESS, address);
        contentValues.put(SQLHelper.LATITUDE, latitude);
        contentValues.put(SQLHelper.LONGITUDE, longitude);

        return db.insert(SQLHelper.TABLE_NAME, null, contentValues);
    }

    public int updateNameAddress(String oldName, String newName, String newAddress) {
        SQLiteDatabase db = sqlHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(SQLHelper.NAME, newName);
        contentValues.put(SQLHelper.ADDRESS, newAddress);
        String whereClause = SQLHelper.NAME + " =? ";
        String[] whereArgs = {oldName};

        return db.update(SQLHelper.TABLE_NAME, contentValues, whereClause, whereArgs);
    }


    public ArrayList<MarkerOptions> getMarkers() {

        SQLiteDatabase db = sqlHelper.getReadableDatabase();

        String[] columns = {SQLHelper.UID, SQLHelper.NAME, SQLHelper.ADDRESS, SQLHelper.LATITUDE, SQLHelper.LONGITUDE};
        Cursor cursor = db.query(SQLHelper.TABLE_NAME, columns, null, null, null, null, null);
        ArrayList<MarkerOptions> markers = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            int indexUID = cursor.getColumnIndex(SQLHelper.UID);
            int uid = cursor.getInt(indexUID) - 1;

            int indexName = cursor.getColumnIndex(SQLHelper.NAME);
            String name = cursor.getString(indexName);

            int indexAddress = cursor.getColumnIndex(SQLHelper.ADDRESS);
            String address = cursor.getString(indexAddress);

            int indexLatitude = cursor.getColumnIndex(SQLHelper.LATITUDE);
            Double latitude = cursor.getDouble(indexLatitude);

            int indexLongitude = cursor.getColumnIndex(SQLHelper.LONGITUDE);
            Double longitude = cursor.getDouble(indexLongitude);

            MarkerOptions marker = new MarkerOptions().title(name).snippet(address).position(new LatLng(latitude, longitude));

            markers.add(uid, marker);

        }
        cursor.close();
        return markers;
    }


    public String getAddress(String name) {
        SQLiteDatabase db = sqlHelper.getReadableDatabase();

        String[] columns = {SQLHelper.ADDRESS};
        String whereClause = SQLHelper.NAME + " =? ";
        String[] whereArgs = {name};
        Cursor cursor = db.query(SQLHelper.TABLE_NAME, columns, whereClause, whereArgs, null, null, null);

        String address = null;
        while (cursor.moveToNext()) {
            int indexAddress = cursor.getColumnIndex(SQLHelper.ADDRESS);
            address = cursor.getString(indexAddress);
        }
        cursor.close();
        return address;
    }


    static class SQLHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "Database";
        private static final String TABLE_NAME = "Restaurants";
        private static final int DATABASE_VERSION = 1;

        private static final String UID = "_id";
        private static final String NAME = "Name";
        private static final String ADDRESS = "Address";
        private static final String LATITUDE = "Latitude";
        private static final String LONGITUDE = "Longitude";

        public SQLHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                    UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    NAME + " VARCHAR(255), " +
                    ADDRESS + " VARCHAR(255), " +
                    LATITUDE + " NUMERIC, " +
                    LONGITUDE + " NUMERIC);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }


}
