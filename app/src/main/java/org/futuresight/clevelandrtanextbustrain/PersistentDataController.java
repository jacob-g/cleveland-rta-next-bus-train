package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jacob on 5/20/16.
 */
public abstract class PersistentDataController {
    static String[] lines;
    static Map<String, Integer> lineIds = new HashMap<>();
    static final int lineExpiry = 60 * 60 * 24 * 14;

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
}
