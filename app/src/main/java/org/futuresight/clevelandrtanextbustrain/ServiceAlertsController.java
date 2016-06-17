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
            boolean alreadyCached = true;
            List<Integer> needToCache = new ArrayList<>();
            //see if each route is cached
            for (String s : routes) {
                if (PersistentDataController.getAlerts(context, PersistentDataController.getLineIdMap(context).get(s)) == null) {
                    alreadyCached = false;
                    needToCache.add(PersistentDataController.getLineIdMap(context).get(s));
                }
            }
            if (alreadyCached) {
                //data is already cached
                Set<String> alreadyUsedUrls = new TreeSet<>();
                for (int id : routeIds) {
                    List<Map<String, String>> tempAlertList = PersistentDataController.getAlerts(context, id);
                    for (Map<String, String> curAlert : tempAlertList) {
                        if (alreadyUsedUrls.add(curAlert.get("url"))) {
                            alertList.add(curAlert);
                        }
                    }
                }
            } else {
                //not cached, so get them from the web
                StringBuilder urlString = new StringBuilder("https://nexttrain.futuresight.org/api/alerts?");
                for (String s : routes) {
                    urlString.append("routes[]=");
                    urlString.append(URLEncoder.encode(s, "UTF-8"));
                    urlString.append("&");
                    urlString.append("ids[]=");
                    urlString.append(URLEncoder.encode(Integer.toString(PersistentDataController.getLineIdMap(context).get(s)), "UTF-8"));
                    urlString.append("&");
                }
                urlString.deleteCharAt(urlString.length() - 1);
                String result = NetworkController.basicHTTPRequest(urlString.toString());
                if (!result.equals("") && !result.equals("Error")) {
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
                                alertList.add(nodeInfo);
                                //cache the alert for each line
                                for (int route : routeIdsForThisAlert) {
                                    if (needToCache.contains(route)) {
                                        PersistentDataController.cacheAlert(context, route, nodeInfo.get("title"), nodeInfo.get("url"), nodeInfo.get("info"));
                                    }
                                }
                                count++;
                            }
                        }
                    }
                    PersistentDataController.markAsSavedForLineAlerts(context, needToCache);
                } else {
                    //HTTP request failed or whatever
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return alertList;
    }
}
