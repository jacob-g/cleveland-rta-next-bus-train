package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ServiceAlertsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String[] selectedRoutes = new String[1];
        if (getIntent().hasExtra("route")) {
            selectedRoutes = new String[]{getIntent().getExtras().getString("route")};
        } else {
            selectedRoutes = new String[]{"1", "2"};
        }

        new GetServiceAlertsTask(this).execute(selectedRoutes);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_alerts);
    }

    //task to get the times of the bus/train
    private class GetServiceAlertsTask extends AsyncTask<String[], Void, String> {
        private Context myContext;
        private String[] routes;
        public GetServiceAlertsTask(Context context) {
            myContext = context;
        }
        protected String doInBackground(String[]... params) {
            routes = params[0]; //save the route for later in case we want to color the results
            String returnData;
            StringBuilder urlString = new StringBuilder("https://nexttrain.futuresight.org/api/alerts?");
            for (String s : routes) {
                urlString.append("routes[]=");
                urlString.append(s);
                urlString.append("&");
            }
            urlString.deleteCharAt(urlString.length() - 1);
            returnData = NetworkController.basicHTTPRequest(urlString.toString());
            return returnData;
        }

        protected void onPostExecute(String result) {
            //parse the result as JSON
            try {
                if (!result.equals("") && !result.equals("Error")) {
                    //get the service alert count
                    int count = 0;
                    DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = dBuilder.parse(new InputSource(new StringReader(result)));
                    Node rootNode = doc.getDocumentElement();
                    List<Map<String, String>> alertList = new ArrayList<>();
                    if (doc.hasChildNodes()) {
                        NodeList nl = rootNode.getChildNodes();
                        for (int i = 0; i < nl.getLength(); i++) {
                            Node curNode = nl.item(i); //<alert> node
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
                                alertList.add(nodeInfo);
                                count++;
                            }
                        }
                    }
                    LinearLayout serviceAlertsLayout = (LinearLayout) findViewById(R.id.serviceAlertVerticalLayout);
                    serviceAlertsLayout.removeAllViews();
                    System.out.println(alertList);
                    for (Map<String, String> alertInfo : alertList) {
                        TextView titleView = new TextView(myContext);
                        titleView.setText(alertInfo.get("title"));
                        titleView.setTypeface(null, Typeface.BOLD);
                        titleView.setTextColor(Color.BLUE);
                        final String url = alertInfo.get("url");
                        titleView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(browserIntent);
                            }
                        });
                        serviceAlertsLayout.addView(titleView);
                        TextView contentView = new TextView(myContext);
                        contentView.setText(alertInfo.get("info"));
                        serviceAlertsLayout.addView(contentView);
                    }
                } else {

                }
            } catch (Exception e) {
                System.err.println("Error in parsing XML");
                e.printStackTrace();
            }
        }
    }
}
