package org.futuresight.clevelandrtanextbustrain;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.*;

public class NextBusTrainActivity extends AppCompatActivity {
    String[] lineNames = new String[1];
    Map<String, Integer> lineIds = new HashMap<>();
    String[] directions = new String[1];
    Map<String, Integer> dirIds = new HashMap<>();
    String[] stops = new String[1];
    Map<String, Integer> stopIds = new HashMap<>();
    Map<String, String> destMappings = new HashMap<>();
    private final int updateInterval = 30; //the number of seconds to wait before refreshing the train information

    class UpdateTimesTask extends TimerTask { //the task to update the stop times
        private View view;
        public UpdateTimesTask(View v) {
            view = v;
        }

        public void run() {
            //if anything here is null, then don't update the times
            String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem() == null ? "" : ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
            String selectedDirStr = ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem() == null ? "" : ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem().toString();
            String selectedStopStr = ((Spinner)findViewById(R.id.stationSpinner)).getSelectedItem() == null ? "" : ((Spinner)findViewById(R.id.stationSpinner)).getSelectedItem().toString();
            if (selectedRouteStr.equals("") || selectedDirStr.equals("") || selectedStopStr.equals("")) {
                //nothing is selected, so try again later
            } else {
                new GetTimesTask(view.getContext()).execute(selectedRouteStr, selectedDirStr, selectedStopStr);
            }
        }
    }

    //listener that runs when a route is selected (loads the appropriat edirections)
    private Spinner.OnItemSelectedListener routeSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                new GetDirectionsTask(view.getContext()).execute(selectedRouteStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    //listener that runs when a direction is selected (loads the appropriate stops)
    private Spinner.OnItemSelectedListener dirSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                String selectedDirStr = ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem().toString();
                new GetStopsTask(view.getContext()).execute(selectedRouteStr, selectedDirStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    //listener that runs when a direction is selected (loads the appropriate stops)
    private Spinner.OnItemSelectedListener stopSelectedSpinner = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                String selectedDirStr = ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem().toString();
                String selectedStopStr = ((Spinner)findViewById(R.id.stationSpinner)).getSelectedItem().toString();
                System.out.println("Updating stop times!");
                new GetTimesTask(view.getContext()).execute(selectedRouteStr, selectedDirStr, selectedStopStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_bus_train);

        //give listeners to the appropriate actions
        Spinner lineSpinner = (Spinner) findViewById(R.id.lineSpinner); //this is for finding directions after a line is selected
        lineSpinner.setOnItemSelectedListener(routeSelectedListener);
        Spinner dirSpinner = (Spinner) findViewById(R.id.dirSpinner); //this is for finding stops after a direction and line are selected
        dirSpinner.setOnItemSelectedListener(dirSelectedListener);
        Spinner stationSpinner = (Spinner) findViewById(R.id.stationSpinner); //this is for finding stops after a direction and line are selected
        stationSpinner.setOnItemSelectedListener(stopSelectedSpinner);

        //cut off some destinations by replacing them with shorter names
        destMappings.put("Blue Line - Van Aken / Warrensville", "Blue - Warrensville");
        destMappings.put("Green Line - Green Road", "Green - Green Road");
        destMappings.put("Blue Line - Waterfront", "Waterfront");
        destMappings.put("Green Line - Waterfront", "Waterfront");
        destMappings.put("Red Line - Stokes / Windermere", "Windermere");
        destMappings.put("Red Line - Airport", "Airport");

        //get the original list of routes
        try {
            new GetLinesTask(this).execute();

            //create a timer to get the list of stops as appropriate
            Timer timer = new Timer();
            TimerTask updateTimes = new UpdateTimesTask(this.findViewById(android.R.id.content));
            timer.scheduleAtFixedRate(updateTimes, 0, updateInterval * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }




    //perform a POST request to a given URL with given data, and request JSON data
    public String  performPostCall(String requestURL, String postData) {

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
            conn.setRequestProperty("Content-Type", "application/json;\tcharset=utf-8");


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
            e.printStackTrace();
        }

        return response;
    }

    private class GetLinesTask extends AsyncTask<Void, Void, String> {
        private Context myContext;
        public GetLinesTask(Context context) {
            myContext = context;
        }
        protected String doInBackground(Void... params) {
            String resp = performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getRoutes", "");
            return resp;
        }

        protected void onPostExecute(String result) {
            //parse the result as JSON
            try {
                JSONObject json = new JSONObject(result);
                JSONArray arr = json.getJSONArray("d");
                lineNames = new String[arr.length()];
                lineIds = new HashMap<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject lineObj = arr.getJSONObject(i);
                    lineNames[i] = lineObj.getString("name");
                    lineIds.put(lineNames[i], lineObj.getInt("id"));
                }

                //put that into the spinner
                Spinner lineSpinner = (Spinner)findViewById(R.id.lineSpinner);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(myContext, android.R.layout.simple_spinner_item, lineNames);
                lineSpinner.setAdapter(adapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetDirectionsTask extends AsyncTask<String, Void, String> {
        private Context myContext;
        public GetDirectionsTask(Context context) {
            myContext = context;
        }
        protected String doInBackground(String... params) {
            int routeId = lineIds.get(params[0]);
            String resp = performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getDirections", "{routeID: " + routeId + "}");
            return resp;
        }

        protected void onPostExecute(String result) {
            //parse the result as JSON
            try {
                JSONObject json = new JSONObject(result);
                JSONArray arr = json.getJSONArray("d");

                directions = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject lineObj = arr.getJSONObject(i);
                    directions[i] = lineObj.getString("name");
                    dirIds.put(directions[i], lineObj.getInt("id"));
                }

                //put that into the spinner
                Spinner dirSpinner = (Spinner)findViewById(R.id.dirSpinner);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(myContext, android.R.layout.simple_spinner_item, directions);
                dirSpinner.setAdapter(adapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetStopsTask extends AsyncTask<String, Void, String> {
        private Context myContext;
        public GetStopsTask(Context context) {
            myContext = context;
        }
        protected String doInBackground(String... params) {
            int routeId = lineIds.get(params[0]);
            int dirId = dirIds.get(params[1]);
            String resp = performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getStops", "{routeID: " + routeId + ", directionID: " + dirId + "}");
            return resp;
        }

        protected void onPostExecute(String result) {
            //parse the result as JSON
            try {
                JSONObject json = new JSONObject(result);
                JSONArray arr = json.getJSONArray("d");

                stops = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject lineObj = arr.getJSONObject(i);
                    stops[i] = lineObj.getString("name");
                    stopIds.put(stops[i], lineObj.getInt("id"));
                }

                //put that into the spinner
                Spinner stationSpinner = (Spinner)findViewById(R.id.stationSpinner);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(myContext, android.R.layout.simple_spinner_item, stops);
                stationSpinner.setAdapter(adapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    //task to get the times of the bus/train
    private class GetTimesTask extends AsyncTask<String, Void, String> {
        private Context myContext;
        private String route;
        public GetTimesTask(Context context) {
            myContext = context;
        }
        protected String doInBackground(String... params) {
            int routeId = lineIds.get(params[0]);
            int dirId = dirIds.get(params[1]);
            int stopId = stopIds.get(params[2]);
            route = params[0]; //save the route for later in case we want to color the results
            String resp = performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getStopTimes", "{routeID: " + routeId + ", directionID: " + dirId + ", stopID:" + stopId + ", useArrivalTimes: false}");
            return resp;
        }

        protected void onPostExecute(String result) {
            //parse the result as JSON
            try {
                //it's d->stops->0->crossings, then an array with the stop information
                JSONObject json = new JSONObject(result);
                JSONObject root = json.getJSONObject("d");
                JSONArray stopsJson = root.getJSONArray("stops");
                JSONObject stops0JSON = stopsJson.getJSONObject(0);

                if (!stops0JSON.isNull("crossings")) {
                    JSONArray stopsListJson = stops0JSON.getJSONArray("crossings");
                    List<String[]> stopList = new ArrayList<>();
                    for (int i = 0; i < stopsListJson.length(); i++) {
                        JSONObject curStopJson = stopsListJson.getJSONObject(i);
                        String time = "", period = "";
                        if (curStopJson.getString("predTime").equals("null")) {
                            time = curStopJson.getString("schedTime");
                            period = curStopJson.getString("schedPeriod");
                        } else {
                            time = curStopJson.getString("predTime");
                            period = curStopJson.getString("predPeriod");
                        }
                        int timeLeft = getTimeLeft(time, period);
                        String dest = curStopJson.getString("destination");
                        dest = destMappings.containsKey(dest) ? destMappings.get(dest) : dest;
                        String[] stopInfo = {time + period, dest, timeLeft + " minute" + (timeLeft == 1 ? "" : "s")};
                        //convert to usable time, this should be put into a function


                        stopList.add(stopInfo);
                    }

                    //populate the text fields with the stop data
                    if (stopList.size() > 0) {
                        ((TextView) findViewById(R.id.train1timebox)).setText(stopList.get(0)[0]);
                        ((TextView) findViewById(R.id.train1destbox)).setText(stopList.get(0)[1]);
                        ((TextView) findViewById(R.id.train1timeLeftBox)).setText(stopList.get(0)[2]);
                    } else {
                        ((TextView) findViewById(R.id.train1timebox)).setText(R.string.no_time);
                        ((TextView) findViewById(R.id.train1destbox)).setText(R.string.no_destination);
                        ((TextView) findViewById(R.id.train1timeLeftBox)).setText(R.string.not_available);
                    }
                    if (stopList.size() > 1) {
                        ((TextView) findViewById(R.id.train2timebox)).setText(stopList.get(1)[0]);
                        ((TextView) findViewById(R.id.train2destbox)).setText(stopList.get(1)[1]);
                        ((TextView) findViewById(R.id.train2timeLeftBox)).setText(stopList.get(1)[2]);
                    } else {
                        ((TextView) findViewById(R.id.train2timebox)).setText(R.string.no_time);
                        ((TextView) findViewById(R.id.train2destbox)).setText(R.string.no_destination);
                        ((TextView) findViewById(R.id.train2timeLeftBox)).setText(R.string.not_available);
                    }
                    if (stopList.size() > 2) {
                        ((TextView) findViewById(R.id.train3timebox)).setText(stopList.get(2)[0]);
                        ((TextView) findViewById(R.id.train3destbox)).setText(stopList.get(2)[1]);
                        ((TextView) findViewById(R.id.train3timeLeftBox)).setText(stopList.get(2)[2]);
                    } else {
                        ((TextView) findViewById(R.id.train3timebox)).setText(R.string.no_time);
                        ((TextView) findViewById(R.id.train3destbox)).setText(R.string.no_destination);
                        ((TextView) findViewById(R.id.train3timeLeftBox)).setText(R.string.not_available);
                    }
                } else {
                    blankAll();
                }
            } catch (JSONException e) {
                blankAll();
                System.err.println("Error in parsing JSON");
                e.printStackTrace();
            }
        }
    }

    private void blankAll() { //set all text slots to the default value
        ((TextView) findViewById(R.id.train1timebox)).setText(R.string.no_time);
        ((TextView) findViewById(R.id.train1destbox)).setText(R.string.no_destination);
        ((TextView) findViewById(R.id.train2timebox)).setText(R.string.no_time);
        ((TextView) findViewById(R.id.train2destbox)).setText(R.string.no_destination);
        ((TextView) findViewById(R.id.train3timebox)).setText(R.string.no_time);
        ((TextView) findViewById(R.id.train3destbox)).setText(R.string.no_destination);
        ((TextView) findViewById(R.id.train1timeLeftBox)).setText(R.string.not_available);
        ((TextView) findViewById(R.id.train2timeLeftBox)).setText(R.string.not_available);
        ((TextView) findViewById(R.id.train3timeLeftBox)).setText(R.string.not_available);
    }

    public int getTimeLeft(String time, String period) { //get the amount of time until a specified arrival
        String[] timeParts = time.split(":");
        int minOfDay = Integer.parseInt(timeParts[0]) * 60 + Integer.parseInt(timeParts[1]) + (period.equals("pm") && !timeParts[0].equals("12") ? 720 : 0);
        int curTime = Calendar.getInstance().getTime().getHours() * 60 + Calendar.getInstance().getTime().getMinutes();
        int timeLeft = minOfDay - curTime;
        return timeLeft;
    }

}

