package org.futuresight.clevelandrtanextbustrain;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ServiceAlertsActivity extends AppCompatActivity {
    int selectedRouteId;

    private Spinner.OnItemSelectedListener lineSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selectedRouteStr = ((Spinner)findViewById(R.id.serviceAlertsLineSpinner)).getSelectedItem().toString();
                if (selectedRouteStr == "Favorites") {
                    DatabaseHandler db = new DatabaseHandler(ServiceAlertsActivity.this);
                    List<Station> favoriteStations = db.getFavoriteLocations();
                    Set<String> lines = new HashSet<>();
                    for (Station st : favoriteStations) {
                        lines.add(st.getLineName());
                    }
                    db.close();
                    System.out.println(lines);
                    String[] arr = new String[lines.size()];
                    int i = 0;
                    for (String s : lines) {
                        arr[i] = s;
                        i++;
                    }
                    new GetServiceAlertsTask(view.getContext(), createDialog()).execute(arr);
                } else {
                    new GetServiceAlertsTask(view.getContext(), createDialog()).execute(new String[]{selectedRouteStr});
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private ProgressDialog createDialog() {
        ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle("Loading");
        dlg.setMessage("Please wait...");
        dlg.show();
        return dlg;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_alerts);
        
        String[] selectedRoutes = new String[1];
        if (getIntent().hasExtra("route") && getIntent().hasExtra("routeId")) {
            selectedRoutes = new String[]{getIntent().getExtras().getString("route")};
            selectedRouteId = getIntent().getExtras().getInt("routeId");
        } else {
            selectedRoutes = new String[]{"1", "2"};
        }

        ((Spinner)findViewById(R.id.serviceAlertsLineSpinner)).setOnItemSelectedListener(lineSelectedListener);
        new GetLinesTask(this, createDialog()).execute();
        new GetServiceAlertsTask(this, createDialog()).execute(selectedRoutes);
    }

    //task to get the times of the bus/train
    private class GetServiceAlertsTask extends AsyncTask<String[], Void, String> {
        private Context myContext;
        private String[] routes;
        private ProgressDialog myProgressDialog;
        public GetServiceAlertsTask(Context context, ProgressDialog pdlg) {
            myContext = context;
            myProgressDialog = pdlg;
        }
        protected String doInBackground(String[]... params) {
            String returnData = "";
            try {
                routes = params[0]; //save the route for later in case we want to color the results
                StringBuilder urlString = new StringBuilder("https://nexttrain.futuresight.org/api/alerts?");
                for (String s : routes) {
                    urlString.append("routes[]=");
                    urlString.append(URLEncoder.encode(s, "UTF-8"));
                    urlString.append("&");
                }
                urlString.deleteCharAt(urlString.length() - 1);
                System.out.println("URL: " + urlString.toString());
                returnData = NetworkController.basicHTTPRequest(urlString.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            myProgressDialog.dismiss();
        }
    }

    private class GetLinesTask extends AsyncTask<Void, Void, String> {
        private Context myContext;
        private ProgressDialog myProgressDialog;
        public GetLinesTask(Context context, ProgressDialog pdlg) {
            myContext = context;
            myProgressDialog = pdlg;
        }
        protected String doInBackground(Void... params) {
            if (PersistentDataController.linesStored(myContext)) {
                return "";
            } else {
                return NetworkController.performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getRoutes", "");
            }
        }

        protected void onPostExecute(String result) {
            //parse the result as JSON
            int selectPos = -1;
            String[] lineNames = new String[1];
            if (result != "" && !PersistentDataController.linesStored(myContext)) {
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray arr = json.getJSONArray("d");
                    lineNames = new String[arr.length()];


                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject lineObj = arr.getJSONObject(i);
                        lineNames[i] = lineObj.getString("name");
                        int id = lineObj.getInt("id");
                        PersistentDataController.getLineIdMap().put(lineNames[i], id);
                        if (selectedRouteId == id) {
                            selectPos = i;
                            selectedRouteId = -1;
                        }
                    }
                    PersistentDataController.saveLineIdMap(myContext);

                    PersistentDataController.setLines(lineNames);
                } catch(JSONException e){
                    e.printStackTrace();
                }
            } else {
                lineNames = PersistentDataController.getLines();
                for (int i = 0; i < lineNames.length; i++) {
                    String line = lineNames[i];
                    int id = PersistentDataController.getLineIdMap().get(line);
                    if (selectedRouteId == id) {
                        selectPos = i;
                        selectedRouteId = -1;
                    }
                }
            }
            //put that into the spinner
            Spinner lineSpinner = (Spinner) findViewById(R.id.serviceAlertsLineSpinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(myContext, android.R.layout.simple_spinner_item);
            adapter.add("Favorites");
            adapter.addAll(lineNames);
            lineSpinner.setAdapter(adapter);
            if (selectPos != -1) {
                lineSpinner.setSelection(selectPos + 1);
            }
            myProgressDialog.dismiss();
        }
    }
}
