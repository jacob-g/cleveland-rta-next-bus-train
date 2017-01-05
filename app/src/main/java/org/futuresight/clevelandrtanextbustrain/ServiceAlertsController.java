package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by jacob on 6/14/16.
 */
public abstract class ServiceAlertsController {

    public static List<Map<String, String>> getAlertsByLine(Context context, String[] routes, int[] routeIds) {
        List<Map<String, String>> alertList = new ArrayList<>();
        try {
            List<String> needToCache = new ArrayList<>();
            List<Integer> cachedLineIds = new ArrayList<>();
            Set<String> alreadyUsedUrls = new TreeSet<>();
            //see if each route is cached
            for (String s : routes) {
                List<Map<String, String>> lineAlerts = PersistentDataController.getAlerts(context, PersistentDataController.getLineIdMap(context).get(s));
                if (lineAlerts == null) {
                    needToCache.add(s); //mark the route if it needs to be downloaded anew
                } else {
                    for (Map<String, String> alert : lineAlerts) {
                        if (alreadyUsedUrls.add(alert.get("url"))) {
                            alertList.add(alert); //if the route is cached, just put the cached entry on the list
                        }
                    }
                }
            }
            if (!needToCache.isEmpty()) {
                //get data from the internet
                StringBuilder urlString = new StringBuilder("https://nexttrain.futuresight.org/api/alerts?version=" + PersistentDataController.API_VERSION + "&");
                for (String s : needToCache) {
                    urlString.append("routes[]=");
                    urlString.append(URLEncoder.encode(s, "UTF-8"));
                    urlString.append("&");
                    urlString.append("ids[]=");
                    urlString.append(URLEncoder.encode(Integer.toString(PersistentDataController.getLineIdMap(context).get(s)), "UTF-8"));
                    urlString.append("&");
                    cachedLineIds.add(PersistentDataController.getLineIdMap(context).get(s));
                }
                urlString.deleteCharAt(urlString.length() - 1);
                String result = NetworkController.basicHTTPRequest(urlString.toString());
                if (result != null && !result.equals("") && !result.equals("Error")) {
                    //get the service alert count
                    int count = 0;
                    DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = dBuilder.parse(new InputSource(new StringReader(result)));
                    Node rootNode = doc.getDocumentElement();

                    if (doc.hasChildNodes()) {
                        NodeList nl = rootNode.getChildNodes();
                        for (int i = 0; i < nl.getLength(); i++) {
                            Node curNode = nl.item(i); //<alert> node
                            if (!curNode.getNodeName().equals("#text")) {
                                Map<String, String> nodeInfo = new HashMap<>();
                                List<Integer> routeIdsForThisAlert = new ArrayList<>();
                                NodeList children = curNode.getChildNodes();
                                for (int j = 0; j < children.getLength(); j++) {
                                    String key = children.item(j).getNodeName();
                                    if (!key.equals("#text")) {
                                        if (key.equals("routeset")) {
                                            NodeList routeChildren = children.item(j).getChildNodes();
                                            for (int k = 0; k < routeChildren.getLength(); k++) {
                                                if (routeChildren.item(k).getNodeName().equals("route")) {
                                                    routeIdsForThisAlert.add(Integer.parseInt(routeChildren.item(k).getTextContent()));
                                                }
                                            }
                                        } else {
                                            String val = children.item(j).getTextContent();
                                            nodeInfo.put(key, val);
                                        }
                                    }
                                }
                                if (alreadyUsedUrls.add(nodeInfo.get("url"))) { //add the alert if it's not already there
                                    alertList.add(nodeInfo);
                                }
                                //cache the alert for each line
                                for (int route : routeIdsForThisAlert) {
                                    boolean unread = PersistentDataController.cacheAlert(context, Integer.parseInt(nodeInfo.get("id")), route, nodeInfo.get("title"), nodeInfo.get("url"), nodeInfo.get("info"));
                                    nodeInfo.put("new", unread ? "true" : "false");
                                }
                                count++;
                            }
                        }
                    }
                    PersistentDataController.markAsSavedForLineAlerts(context, cachedLineIds);
                } else {
                    //HTTP request failed or whatever
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return alertList;
    }

    public static void markAsRead(Context context, List<Integer> ids) {
        DatabaseHandler db = new DatabaseHandler(context);
        db.markAlertsAsRead(ids);
        db.close();
    }
}
