package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by jacob on 5/19/16.
 */
public abstract class NetworkController {
    //perform a POST request to a given URL with given data, and request JSON data
    public static String  performPostCall(String requestURL, String postData) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Cookie", "gali=edit-facility-select&has_js=1");



            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(postData);

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response="";

            }
        } catch (Exception e) {
            if (!e.getMessage().contains("Unable to resolve host")) {
                e.printStackTrace();
            }
            response = null;
        }

        return response;
    }

    public static String basicHTTPRequest(String requestURL) {
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            int responseCode=conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line + "\n";
                }
            } else {
                response="";
            }
        } catch (Exception e) {
            if (!e.getMessage().contains("Unable to resolve host")) {
                e.printStackTrace();
            }
            response = null;
        }

        return response;
    }

    public static boolean connected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static LatLng getLocationForStation(int id) {
        LatLng out = null;
        try {
            String data = basicHTTPRequest("https://nexttrain.futuresight.org/api/getstopinfo?version=" + PersistentDataController.API_VERSION + "&id=" + id);
            if (data.startsWith("<?xml")) {
                Map<String, String> stopDataMap = new HashMap<>();
                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(data)));
                Node rootNode = doc.getDocumentElement();

                if (doc.hasChildNodes()) {
                    NodeList nl = rootNode.getChildNodes();
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node curNode = nl.item(i);
                        if (!curNode.getNodeName().equals("#text")) {
                            stopDataMap.put(curNode.getNodeName(), curNode.getTextContent());
                        }
                    }
                }

                if (stopDataMap.containsKey("lat")) {
                    out = new LatLng(Double.parseDouble(stopDataMap.get("lat")), Double.parseDouble(stopDataMap.get("lng")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    private static int getTimeLeft(String time, String period) { //get the amount of time until a specified arrival
        String[] timeParts = time.split(":");
        int minOfDay = Integer.parseInt(timeParts[0]) * 60 + Integer.parseInt(timeParts[1]) + (period.equals("pm") && !timeParts[0].equals("12") ? 720 : 0) - (period.equals("am") && timeParts[0].equals("12") ? 720 : 0);
        int curTime = Calendar.getInstance().getTime().getHours() * 60 + Calendar.getInstance().getTime().getMinutes();
        if (minOfDay < curTime) {
            minOfDay += 1440;
        }
        return minOfDay - curTime;
    }

    public static List<String[]> getStopTimes(Context context, int routeId, int dirId, int stopId) {
        Map<String, String> destMappings = PersistentDataController.getDestMappings(context);
        String result = performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getStopTimes", "{routeID: " + routeId + ", directionID: " + dirId + ", stopID:" + stopId + ", useArrivalTimes: false}");
        if (!connected(context) || result == null || result.equals("")) {
            return null;
        }
        //it's d->stops->0->crossings, then an array with the stop information
        List<String[]> stopList = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(result);
            JSONObject root = json.getJSONObject("d");
            JSONArray stopsJson = root.getJSONArray("stops");
            JSONObject stops0JSON = stopsJson.getJSONObject(0);
            if (!stops0JSON.isNull("crossings")) {
                JSONArray stopsListJson = stops0JSON.getJSONArray("crossings");

                for (int i = 0; i < stopsListJson.length(); i++) {
                    JSONObject curStopJson = stopsListJson.getJSONObject(i);
                    String time, period;
                    if (!curStopJson.getBoolean("cancelled")) { //make sure the train isn't cancelled
                        if (curStopJson.getString("predTime").equals("null")) { //if we don't have an actual time (i.e. for trains that haven't left yet), use the scheduled time
                            time = curStopJson.getString("schedTime");
                            period = curStopJson.getString("schedPeriod");
                        } else {
                            time = curStopJson.getString("predTime");
                            period = curStopJson.getString("predPeriod");
                        }
                        int timeLeft = getTimeLeft(time, period); //get the time left
                        String dest = curStopJson.getString("destination");
                        dest = destMappings.containsKey(dest) ? destMappings.get(dest) : dest; //use the destination mapping
                        String[] stopInfo = {time + period, dest, timeLeft + " minute" + (timeLeft == 1 ? "" : "s")};
                        stopList.add(stopInfo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stopList;
    }
}
