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

public class MainMenu extends AppCompatActivity {
    private class GetStopsTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog pDlg;

        public GetStopsTask() {
            //pDlg = createDialog(getResources().getString(R.string.loading_stops), getResources().getString(R.string.take_a_while));
        }

        protected Void doInBackground(Void... params) {
            PersistentDataController.getLines(MainMenu.this);
            PersistentDataController.getMapMarkers(MainMenu.this);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
