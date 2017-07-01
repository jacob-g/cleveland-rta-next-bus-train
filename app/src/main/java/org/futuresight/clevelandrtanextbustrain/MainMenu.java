package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.List;
import java.util.Map;

public class MainMenu extends AppCompatActivity {
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

        //new GetServiceAlertsTask(this).execute(); //TODO: reimplement this once the relevant bugs are fixed
    }

    //task to get the times of the bus/train
    private class GetServiceAlertsTask extends AsyncTask<Void, Void, int[]> {
        private Context myContext;
        private String[] routes;
        private int[] routeIds;
        public GetServiceAlertsTask(Context context) {
            myContext = context;
        }
        protected int[] doInBackground(Void... params) {
            PersistentDataController.getLineIdMap(myContext); //pre-load the line ID map just in case
            String[][] faves = PersistentDataController.getFavoriteLines(myContext);
            if (faves.length == 0) {
                return new int[]{0, 0};
            }
            routes = new String[faves[0].length];
            routeIds = new int[faves[0].length];
            int i = 0;
            for (String[] s : faves) {
                routes[i] = s[0]; //save the route for later in case we want to color the results
                routeIds[i] = Integer.parseInt(s[1]);
                i++;
            }
            List<Map<String, String>> alerts = ServiceAlertsController.getAlertsByLine(myContext, routes, routeIds);
            int unread = 0;
            for (Map<String, String> alert : alerts) {
                if (alert.containsKey("new") && alert.get("new").equals("true")) {
                    unread = 1;
                    break;
                }
            }
            return new int[]{alerts.size(), unread};
        }

        protected void onPostExecute(int[] params) {
            Button serviceAlertsBtn = (Button) findViewById(R.id.serviceAlertsBtn);
            if (serviceAlertsBtn != null) {
                if (params[1] == 1) {
                    serviceAlertsBtn.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                } else {
                    serviceAlertsBtn.getBackground().clearColorFilter();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //new GetServiceAlertsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); //TODO: put this back once the bug is fixed
        //it appears that the bug is that the database is being locked while this is running and if any other pages are opened while this is running, this will result in a database crash
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_menu, menu);
        return true;
    }

   public void openNextBusTrainPage(View v) {
       Intent intent = new Intent(this, NextBusTrainActivity.class);
       startActivityForResult(intent, 1);
   }

    public void openManageLocationsPage(View v) {
        Intent intent = new Intent(this, ManageLocationsActivity.class);
        startActivityForResult(intent, 1);
    }

    public void openServiceAlertsPage(View v) {
        Intent intent = new Intent(this, ServiceAlertsActivity.class);
        v.getBackground().clearColorFilter();
        startActivity(intent);
    }

    public void openMap(View v) {
        Intent intent = new Intent(this, MapTypeListActivity.class);
        startActivityForResult(intent, 1);
    }

    public void openNearMe(View v) {
        Intent intent = new Intent(this, NearMeActivity.class);
        startActivityForResult(intent, 1);
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
