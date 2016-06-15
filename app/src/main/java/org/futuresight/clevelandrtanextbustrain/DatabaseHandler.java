package org.futuresight.clevelandrtanextbustrain;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

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

    private static final String DIRS_TABLE = "directions";

    private static final String STATIONS_TABLE = "stations";

    private static final String CONFIG_TABLE = "config";
        private static final String FIELD_VALUE = "value";

    private static final String ALERTS_TABLE = "alerts";
        private static final String FIELD_TITLE = "title";
        private static final String FIELD_DESCRIPTION = "description";
        private static final String FIELD_URL = "url";

    private static final String CACHED_LINE_ALERTS_TABLE = "cached_line_alerts";

    //the universal names for fields
    private static final String ID = "id"; //the universal "id" field in each table
    private static final String NAME = "name";
    private static final String FIELD_EXPIRES = "expires";

    //config values
    private static final String CONFIG_LAST_SAVED_LINES = "last_saved_lines";

    private static boolean initialized = false;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        if (!initialized) {
            SQLiteDatabase db = this.getWritableDatabase();
            onCreate(db);
            db.close();
            initialized = true;
        }
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

        String CREATE_DIRS_TABLE = "CREATE TABLE IF NOT EXISTS " + DIRS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_DIR_ID + " INTEGER,"
                + NAME + " TEXT,"
                + FIELD_LINE_ID + " INTEGER,"
                + FIELD_EXPIRES + " INTEGER"
                + ")";
        db.execSQL(CREATE_DIRS_TABLE);

        String CREATE_STATIONS_TABLE = "CREATE TABLE IF NOT EXISTS " + STATIONS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_STATION_ID + " INTEGER,"
                + FIELD_DIR_ID + " INTEGER,"
                + NAME + " TEXT,"
                + FIELD_LINE_ID + " INTEGER,"
                + FIELD_EXPIRES + " INTEGER"
                + ")";
        db.execSQL(CREATE_STATIONS_TABLE);

        String CREATE_ALERTS_TABLE = "CREATE TABLE IF NOT EXISTS " + ALERTS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_LINE_ID + " INTEGER,"
                + FIELD_TITLE + " TEXT,"
                + FIELD_URL + " TEXT,"
                + FIELD_DESCRIPTION + " TEXT,"
                + FIELD_EXPIRES + " INTEGER"
                + ")";
        db.execSQL(CREATE_ALERTS_TABLE);

        String CREATE_CACHED_LINE_ALERTS_TABLE = "CREATE TABLE IF NOT EXISTS " + CACHED_LINE_ALERTS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_LINE_ID + " INTEGER,"
                + FIELD_EXPIRES + " INTEGER"
                + ")";
        db.execSQL(CREATE_CACHED_LINE_ALERTS_TABLE);

        boolean shouldPopulateConfig = !hasTable(db, CONFIG_TABLE);
        String CREATE_CONFIG_TABLE = "CREATE TABLE IF NOT EXISTS " + CONFIG_TABLE + "("
                + FIELD_NAME + " TEXT PRIMARY KEY,"
                + FIELD_VALUE + " TEXT"
                + ")";
        db.execSQL(CREATE_CONFIG_TABLE);
        if (shouldPopulateConfig) {
            setConfig(db, CONFIG_LAST_SAVED_LINES, "0");
        }
    }

    private boolean hasTable(SQLiteDatabase db, String table) {
        Cursor cursor = db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = '" + table + "'", null);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
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

    public void fryCache() { //erase everything, hopefully not needed
        SQLiteDatabase db = this.getWritableDatabase();

        // Drop older table if existed
        db.execSQL("DELETE FROM " + LINES_TABLE);
        db.execSQL("DELETE FROM " + DIRS_TABLE);
        db.execSQL("DELETE FROM " + STATIONS_TABLE);
        db.execSQL("DELETE FROM " + ALERTS_TABLE);
        db.execSQL("DELETE FROM " + CACHED_LINE_ALERTS_TABLE);
        setConfig(db, CONFIG_LAST_SAVED_LINES, "0");

        // Create tables again
        db.close();
    }

    public void fry() { //erase everything, hopefully not needed
        SQLiteDatabase db = this.getWritableDatabase();

        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + FAVORITE_LOCATIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LINES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DIRS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + STATIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CONFIG_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ALERTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CACHED_LINE_ALERTS_TABLE);

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
        db.close(); // Closing database connection
        return true;
    }

    public List<Station> getFavoriteLocations() {
        List<Station> stations = new ArrayList<Station>();
        String selectQuery = "SELECT f." + FIELD_NAME + ",f." + FIELD_STATION_NAME + ",f." + FIELD_STATION_ID + ",f." + FIELD_DIR_NAME + ",f." + FIELD_DIR_ID + ",l." + NAME + ",f." + FIELD_LINE_ID + " FROM " + FAVORITE_LOCATIONS_TABLE + " AS f LEFT JOIN " + LINES_TABLE + " AS l ON l." + ID + "=f." + FIELD_LINE_ID + " ORDER BY f." + FIELD_STATION_NAME + " ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                do {
                    Station st = new Station(cursor.getString(1), Integer.parseInt(cursor.getString(2)), cursor.getString(3), Integer.parseInt(cursor.getString(4)), cursor.getString(5), Integer.parseInt(cursor.getString(6)), cursor.getString(0));
                    stations.add(st);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return stations;
        }
        db.close();
        return stations;
    }

    public boolean hasFavoriteLocation(int lineId, int dirId, int stationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + FAVORITE_LOCATIONS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_DIR_ID + "=" + dirId + " AND " + FIELD_STATION_ID + "=" + stationId, null);
        boolean out = cursor.moveToFirst();
        db.close();
        return out;
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

    public boolean hasStoredLines() {
        SQLiteDatabase db = this.getWritableDatabase();

        //make sure it's recent
        String lastSavedStr = getConfig(db, CONFIG_LAST_SAVED_LINES);
        int lastSavedInt = 0;
        if (lastSavedStr != "") {
            lastSavedInt = Integer.parseInt(lastSavedStr);
        }
        if (lastSavedInt < PersistentDataController.getCurTime() - PersistentDataController.getLineExpiry()) {
            db.execSQL("DELETE FROM " + LINES_TABLE);
            db.close(); // Closing database connection
            return false;
        }

        String selectQuery = "SELECT " + ID + "," + FIELD_NAME + " FROM " + LINES_TABLE + " ORDER BY " + FIELD_NAME + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);
        return cursor.getCount() > 0;
    }

    public void saveLines(Map<String, Integer> lines) {
        SQLiteDatabase db = this.getWritableDatabase();
        //db.beginTransaction();
        /*for (String l : lines.keySet()) {
            ContentValues values = new ContentValues();
            values.put(ID, lines.get(l));
            values.put(NAME, l); //station name
            db.insert(LINES_TABLE, null, values);
        }*/
        //db.endTransaction();
        db.beginTransaction();
        String sql = "INSERT INTO " + LINES_TABLE + "(" + ID + "," + NAME + ") VALUES(?,?)";
        SQLiteStatement statement = db.compileStatement(sql);
        for (String l : lines.keySet()) {
            statement.clearBindings();
            statement.bindLong(1, lines.get(l));
            statement.bindString(2, l);
            statement.execute();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        //save config entry
        setConfig(db, CONFIG_LAST_SAVED_LINES, String.valueOf(PersistentDataController.getCurTime()));
        db.close();
    }

    public Map<String, Integer> getDirs(int lineId) {
        SQLiteDatabase db = this.getWritableDatabase();

        Map<String, Integer> outMap = new HashMap<>();

        String selectQuery = "SELECT " + FIELD_DIR_ID + "," + FIELD_NAME + " FROM " + DIRS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime()) + " ORDER BY " + FIELD_NAME + " ASC";

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

    public void saveDirs(int lineId, Map<String, Integer> directions) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (String dirName : directions.keySet()) {
            ContentValues values = new ContentValues();
            values.put(FIELD_DIR_ID, directions.get(dirName));
            values.put(NAME, dirName);
            values.put(FIELD_LINE_ID, lineId);
            values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + PersistentDataController.getLineExpiry());
            db.insert(DIRS_TABLE, null, values);
        }
        //delete old ones
        db.execSQL("DELETE FROM " + DIRS_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime());
        db.close();
    }

    public Map<String, Integer> getStations(int lineId, int dirId) {
        SQLiteDatabase db = this.getWritableDatabase();

        Map<String, Integer> outMap = new HashMap<>();

        String selectQuery = "SELECT " + FIELD_STATION_ID + "," + FIELD_NAME + " FROM " + STATIONS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_DIR_ID + "=" + dirId + " AND " + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime()) + " ORDER BY " + FIELD_NAME + " ASC";

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

    public void saveStations(int lineId, int dirId, Map<String, Integer> stations) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        String sql = "INSERT INTO " + STATIONS_TABLE + "(" + FIELD_DIR_ID + "," + NAME + "," + FIELD_LINE_ID + "," + FIELD_STATION_ID + "," + FIELD_EXPIRES + ") VALUES(?,?,?,?,?)";
        SQLiteStatement statement = db.compileStatement(sql);
        for (String name : stations.keySet()) {
            statement.clearBindings();
            statement.bindLong(1, dirId);
            statement.bindString(2, name);
            statement.bindLong(3, lineId);
            statement.bindLong(4, stations.get(name));
            statement.bindLong(5, PersistentDataController.getCurTime() + PersistentDataController.getLineExpiry());
            statement.execute();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        //delete old ones
        db.execSQL("DELETE FROM " + STATIONS_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime());
        db.close();
    }

    public void saveAlert(int lineId, String title, String url, String text) {
        SQLiteDatabase db = this.getWritableDatabase();
        //insert the alert
        ContentValues values = new ContentValues();
        values.put(FIELD_LINE_ID, lineId);
        values.put(FIELD_TITLE, title);
        values.put(FIELD_URL, url);
        values.put(FIELD_DESCRIPTION, text);
        values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + PersistentDataController.getAlertExpiry());
        db.insert(ALERTS_TABLE, null, values);

        //delete old ones
        db.execSQL("DELETE FROM " + ALERTS_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime());
        db.execSQL("DELETE FROM " + CACHED_LINE_ALERTS_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime());
        db.close();
    }

    public void markAsSavedForLineAlerts(List<Integer> lineIds) {
        //mark the line as cached
        SQLiteDatabase db = this.getWritableDatabase();
        for (int lineId : lineIds) {
            ContentValues values = new ContentValues();
            values.put(FIELD_LINE_ID, lineId);
            values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + PersistentDataController.getAlertExpiry());
            db.insert(CACHED_LINE_ALERTS_TABLE, null, values);
        }
        db.close();
    }

    public List<Map<String, String>> getAlerts(int lineId) {
        List<Map<String, String>> out = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();

        //first see if the data is cached at all
        String selectQuery = "SELECT 1 FROM " + CACHED_LINE_ALERTS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime());
        if (db.rawQuery(selectQuery, null).getCount() == 0) { //no results
            return null;
        }

        //otherwise get the results
        List<Map<String, String>> outList = new ArrayList<>();

        selectQuery = "SELECT " + FIELD_URL + "," + FIELD_TITLE + "," + FIELD_DESCRIPTION + " FROM " + ALERTS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime()) + " ORDER BY " + FIELD_TITLE + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Map<String, String> alertInfo = new HashMap<>();
                alertInfo.put("url", cursor.getString(0));
                alertInfo.put("title", cursor.getString(1));
                alertInfo.put("info", cursor.getString(2));
                outList.add(alertInfo);
            } while (cursor.moveToNext());
        }
        db.close();
        return outList;
    }
}
