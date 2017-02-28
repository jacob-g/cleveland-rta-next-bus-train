package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MainMenu extends AppCompatActivity {
    private class GetStopsTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog pDlg;

        public GetStopsTask() {
            //pDlg = createDialog(getResources().getString(R.string.loading_stops), getResources().getString(R.string.take_a_while));
        }

        //pre-fetching of data
        protected Void doInBackground(Void... params) {
            PersistentDataController.loadLines(MainMenu.this); //pre-fetch line list

            //pre-fetch the stops for the user's favorite locations
            DatabaseHandler db = new DatabaseHandler(MainMenu.this);
            List<Station> favoriteStations = db.getFavoriteLocations();
            db.close();
            Set<Integer> linesToFetch = new TreeSet<>();
            for (Station st : favoriteStations) {
                linesToFetch.add(st.getLineId());
            }
            if (PersistentDataController.getLines(MainMenu.this).length == 0) {
                return null;
            }
            linesToFetch.add(PersistentDataController.getLineIdMap(MainMenu.this).get(PersistentDataController.getLines(MainMenu.this)[0])); //also add the first line in the list as the next bus/train will attempt to load that upon being opened
            for (int line : linesToFetch) {
                Map<String, Integer> dirIdsForFavorite = PersistentDataController.getDirIds(MainMenu.this, line);
                if (dirIdsForFavorite.isEmpty()) {
                    return null;
                }
                for (String key : dirIdsForFavorite.keySet()) {
                    int dirId = dirIdsForFavorite.get(key);
                    PersistentDataController.loadStationIds(MainMenu.this, line, dirId);
                }
            }

            return null;
        }

        protected void onPostExecute(Void v) {

        }
    }

    private class GetMarkersTask extends AsyncTask<Void, Void, Void> { //TODO: make this run completely independently of everything else to avoid interference
        private ProgressDialog pDlg;

        public GetMarkersTask() {
            //pDlg = createDialog(getResources().getString(R.string.loading_stops), getResources().getString(R.string.take_a_while));
        }

        protected Void doInBackground(Void... params) {
            PersistentDataController.getMapMarkers(MainMenu.this, null); //pre-fetch the map markers

            return null;
        }

        protected void onPostExecute(Void v) {

        }
    }

    //CREDITS: base GPS pin and bus icon by Freepik from flaticon.com, train icon by Scott de Jonge on flaticon.com, star icon by Madebyoliver on flaticon.com
    private void alertDialog(String title, String msg, final boolean die) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (die) {
                            System.exit(0);
                        }
                    }
                });
        alertDialog.show();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(getResources().getString(R.string.main_menu_label));
        setContentView(R.layout.activity_main_menu);
        if (!NetworkController.connected(this)) {
            alertDialog(getResources().getString(R.string.network), getResources().getString(R.string.nonetworkmsg), false);
        }
        new GetStopsTask().execute();
        new GetMarkersTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_menu, menu);
        return true;
    }

   public void openNextBusTrainPage(View v) {
       Intent intent = new Intent(this, NextBusTrainActivity.class);
       startActivity(intent);
   }

    public void openManageLocationsPage(View v) {
        Intent intent = new Intent(this, ManageLocationsActivity.class);
        startActivity(intent);
    }

    public void openServiceAlertsPage(View v) {
        Intent intent = new Intent(this, ServiceAlertsActivity.class);
        startActivity(intent);
    }

    public void openMap(View v) {
        Intent intent = new Intent(this, MapTypeListActivity.class);
        startActivity(intent);
    }

    public void openNearMe(View v) {
        Intent intent = new Intent(this, NearMeActivity.class);
        startActivity(intent);
    }

    public void openSettings(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void openCredits(View v) {
        Intent intent = new Intent(this, CreditsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}
