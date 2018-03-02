package org.futuresight.clevelandrtanextbustrain;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by jacob on 5/15/16.
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 6;
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
        private static final String FIELD_LAT = "lat";
        private static final String FIELD_LNG = "lng";

    private static final String LINES_TABLE = "lines";

    private static final String DIRS_TABLE = "directions";

    private static final String STATIONS_TABLE = "stations";

    private static final String CONFIG_TABLE = "config";
        private static final String FIELD_VALUE = "value";

    private static final String ALERTS_TABLE = "alerts";
        private static final String FIELD_READ = "read";
        private static final String FIELD_TITLE = "title";
        private static final String FIELD_DESCRIPTION = "description";
        private static final String FIELD_URL = "url";

    private static final String CACHED_LINE_ALERTS_TABLE = "cached_line_alerts";

    private static final String LINE_ALERTS_TABLE = "line_alerts";
        private static final String FIELD_ALERT_ID = "alert_id";

    private static final String ESCEL_STATUSES_TABLE = "escel_statuses";
        private static final String FIELD_STATUS = "status";

    private static final String ALL_STOPS_TABLE = "all_stops";
        private static final String FIELD_IS_TRANSFER = "is_transfer";
        private static final String FIELD_CHAIN_ID = "chain_id";

    private static final String LINE_PATHS_TABLE = "line_paths";
        private static final String FIELD_PATH_ID = "path_id";
        private static final String FIELD_RED = "red";
        private static final String FIELD_GREEN = "green";
        private static final String FIELD_BLUE = "blue";
        private static final String FIELD_TYPE = "type";

    private static final String DEST_MAPPINGS_TABLE = "dest_mappings";
        private static final String FIELD_ORIGINAL = "original";
        private static final String FIELD_REPLACEMENT = "replacement";

    //the universal names for fields
    private static final String ID = "id"; //the universal "id" field in each table
    private static final String NAME = "name";
    private static final String FIELD_EXPIRES = "expires";

    //config values
    public static final String CONFIG_LAST_SAVED_LINES = "last_saved_lines";
    public static final String CONFIG_LAST_SAVED_ALL_PATHS = "lastSavedAllPaths";
    public static final String CONFIG_LAST_SAVED_ALL_STOPS = "lastSavedAllStops";

    private static boolean initialized = false;
    private static Context context;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        if (!initialized) {
            SQLiteDatabase db = this.getWritableDatabase();
            onCreate(db);
            db.close();
            initialized = true;
        }
        DatabaseHandler.context = context;
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
                + FIELD_STATION_ID + " INT,"
                + FIELD_LAT + " REAL,"
                + FIELD_LNG + " REAL"
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
                + ID + " INTEGER PRIMARY KEY,"
                + FIELD_READ + " INTEGER,"
                + FIELD_LINE_ID + " INTEGER,"
                + FIELD_TITLE + " TEXT,"
                + FIELD_URL + " TEXT,"
                + FIELD_DESCRIPTION + " TEXT,"
                + FIELD_EXPIRES + " INTEGER"
                + ")";
        db.execSQL(CREATE_ALERTS_TABLE);

        db.execSQL("CREATE TABLE IF NOT EXISTS " + LINE_ALERTS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_LINE_ID + " INTEGER,"
                + FIELD_ALERT_ID + " INTEGER,"
                + FIELD_EXPIRES + " INTEGER)");

        String CREATE_CACHED_LINE_ALERTS_TABLE = "CREATE TABLE IF NOT EXISTS " + CACHED_LINE_ALERTS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_LINE_ID + " INTEGER,"
                + FIELD_EXPIRES + " INTEGER"
                + ")";
        db.execSQL(CREATE_CACHED_LINE_ALERTS_TABLE);

        String CREATE_ESCEL_STATUSES_TABLE = "CREATE TABLE IF NOT EXISTS " + ESCEL_STATUSES_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_STATION_ID + " INTEGER,"
                + FIELD_NAME + " TEXT,"
                + FIELD_STATUS + " INTEGER,"
                + FIELD_EXPIRES + " INTEGER"
                + ")";
        db.execSQL(CREATE_ESCEL_STATUSES_TABLE);

        String CREATE_ALL_STOPS_TABLE = "CREATE TABLE IF NOT EXISTS " + ALL_STOPS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_STATION_ID + " INTEGER,"
                + FIELD_NAME + " INTEGER,"
                + FIELD_LINE_ID + " INTEGER,"
                + FIELD_LINE_NAME + " TEXT,"
                + FIELD_DIR_ID + " INTEGER,"
                + FIELD_DIR_NAME + " TEXT,"
                + FIELD_LAT + " REAL,"
                + FIELD_LNG + " REAL,"
                + FIELD_TYPE + " STRING,"
                + FIELD_IS_TRANSFER + " INTEGER,"
                + FIELD_CHAIN_ID + " INTEGER"
                + ")";
        db.execSQL(CREATE_ALL_STOPS_TABLE);

        String CREATE_LINE_PATHS_TABLE = "CREATE TABLE IF NOT EXISTS " + LINE_PATHS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_PATH_ID + " INTEGER,"
                + FIELD_LAT + " REAL,"
                + FIELD_LNG + " REAL,"
                + FIELD_RED + " INTEGER,"
                + FIELD_GREEN + " INTEGER,"
                + FIELD_BLUE + " INTEGER,"
                + FIELD_LINE_ID + " INTEGER,"
                + FIELD_LINE_NAME + " TEXT"
                + ")";
        db.execSQL(CREATE_LINE_PATHS_TABLE);

        String CREATE_DEST_MAPPINGS_TABLE = "CREATE TABLE IF NOT EXISTS " + DEST_MAPPINGS_TABLE + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + FIELD_ORIGINAL + " STRING,"
                + FIELD_REPLACEMENT + " STRING,"
                + FIELD_EXPIRES + " INTEGER"
                + ")";
        db.execSQL(CREATE_DEST_MAPPINGS_TABLE);

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
        String out;
        if (cursor.moveToFirst()) {
            out = cursor.getString(0);
        } else {
            out = "";
        }
        cursor.close();
        return out;
    }

    public String getConfig(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        String out = getConfig(db, key);
        db.close();
        return out;
    }

    public void setConfig(String key, String val) {
        SQLiteDatabase db = this.getWritableDatabase();
        setConfig(db, key, val);
        db.close();
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Create tables again
        if (oldVersion < 2) { //need to add lines to the line path table
            //db.execSQL("ALTER TABLE " + LINE_PATHS_TABLE + " ADD COLUMN " + FIELD_LINE_ID + " INTEGER");
            db.execSQL("DROP TABLE IF EXISTS " + LINE_PATHS_TABLE);
        }
        if (oldVersion < 3) { //create a table for line-specific alerts
            db.execSQL("DROP TABLE IF EXISTS " + LINE_ALERTS_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + ALERTS_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + CACHED_LINE_ALERTS_TABLE);
            /*db.execSQL("CREATE TABLE IF NOT EXISTS " + LINE_ALERTS_TABLE + "("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + FIELD_LINE_ID + " INTEGER,"
                    + FIELD_ALERT_ID + " INTEGER,"
                    + FIELD_EXPIRES + " INTEGER)");*/
        }
        if (oldVersion < 4) { //create a table for line-specific alerts
            db.execSQL("DROP TABLE IF EXISTS " + ALL_STOPS_TABLE);
            //db.execSQL("ALTER TABLE " + ALL_STOPS_TABLE + " ADD COLUMN " + FIELD_IS_TRANSFER + " INTEGER");
        }
        if (oldVersion < 5) { //add the line name to the paths table, current version is 5
            db.execSQL("DROP TABLE IF EXISTS " + LINE_PATHS_TABLE);

            setConfig(db, CONFIG_LAST_SAVED_LINES, "0");
            setConfig(db, CONFIG_LAST_SAVED_ALL_PATHS, "0");
            setConfig(db, CONFIG_LAST_SAVED_ALL_STOPS, "0");
            //db.execSQL("ALTER TABLE " + LINE_PATHS_TABLE + " ADD COLUMN " + FIELD_LINE_NAME + " TEXT");
        }

        if (oldVersion < 6) { //add the line name to the paths table, current version is 5
            db.execSQL("DROP TABLE IF EXISTS " + STATIONS_TABLE);
        }
        onCreate(db);
    }

    public void recreateTablesWithoutErasing() {
        SQLiteDatabase db = this.getWritableDatabase();
        onCreate(db);
        db.close();
    }

    public void fryCache() { //erase everything, hopefully not needed
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS " + LINES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DIRS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + STATIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ALERTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CACHED_LINE_ALERTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ESCEL_STATUSES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ESCEL_STATUSES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ALL_STOPS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LINE_PATHS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LINE_ALERTS_TABLE);
        setConfig(db, CONFIG_LAST_SAVED_LINES, "0");
        setConfig(db, CONFIG_LAST_SAVED_ALL_PATHS, "0");
        setConfig(db, CONFIG_LAST_SAVED_ALL_STOPS, "0");
        onCreate(db);

        PersistentDataController.removeCachedStuff();

        // Create tables again
        db.close();
    }

    public void fryStations() { //erase everything, hopefully not needed
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS " + STATIONS_TABLE);
        setConfig(db, CONFIG_LAST_SAVED_ALL_STOPS, "0");
        onCreate(db);

        PersistentDataController.removeCachedStuff();

        // Create tables again
        db.close();
    }

    public void fryAlerts() { //erase everything, hopefully not needed
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(FIELD_EXPIRES, 0);
        db.update(ALERTS_TABLE, values, "1", new String[]{});
        db.update(LINE_ALERTS_TABLE, values, "1", new String[]{});
        db.update(CACHED_LINE_ALERTS_TABLE, values, "1", new String[]{});

        // Create tables again
        db.close();
    }

    public void fry() { //erase everything, hopefully not needed
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS " + FAVORITE_LOCATIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LINES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DIRS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + STATIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CONFIG_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ALERTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CACHED_LINE_ALERTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ESCEL_STATUSES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ALL_STOPS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LINE_PATHS_TABLE);

        PersistentDataController.removeCachedStuff();

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
        values.put(FIELD_LAT, st.getLatLng() == null ? 0 : st.getLatLng().latitude);
        values.put(FIELD_LNG, st.getLatLng() == null ? 0 : st.getLatLng().longitude);
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
        List<Station> stations = new ArrayList<>();
        String selectQuery = "SELECT f." + FIELD_NAME + ",f." + FIELD_STATION_NAME + ",f." + FIELD_STATION_ID + ",f." + FIELD_DIR_NAME + ",f." + FIELD_DIR_ID + ",l." + NAME + ",f." + FIELD_LINE_ID + ",f." + FIELD_LAT + ",f." + FIELD_LNG + " FROM " + FAVORITE_LOCATIONS_TABLE + " AS f LEFT JOIN " + LINES_TABLE + " AS l ON l." + ID + "=f." + FIELD_LINE_ID + " ORDER BY f." + FIELD_STATION_NAME + " ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                do {
                    Station st;
                    if (cursor.getDouble(7) == 0) {
                        st = new Station(cursor.getString(1), Integer.parseInt(cursor.getString(2)), cursor.getString(3), Integer.parseInt(cursor.getString(4)), cursor.getString(5), Integer.parseInt(cursor.getString(6)), cursor.getString(0));
                    } else {
                        st = new Station(cursor.getString(1), Integer.parseInt(cursor.getString(2)), cursor.getString(3), Integer.parseInt(cursor.getString(4)), cursor.getString(5), Integer.parseInt(cursor.getString(6)), cursor.getString(0), cursor.getDouble(7), cursor.getDouble(8));
                    }
                    stations.add(st);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
            return stations;
        }
        db.close();
        return stations;
    }

    public boolean hasFavoriteLocation(int lineId, int dirId, int stationId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + FAVORITE_LOCATIONS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_DIR_ID + "=" + dirId + " AND " + FIELD_STATION_ID + "=" + stationId, null);
        boolean out = cursor.moveToFirst();
        cursor.close();
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

    public void updateStationLocation(Station st, LatLng pos) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FIELD_LAT, pos.latitude);
        values.put(FIELD_LNG, pos.longitude);
        db.update(FAVORITE_LOCATIONS_TABLE, values, FIELD_STATION_ID + "=" + st.getStationId() + " AND " + FIELD_DIR_ID + "=" + st.getDirId() + " AND " + FIELD_LINE_ID + "=" + st.getLineId(), null);
        db.close(); // Closing database connection
    }

    public Map<String, Integer> getStoredLines() {
        int lineExpiry = PersistentDataController.getLineExpiry(context);
        SQLiteDatabase db = this.getWritableDatabase();

        Map<String, Integer> outMap = new HashMap<>();

        //make sure it's recent
        String lastSavedStr = getConfig(db, CONFIG_LAST_SAVED_LINES);
        int lastSavedInt = 0;
        if (lastSavedStr != "") {
            lastSavedInt = Integer.parseInt(lastSavedStr);
        }
        if (lastSavedInt < PersistentDataController.getCurTime() - lineExpiry) {
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
        cursor.close();
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
        if (lastSavedInt < PersistentDataController.getCurTime() - PersistentDataController.lineListExpiry) {
            db.execSQL("DELETE FROM " + LINES_TABLE);
            db.close(); // Closing database connection
            return false;
        }

        String selectQuery = "SELECT " + ID + "," + FIELD_NAME + " FROM " + LINES_TABLE + " ORDER BY " + FIELD_NAME + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);
        boolean out = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return out;
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
        SQLiteDatabase db = this.getReadableDatabase();

        Map<String, Integer> outMap = new TreeMap<>();

        String selectQuery = "SELECT " + FIELD_DIR_ID + "," + FIELD_NAME + " FROM " + DIRS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime()) + " ORDER BY " + FIELD_NAME + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                outMap.put(name, id);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return outMap;
    }

    public void saveDirs(int lineId, Map<String, Integer> directions) {
        int lineExpiry = PersistentDataController.getLineExpiry(context);
        SQLiteDatabase db = this.getWritableDatabase();
        for (String dirName : directions.keySet()) {
            ContentValues values = new ContentValues();
            values.put(FIELD_DIR_ID, directions.get(dirName));
            values.put(NAME, dirName);
            values.put(FIELD_LINE_ID, lineId);
            values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + lineExpiry);
            db.insert(DIRS_TABLE, null, values);
        }
        //delete old ones
        db.execSQL("DELETE FROM " + DIRS_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime());
        db.close();
    }

    public Map<String, Integer> getStations(int lineId, int dirId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Map<String, Integer> outMap = new TreeMap<>();

        String selectQuery = "SELECT " + FIELD_STATION_ID + "," + FIELD_NAME + " FROM " + STATIONS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_DIR_ID + "=" + dirId + " AND " + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime()) + " ORDER BY " + FIELD_NAME + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                outMap.put(name, id);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return outMap;
    }

    public void saveStations(int lineId, int dirId, Map<String, Integer> stations) {
        int stationExpiry = PersistentDataController.getStationExpiry(context);
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
            statement.bindLong(5, PersistentDataController.getCurTime() + stationExpiry);
            statement.execute();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        //delete old ones
        db.execSQL("DELETE FROM " + STATIONS_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime());
        db.close();
    }

    //save an alert, returns true if it's new and false if it's an update
    public boolean saveAlert(int alertId, int lineId, String title, String url, String text) {
        int alertExpiry = PersistentDataController.getAlertExpiry(context);
        SQLiteDatabase db = this.getWritableDatabase();
        //PROBLEM: cache it for the LINE instead of just the alert as a whole (probably need a separate table for this)
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + ALERTS_TABLE + " WHERE " + ID + "=" + alertId, null);
        if (cursor.getCount() > 0) {
            ContentValues values = new ContentValues();
            values.put(FIELD_TITLE, title);
            values.put(FIELD_DESCRIPTION, text);
            values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + alertExpiry);
            db.update(ALERTS_TABLE, values, ID + "=" + alertId, null);
            cursor.close();
            cursor = db.rawQuery("SELECT 1 FROM " + LINE_ALERTS_TABLE + " WHERE " + FIELD_ALERT_ID + "=" + alertId + " AND " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_EXPIRES + ">=" + PersistentDataController.getCurTime(), null);
            if (cursor.getCount() == 0) {
                values = new ContentValues();
                values.put(FIELD_LINE_ID, lineId);
                values.put(FIELD_ALERT_ID, alertId);
                values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + alertExpiry);
                db.insert(LINE_ALERTS_TABLE, null, values);
            }
            cursor.close();
            db.execSQL("DELETE FROM " + LINE_ALERTS_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime() + " AND " + FIELD_LINE_ID + "=" + lineId);
            db.close();
            return false;
        } else {
            cursor.close();

            //insert the alert
            ContentValues values = new ContentValues();
            values.put(ID, alertId);
            values.put(FIELD_READ, 0);
            //values.put(FIELD_LINE_ID, lineId);
            values.put(FIELD_TITLE, title);
            values.put(FIELD_URL, url);
            values.put(FIELD_DESCRIPTION, text);
            values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + alertExpiry);
            db.insert(ALERTS_TABLE, null, values);

            values = new ContentValues();
            values.put(FIELD_LINE_ID, lineId);
            values.put(FIELD_ALERT_ID, alertId);
            values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + alertExpiry);
            db.insert(LINE_ALERTS_TABLE, null, values);

            //delete old alerts, start with the alert entry linking the line with the alert
            //db.execSQL("DELETE FROM " + CACHED_LINE_ALERTS_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime() + " AND " + FIELD_LINE_ID + "=" + lineId);
            db.execSQL("DELETE FROM " + LINE_ALERTS_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime() + " AND " + FIELD_LINE_ID + "=" + lineId);
            db.close();
            return true;
        }
    }

    public void markAlertsAsRead(List<Integer> ids) {
        SQLiteDatabase db = this.getWritableDatabase();

        StringBuilder whereClause = new StringBuilder(ID + " IN(");
        for (int id : ids) {
            whereClause.append(id + ",");
        }
        whereClause.deleteCharAt(whereClause.length() - 1);
        whereClause.append(") AND " + FIELD_READ + "=0");

        ContentValues values = new ContentValues();
        values.put(FIELD_READ, 1);
        db.update(ALERTS_TABLE, values, whereClause.toString(), null);

        db.close();
    }

    public void markAsSavedForLineAlerts(List<Integer> lineIds) {
        int alertExpiry = PersistentDataController.getAlertExpiry(context);
        //mark the line as cached
        SQLiteDatabase db = this.getWritableDatabase();
        for (int lineId : lineIds) {
            ContentValues values = new ContentValues();
            values.put(FIELD_LINE_ID, lineId);
            values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + alertExpiry);
            db.insert(CACHED_LINE_ALERTS_TABLE, null, values);
        }
        db.close();
    }

    public List<Map<String, String>> getAlerts(int lineId) {
        SQLiteDatabase db = this.getReadableDatabase();

        //first see if the data is cached at all
        String selectQuery = "SELECT 1 FROM " + CACHED_LINE_ALERTS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime());
        if (db.rawQuery(selectQuery, null).getCount() == 0) { //no results
            return null;
        }

        //otherwise get the results
        List<Map<String, String>> outList = new ArrayList<>();

        //selectQuery = "SELECT " + ID + "," + FIELD_URL + "," + FIELD_TITLE + "," + FIELD_DESCRIPTION + "," + FIELD_READ + " FROM " + ALERTS_TABLE + " WHERE " + FIELD_LINE_ID + "=" + lineId + " AND " + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime()) + " ORDER BY " + FIELD_TITLE + " ASC";
        selectQuery = "SELECT a." + ID + ",a." + FIELD_URL + ",a." + FIELD_TITLE + ",a." + FIELD_DESCRIPTION + ",a." + FIELD_READ + " FROM " + LINE_ALERTS_TABLE + " AS l LEFT JOIN " + ALERTS_TABLE + " AS a ON a.id=l." + FIELD_ALERT_ID + " WHERE l." + FIELD_LINE_ID + "=" + lineId + " AND l." + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime()) + " ORDER BY " + FIELD_TITLE + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Map<String, String> alertInfo = new HashMap<>();
                alertInfo.put("id", Integer.toString(cursor.getInt(0)));
                alertInfo.put("url", cursor.getString(1));
                alertInfo.put("title", cursor.getString(2));
                alertInfo.put("info", cursor.getString(3));
                alertInfo.put("new", cursor.getInt(4) == 0 ? "true" : "false");
                outList.add(alertInfo);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return outList;
    }

    //returns null if there isn't anything cached and an empty list if there aren't any alerts
    public List<EscalatorElevatorAlert> getEscElStatusesForStation (int stationId) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT " + FIELD_NAME + "," + FIELD_STATUS + " FROM " + ESCEL_STATUSES_TABLE + " WHERE " + FIELD_STATION_ID + "=" + stationId + " AND " + FIELD_EXPIRES + ">=" + (PersistentDataController.getCurTime()) + " ORDER BY " + FIELD_NAME + " ASC";

        List<EscalatorElevatorAlert> outList = new ArrayList<>();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getInt(1) == -1) { //there are no escalators or elevators at this station
                    break;
                }
                outList.add(new EscalatorElevatorAlert(cursor.getString(0), cursor.getInt(1) == 1));
            } while (cursor.moveToNext());
        } else {
            outList = null;
        }
        cursor.close();
        db.close();
        return outList;
    }

    public void saveEscElStatuses(int stationId, List<EscalatorElevatorAlert> statuses) {
        int stationExpiry = PersistentDataController.getStationExpiry(context);
        SQLiteDatabase db = this.getWritableDatabase();
        if (statuses.isEmpty()) {
            //no escalators or elevators, but cache that too
            ContentValues values = new ContentValues();
            values.put(FIELD_STATION_ID, stationId);
            values.put(FIELD_NAME, "");
            values.put(FIELD_STATUS, -1);
            values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + stationExpiry);
            db.insert(ESCEL_STATUSES_TABLE, null, values);
        } else {
            for (EscalatorElevatorAlert status : statuses) {
                ContentValues values = new ContentValues();
                values.put(FIELD_STATION_ID, stationId);
                values.put(FIELD_NAME, status.name);
                values.put(FIELD_STATUS, status.working ? 1 : 0);
                values.put(FIELD_EXPIRES, PersistentDataController.getCurTime() + stationExpiry);
                db.insert(ESCEL_STATUSES_TABLE, null, values);
            }
        }
        db.execSQL("DELETE FROM " + ESCEL_STATUSES_TABLE + " WHERE " + FIELD_EXPIRES + "<" + PersistentDataController.getCurTime());
        db.close();
    }

    public void cacheAllStops(List<Station> stations) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("DELETE FROM " + ALL_STOPS_TABLE);

        String sql = "INSERT INTO " + ALL_STOPS_TABLE + "(" + FIELD_STATION_ID + "," + FIELD_NAME + "," + FIELD_LINE_ID + "," + FIELD_LINE_NAME + "," + FIELD_DIR_NAME + "," + FIELD_DIR_ID + "," + FIELD_LAT + "," + FIELD_LNG + "," + FIELD_TYPE + "," + FIELD_IS_TRANSFER + "," + FIELD_CHAIN_ID + ") VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        SQLiteStatement statement = db.compileStatement(sql);
        for (Station st : stations) {
            statement.clearBindings();
            statement.bindLong(1, st.getStationId());
            statement.bindString(2, st.getStationName());
            statement.bindLong(3, st.getLineId());
            statement.bindString(4, st.getLineName());
            statement.bindString(5, st.getDirName());
            statement.bindLong(6, st.getDirId());
            statement.bindDouble(7, st.getLatLng().latitude);
            statement.bindDouble(8, st.getLatLng().longitude);
            statement.bindString(9, Character.toString(st.getType()));
            statement.bindLong(10, st.isTransfer() ? 1 : 0);
            statement.bindLong(11, st.getChainId());
            statement.execute();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        setConfig(CONFIG_LAST_SAVED_ALL_STOPS, Integer.toString(PersistentDataController.getCurTime()));
    }

    public List<Station> getCachedStopLocations() {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT " + FIELD_STATION_ID + "," + FIELD_NAME + "," + FIELD_LINE_ID + "," + FIELD_LINE_NAME + "," + FIELD_DIR_NAME + "," + FIELD_DIR_ID + "," + FIELD_LAT + "," + FIELD_LNG + "," + FIELD_TYPE + "," + FIELD_IS_TRANSFER + "," + FIELD_CHAIN_ID + " FROM " + ALL_STOPS_TABLE + " ORDER BY " + FIELD_STATION_ID + " ASC";


        Cursor cursor = db.rawQuery(selectQuery, null);
        List<Station> outList = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                //String stationName, int stationId, String dirName, int dirId, String lineName, int lineId, String name, double lat, double lng
                Station st = new Station(cursor.getString(1), cursor.getInt(0), cursor.getString(4), cursor.getInt(5), cursor.getString(3), cursor.getInt(2), "", cursor.getDouble(6), cursor.getDouble(7), 'b', cursor.getInt(9) == 1);
                if (cursor.getString(8) != null && cursor.getString(8).length() != 0) {
                    st.setType(cursor.getString(8).charAt(0));
                }
                st.chain(cursor.getInt(10));
                outList.add(st);
            } while (cursor.moveToNext());
        } else {
            outList = null;
        }
        cursor.close();
        db.close();
        return outList;
    }

    public void cacheAllPaths(List<NearMeActivity.ColoredPointList> paths) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("DELETE FROM " + LINE_PATHS_TABLE);

        String sql = "INSERT INTO " + LINE_PATHS_TABLE + "(" + FIELD_PATH_ID + "," + FIELD_LAT + "," + FIELD_LNG + "," + FIELD_RED + "," +
                FIELD_GREEN + "," + FIELD_BLUE + "," + FIELD_LINE_ID + "," + FIELD_LINE_NAME + ") VALUES(?,?,?,?,?,?,?,?)";
        SQLiteStatement statement = db.compileStatement(sql);
        int pathId = 0;
        for (NearMeActivity.ColoredPointList path : paths) {
            for (LatLng point : path.points) {
                statement.clearBindings();
                statement.bindLong(1, pathId);
                statement.bindDouble(2, point.latitude);
                statement.bindDouble(3, point.longitude);
                statement.bindLong(4, Color.red(path.color));
                statement.bindLong(5, Color.green(path.color));
                statement.bindLong(6, Color.blue(path.color));
                statement.bindLong(7, path.lineId);
                statement.bindString(8, path.lineName);
                statement.execute();
            }
            pathId++;
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        setConfig(CONFIG_LAST_SAVED_ALL_PATHS, Integer.toString(PersistentDataController.getCurTime()));
    }

    public List<NearMeActivity.ColoredPointList> getAllPaths() {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT " + FIELD_PATH_ID + "," + FIELD_LAT + "," + FIELD_LNG + "," + FIELD_RED + "," + FIELD_GREEN + "," + FIELD_BLUE + "," + FIELD_LINE_ID + "," + FIELD_LINE_NAME + " FROM " + LINE_PATHS_TABLE + " ORDER BY " + FIELD_PATH_ID + " ASC, " + ID + " ASC";

        List<NearMeActivity.ColoredPointList> outList = new ArrayList<>();
        Cursor cursor = db.rawQuery(selectQuery, null);
        int lastPath = -1;
        NearMeActivity.ColoredPointList path = new NearMeActivity.ColoredPointList(Color.BLACK, 0, "");
        if (cursor.moveToFirst()) {
            do {
                if (lastPath != cursor.getInt(0)) {
                    lastPath = cursor.getInt(0);
                    path = new NearMeActivity.ColoredPointList(Color.rgb(cursor.getInt(3), cursor.getInt(4), cursor.getInt(5)), cursor.getInt(6), cursor.getString(7));
                    outList.add(path);
                }
                path.points.add(new LatLng(cursor.getDouble(1), cursor.getDouble(2)));
            } while (cursor.moveToNext());
        } else {
            outList = null;
        }
        cursor.close();
        db.close();
        return outList;
    }

    public Map<String, String> getDestMappings() {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, String> out = new HashMap<>();
        String selectQuery = "SELECT " + FIELD_ORIGINAL + "," + FIELD_REPLACEMENT + " FROM " + DEST_MAPPINGS_TABLE + " WHERE " + FIELD_EXPIRES + ">" + PersistentDataController.getCurTime();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                out.put(cursor.getString(0), cursor.getString(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return out;
    }

    public void saveDestMappings(Map<String, String> mappings) {
        int stationExpiry = PersistentDataController.getStationExpiry(context);
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.execSQL("DELETE FROM " + DEST_MAPPINGS_TABLE);

        String sql = "INSERT INTO " + DEST_MAPPINGS_TABLE + "(" + FIELD_ORIGINAL + "," + FIELD_REPLACEMENT + "," + FIELD_EXPIRES + ") VALUES(?,?,?)";
        SQLiteStatement statement = db.compileStatement(sql);
        for (String orig : mappings.keySet()) {
            statement.clearBindings();
            statement.bindString(1, orig);
            statement.bindString(2, mappings.get(orig));
            statement.bindDouble(3, PersistentDataController.getCurTime() + stationExpiry);
            statement.execute();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }
}
