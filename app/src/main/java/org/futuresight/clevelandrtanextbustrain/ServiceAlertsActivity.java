package org.futuresight.clevelandrtanextbustrain;

import android.app.ProgressDialog;
import android.content.Context;
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
                if (selectedRouteStr == "Favorites") {
                    DatabaseHandler db = new DatabaseHandler(ServiceAlertsActivity.this);
                    List<Station> favoriteStations = db.getFavoriteLocations();
                    Set<String> lines = new HashSet<>();
                    for (Station st : favoriteStations) {
                        lines.add(st.getLineName());
                    }
                    db.close();
                    String[][] arr = new String[lines.size()][2];
                    int i = 0;
                    for (String s : lines) {
                        arr[i][0] = s;
                        arr[i][1] = Integer.toString(PersistentDataController.getLineIdMap(view.getContext()).get(s));
                        i++;
                    }
                    new GetServiceAlertsTask(view.getContext(), createDialog()).execute(arr);
                } else {
                    new GetServiceAlertsTask(view.getContext(), createDialog()).execute(new String[][]{{selectedRouteStr, Integer.toString(PersistentDataController.getLineIdMap(view.getContext()).get(selectedRouteStr))}});
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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        new GetLinesTask(this, createDialog()).execute();
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    //task to get the times of the bus/train
    private class GetServiceAlertsTask extends AsyncTask<String[][], Void, List<Map<String, String>>> {
        private Context myContext;
        private String[] routes;
        private int[] routeIds;
        private ProgressDialog myProgressDialog;
        public GetServiceAlertsTask(Context context, ProgressDialog pdlg) {
            myContext = context;
            myProgressDialog = pdlg;
        }
        protected List<Map<String, String>> doInBackground(String[][]... params) {
            String returnData = "";
            routes = new String[params[0].length];
            routeIds = new int[params[0].length];
            int i = 0;
            for (String[] s : params[0]) {
                routes[i] = s[0]; //save the route for later in case we want to color the results
                routeIds[i] = Integer.parseInt(s[1]);
                i++;
            }
            return ServiceAlertsController.getAlertsByLine(myContext, routes, routeIds);
        }

        protected void onPostExecute(List<Map<String, String>> alertList) {
            //put the data into the layout
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
                contentView.setMaxLines(10);
                contentView.setText(alertInfo.get("info").replace("!br!", System.getProperty("line.separator")));
                serviceAlertsLayout.addView(contentView);
            }
            myProgressDialog.dismiss();
        }
    }

    private class GetLinesTask extends AsyncTask<Void, Void, String[]> {
        private Context myContext;
        private ProgressDialog myProgressDialog;
        public GetLinesTask(Context context, ProgressDialog pdlg) {
            myContext = context;
            myProgressDialog = pdlg;
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
            myProgressDialog.dismiss();

            try {
                String[][] routeList = new String[selectedRoutes.length][2];
                int i = 0;
                for (String s : selectedRoutes) {
                    routeList[i] = new String[]{s, Integer.toString(PersistentDataController.getLineIdMap(myContext).get(s))}; //TODO: make this not crash when the route list isn't ready
                    i++;
                }
                ((Spinner)findViewById(R.id.serviceAlertsLineSpinner)).setOnItemSelectedListener(lineSelectedListener);
                new GetServiceAlertsTask(myContext, createDialog()).execute(routeList);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
