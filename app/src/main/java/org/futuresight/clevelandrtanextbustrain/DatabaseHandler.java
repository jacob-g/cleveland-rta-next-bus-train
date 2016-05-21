package org.futuresight.clevelandrtanextbustrain;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jacob on 5/15/16.
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "rtaNextBusTrain";

    //the names for the various tables
    //favorite_locations table
    private static final String FAVORITE_LOCATIONS_TABLE = "favorite_locations";
        private static final String FIELD_NAME = "name";
        private static final String FIELD_LINE_NAME = "line_name";
        private static final String FIELD_LINE_ID = "line_id";
        private static final String FIELD_DIR_NAME = "dir_name";
        private static final String FIELD_DIR_ID = "dir_id";
        private static final String FIELD_STATION_NAME = "station_name";
        private static final String FIELD_STATION_ID = "station_id";

    private static final String LINES_TABLE = "lines";

    private static final String CONFIG_TABLE = "config";
        private static final String FIELD_VALUE = "value";

    //the universal names for fields
    private static final String ID = "id"; //the universal "id" field in each table
    private static final String NAME = "name";
    private static final String FIELD_EXPIRES = "expires";

    //config values
    private static final String CONFIG_LAST_SAVED_LINES = "last_saved_lines";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_FAVORITE_LOCATIONS_TABLE = "CREATE TABLE IF NOT EXISTS " + FAVORITE_LOCATIONS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_NAME + " TEXT,"
                + FIELD_LINE_NAME + " TEXT,"
                + FIELD_LINE_ID + " INT,"
                + FIELD_DIR_NAME + " TEXT,"
                + FIELD_DIR_ID + " INT,"
                + FIELD_STATION_NAME + " TEXT,"
                + FIELD_STATION_ID + " INT"
                + ")";
        db.execSQL(CREATE_FAVORITE_LOCATIONS_TABLE);

        String CREATE_LINES_TABLE = "CREATE TABLE IF NOT EXISTS " + LINES_TABLE + "("
                + ID + " INTEGER PRIMARY KEY,"
                + NAME + " TEXT"
                + ")";
        db.execSQL(CREATE_LINES_TABLE);

        String CREATE_CONFIG_TABLE = "CREATE TABLE IF NOT EXISTS " + CONFIG_TABLE + "("
                + FIELD_NAME + " TEXT PRIMARY KEY,"
                + FIELD_VALUE + " TEXT"
                + ")";
        db.execSQL(CREATE_CONFIG_TABLE);
        setConfig(db, CONFIG_LAST_SAVED_LINES, "0");
    }

    private void setConfig(SQLiteDatabase db, String key, String val) {
        String selectQuery = "SELECT 1 FROM " + CONFIG_TABLE + " WHERE " + FIELD_NAME + "=?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{key});
        if (cursor.moveToFirst()) {
            //insert
            ContentValues values = new ContentValues();
            values.put(FIELD_VALUE, val);
            db.update(CONFIG_TABLE, values, FIELD_NAME + "=?", new String[]{key});
        } else {
            ContentValues values = new ContentValues();
            values.put(FIELD_NAME, key);
            values.put(FIELD_VALUE, val);
            db.insert(CONFIG_TABLE, null, values);
        }
        cursor.close();
    }

    private String getConfig(SQLiteDatabase db, String key) {
        String selectQuery = "SELECT " + FIELD_VALUE + " FROM " + CONFIG_TABLE + " WHERE " + FIELD_NAME + "=?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{key});
        if (cursor.moveToFirst()) {
            System.out.println("It is there.");
            return cursor.getString(0);
        } else {
            return "";
        }
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Create tables again
        onCreate(db);
    }

    public void recreateTablesWithoutErasing() {
        SQLiteDatabase db = this.getWritableDatabase();
        onCreate(db);
        db.close();
    }

    public void fry() { //erase everything, hopefully not needed
        SQLiteDatabase db = this.getWritableDatabase();

        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + FAVORITE_LOCATIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LINES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CONFIG_TABLE);

        // Create tables again
        onCreate(db);
        db.close();
    }

    public void addFavoriteLocation(Station st) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FIELD_NAME, st.getName());
        values.put(FIELD_STATION_NAME, st.getStationName()); //station name
        values.put(FIELD_STATION_ID, st.getStationId()); //station name
        values.put(FIELD_DIR_NAME, st.getDirName()); //station name
        values.put(FIELD_DIR_ID, st.getDirId()); //station name
        values.put(FIELD_LINE_NAME, st.getLineName()); //station name
        values.put(FIELD_LINE_ID, st.getLineId()); //station name
        // Inserting Row
        db.insert(FAVORITE_LOCATIONS_TABLE, null, values);
        db.close(); // Closing database connection
    }

    public boolean deleteFavoriteStation(Station st) { //delete a station from the favorites
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + FAVORITE_LOCATIONS_TABLE + " WHERE " + FIELD_STATION_ID + "=" + st.getStationId() + " AND " + FIELD_DIR_ID + "=" + st.getDirId() + " AND " + FIELD_LINE_ID + "=" + st.getLineId());
        System.out.println("DELETE FROM " + FAVORITE_LOCATIONS_TABLE + " WHERE " + FIELD_STATION_ID + "=" + st.getStationId() + " AND " + FIELD_DIR_ID + "=" + st.getDirId() + " AND " + FIELD_LINE_ID + "=" + st.getLineId());
        db.close(); // Closing database connection
        return true;
    }

    public List<Station> getFavoriteLocations() {
        List<Station> stations = new ArrayList<Station>();
        String selectQuery = "SELECT " + FIELD_NAME + "," + FIELD_STATION_NAME + "," + FIELD_STATION_ID + "," + FIELD_DIR_NAME + "," + FIELD_DIR_ID + "," + FIELD_LINE_NAME + "," + FIELD_LINE_ID + " FROM " + FAVORITE_LOCATIONS_TABLE + " ORDER BY " + FIELD_STATION_NAME + " ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Station st = new Station(cursor.getString(1), Integer.parseInt(cursor.getString(2)), cursor.getString(3), Integer.parseInt(cursor.getString(4)), cursor.getString(5), Integer.parseInt(cursor.getString(6)), cursor.getString(0));
                stations.add(st);
            } while (cursor.moveToNext());
        }
        db.close();
        return stations;
    }

    public void renameStation(Station st) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FIELD_NAME, st.getName());
        db.update(FAVORITE_LOCATIONS_TABLE, values, FIELD_STATION_ID + "=" + st.getStationId() + " AND " + FIELD_DIR_ID + "=" + st.getDirId() + " AND " + FIELD_LINE_ID + "=" + st.getLineId(), null);
        db.close(); // Closing database connection
    }

    public Map<String, Integer> getStoredLines() {
        SQLiteDatabase db = this.getWritableDatabase();

        Map<String, Integer> outMap = new HashMap<>();

        //make sure it's recent
        String lastSavedStr = getConfig(db, CONFIG_LAST_SAVED_LINES);
        int lastSavedInt = 0;
        if (lastSavedStr != "") {
            lastSavedInt = Integer.parseInt(lastSavedStr);
        }
        if (lastSavedInt < PersistentDataController.getCurTime() - PersistentDataController.getLineExpiry()) {
            System.out.println("Lines too old! " + lastSavedInt);
            db.execSQL("DELETE FROM " + LINES_TABLE);
            db.close(); // Closing database connection
            return outMap;
        }

        String selectQuery = "SELECT " + ID + "," + FIELD_NAME + " FROM " + LINES_TABLE + " ORDER BY " + FIELD_NAME + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                outMap.put(name, id);
            } while (cursor.moveToNext());
        }
        db.close();
        return outMap;
    }

    public void saveLines(Map<String, Integer> lines) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (String l : lines.keySet()) {
            ContentValues values = new ContentValues();
            values.put(ID, lines.get(l));
            values.put(NAME, l); //station name
            db.insert(LINES_TABLE, null, values);
        }
        //save config entry
        setConfig(db, CONFIG_LAST_SAVED_LINES, String.valueOf(PersistentDataController.getCurTime()));
        System.out.println("Last saved: " + getConfig(db, CONFIG_LAST_SAVED_LINES));
        db.close();
    }
}
