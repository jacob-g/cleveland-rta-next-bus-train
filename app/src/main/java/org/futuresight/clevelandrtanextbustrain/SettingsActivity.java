package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {
    class CacheWatcher implements TextWatcher {
        private String config;
        public CacheWatcher(String cfgValue) {
            config = cfgValue;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (Pattern.matches("^[0-9]+", s.toString())) {
                int newDuration = Integer.parseInt(s.toString()) * 3600;
                PersistentDataController.setConfig(SettingsActivity.this, config, Integer.toString(newDuration));
                PersistentDataController.setCacheDuration(config, newDuration);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            //load settings
            //sort favorites by proximity/alpha
            Spinner sortFavoritesSpinner = (Spinner) findViewById(R.id.sortFavoritesSpinner);
            sortFavoritesSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.sort_favorites_options)));
            if (PersistentDataController.getConfig(this, "orderByClosenessBoxChecked").equals("true")) {
                sortFavoritesSpinner.setSelection(1);
            } else {
                sortFavoritesSpinner.setSelection(0);
            }
            sortFavoritesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        PersistentDataController.setConfig(SettingsActivity.this, "orderByClosenessBoxChecked", position == 1 ? "true" : "false");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });


            //line cache duration box
            EditText lineCacheBox = (EditText) findViewById(R.id.lineCacheBox);
            lineCacheBox.setText(Integer.toString(PersistentDataController.getLineExpiry(this) / 3600));
            lineCacheBox.addTextChangedListener(new CacheWatcher("lineExpiry"));

            //station cache duration box
            EditText stationCacheBox = (EditText) findViewById(R.id.stationCacheBox);
            stationCacheBox.setText(Integer.toString(PersistentDataController.getStationExpiry(this) / 3600));
            stationCacheBox.addTextChangedListener(new CacheWatcher("stationExpiry"));

            //service alert cache duration box
            EditText serviceAlertCacheBox = (EditText) findViewById(R.id.serviceAlertsCacheBox);
            serviceAlertCacheBox.setText(Integer.toString(PersistentDataController.getAlertExpiry(this) / 3600));
            serviceAlertCacheBox.addTextChangedListener(new CacheWatcher("alertExpiry"));

            //escel cache duration box
            EditText escelCacheBox = (EditText) findViewById(R.id.escelCacheBox);
            escelCacheBox.setText(Integer.toString(PersistentDataController.getEscElExpiry(this) / 3600));
            escelCacheBox.addTextChangedListener(new CacheWatcher("escElExpiry"));

            //favorite locations cache duration box
            EditText stationLocationCacheBox = (EditText) findViewById(R.id.stationLocationCacheBox);
            stationLocationCacheBox.setText(Integer.toString(PersistentDataController.getFavLocationExpiry(this) / 3600));
            stationLocationCacheBox.addTextChangedListener(new CacheWatcher("favLocationExpiry"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    public void fryDb(View v) {
        final AlertDialog.Builder inputAlert = new AlertDialog.Builder(SettingsActivity.this);
        inputAlert.setTitle(getResources().getText(R.string.deletefavorite));
        inputAlert.setMessage(getResources().getText(R.string.confirmfrydb).toString());
        inputAlert.setPositiveButton(getResources().getText(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DatabaseHandler db = new DatabaseHandler(SettingsActivity.this);
                db.fry();
                db.close();
                finish();
            }
        });
        inputAlert.setNegativeButton(getResources().getText(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = inputAlert.create();
        alertDialog.setCancelable(false);
        alertDialog.show();

    }

    public void clearCache(View v) {
        DatabaseHandler db = new DatabaseHandler(this);
        db.fryCache();
        db.close();
    }
}
