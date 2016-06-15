package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Persistent Data Controller
The purpose of this class is to act as intermediary between the interface and the database. Basically a class accesses functions in this class to read/write the database.
 */

public abstract class PersistentDataController {
    static String[] lines;
    static Map<String, Integer> lineIds = new HashMap<>();
    static Map<Integer, Map<String, Integer>> directions = new HashMap<>();
    public static final int lineExpiry = 60 * 60 * 24 * 14;
    public static final int stationExpiry = 60 * 60 * 24 * 7;
    public static final int alertExpiry = 60 * 60 * 24 * 1;

    private static class LineForSorting implements Comparable<LineForSorting> {
        int id;
        String name;
        final Pattern p = Pattern.compile("^(\\d+)[a-z]? ");
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

    public static boolean linesStored(Context context) {
        if (lines != null) {
            return true;
        }
        DatabaseHandler db = new DatabaseHandler(context);
        lineIds = db.getStoredLines();
        db.close();
        if (lineIds.size() == 0) {
            return false;
        }
        Set<String> lineList = lineIds.keySet();
        LineForSorting[] tempLines = new LineForSorting[lineList.size()];
        int i = 0;
        for (String s : lineList) {
            tempLines[i] = new LineForSorting(s);
            i++;
        }
        Arrays.sort(tempLines);
        lines = new String[tempLines.length];
        for (i = 0; i < tempLines.length; i++) {
            lines[i] = tempLines[i].toString();
        }
        return true;
    }

    public static String[] getLines() {
        return lines;
    }

    public static void setLines(String[] l) {
        lines = l;
    }

    public static void saveLineIdMap(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        db.saveLines(lineIds);
        db.close();
    }

    public static Map<String,Integer> getLineIdMap() {
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
