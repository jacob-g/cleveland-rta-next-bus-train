package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServiceAlertsActivity extends AppCompatActivity {
    int selectedRouteId;
    private Spinner.OnItemSelectedListener lineSelectedListener = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selectedRouteStr = ((Spinner)findViewById(R.id.serviceAlertsLineSpinner)).getSelectedItem().toString();
                if (!selectedRouteStr.equals(getResources().getString(R.string.loadingellipsis)) && !selectedRouteStr.equals("")) {
                    if (selectedRouteStr == "Favorites") {
                        new GetServiceAlertsTask(view.getContext()).execute(PersistentDataController.getFavoriteLines(ServiceAlertsActivity.this));
                    } else {
                        new GetServiceAlertsTask(view.getContext()).execute(new String[][]{{selectedRouteStr, Integer.toString(PersistentDataController.getLineIdMap(view.getContext()).get(selectedRouteStr))}});
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
        AlertDialog alertDialog = new AlertDialog.Builder(ServiceAlertsActivity.this).create();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_alerts);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!NetworkController.connected(this)) {
            alertDialog(getResources().getString(R.string.network), getResources().getString(R.string.nonetworkmsg), true);
            return;
        }

        new GetLinesTask(this).execute();
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    //task to get the service alerts
    private class GetServiceAlertsTask extends AsyncTask<String[][], Void, List<Map<String, String>>> {
        private Context myContext;
        private String[] routes;
        private int[] routeIds;
        public GetServiceAlertsTask(Context context) {
            myContext = context;

            LinearLayout serviceAlertsLayout = (LinearLayout) findViewById(R.id.serviceAlertVerticalLayout);
            serviceAlertsLayout.removeAllViews();
            TextView noAlertsNotice = new TextView(myContext);
            noAlertsNotice.setText(getResources().getString(R.string.loadingellipsis));
            serviceAlertsLayout.addView(noAlertsNotice);
        }
        protected List<Map<String, String>> doInBackground(String[][]... params) {
            routes = new String[params[0].length];
            routeIds = new int[params[0].length];
            int i = 0;
            for (String[] s : params[0]) {
                routes[i] = s[0]; //save the route for later in case we want to color the results
                routeIds[i] = Integer.parseInt(s[1]);
                i++;
            }
            List<Map<String, String>> out = ServiceAlertsController.getAlertsByLine(myContext, routes, routeIds);
            if (!out.isEmpty()) {
                List<Integer> ids = new ArrayList<>();
                for (Map<String, String> alert : out) {
                    ids.add(Integer.parseInt(alert.get("id")));
                }
                ServiceAlertsController.markAsRead(myContext, ids);
            }
            return out;
        }

        protected void onPostExecute(List<Map<String, String>> alertList) {
            //put the data into the layout
            LinearLayout serviceAlertsLayout = (LinearLayout) findViewById(R.id.serviceAlertVerticalLayout);
            serviceAlertsLayout.removeAllViews();
            if (alertList.isEmpty()) {
                //no alerts, so just show a brief alert
                TextView noAlertsNotice = new TextView(myContext);
                noAlertsNotice.setText("There are no alerts for the selected line at this time.");
                serviceAlertsLayout.addView(noAlertsNotice);
            } else {
                //there are alerts, so display them
                for (Map<String, String> alertInfo : alertList) {
                    TextView titleView = new TextView(myContext);
                    titleView.setText(alertInfo.get("title"));
                    titleView.setTypeface(null, Typeface.BOLD);
                    boolean unread = alertInfo.containsKey("new") && alertInfo.get("new").equals("true");
                    titleView.setTextColor(unread ? Color.RED : Color.BLUE);
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
                    contentView.setMaxLines(10);
                    contentView.setText(alertInfo.get("info").replace("!br!", System.getProperty("line.separator")));
                    serviceAlertsLayout.addView(contentView);
                }
            }
        }
    }

    private class GetLinesTask extends AsyncTask<Void, Void, String[]> {
        private Context myContext;
        public GetLinesTask(Context context) {
            myContext = context;
            Spinner lineSpinner = (Spinner) findViewById(R.id.serviceAlertsLineSpinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(myContext, android.R.layout.simple_spinner_item, new String[]{getResources().getString(R.string.loadingellipsis)});
            lineSpinner.setAdapter(adapter);
            lineSpinner.setEnabled(false);
        }
        protected String[] doInBackground(Void... params) {
            return PersistentDataController.getLines(myContext);
        }

        protected void onPostExecute(String[] lineNames) {
            //parse the result as JSON
            //if a line is passed in, load it; otherwise load the favorites
            String[] selectedRoutes = new String[1];
            if (getIntent().hasExtra("route") && getIntent().hasExtra("routeId")) {
                //if a route is passed, open it
                selectedRoutes = new String[]{getIntent().getExtras().getString("route")};
                selectedRouteId = getIntent().getExtras().getInt("routeId");
            } else {
                //otherwise open the favorite routes
                DatabaseHandler db = new DatabaseHandler(ServiceAlertsActivity.this);
                List<Station> favoriteStations = db.getFavoriteLocations();
                class Line {
                    private String name;
                    private int id;
                    public Line(String name, int id) {
                        this.name = name;
                        this.id = id;
                    }
                    public String getName() {
                        return name;
                    }
                    public int getId() {
                        return id;
                    }
                    public boolean equals(Line other) {
                        return this.id == other.id;
                    }
                }
                Set<Line> lines = new HashSet<>();
                for (Station st : favoriteStations) {
                    lines.add(new Line(st.getLineName(), st.getLineId()));
                }
                db.close();
                selectedRoutes = new String[lines.size()];
                int i = 0;
                for (Line s : lines) {
                    selectedRoutes[i] = s.getName();
                    i++;
                }
            }
            //put that into the spinner
            Spinner lineSpinner = (Spinner) findViewById(R.id.serviceAlertsLineSpinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(myContext, android.R.layout.simple_spinner_item);
            adapter.add("Favorites");
            adapter.addAll(lineNames);
            lineSpinner.setAdapter(adapter);
            if (selectedRouteId != -1) {
                for (int i = 1; i < adapter.getCount(); i++) {
                    if (selectedRouteId == PersistentDataController.getLineIdMap(myContext).get(adapter.getItem(i))) {
                        lineSpinner.setSelection(i);
                        break;
                    }
                }
            }
            lineSpinner.setEnabled(true);

            try {
                String[][] routeList = new String[selectedRoutes.length][2];
                int i = 0;
                for (String s : selectedRoutes) {
                    routeList[i] = new String[]{s, Integer.toString(PersistentDataController.getLineIdMap(myContext).get(s))};
                    i++;
                }
                ((Spinner)findViewById(R.id.serviceAlertsLineSpinner)).setOnItemSelectedListener(lineSelectedListener);
                //new GetServiceAlertsTask(myContext, createDialog()).execute(routeList);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
