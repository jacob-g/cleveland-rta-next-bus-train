package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.*;
import android.content.*;

public class MainMenu extends AppCompatActivity {
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
        getSupportActionBar().setTitle(R.string.main_menu_label);
        setContentView(R.layout.activity_main_menu);
        if (!NetworkController.connected(this)) {
            alertDialog(getResources().getString(R.string.network), getResources().getString(R.string.nonetworkmsg), true);
        }

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

    public void fryDb(View v) {
        DatabaseHandler db = new DatabaseHandler(this);
        db.fry();
        db.close();
    }

    public void clearCache(View v) {
        DatabaseHandler db = new DatabaseHandler(this);
        db.fryCache();
        db.close();
        System.out.println("Cache cleared!");
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
