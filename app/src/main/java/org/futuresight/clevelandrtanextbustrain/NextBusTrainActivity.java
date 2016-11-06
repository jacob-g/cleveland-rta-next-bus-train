package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class NextBusTrainActivity extends AppCompatActivity {
    String[] directions = new String[1];
    Map<String, Integer> dirIds = new HashMap<>();
    String[] stops = new String[1];
    Map<String, Integer> stopIds = new HashMap<>();
    Map<String, String> destMappings = new HashMap<>();
    Map<String,Integer> alertCounts = new HashMap<>(); //lines for we have already checked service alerts

    private int preSelectedLineId = -1, preSelectedDirId = -1, preSelectedStopId = -1;

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
                new GetTimesTask(view.getContext(), null).execute(selectedRouteStr, selectedDirStr, selectedStopStr);
            }
        }
    }

    //listener that runs when a route is selected (loads the appropriate directions)
    private Spinner.OnItemSelectedListener routeSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                new GetDirectionsTask(view.getContext(), createDialog()).execute(selectedRouteStr);
                new GetServiceAlertsTask(view.getContext()).execute(selectedRouteStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private void alertDialog(String title, String msg, final boolean die) {
        AlertDialog alertDialog = new AlertDialog.Builder(NextBusTrainActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (die) {
                            finish();
                        }
                    }
                });
        alertDialog.show();
    }

    //listener that runs when the "add favorite" button is clicked
    private Button.OnClickListener addFavoriteClickedListener = new AdapterView.OnClickListener() {
        public void onClick(View view) {
            try {
                //get the current station information
                final String stationName = ((Spinner) findViewById(R.id.stationSpinner)).getSelectedItem().toString();
                final String dirName = ((Spinner) findViewById(R.id.dirSpinner)).getSelectedItem().toString();
                final String lineName = ((Spinner) findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                final int stationId = stopIds.get(stationName);
                final int lineId = PersistentDataController.getLineIdMap(view.getContext()).get(lineName);
                final int dirId = dirIds.get(dirName);

                //check if there's already a station
                DatabaseHandler db = new DatabaseHandler(NextBusTrainActivity.this);
                if (db.hasFavoriteLocation(lineId, dirId, stationId)) {
                    alertDialog(getResources().getString(R.string.error), getResources().getString(R.string.alreadyfavorited), false);
                    return;
                }
                db.close();

                //use a dialog box to ask the user for the name of the station
                final AlertDialog.Builder inputAlert = new AlertDialog.Builder(view.getContext());
                inputAlert.setTitle(R.string.add_favorite);
                inputAlert.setMessage(R.string.favorite_save_name_prompt);

                //create the text box and automatically populate it with the current station name
                final EditText userInput = new EditText(view.getContext());
                userInput.setText(stationName + " (" + dirName + ")");
                inputAlert.setView(userInput);

                inputAlert.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String name = userInput.getText().toString();
                            new SaveFavoriteTask(NextBusTrainActivity.this, stationName, stationId, dirName, dirId, lineName, lineId, name).execute(stationId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                inputAlert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog alertDialog = inputAlert.create();
                alertDialog.setCancelable(false);
                alertDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private class SaveFavoriteTask extends AsyncTask<Integer, Void, LatLng> {
        private Context myContext;
        String stationName, dirName, lineName, name;
        int stationId, dirId, lineId;
        public SaveFavoriteTask(Context context, String stationName, int stationId, String dirName, int dirId, String lineName, int lineId, String name) {
            myContext = context;
            this.stationName = stationName;
            this.stationId = stationId;
            this.dirName = dirName;
            this.dirId = dirId;
            this.lineName = lineName;
            this.lineId = lineId;
            this.name = name;
        }
        protected LatLng doInBackground(Integer... params) {
            return NetworkController.getLocationForStation(params[0]);
        }

        protected void onPostExecute(LatLng pos) {
            try {
                //TODO: on network failure, fail gracefully
                DatabaseHandler db = new DatabaseHandler(myContext);
                Station st;
                if (pos == null) {
                    st = new Station(stationName, stationId, dirName, dirId, lineName, lineId, name);
                } else {
                    st = new Station(stationName, stationId, dirName, dirId, lineName, lineId, name, pos.latitude, pos.longitude);
                }
                System.out.println(st);
                db.addFavoriteLocation(st);
                db.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //listener that runs when the "select favorite" button is clicked
    private Button.OnClickListener serviceAlertsClickedClickedListener = new AdapterView.OnClickListener() {
        public void onClick(View view) {
            Intent intent = new Intent(view.getContext(), ServiceAlertsActivity.class);
            intent.putExtra("route", ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString());
            intent.putExtra("routeId", PersistentDataController.getLineIdMap(view.getContext()).get(((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString()));
            startActivity(intent);
        }
    };

    //listener that runs when the "select favorite" button is clicked
    private Button.OnClickListener selectFavoriteClickedListener = new AdapterView.OnClickListener() {
        public void onClick(View view) {
            Intent intent = new Intent(view.getContext(), ManageLocationsActivity.class);
            intent.putExtra("return", true);
            startActivityForResult(intent, 1);
        }
    };

    private Button.OnClickListener viewOnMapClickedListener = new AdapterView.OnClickListener() {
        public void onClick(View v) {
            final String stationName = ((Spinner) findViewById(R.id.stationSpinner)).getSelectedItem().toString();
            if (stopIds == null || !stopIds.containsKey(stationName)) {
                return;
            }
            final int stationId = stopIds.get(stationName);
            Intent intent = new Intent(NextBusTrainActivity.this, NearMeActivity.class);
            intent.putExtra("stationId", stationId);
            startActivity(intent);
        }
    };

    //listener that runs when a direction is selected (loads the appropriate stops)
    private Spinner.OnItemSelectedListener dirSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                String selectedDirStr = ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem().toString();
                new GetStopsTask(view.getContext(), createDialog()).execute(selectedRouteStr, selectedDirStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private ProgressDialog createDialog() {
        ProgressDialog dlg = new ProgressDialog(NextBusTrainActivity.this);
        dlg.setTitle("Loading");
        dlg.setMessage("Please wait...");
        dlg.setCancelable(false);
        dlg.show();
        return dlg;
    }

    //listener that runs when a direction is selected (loads the appropriate stops)
    private Spinner.OnItemSelectedListener stopSelectedSpinner = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                String selectedDirStr = ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem().toString();
                String selectedStopStr = ((Spinner)findViewById(R.id.stationSpinner)).getSelectedItem().toString();

                new GetTimesTask(view.getContext(), createDialog()).execute(selectedRouteStr, selectedDirStr, selectedStopStr);
                new GetEscalatorElevatorStatus(view.getContext()).execute(stopIds.get(selectedStopStr));
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //give listeners to the appropriate actions
        Spinner lineSpinner = (Spinner) findViewById(R.id.lineSpinner); //this is for finding directions after a line is selected
        lineSpinner.setOnItemSelectedListener(routeSelectedListener);
        Spinner dirSpinner = (Spinner) findViewById(R.id.dirSpinner); //this is for finding stops after a direction and line are selected
        dirSpinner.setOnItemSelectedListener(dirSelectedListener);
        Spinner stationSpinner = (Spinner) findViewById(R.id.stationSpinner); //this is for finding stops after a direction and line are selected
        stationSpinner.setOnItemSelectedListener(stopSelectedSpinner);
        ImageButton addFavoriteBtn = (ImageButton) findViewById(R.id.addFavoriteBtn); //this is for when the "add favorite" button is clicked
        addFavoriteBtn.setOnClickListener(addFavoriteClickedListener);
        ImageButton selectFavoriteBtn = (ImageButton) findViewById(R.id.selectFavoriteBtn); //this is for when the "add favorite" button is clicked
        selectFavoriteBtn.setOnClickListener(selectFavoriteClickedListener);
        Button serviceAlertsBtn = (Button)findViewById(R.id.serviceAlertsBtn);
        serviceAlertsBtn.setOnClickListener(serviceAlertsClickedClickedListener);
        ImageButton viewOnMapBtn = (ImageButton)findViewById(R.id.viewOnMapBtn);
        viewOnMapBtn.setOnClickListener(viewOnMapClickedListener);

        if (!NetworkController.connected(this)) {
            alertDialog(getResources().getString(R.string.network), getResources().getString(R.string.nonetworkmsg), true);
            return;
        }

        //get the original list of routes
        try {
            //see if a pre-selected location was sent in
            if (getIntent().hasExtra("lineId")) {
                preSelectedLineId = getIntent().getExtras().getInt("lineId");
            }
            if (getIntent().hasExtra("dirId")) {
                preSelectedDirId = getIntent().getExtras().getInt("dirId");
            }
            if (getIntent().hasExtra("stopId")) {
                preSelectedStopId = getIntent().getExtras().getInt("stopId");
            }
            new GetLinesTask(this, createDialog()).execute();

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            //create a timer to get the list of stops as appropriate
            Timer timer = new Timer();
            TimerTask updateTimes = new UpdateTimesTask(this.findViewById(android.R.id.content));
            timer.scheduleAtFixedRate(updateTimes, 0, updateInterval * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private class GetLinesTask extends AsyncTask<Void, Void, String[]> {
        private Context myContext;
        private ProgressDialog myProgressDialog;
        public GetLinesTask(Context context, ProgressDialog pdlg) {
            myContext = context;
            myProgressDialog = pdlg;
        }
        protected String[] doInBackground(Void... params) {
            destMappings = PersistentDataController.getDestMappings(myContext);
            return PersistentDataController.getLines(myContext);
        }

        protected void onPostExecute(String[] lineNames) {
            if (lineNames.length == 0) {
                alertDialog(getResources().getString(R.string.error), getResources().getString(R.string.nextconnectdown), true);
            }
            //put that into the spinner
            Spinner lineSpinner = (Spinner) findViewById(R.id.lineSpinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(myContext, android.R.layout.simple_spinner_item, lineNames);
            lineSpinner.setAdapter(adapter);
            if (preSelectedLineId != -1) {
                for (int i = 0; i < lineNames.length; i++) {
                    if (preSelectedLineId == PersistentDataController.getLineIdMap(myContext).get(adapter.getItem(i))) {
                        lineSpinner.setSelection(i);
                        break;
                    }
                }
            }
            myProgressDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            preSelectedStopId = data.getExtras().getInt("stopId");
            preSelectedDirId = data.getExtras().getInt("dirId");
            preSelectedLineId = data.getExtras().getInt("lineId");
            new GetLinesTask(this, createDialog()).execute();
        }
    }

    private class GetDirectionsTask extends AsyncTask<String, Void, Map<String, Integer>> {
        private Context myContext;
        private ProgressDialog myProgressDialog;
        private Map<String, Integer> dirs;
        private int route;
        public GetDirectionsTask(Context context, ProgressDialog pdlg) {
            myContext = context;
            myProgressDialog = pdlg;
        }
        protected Map<String, Integer> doInBackground(String... params) {
            route = PersistentDataController.getLineIdMap(myContext).get(params[0]);
            dirs = PersistentDataController.getDirIds(myContext, route);
            return dirIds = dirs;
            /*if (dirs.isEmpty()) {
                return NetworkController.performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getDirections", "{routeID: " + route + "}");
            } else {
                return "";
            }*/
        }

        protected void onPostExecute(Map<String, Integer> result) {
            //parse the result as JSON
            try {
                if (result == null) { //connection failed
                    myProgressDialog.dismiss();
                    return;
                } else if (result.isEmpty()) {
                    alertDialog(getResources().getString(R.string.error), getResources().getString(R.string.nextconnectdown), true);
                }
                Set<String> dirSet = result.keySet();
                directions = new String[dirSet.size()];
                dirSet.toArray(directions);
                //put that into the spinner
                Spinner dirSpinner = (Spinner) findViewById(R.id.dirSpinner);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(myContext, android.R.layout.simple_spinner_item, directions);
                dirSpinner.setAdapter(adapter);

                if (preSelectedDirId != -1) {
                    for (int i = 0; i < directions.length; i++) {
                        if (preSelectedDirId == dirIds.get(adapter.getItem(i))) {
                            dirSpinner.setSelection(i);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            myProgressDialog.dismiss();
        }
    }

    private class GetStopsTask extends AsyncTask<String, Void, Map<String, Integer>> {
        private Context myContext;
        private Map<String, Integer> stations;
        private int myRouteId, myDirId;
        private ProgressDialog myProgressDialog;
        public GetStopsTask(Context context, ProgressDialog pdlg) {
            myContext = context;
            myProgressDialog = pdlg;
        }
        protected Map<String, Integer> doInBackground(String... params) {
            myRouteId = PersistentDataController.getLineIdMap(myContext).get(params[0]);
            myDirId = dirIds.get(params[1]);
            stations = PersistentDataController.getStationIds(myContext, myRouteId, myDirId);
            return PersistentDataController.getStationIds(myContext, myRouteId, myDirId);
        }

        protected void onPostExecute(Map<String, Integer> result) {
            //parse the result as JSON
            try {
                if (result == null) {
                    myProgressDialog.dismiss();
                    return;
                }

                Spinner stationSpinner = (Spinner) findViewById(R.id.stationSpinner);
                int selectPos = -1;

                stopIds = result;
                Set<String> stopSet = result.keySet();
                stops = new String[stopSet.size()];
                stopSet.toArray(stops);

                //get the current selection in case the direction is changing
                String curSelection = "";
                if (stationSpinner.getSelectedItem() != null) {
                    curSelection = stationSpinner.getSelectedItem().toString();
                }

                //put that into the spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(myContext, android.R.layout.simple_spinner_item, stops);
                stationSpinner.setAdapter(adapter);

                if (preSelectedStopId != -1) {
                    for (int i = 0; i < stops.length; i++) { //if there was a stop previously selected then select it again (this is to avoid resetting the spinner when changing directions), but if there is a stop sent in to the activity that overrides the previously selected stop
                        if (preSelectedStopId == stopIds.get(adapter.getItem(i)) || (preSelectedStopId == -1 && curSelection.equals(adapter.getItem(i)))) {
                            stationSpinner.setSelection(i);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            myProgressDialog.dismiss();
        }
    }

    //task to get the times of the bus/train
    private class GetTimesTask extends AsyncTask<String, Void, String> {
        private Context myContext;
        private String route;
        private ProgressDialog myProgressDlg;
        private int stopId;
        public GetTimesTask(Context context, ProgressDialog pdlg) {
            myContext = context;
            myProgressDlg = pdlg;
        }
        protected String doInBackground(String... params) {
            if (params[0] == null || params[1] == null || params[2] == null) {
                blankAll();
            }
            int routeId = PersistentDataController.getLineIdMap(myContext).get(params[0]);
            if (dirIds == null || stopIds == null) {
                return null;
            }
            int dirId = dirIds.get(params[1]);
            stopId = stopIds.get(params[2]);
            route = params[0]; //save the route for later in case we want to color the results

            //returns an array of {stop JSON, alerts XML}
            return NetworkController.performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getStopTimes", "{routeID: " + routeId + ", directionID: " + dirId + ", stopID:" + stopId + ", useArrivalTimes: false}");
        }

        protected void onPostExecute(String result) {
            //parse the result as JSON
            try {
                if (!NetworkController.connected(myContext) || result == null) {
                    return;
                }
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
                            dest = destMappings.containsKey(dest) ? destMappings.get(dest) : dest; //use the desti
                            String[] stopInfo = {time + period, dest, timeLeft + " minute" + (timeLeft == 1 ? "" : "s")};
                            stopList.add(stopInfo);
                        }
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
                if (myProgressDlg != null) {
                    myProgressDlg.dismiss();
                }
            } catch (Exception e) {
                myProgressDlg.dismiss();
                blankAll();
                System.err.println("Error in parsing JSON or XML");
                e.printStackTrace();
            }
        }
    }

    //task to get the times of the bus/train
    private class GetServiceAlertsTask extends AsyncTask<String, Void, Integer> {
        private Context myContext;
        private String route;
        public GetServiceAlertsTask(Context context) {
            myContext = context;
        }
        protected Integer doInBackground(String... params) {
            int routeId = PersistentDataController.getLineIdMap(myContext).get(params[0]);
            route = params[0]; //save the route for later in case we want to color the results
            return ServiceAlertsController.getAlertsByLine(myContext, new String[]{route}, new int[]{routeId}).size();
        }

        protected void onPostExecute(Integer alertCount) {
            Button serviceAlertsBtn = (Button) findViewById(R.id.serviceAlertsBtn);
            if (alertCount > 0) {
                serviceAlertsBtn.setVisibility(View.VISIBLE);
                serviceAlertsBtn.setText(String.format(getResources().getString(R.string.there_are_n_service_alerts), alertCount));
            } else {
                serviceAlertsBtn.setVisibility(View.INVISIBLE);
            }
        }
    }

    private class GetEscalatorElevatorStatus extends AsyncTask<Integer, Void, List<EscalatorElevatorAlert>> {
        private Context myContext;
        public GetEscalatorElevatorStatus(Context context) {
            myContext = context;
        }
        protected List<EscalatorElevatorAlert> doInBackground(Integer... params) {
            int stationId = params[0];
            return PersistentDataController.getEscalatorAlerts(myContext, stationId);
        }

        protected void onPostExecute(List<EscalatorElevatorAlert> statuses) {
            LinearLayout escElLayout = (LinearLayout) findViewById(R.id.escalatorElevatorAlertLayout);
            escElLayout.removeAllViews();
            if (statuses.size() > 0) {
                TextView title = new TextView(myContext);
                title.setTextColor(Color.BLACK);
                title.setTextSize(18);
                title.setText(getResources().getString(R.string.escelstatus
                ));
                escElLayout.addView(title);
                for (EscalatorElevatorAlert alert : statuses) {
                    ImageView im = new ImageView(myContext);
                    if (alert.working) {
                        im.setImageDrawable(ResourcesCompat.getDrawable(getResources(), android.R.drawable.presence_online, null));
                    } else {
                        im.setImageDrawable(ResourcesCompat.getDrawable(getResources(), android.R.drawable.presence_busy, null));
                    }

                    TextView cap = new TextView(myContext);
                    cap.setText(alert.name);
                    if (!alert.working) {
                        cap.setTextColor(Color.RED);
                    }

                    LinearLayout layout = new LinearLayout(myContext);
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.addView(im);
                    layout.addView(cap);

                    escElLayout.addView(layout);
                }
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
        int minOfDay = Integer.parseInt(timeParts[0]) * 60 + Integer.parseInt(timeParts[1]) + (period.equals("pm") && !timeParts[0].equals("12") ? 720 : 0) - (period.equals("am") && timeParts[0].equals("12") ? 720 : 0);
        int curTime = Calendar.getInstance().getTime().getHours() * 60 + Calendar.getInstance().getTime().getMinutes();
        if (minOfDay < curTime) {
            minOfDay += 1440;
        }
        return minOfDay - curTime;
    }

}

