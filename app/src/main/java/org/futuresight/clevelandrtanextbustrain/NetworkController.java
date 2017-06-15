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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    private static int getTimeLeft(String time1, String period1, String time2, String period2, boolean correctNegative) { //get the difference between two times, with the earlier one coming first
        String[] time1Parts = time1.split(":");
        int time1MinOfDay = Integer.parseInt(time1Parts[0]) * 60 + Integer.parseInt(time1Parts[1]) + (period1.equals("pm") && !time1Parts[0].equals("12") ? 720 : 0) - (period1.equals("am") && time1Parts[0].equals("12") ? 720 : 0);
        String[] time2Parts = time2.split(":");
        int time2MinOfDay = Integer.parseInt(time2Parts[0]) * 60 + Integer.parseInt(time2Parts[1]) + (period2.equals("pm") && !time2Parts[0].equals("12") ? 720 : 0) - (period2.equals("am") && time2Parts[0].equals("12") ? 720 : 0);

        int result = time2MinOfDay - time1MinOfDay;
        if (result > 1440) {
            result -= 1440;
        } else if (result < 0 && correctNegative) {
            result += 1440;
        }
        return result;
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
            DateFormat df1 = new SimpleDateFormat("hh:mm");
            DateFormat df2 = new SimpleDateFormat("aa");

            JSONObject json = new JSONObject(result);
            JSONObject root = json.getJSONObject("d");
            JSONArray stopsJson = root.getJSONArray("stops");
            JSONObject stops0JSON = stopsJson.getJSONObject(0);
            if (!stops0JSON.isNull("crossings")) {
                JSONArray stopsListJson = stops0JSON.getJSONArray("crossings");

                for (int i = 0; i < stopsListJson.length(); i++) {
                    JSONObject curStopJson = stopsListJson.getJSONObject(i);
                    String time, period, schedTime, schedPeriod;
                    boolean realTime;
                    if (!curStopJson.getBoolean("cancelled")) { //make sure the train isn't cancelled
                        if (curStopJson.getString("predTime").equals("null")) { //if we don't have an actual time (i.e. for trains that haven't left yet), use the scheduled time
                            realTime = false;
                            time = curStopJson.getString("schedTime");
                            schedTime = time;
                            period = curStopJson.getString("schedPeriod");
                            schedPeriod = period;
                        } else {
                            realTime = true;
                            time = curStopJson.getString("predTime");
                            schedTime = curStopJson.getString("schedTime");
                            period = curStopJson.getString("predPeriod");
                            schedPeriod = curStopJson.getString("schedPeriod");
                        }
                        Calendar c = Calendar.getInstance();
                        Date curDate = c.getTime();

                        int timeLeft = getTimeLeft(df1.format(curDate), df2.format(curDate).toLowerCase(), time, period, true); //get the time left
                        String schedInfo;
                        if (realTime) {
                            if (schedTime.equals(time)) {
                                schedInfo = "On time";
                            } else {
                                //TODO: get exact time and specify early/late
                                int lateness = getTimeLeft(schedTime, schedPeriod, time, period, true);
                                schedInfo = lateness + " ";
                                if (lateness >= 0) {
                                    schedInfo += "late";
                                } else {
                                    schedInfo += "early";
                                }
                            }
                        } else {
                            schedInfo = "Scheduled";
                        }
                        String dest = curStopJson.getString("destination");
                        dest = destMappings.containsKey(dest) ? destMappings.get(dest) : dest; //use the destination mapping
                        String[] stopInfo = {time + period, dest, timeLeft + " minute" + (timeLeft == 1 ? "" : "s"), schedInfo};
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
