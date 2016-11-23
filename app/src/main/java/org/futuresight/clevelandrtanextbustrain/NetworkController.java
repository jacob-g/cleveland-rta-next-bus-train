package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.android.gms.maps.model.LatLng;

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
import java.util.HashMap;
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
}
