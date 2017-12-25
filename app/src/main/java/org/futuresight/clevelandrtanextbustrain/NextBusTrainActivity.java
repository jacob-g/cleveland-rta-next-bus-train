package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class NextBusTrainActivity extends AppCompatActivity {
    private String[] directions = new String[1];
    private Map<String, Integer> dirIds = new HashMap<>();
    private String[] stops = new String[1];
    private Map<String, Integer> stopIds = new HashMap<>();

    private int preSelectedLineId = -1, preSelectedDirId = -1, preSelectedStopId = -1;

    private boolean showingErrorDialog = false;

    private final int updateInterval = 30; //the number of seconds to wait before refreshing the train information

    class UpdateTimesTask extends TimerTask { //the task to update the stop times
        private View view;
        public UpdateTimesTask(View v) {
            view = v;
        }

        public void run() {
            //if anything here is null, then don't update the times
            final String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem() == null ? "" : ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
            final String selectedDirStr = ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem() == null ? "" : ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem().toString();
            final String selectedStopStr = ((Spinner)findViewById(R.id.stationSpinner)).getSelectedItem() == null ? "" : ((Spinner)findViewById(R.id.stationSpinner)).getSelectedItem().toString();
            if (selectedRouteStr.equals("") || selectedDirStr.equals("") || selectedStopStr.equals("")) {
                //nothing is selected, so try again later
            } else {
                //run on the UI thread in order to avoid problems
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new GetTimesTask(NextBusTrainActivity.this, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedRouteStr, selectedDirStr, selectedStopStr);
                    }
                });
            }
        }
    }

    //listener that runs when a route is selected (loads the appropriate directions)
    private Spinner.OnItemSelectedListener routeSelectedListener = new Spinner.OnItemSelectedListener() {
        int lastSelectedPos = -1;

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                if (id == -1) {
                    lastSelectedPos = -1;
                }
                String selectedRouteStr = ((Spinner) findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                if (!selectedRouteStr.equals("") && !selectedRouteStr.equals(getResources().getString(R.string.loadingellipsis))) {
                    System.out.println("Line selected!");
                    if (lastSelectedPos != pos || selectingFromFavorites) { //if the current route is being selected again, ignore it
                        System.out.println(" -> (" + selectedRouteStr + ")");
                        new GetDirectionsTask(NextBusTrainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedRouteStr);
                        new GetServiceAlertsTask(NextBusTrainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedRouteStr);
                        lastSelectedPos = pos;
                    } else {
                        System.out.println(" -> Ignored");
                    }
                }
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
                final int lineId = PersistentDataController.getLineIdMap(NextBusTrainActivity.this).get(lineName);
                final int dirId = dirIds.get(dirName);

                //check if there's already a station
                DatabaseHandler db = new DatabaseHandler(NextBusTrainActivity.this);
                if (db.hasFavoriteLocation(lineId, dirId, stationId)) {
                    alertDialog(getResources().getString(R.string.error), getResources().getString(R.string.alreadyfavorited), false);
                    return;
                }
                db.close();

                //use a dialog box to ask the user for the name of the station
                final AlertDialog.Builder inputAlert = new AlertDialog.Builder(NextBusTrainActivity.this);
                inputAlert.setTitle(R.string.add_favorite);
                inputAlert.setMessage(R.string.favorite_save_name_prompt);

                //create the text box and automatically populate it with the current station name
                final EditText userInput = new EditText(NextBusTrainActivity.this);
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
        private final Context myContext;
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
                DatabaseHandler db = new DatabaseHandler(myContext);
                Station st;
                if (pos == null) {
                    st = new Station(stationName, stationId, dirName, dirId, lineName, lineId, name);
                } else {
                    st = new Station(stationName, stationId, dirName, dirId, lineName, lineId, name, pos.latitude, pos.longitude);
                }
                db.addFavoriteLocation(st);
                db.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //listener that runs when the "select favorite" button is clicked
    private Button.OnClickListener serviceAlertsClickedListener = new AdapterView.OnClickListener() {
        public void onClick(View view) {
            Button myBtn = (Button) view;
            myBtn.getBackground().clearColorFilter();
            Intent intent = new Intent(NextBusTrainActivity.this, ServiceAlertsActivity.class);
            intent.putExtra("route", ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString());
            intent.putExtra("routeId", PersistentDataController.getLineIdMap(NextBusTrainActivity.this).get(((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString()));
            startActivity(intent);
        }
    };

    //listener that runs when the "full schedule" button is clicked
    private Button.OnClickListener scheduleClickedListener = new AdapterView.OnClickListener() {
        public void onClick(View view) {
            try {
                Intent intent = new Intent(NextBusTrainActivity.this, ScheduleActivity.class);
                final String stationName = ((Spinner) findViewById(R.id.stationSpinner)).getSelectedItem().toString();
                final String dirName = ((Spinner) findViewById(R.id.dirSpinner)).getSelectedItem().toString();
                final String lineName = ((Spinner) findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                final int stationId = stopIds.get(stationName);
                final int lineId = PersistentDataController.getLineIdMap(NextBusTrainActivity.this).get(lineName);
                final int dirId = dirIds.get(dirName);

                intent.putExtra("stationId", stationId);
                intent.putExtra("dirId", dirId);
                intent.putExtra("lineId", lineId);
                intent.putExtra("station", stationName);
                intent.putExtra("dir", dirName);
                intent.putExtra("line", lineName);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    //listener that runs when the "select favorite" button is clicked
    private Button.OnClickListener selectFavoriteClickedListener = new AdapterView.OnClickListener() {
        public void onClick(View view) {
            Intent intent = new Intent(NextBusTrainActivity.this, ManageLocationsActivity.class);
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

    private Button.OnClickListener addToHomeScreenButtonClickedListener = new AdapterView.OnClickListener() {
        public void onClick(View v) {
            final String stationName = ((Spinner) findViewById(R.id.stationSpinner)).getSelectedItem().toString();
            final String dirName = ((Spinner) findViewById(R.id.dirSpinner)).getSelectedItem().toString();
            final String lineName = ((Spinner) findViewById(R.id.lineSpinner)).getSelectedItem().toString();
            final int stationId = stopIds.get(stationName);
            final int lineId = PersistentDataController.getLineIdMap(NextBusTrainActivity.this).get(lineName);
            final int dirId = dirIds.get(dirName);

            //use a dialog box to ask the user for the name of the station
            final AlertDialog.Builder inputAlert = new AlertDialog.Builder(NextBusTrainActivity.this);
            inputAlert.setTitle(getResources().getString(R.string.add_home_screen_icon));
            inputAlert.setMessage(getResources().getString(R.string.shortcut_text_prompt));

            final LinearLayout optionLayout = new LinearLayout(NextBusTrainActivity.this);
            optionLayout.setOrientation(LinearLayout.VERTICAL);

            final RadioGroup iconButtons = new RadioGroup(NextBusTrainActivity.this);
            final RadioButton favPinButton = new RadioButton(NextBusTrainActivity.this);
            favPinButton.setText(getResources().getString(R.string.favorite_pin));
            iconButtons.addView(favPinButton);
            favPinButton.setChecked(true);
            final RadioButton railPinButton = new RadioButton(NextBusTrainActivity.this);
            railPinButton.setText(getResources().getString(R.string.rail_pin));
            iconButtons.addView(railPinButton);
            final RadioButton busPinButton = new RadioButton(NextBusTrainActivity.this);
            busPinButton.setText(getResources().getString(R.string.bus_pin));
            iconButtons.addView(busPinButton);
            optionLayout.addView(iconButtons);

            //create the text box and automatically populate it with the current station name
            final EditText userInput = new EditText(NextBusTrainActivity.this);
            userInput.setText(stationName);
            optionLayout.addView(userInput);
            inputAlert.setView(optionLayout);

            inputAlert.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        int radioButtonId = iconButtons.indexOfChild(iconButtons.findViewById(iconButtons.getCheckedRadioButtonId()));
                        //IDs: 0 - favorite, 1 - rail, 2 - bus
                        int iconId;
                        switch (radioButtonId) {
                            case 1:
                                iconId = R.mipmap.ic_rail_pin_icon;
                                break;
                            case 2:
                                iconId = R.mipmap.ic_bus_pin_icon;
                                break;
                            case 0:
                            default:
                                iconId = R.mipmap.ic_favorite_pin_icon;
                                break;
                        }

                        Intent shortcutIntent = new Intent(getApplicationContext(), MainMenu.class);
                        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        shortcutIntent.putExtra("stopId",stationId);
                        shortcutIntent.putExtra("lineId",lineId);
                        shortcutIntent.putExtra("dirId",dirId);
                        shortcutIntent.putExtra("nextactivity","nextbustrain");

                        Intent addIntent = new Intent();
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, userInput.getText().toString());
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), iconId));
                        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                        getApplicationContext().sendBroadcast(addIntent);
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
        }
    };

    //listener that runs when a direction is selected (loads the appropriate stops)
    private Spinner.OnItemSelectedListener dirSelectedListener = new AdapterView.OnItemSelectedListener() {
        String lastRoute = "";
        String lastDirection = "";
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                if (id == -1) {
                    lastRoute = "";
                    lastDirection = "";
                }
                String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                String selectedDirStr = ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem().toString();
                System.out.println("Direction selected");
                if (!selectedRouteStr.equals(getResources().getString(R.string.loadingellipsis)) && !selectedRouteStr.equals("") &&
                        !selectedDirStr.equals(getResources().getString(R.string.loadingellipsis)) && !selectedDirStr.equals("")) {
                    if (!selectedRouteStr.equals(lastRoute) || !selectedDirStr.equals(lastDirection) || selectingFromFavorites) { //ignore re-selections
                        new GetStopsTask(NextBusTrainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedRouteStr, selectedDirStr); //NextBusTrainActivity.this sometimes results in nullpointerexception
                        lastRoute = selectedRouteStr;
                        lastDirection = selectedDirStr;
                        System.out.println(" -> (" + selectedRouteStr + ", " + selectedDirStr + ")");
                        if (selectingFromFavorites) {
                            selectingFromFavorites = false; //this is the last step where duplicate entries need to be repeated when selecting from favorites, so mark that we are no longer doing so
                        }
                    } else {
                        System.out.println(" -> (Ignored)");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    //listener that runs when a direction is selected (loads the appropriate stops)
    private Spinner.OnItemSelectedListener stopSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selectedRouteStr = ((Spinner)findViewById(R.id.lineSpinner)).getSelectedItem().toString();
                String selectedDirStr = ((Spinner)findViewById(R.id.dirSpinner)).getSelectedItem().toString();
                String selectedStopStr = ((Spinner)findViewById(R.id.stationSpinner)).getSelectedItem().toString();

                System.out.println("Stop selected (" + selectedRouteStr + ", " + selectedDirStr + ", " + selectedStopStr + ")");

                if (!selectedRouteStr.equals("") && !selectedRouteStr.equals(getResources().getString(R.string.loadingellipsis)) &&
                        !selectedDirStr.equals("") && !selectedDirStr.equals(getResources().getString(R.string.loadingellipsis)) &&
                        !selectedStopStr.equals("") && !selectedStopStr.equals(getResources().getString(R.string.loadingellipsis))) {
                    new GetTimesTask(NextBusTrainActivity.this, true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedRouteStr, selectedDirStr, selectedStopStr);
                    new GetEscalatorElevatorStatus(NextBusTrainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, stopIds.get(selectedStopStr));
                }
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

        System.out.println("Starting activity");

        //give listeners to the appropriate actions
        Spinner lineSpinner = (Spinner) findViewById(R.id.lineSpinner); //this is for finding directions after a line is selected
        lineSpinner.setOnItemSelectedListener(routeSelectedListener);
        Spinner dirSpinner = (Spinner) findViewById(R.id.dirSpinner); //this is for finding stops after a direction and line are selected
        dirSpinner.setOnItemSelectedListener(dirSelectedListener);
        Spinner stationSpinner = (Spinner) findViewById(R.id.stationSpinner); //this is for finding stops after a direction and line are selected
        stationSpinner.setOnItemSelectedListener(stopSelectedListener);
        ImageButton addFavoriteBtn = (ImageButton) findViewById(R.id.addFavoriteBtn); //this is for when the "add favorite" button is clicked
        addFavoriteBtn.setOnClickListener(addFavoriteClickedListener);
        ImageButton selectFavoriteBtn = (ImageButton) findViewById(R.id.selectFavoriteBtn); //this is for when the "add favorite" button is clicked
        selectFavoriteBtn.setOnClickListener(selectFavoriteClickedListener);
        Button serviceAlertsBtn = (Button)findViewById(R.id.serviceAlertsBtn);
        serviceAlertsBtn.setOnClickListener(serviceAlertsClickedListener);
        Button scheduleBtn = (Button)findViewById(R.id.scheduleBtn);
        scheduleBtn.setOnClickListener(scheduleClickedListener);
        ImageButton viewOnMapBtn = (ImageButton)findViewById(R.id.viewOnMapBtn);
        viewOnMapBtn.setOnClickListener(viewOnMapClickedListener);
        ImageButton addToHomeScreenButton = (ImageButton)findViewById(R.id.addToHomeScreenButton);
        addToHomeScreenButton.setOnClickListener(addToHomeScreenButtonClickedListener);

        if (!NetworkController.connected(this)) {
            alertDialog(getResources().getString(R.string.network), getResources().getString(R.string.nonetworkmsg), true);
            return;
        }

        if (isTaskRoot()) {
            //alertDialog(getResources().getString(R.string.error), getResources().getString(R.string.nohomescreenshortcuts), true);
            Intent intent = new Intent(NextBusTrainActivity.this, MainMenu.class);
            intent.putExtra("nextactivity", "nextbustrain");
            if (getIntent().hasExtra("lineId")) {
                intent.putExtra("lineId", getIntent().getExtras().getInt("lineId"));
            }
            if (getIntent().hasExtra("dirId")) {
                intent.putExtra("dirId", getIntent().getExtras().getInt("dirId"));
            }
            if (getIntent().hasExtra("stopId")) {
                intent.putExtra("stopId", getIntent().getExtras().getInt("stopId"));
            }
            startActivity(intent);
            finish();
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
            new GetLinesTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            if (savedInstanceState != null) {
                preSelectedLineId = savedInstanceState.getInt("lineId");
                preSelectedDirId = savedInstanceState.getInt("dirId");
                preSelectedStopId = savedInstanceState.getInt("stationId");
            }

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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current state
        try {
            System.out.println("Saving activity state");

            final String stationName = ((Spinner) findViewById(R.id.stationSpinner)).getSelectedItem().toString();
            final String dirName = ((Spinner) findViewById(R.id.dirSpinner)).getSelectedItem().toString();
            final String lineName = ((Spinner) findViewById(R.id.lineSpinner)).getSelectedItem().toString();
            final int stationId = stopIds.containsKey(stationName) ? stopIds.get(stationName) : -1;
            final int lineId = PersistentDataController.getLineIdMap(NextBusTrainActivity.this).containsKey(lineName) ? PersistentDataController.getLineIdMap(NextBusTrainActivity.this).get(lineName) : -1;
            final int dirId = dirIds.containsKey(dirName) ? dirIds.get(dirName) : -1;

            savedInstanceState.putInt("stationId", stationId);
            savedInstanceState.putInt("dirId", dirId);
            savedInstanceState.putInt("lineId", lineId);
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private class GetLinesTask extends AsyncTask<Void, Void, String[]> {
        private final Context myContext;

        public GetLinesTask(Context context) {
            myContext = context;

            Spinner lineSpinner = (Spinner) findViewById(R.id.lineSpinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(myContext, android.R.layout.simple_spinner_item, new String[]{getResources().getString(R.string.loadingellipsis)});
            lineSpinner.setAdapter(adapter);
            lineSpinner.setEnabled(false);
        }

        protected String[] doInBackground(Void... params) {
            return PersistentDataController.getLines(myContext);
        }

        protected void onPostExecute(String[] lineNames) {
            Spinner lineSpinner = (Spinner) findViewById(R.id.lineSpinner);
            if (lineNames.length == 0) {
                alertDialog(getResources().getString(R.string.error), getResources().getString(R.string.nextconnectdown), true);
                return;
            }
            //see if there is a pre-selected line sent in from elsewhere
            int preSelectIndex = 0;
            boolean foundLine = false;
            if (preSelectedLineId != -1) {
                for (int i = 0; i < lineNames.length; i++) {
                    if (preSelectedLineId == PersistentDataController.getLineIdMap(myContext).get(lineNames[i])) {
                        preSelectIndex = i;
                        foundLine = true;
                        break;
                    }
                }
            }
            if (preSelectedLineId != -1 && !foundLine) {
                alertDialog(getResources().getString(R.string.error), getResources().getString(R.string.line_not_found), true);
            }
            //put everything into the spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(myContext, android.R.layout.simple_spinner_item);
            lineSpinner.setAdapter(adapter);
            adapter.addAll(lineNames);
            if (preSelectIndex > 0) {
                lineSpinner.setSelection(preSelectIndex);
            }

            lineSpinner.setEnabled(true);
        }
    }

    private boolean selectingFromFavorites = false;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            preSelectedStopId = data.getExtras().getInt("stopId");
            preSelectedDirId = data.getExtras().getInt("dirId");
            preSelectedLineId = data.getExtras().getInt("lineId");
            selectingFromFavorites = true; //mark that we are selecting a favorite so it doesn't ignore duplicate lines or whatever
            new GetLinesTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private class GetDirectionsTask extends AsyncTask<String, Void, Map<String, Integer>> {
        private final Context myContext;
        private Map<String, Integer> dirs;
        private int route;
        public GetDirectionsTask(Context context) {
            myContext = context;
            Spinner dirSpinner = (Spinner) findViewById(R.id.dirSpinner);
            dirSpinner.setEnabled(false);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(myContext, android.R.layout.simple_spinner_item, new String[]{getResources().getString(R.string.loadingellipsis)});
            dirSpinner.setAdapter(adapter);
        }
        protected Map<String, Integer> doInBackground(String... params) {
            try {
                route = PersistentDataController.getLineIdMap(myContext).get(params[0]);
                dirs = PersistentDataController.getDirIds(myContext, route);
                return dirIds = dirs;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            /*if (dirs.isEmpty()) {
                return NetworkController.performPostCall("http://www.nextconnect.riderta.com/Arrivals.aspx/getDirections", "{routeID: " + route + "}");
            } else {
                return "";
            }*/
        }

        protected void onPostExecute(Map<String, Integer> result) {
            //parse the result as JSON
            try {
                System.out.println("Got directions");
                Spinner dirSpinner = (Spinner) findViewById(R.id.dirSpinner);
                Spinner stationSpinner = (Spinner) findViewById(R.id.stationSpinner);
                if (result == null) { //connection failed
                    if (!showingErrorDialog) {
                        //show dialog asking to reload directions if one isn't already showing
                        showingErrorDialog = true;
                        AlertDialog alertDialog = new AlertDialog.Builder(NextBusTrainActivity.this).create();
                        alertDialog.setTitle(getResources().getString(R.string.error));
                        alertDialog.setMessage(getResources().getString(R.string.failed_to_load_directions_try_again));
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        routeSelectedListener.onItemSelected(null, null, 0, -1L);
                                    }
                                });
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                });
                        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                showingErrorDialog = false;
                            }
                        });
                        alertDialog.show();
                    }

                    //leave the dropdown blank
                    dirSpinner.setAdapter(new ArrayAdapter<>(myContext, android.R.layout.simple_spinner_item, new String[]{}));
                    dirSpinner.setEnabled(true);

                    stationSpinner.setAdapter(new ArrayAdapter<>(myContext, android.R.layout.simple_spinner_item, new String[]{}));
                    stationSpinner.setEnabled(true);
                    return;
                } else if (result.isEmpty()) {
                    alertDialog(getResources().getString(R.string.error), getResources().getString(R.string.nextconnectdown), true);
                }
                Set<String> dirSet = result.keySet();
                directions = new String[dirSet.size()];
                dirSet.toArray(directions);
                //put that into the spinner

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
                dirSpinner.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class GetStopsTask extends AsyncTask<String, Void, Map<String, Integer>> {
        private final Context myContext;
        private int myRouteId, myDirId;
        private String curSelection = "";
        public GetStopsTask(Context context) {
            myContext = context;
            Spinner stationSpinner = (Spinner) findViewById(R.id.stationSpinner);

            //get the current selection in case the direction is changing
            if (stationSpinner.getSelectedItem() != null) {
                curSelection = stationSpinner.getSelectedItem().toString();
            }

            //replace the content of the spinner with a loading message
            ArrayAdapter<String> adapter = new ArrayAdapter<>(myContext, android.R.layout.simple_spinner_item, new String[]{getResources().getString(R.string.loadingellipsis)});
            stationSpinner.setAdapter(adapter);
            stationSpinner.setEnabled(false);
        }
        protected Map<String, Integer> doInBackground(String... params) {
            try {
                myRouteId = PersistentDataController.getLineIdMap(myContext).get(params[0]);
                myDirId = dirIds.get(params[1]);
                return PersistentDataController.getStationIds(myContext, myRouteId, myDirId);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Map<String, Integer> result) {
            //parse the result as JSON
            try {
                System.out.println("Got stations");
                Spinner stationSpinner = (Spinner) findViewById(R.id.stationSpinner);
                if (result == null) {
                    if (!showingErrorDialog) {
                        //show dialog asking to reload if the connection failed and a dialog isn't already showing
                        showingErrorDialog = true;
                        AlertDialog alertDialog = new AlertDialog.Builder(NextBusTrainActivity.this).create();
                        alertDialog.setTitle(getResources().getString(R.string.error));
                        alertDialog.setMessage(getResources().getString(R.string.failed_to_load_stops_try_again));
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        dirSelectedListener.onItemSelected(null, null, 0, -1L);
                                    }
                                });
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                });
                        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                showingErrorDialog = false;
                            }
                        });
                        alertDialog.show();
                    }

                    //leave the dropdown empty
                    stationSpinner.setAdapter(new ArrayAdapter<>(myContext, android.R.layout.simple_spinner_item, new String[]{""}));
                    stationSpinner.setEnabled(true);
                    return;
                }

                stopIds = result;
                Set<String> stopSet = result.keySet();
                stops = new String[stopSet.size()];
                stopSet.toArray(stops);

                //put the result into the spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(myContext, android.R.layout.simple_spinner_item, stops);
                stationSpinner.setAdapter(adapter);

                for (int i = 0; i < adapter.getCount(); i++) { //if there was a stop previously selected then select it again (this is to avoid resetting the spinner when changing directions), but if there is a stop sent in to the activity that overrides the previously selected stop
                    if (preSelectedStopId == stopIds.get(adapter.getItem(i)) || (preSelectedStopId == -1 && curSelection.equals(adapter.getItem(i)))) {
                        stationSpinner.setSelection(i);
                        preSelectedStopId = -1;
                        break;
                    }
                }
                stationSpinner.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //task to get the times of the bus/train
    private class GetTimesTask extends AsyncTask<String, Void, List<String[]>> {
        private final Context myContext;
        private int stopId;
        public GetTimesTask(Context context, boolean showLoading) {
            myContext = context;
            try {
                if (showLoading) {
                    blankAll();
                    ((TextView) findViewById(R.id.train1destbox)).setText(getResources().getString(R.string.loadingellipsis));
                }
                findViewById(R.id.scheduleBtn).setVisibility(View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected List<String[]> doInBackground(String... params) {
            try {
                if (params[0] == null || params[1] == null || params[2] == null) {
                    blankAll();
                }

                int routeId = PersistentDataController.getLineIdMap(myContext).get(params[0]);
                if (dirIds == null || stopIds == null) {
                    return null;
                }
                int dirId = dirIds.get(params[1]);
                stopId = stopIds.get(params[2]);

                //returns an array of {stop JSON, alerts XML}
                return NetworkController.getStopTimes(myContext, routeId, dirId, stopId);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(List<String[]> stopList) {
            //parse the result as JSON
            try {
                if (stopList == null) {
                    blankAll();
                    if (!showingErrorDialog) {
                        //show dialog asking to reload arrivals if one isn't already showing
                        showingErrorDialog = true;
                        AlertDialog alertDialog = new AlertDialog.Builder(NextBusTrainActivity.this).create();
                        alertDialog.setTitle(getResources().getString(R.string.error));
                        alertDialog.setMessage(getResources().getString(R.string.failed_to_load_arrivals_try_again));
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        stopSelectedListener.onItemSelected(null, null, 0, -1L);
                                    }
                                });
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                });
                        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                showingErrorDialog = false;
                            }
                        });
                        alertDialog.show();
                    }
                    return;
                }
                //populate the text fields with the stop data
                if (stopList.size() > 0) {
                    ((TextView) findViewById(R.id.train1timebox)).setText(stopList.get(0)[0]);
                    ((TextView) findViewById(R.id.train1destbox)).setText(stopList.get(0)[1]);
                    ((TextView) findViewById(R.id.train1timeLeftBox)).setText(stopList.get(0)[2] + "\n" + stopList.get(0)[3]);
                } else {
                    ((TextView) findViewById(R.id.train1timebox)).setText(R.string.no_time);
                    ((TextView) findViewById(R.id.train1destbox)).setText(R.string.no_destination);
                    ((TextView) findViewById(R.id.train1timeLeftBox)).setText(R.string.not_available);
                }
                if (stopList.size() > 1) {
                    ((TextView) findViewById(R.id.train2timebox)).setText(stopList.get(1)[0]);
                    ((TextView) findViewById(R.id.train2destbox)).setText(stopList.get(1)[1]);
                    ((TextView) findViewById(R.id.train2timeLeftBox)).setText(stopList.get(1)[2] + "\n" + stopList.get(1)[3]);
                } else {
                    ((TextView) findViewById(R.id.train2timebox)).setText(R.string.no_time);
                    ((TextView) findViewById(R.id.train2destbox)).setText(R.string.no_destination);
                    ((TextView) findViewById(R.id.train2timeLeftBox)).setText(R.string.not_available);
                }
                if (stopList.size() > 2) {
                    ((TextView) findViewById(R.id.train3timebox)).setText(stopList.get(2)[0]);
                    ((TextView) findViewById(R.id.train3destbox)).setText(stopList.get(2)[1]);
                    ((TextView) findViewById(R.id.train3timeLeftBox)).setText(stopList.get(2)[2] + "\n" + stopList.get(2)[3]);
                } else {
                    ((TextView) findViewById(R.id.train3timebox)).setText(R.string.no_time);
                    ((TextView) findViewById(R.id.train3destbox)).setText(R.string.no_destination);
                    ((TextView) findViewById(R.id.train3timeLeftBox)).setText(R.string.not_available);
                }
                //show the schedule button
                //findViewById(R.id.scheduleBtn).setVisibility(View.VISIBLE); //TODO: uncomment this once the server backend issues have been resolved to re-enable showing schedules
            } catch (Exception e) {
                blankAll();
                System.err.println("Error in parsing JSON or XML");
                e.printStackTrace();
            }
        }
    }

    //task to get the times of the bus/train
    private class GetServiceAlertsTask extends AsyncTask<String, Void, int[]> {
        private final Context myContext;
        private String route;
        public GetServiceAlertsTask(Context context) {
            myContext = context;
        }
        protected int[] doInBackground(String... params) {
            int routeId = PersistentDataController.getLineIdMap(myContext).get(params[0]);
            route = params[0]; //save the route for later in case we want to color the results
            int unread = 0; //0 if all alerts are unread, 1 if there are any new ones
            int count = -1;
            List<Map<String, String>> alerts = ServiceAlertsController.getAlertsByLine(myContext, new String[]{route}, new int[]{routeId});
            if (alerts != null) {
                count = alerts.size();
                for (Map<String, String> alert : alerts) {
                    if (alert.containsKey("new") && alert.get("new").equals("true")) {
                        unread = 1;
                        break;
                    }
                }
            }
            return new int[]{count, unread};
        }

        protected void onPostExecute(int[] params) {
            int alertCount = params[0];
            Button serviceAlertsBtn = (Button) findViewById(R.id.serviceAlertsBtn);
            if (alertCount > 0) {
                serviceAlertsBtn.setVisibility(View.VISIBLE);
                serviceAlertsBtn.setText(String.format(getResources().getString(R.string.there_are_n_service_alerts), alertCount));
                if (params[1] == 1) {
                    serviceAlertsBtn.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                } else {
                    serviceAlertsBtn.getBackground().clearColorFilter();
                }
            } else {
                serviceAlertsBtn.setVisibility(View.GONE);
            }
        }
    }

    private class GetEscalatorElevatorStatus extends AsyncTask<Integer, Void, List<EscalatorElevatorAlert>> {
        private final Context myContext;
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

    public void markTime(View v) {
        System.out.println(" >>> TIME MARKED <<< ");
    }

}

