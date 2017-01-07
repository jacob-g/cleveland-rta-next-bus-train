package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    static Map<String, String> destMappings = new HashMap<>();
    public static final int lineExpiry = 60 * 60 * 24 * 14;
    public static final int stationExpiry = 60 * 60 * 24 * 7;
    public static final int alertExpiry = 60 * 60 * 24 * 1;
    public static final int escElExpiry = 60 * 60;
    public static final int favLocationExpiry = 60 * 60 * 24 * 14;
    public static final int noLocationRefreshPeriod = 60 * 60 * 12;
    public static final int API_VERSION = 1;

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

    public static void loadLines(Context context) {
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
                if (result == null) {
                    PersistentDataController.setLines(new String[0]);
                    return;
                }
                JSONObject json = new JSONObject(result);
                JSONArray arr = json.getJSONArray("d");
                lineNames = new String[arr.length()];

                if (arr.length() == 0) { //nextconnect is down, fail gracefully
                    return;
                }

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

    private static void loadDestMappings(Context context) {
        DatabaseHandler db = new DatabaseHandler(context);
        if ((destMappings = db.getDestMappings()).isEmpty()) {
            //not cached
            try {
                String rawXML = NetworkController.performPostCall("https://nexttrain.futuresight.org/api/getdestmappings?version=" + PersistentDataController.API_VERSION, "");
                if (rawXML == null) {
                    destMappings = new HashMap<>();
                    return;
                }
                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(rawXML)));
                Node rootNode = doc.getDocumentElement();

                if (doc.hasChildNodes()) {
                    NodeList nl = rootNode.getChildNodes();
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node curNode = nl.item(i); //<e> node
                        if (!curNode.getNodeName().equals("#text")) {
                            Map<String, String> nodeInfo = new HashMap<>();
                            NamedNodeMap attributes = curNode.getAttributes();
                            for (int j = 0; j < attributes.getLength(); j++) {
                                String key = attributes.item(j).getNodeName();
                                if (!key.equals("#text")) {
                                    String val = attributes.item(j).getTextContent();
                                    nodeInfo.put(key, val);
                                }
                            }
                            destMappings.put(nodeInfo.get("o"), nodeInfo.get("r"));
                        }
                    }
                }
                db.saveDestMappings(destMappings);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        db.close();
    }

    public static Map<String, String> getDestMappings(Context context) {
        if (destMappings.isEmpty()) {
            loadDestMappings(context);
        }
        return destMappings;
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
        if (out.isEmpty()) {
            out = loadDirIds(context, lineId);
        }
        return out;
    }

    public static Map<String, Integer> loadDirIds(Context context, int lineId) {
        System.out.println("Loading directions from network");
        String httpData = NetworkController.performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getDirections", "{routeID: " + lineId + "}");
        try {
            if (httpData == null) {
                return null;
            }
            JSONObject json = new JSONObject(httpData);
            JSONArray arr = json.getJSONArray("d");

            if (arr.length() == 0) {
                return new HashMap<>();
            }

            Map<String, Integer> dirIds = new TreeMap<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject dirObj = arr.getJSONObject(i);
                String name = dirObj.getString("name");
                int id = dirObj.getInt("id");
                dirIds.put(name, id);
            }
            PersistentDataController.saveDirIds(context, lineId, dirIds);
            return dirIds;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
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
        if (out.isEmpty()) {
            loadStationIds(context, lineId, dirId);
        }
        return out;
    }

    public static Map<String, Integer> loadStationIds(Context context, int lineId, int dirId) {
        System.out.println("Getting stations from network");
        String httpData = NetworkController.performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getStops", "{routeID: " + lineId + ", directionID: " + dirId + "}");
        if (httpData == null) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(httpData);
            JSONArray arr = json.getJSONArray("d");

            if (arr.length() == 0) {
                return new HashMap<>();
            }

            Map<String, Integer> stopIds = new TreeMap<>();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject stopObj = arr.getJSONObject(i);
                String name = stopObj.getString("name");
                int id = stopObj.getInt("id");
                stopIds.put(name, id);
            }
            PersistentDataController.saveStationIds(context, lineId, dirId, stopIds);
            return stopIds;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
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

    public static boolean cacheAlert(Context context, int alertId, int lineId, String title, String url, String text) {
        DatabaseHandler db = new DatabaseHandler(context);
        boolean out = db.saveAlert(alertId, lineId, title, url, text);
        db.close();
        return out;
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
            String rawXML = NetworkController.basicHTTPRequest("https://nexttrain.futuresight.org/api/escelstatus?version=" + PersistentDataController.API_VERSION + "&stationid=" + stationId);
            try {
                if (rawXML == null) {
                    return new ArrayList<>();
                }

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

    public static double distance(LatLng first, LatLng second) {
        //from http://www.movable-type.co.uk/scripts/latlong.html
        int R = 6371000;
        double lat1 = Math.toRadians(first.latitude), lat2 = Math.toRadians(second.latitude);
        double lng1 = Math.toRadians(first.longitude), lng2 = Math.toRadians(second.longitude);
        double dLat = lat2 - lat1;
        double dLng = lng2 - lng1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static boolean attemptedMapMarkers = false;
    public static boolean haveMapMarkers = true;
    public static List<Station> getMapMarkers(Context context) {
        try {
            while (attemptedMapMarkers && !haveMapMarkers); //don't run this task two times at once
            attemptedMapMarkers = true;
            List<Station> out = new ArrayList<>();
            String cfgValue = PersistentDataController.getConfig(context, DatabaseHandler.CONFIG_LAST_SAVED_ALL_STOPS);
            boolean expired = false;
            if (cfgValue.equals("") || Integer.parseInt(cfgValue) < PersistentDataController.getCurTime() - PersistentDataController.getFavLocationExpiry(context)) {
                expired = true;
            }
            DatabaseHandler db = new DatabaseHandler(context);
            List<Station> fromDb = db.getCachedStopLocations();

            if (!expired && fromDb != null) {
                out = fromDb;
            } else {
                String httpData = NetworkController.basicHTTPRequest("https://nexttrain.futuresight.org/api/getallstops?version=" + PersistentDataController.API_VERSION);
                if (httpData == null) {
                    return new ArrayList<>();
                }
                Map<Integer, String> directions = new HashMap<>();

                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(httpData)));
                Node rootNode = doc.getDocumentElement();

                if (doc.hasChildNodes()) {
                    NodeList nl = rootNode.getChildNodes();
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node curNode = nl.item(i); //either <ds> or <ls>
                        switch (curNode.getNodeName()) {
                            case "ds":
                                NodeList dirNodes = curNode.getChildNodes();
                                for (int j = 0; j < dirNodes.getLength(); j++) {
                                    Node dirNode = dirNodes.item(j);
                                    if (dirNode.getNodeName().equals("d")) {
                                        int id = Integer.parseInt(dirNode.getAttributes().getNamedItem("i").getTextContent());
                                        String name = dirNode.getAttributes().getNamedItem("n").getTextContent();
                                        directions.put(id, name);
                                    }
                                }
                                break;
                            case "ls":
                                NodeList lineNodes = curNode.getChildNodes();
                                for (int j = 0; j < lineNodes.getLength(); j++) {
                                    Node lineNode = lineNodes.item(j);
                                    if (lineNode.getNodeName().equals("l")) {
                                        int lineId = Integer.parseInt(lineNode.getAttributes().getNamedItem("i").getTextContent());
                                        String lineName = lineNode.getAttributes().getNamedItem("n").getTextContent();
                                        int dirId = Integer.parseInt(lineNode.getAttributes().getNamedItem("d").getTextContent());
                                        char lineType = lineNode.getAttributes().getNamedItem("t").getTextContent().charAt(0); //"b" is bus, "r" is rail
                                        NodeList stopNodes = lineNode.getChildNodes();
                                        for (int k = 0; k < stopNodes.getLength(); k++) {
                                            Node stopNode = stopNodes.item(k);
                                            if (stopNode.getNodeName().equals("s")) {
                                                int id = Integer.parseInt(stopNode.getAttributes().getNamedItem("i").getTextContent());
                                                String name = stopNode.getAttributes().getNamedItem("n").getTextContent();
                                                double lat = Double.parseDouble(stopNode.getAttributes().getNamedItem("lt").getTextContent());
                                                double lng = Double.parseDouble(stopNode.getAttributes().getNamedItem("ln").getTextContent());

                                                Station st = new Station(name, id, directions.get(dirId), dirId, lineName, lineId, "", lat, lng, lineType);
                                                out.add(st);
                                            }
                                        }
                                    }
                                }
                                break;
                        }
                    }
                }
                db.cacheAllStops(out);
            }
            db.close();
            haveMapMarkers = true;
            return out;
        } catch (Exception e) {
            attemptedMapMarkers = false;
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}
