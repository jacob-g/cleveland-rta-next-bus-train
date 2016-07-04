package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
    public static final int escElExpiry = 60 * 60;
    public static final int favLocationExpiry = 60 * 60 * 24 * 14;
    public static final int noLocationRefreshPeriod = 60 * 60 * 12;

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

    private static Map<String, Integer> cacheDurations;
    public static void loadCacheDurations(Context context) {
        cacheDurations = new HashMap<>();
        String[] keys = {"lineExpiry", "stationExpiry", "alertExpiry", "escElExpiry", "favLocationExpiry"};
        int[] defaults = {lineExpiry, stationExpiry, alertExpiry, escElExpiry, favLocationExpiry};
        for (int i = 0; i < keys.length; i++) {
            cacheDurations.put(keys[i], getFromDatabase(context, keys[i], defaults[i]));
        }
    }

    public static void setCacheDuration(String key, int val) {
        cacheDurations.put(key, val);
    }

    private static int getFromDatabase(Context context, String key, int other) {
        String out = getConfig(context, key);
        if (out.equals("")) {
            return other;
        } else {
            return Integer.parseInt(out);
        }
    }

    public static int getLineExpiry(Context context) {
        if (cacheDurations == null) {
            loadCacheDurations(context);
        }
        return cacheDurations == null || !cacheDurations.containsKey("lineExpiry") ? lineExpiry : cacheDurations.get("lineExpiry");
    }
    public static int getStationExpiry(Context context) {
        if (cacheDurations == null) {
            loadCacheDurations(context);
        }
        return cacheDurations == null || !cacheDurations.containsKey("stationExpiry") ? stationExpiry : cacheDurations.get("stationExpiry");
    }
    public static int getAlertExpiry(Context context) {
        if (cacheDurations == null) {
            loadCacheDurations(context);
        }
        return cacheDurations == null || !cacheDurations.containsKey("alertExpiry") ? alertExpiry : cacheDurations.get("alertExpiry");
    }
    public static int getEscElExpiry(Context context) {
        if (cacheDurations == null) {
            loadCacheDurations(context);
        }
        return cacheDurations == null || !cacheDurations.containsKey("escElExpiry") ? escElExpiry : cacheDurations.get("escElExpiry");
    }
    public static int getFavLocationExpiry(Context context) {
        if (cacheDurations == null) {
            loadCacheDurations(context);
        }
        return cacheDurations == null || !cacheDurations.containsKey("favLocationExpiry") ? favLocationExpiry : cacheDurations.get("favLocationExpiry");
    }
    public static int getNoLocationRefreshPeriod() {
        return noLocationRefreshPeriod;
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

    public static List<EscalatorElevatorAlert> getEscalatorAlerts(Context context, int stationId) {
        DatabaseHandler db = new DatabaseHandler(context);
        List<EscalatorElevatorAlert> statuses;
        if ((statuses = db.getEscElStatusesForStation(stationId)) != null) {
        } else {
            statuses = new ArrayList<>();
            String rawXML = NetworkController.basicHTTPRequest("https://nexttrain.futuresight.org/api/escelstatus?version=1&stationid=" + stationId);
            try {
                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(rawXML)));
                Node rootNode = doc.getDocumentElement();


                if (doc.hasChildNodes()) {
                    NodeList nl = rootNode.getChildNodes();
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node curNode = nl.item(i); //<escel> node
                        if (!curNode.getNodeName().equals("#text")) {
                            Map<String, String> nodeInfo = new HashMap<>();
                            NodeList children = curNode.getChildNodes();
                            for (int j = 0; j < children.getLength(); j++) {
                                String key = children.item(j).getNodeName();
                                if (!key.equals("#text")) {
                                    String val = children.item(j).getTextContent();
                                    nodeInfo.put(key, val);
                                }
                            }
                            statuses.add(new EscalatorElevatorAlert(nodeInfo.get("name"), nodeInfo.get("status").equals("Working")));
                        }
                    }
                }
                db.saveEscElStatuses(stationId, statuses);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        db.close();
        return statuses;
    }

    public static String getConfig(Context context, String key) {
        DatabaseHandler db = new DatabaseHandler(context);
        String out = db.getConfig(key);
        db.close();
        return out;
    }

    public static void setConfig(Context context, String key, String val) {
        DatabaseHandler db = new DatabaseHandler(context);
        db.setConfig(key, val);
        db.close();
    }
}
