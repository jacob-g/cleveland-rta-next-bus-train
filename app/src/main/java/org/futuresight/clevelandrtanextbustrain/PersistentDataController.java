package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Persistent Data Controller
The purpose of this class is to act as intermediary between the interface and the database. Basically a class accesses functions in this class to read/write the database.
 */

public abstract class PersistentDataController {
    static String[] lines = new String[0];
    static Map<String, Integer> lineIds = new HashMap<>();
    static Map<Integer, Map<String, Integer>> directions = new HashMap<>();
    public static final int lineExpiry = 60 * 60 * 24 * 14;
    public static final int stationExpiry = 60 * 60 * 24 * 7;
    public static final int alertExpiry = 60 * 60 * 24 * 1;

    private static class LineForSorting implements Comparable<LineForSorting> {
        int id;
        String name;
        final static Pattern p = Pattern.compile("^(\\d+)[a-zA-Z]? ");
        public LineForSorting(String l) {
            Matcher m = p.matcher(l);
            if (m.find()) {
                id = Integer.parseInt(m.group(1));
            } else {
                id = 9999;
            }
            name = l;
        }

        public int compareTo(LineForSorting other) {
            if (this.id != other.id) {
                return this.id - other.id;
            } else {
                return name.compareTo(other.name);
            }
        }

        public String toString() {
            return name;
        }
    }

    private static boolean linesStored(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        boolean out = db.hasStoredLines();
        db.close();
        return out;
    }

    public static void removeCachedStuff() {
        lines = new String[0];
        lineIds = new HashMap<>();
        directions = new HashMap<>();
    }

    private static void loadLines(Context context) {
        String[] lineNames = new String[0];
        Map<String, Integer> ids = new HashMap<>();
        if (linesStored(context)) {
            //cached
            DatabaseHandler db = new DatabaseHandler(context);
            ids = db.getStoredLines();
            db.close();
            lineNames = ids.keySet().toArray(new String[ids.size()]);
            LineForSorting[] lineNamesForSorting = new LineForSorting[lineNames.length];
            int i = 0;
            for (String s : lineNames) {
                lineNamesForSorting[i] = new LineForSorting(s);
                i++;
            }
            Arrays.sort(lineNamesForSorting);
            i = 0;
            for (LineForSorting l : lineNamesForSorting) {
                lineNames[i] = l.toString();
                i++;
            }
        } else {
            //not cached
            try {
                String result = NetworkController.performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getRoutes", "");
                JSONObject json = new JSONObject(result);
                JSONArray arr = json.getJSONArray("d");
                lineNames = new String[arr.length()];

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject lineObj = arr.getJSONObject(i);
                    lineNames[i] = lineObj.getString("name");
                    int id = lineObj.getInt("id");
                    ids.put(lineNames[i], id);
                }

                PersistentDataController.setLines(lineNames);
                DatabaseHandler db = new DatabaseHandler(context);
                db.saveLines(ids);
                db.close();
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        lines = lineNames;
        lineIds = ids;
    }

    public static String[] getLines(Context context) {
        if (lines.length == 0) {
            loadLines(context);
        }
        return lines;
    }

    public static void setLines(String[] l) {
        lines = l;
    }

    public static Map<String,Integer> getLineIdMap(Context context) {
        if (lineIds.isEmpty()) {
            loadLines(context);
        }
        return lineIds;
    }

    public static int getCurTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public static int getLineExpiry() {
        return lineExpiry;
    }
    public static int getStationExpiry() {
        return stationExpiry;
    }
    public static int getAlertExpiry() {
        return alertExpiry;
    }

    public static Map<String, Integer> getDirIds(Context context, int lineId) {
        DatabaseHandler db = new DatabaseHandler(context);
        Map<String, Integer> out = db.getDirs(lineId);
        db.close();
        return out;
    }

    public static void saveDirIds(Context context, int lineId, Map<String, Integer> directions) {
        DatabaseHandler db = new DatabaseHandler(context);
        db.saveDirs(lineId, directions);
        db.close();
    }

    public static Map<String, Integer> getStationIds(Context context, int lineId, int dirId) {
        DatabaseHandler db = new DatabaseHandler(context);
        Map<String, Integer> out = db.getStations(lineId, dirId);
        db.close();
        return out;
    }

    public static void saveStationIds(Context context, int lineId, int dirId, Map<String, Integer> stations) {
        DatabaseHandler db = new DatabaseHandler(context);
        db.saveStations(lineId, dirId, stations);
        db.close();
    }
    public static List<Map<String, String>> getAlerts(Context context, int lineId) {
        DatabaseHandler db = new DatabaseHandler(context);
        List<Map<String, String>> out = db.getAlerts(lineId);
        db.close();
        return out;
    }

    public static void cacheAlert(Context context, int lineId, String title, String url, String text) {
        DatabaseHandler db = new DatabaseHandler(context);
        db.saveAlert(lineId, title, url, text);
        db.close();
    }

    public static void markAsSavedForLineAlerts(Context context, List<Integer> lineIds) {
        DatabaseHandler db = new DatabaseHandler(context);
        db.markAsSavedForLineAlerts(lineIds);
        db.close();
    }
}
