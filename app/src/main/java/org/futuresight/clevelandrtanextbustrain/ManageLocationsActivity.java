package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class ManageLocationsActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks {
    boolean returnToParent = false;
    private List<Station> stations;
    private class FavoriteStationListItemActivity extends RelativeLayout {
        Button mainBtn;
        ImageButton editBtn, deleteBtn;
        Station station;
        Context context;
        LinearLayout parent;
        ManageLocationsActivity activity;
        boolean returnToParent;
        public FavoriteStationListItemActivity(Context c, Station s, LinearLayout p, boolean rtp, ManageLocationsActivity a) {
            super(c);

            station = s;
            context = c;
            parent = p;
            returnToParent = rtp;
            activity = a;

            mainBtn = new Button(context);
            mainBtn.setText(station.getName());
            mainBtn.setOnClickListener(new AdapterView.OnClickListener() {
                public void onClick(View view) {
                    if (returnToParent) {
                        //find the parent activity and use loadStation()
                        Intent intent = new Intent();
                        intent.putExtra("stopId",station.getStationId());
                        intent.putExtra("lineId",station.getLineId());
                        intent.putExtra("dirId",station.getDirId());
                        activity.setResult(RESULT_OK, intent);
                        if (apiClient != null) {
                            apiClient.disconnect();
                        }
                        finish();
                    } else {
                        Intent intent = new Intent(context, NextBusTrainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("lineId", station.getLineId());
                        intent.putExtra("lineName", station.getLineName());
                        intent.putExtra("dirId", station.getDirId());
                        intent.putExtra("dirName", station.getDirName());
                        intent.putExtra("stopId", station.getStationId());
                        intent.putExtra("stopName", station.getStationName());
                        startActivity(intent);
                        if (apiClient != null) {
                            apiClient.disconnect();
                        }
                        finish();
                    }
                }
            });
            mainBtn.setId(3000);

            RelativeLayout.LayoutParams lp;
            editBtn = new ImageButton(context);
            editBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_edit));
            editBtn.setOnClickListener(new AdapterView.OnClickListener() {
                public void onClick(View view) {
                    final AlertDialog.Builder inputAlert = new AlertDialog.Builder(view.getContext());
                    inputAlert.setTitle("Edit Favorite");
                    inputAlert.setMessage("Please enter the new name for this station");

                    //create the text box and automatically populate it with the current station name
                    final EditText userInput = new EditText(view.getContext());
                    userInput.setText(station.getName());
                    inputAlert.setView(userInput);

                    inputAlert.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //save the new station name
                            String newName = userInput.getText().toString();
                            station.setName(newName);
                            mainBtn.setText(newName);
                            DatabaseHandler db = new DatabaseHandler(context);
                            db.renameStation(station);
                            db.close();
                        }
                    });
                    inputAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alertDialog = inputAlert.create();
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                }
            });
            editBtn.setId(3001);

            deleteBtn = new ImageButton(context);
            deleteBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_delete));
            deleteBtn.setOnClickListener(new AdapterView.OnClickListener() {
                public void onClick(View view) {
                    final AlertDialog.Builder inputAlert = new AlertDialog.Builder(view.getContext());
                    inputAlert.setTitle(getResources().getText(R.string.deletefavorite));
                    inputAlert.setMessage(String.format(getResources().getText(R.string.deletefavoritemsg).toString(), station.getName()));
                    inputAlert.setPositiveButton(getResources().getText(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DatabaseHandler db = new DatabaseHandler(context);
                            db.deleteFavoriteStation(station);
                            db.close();
                            destroyMyself();
                            dialog.dismiss();
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
            });
            deleteBtn.setId(3002);

            lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_START);
            lp.addRule(RelativeLayout.LEFT_OF, editBtn.getId());
            this.addView(mainBtn, lp);

            lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.LEFT_OF, deleteBtn.getId());
            lp.addRule(RelativeLayout.ALIGN_TOP, mainBtn.getId());
            lp.addRule(RelativeLayout.ALIGN_BOTTOM, mainBtn.getId());
            editBtn.setLayoutParams(lp);
            this.addView(editBtn, lp);

            lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_END);
            lp.addRule(RelativeLayout.ALIGN_TOP, mainBtn.getId());
            lp.addRule(RelativeLayout.ALIGN_BOTTOM, mainBtn.getId());
            deleteBtn.setLayoutParams(lp);
            this.addView(deleteBtn, lp);
        }

        private void destroyMyself() {
            parent.removeView(this);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_locations);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        PersistentDataController.loadCacheDurations(this);

        //populate the layout with the items
        LinearLayout favoriteListLayout = (LinearLayout) findViewById(R.id.favoriteLocationsListView);
        DatabaseHandler db = new DatabaseHandler(this);
        List<Station> stl = db.getFavoriteLocations();
        if (getIntent().hasExtra("return")) {
            returnToParent = true;
        }
        stations = stl;
        for (Station s : stations) {
            FavoriteStationListItemActivity t = new FavoriteStationListItemActivity(this, s, favoriteListLayout, returnToParent, this);
            favoriteListLayout.addView(t);
        }
        db.close();

        CheckBox orderByClosenessBox = (CheckBox)findViewById(R.id.orderByClosenessCheckBox);
        orderByClosenessBox.setOnCheckedChangeListener(orderByClosenessBoxChecked);
        String orderSetting = PersistentDataController.getConfig(this, "orderByClosenessBoxChecked");
        if (orderSetting.equals("true")) {
            orderByClosenessBox.setChecked(true);
        }

        /*PersistentDataController.setConfig(this, "lastCheckedLocationsForAll", "0");
        db = new DatabaseHandler(this);
        db.updateStationLocation(stations.get(0), new LatLng(0, 0));
        stations.get(0).setLatLng(new LatLng(0, 0));
        db.close();*/
        new RefreshLocationsTask(this).execute();
    }

    //periodically reload the locations of the stations
    private class RefreshLocationsTask extends AsyncTask<Void, Void, Void> {
        private Context myContext;
        public RefreshLocationsTask(Context context) {
            myContext = context;
        }
        protected Void doInBackground(Void... params) {
            try {
                String lastCheckedAllStr = PersistentDataController.getConfig(myContext, "lastCheckedLocationsForAll");
                String lastCheckedNoLocStr = PersistentDataController.getConfig(myContext, "lastCheckedLocationsForNoLocations");
                int lastCheckedLocationsForAll = lastCheckedAllStr == "" ? 0 : Integer.parseInt(lastCheckedAllStr);
                int lastCheckedLocationsForNoLocations = lastCheckedNoLocStr == "" ? 0 : Integer.parseInt(lastCheckedNoLocStr);
                List<Station> toRefresh = new ArrayList<>();
                if (lastCheckedLocationsForAll == 0 || lastCheckedLocationsForAll < PersistentDataController.getCurTime() - PersistentDataController.getFavLocationExpiry(myContext)) {
                    //go for all stations
                    for (Station s : stations) {
                        toRefresh.add(s);
                    }
                    PersistentDataController.setConfig(myContext, "lastCheckedLocationsForAll", Integer.toString(PersistentDataController.getCurTime()));
                } else if (lastCheckedLocationsForNoLocations == 0 || lastCheckedLocationsForNoLocations < PersistentDataController.getCurTime() - PersistentDataController.getNoLocationRefreshPeriod()) {
                    //go for the stations with no location
                    for (Station s : stations) {
                        if (s.getLatLng() == null) {
                            toRefresh.add(s);
                        }
                    }
                }
                PersistentDataController.setConfig(myContext, "lastCheckedLocationsForNoLocations", Integer.toString(PersistentDataController.getCurTime()));
                if (!toRefresh.isEmpty()) {
                    DatabaseHandler db = new DatabaseHandler(myContext);
                    for (Station s : toRefresh) {
                        LatLng loc = NetworkController.getLocationForStation(s.getStationId());
                        if (loc != null && !loc.equals(s.getLatLng())) {
                            s.setLatLng(loc);
                            db.updateStationLocation(s, loc);
                        }
                    }
                    db.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void nul) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //disconnect API client and stop using location services when closing this
        if (apiClient != null) {
            apiClient.disconnect();
        }
    }


    //listener when the "order by closeness" box is (un)checked
    private CompoundButton.OnCheckedChangeListener orderByClosenessBoxChecked = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                //order by closeness, so start up the location API
                apiClient = new GoogleApiClient.Builder(ManageLocationsActivity.this).addConnectionCallbacks(ManageLocationsActivity.this)
                        .addApi(LocationServices.API)
                        .build();
                apiClient.connect();
                PersistentDataController.setConfig(ManageLocationsActivity.this, "orderByClosenessBoxChecked", "true");
            } else {
                //order alphabetically
                apiClient.disconnect();
                PersistentDataController.setConfig(ManageLocationsActivity.this, "orderByClosenessBoxChecked", "false");
                LinearLayout favoriteListLayout = (LinearLayout) findViewById(R.id.favoriteLocationsListView);
                favoriteListLayout.removeAllViews();
                for (Station s : stations) {
                    FavoriteStationListItemActivity t = new FavoriteStationListItemActivity(ManageLocationsActivity.this, s, favoriteListLayout, returnToParent, ManageLocationsActivity.this);
                    favoriteListLayout.addView(t);
                }
            }
        }
    };

    //CODE TO REQUEST LOCATION
    private LocationRequest mLocationRequest;
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        try {
            try {
                createLocationRequest();
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        apiClient, mLocationRequest, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class StationComparableByLocation implements Comparable<StationComparableByLocation> {
        final Station s;
        final double distance;
        public StationComparableByLocation(Station s, double lat, double lng) {
            this.s = s;
            if (s.getLatLng() == null) {
                distance = Double.MAX_VALUE;
            } else {
                distance = Math.sqrt(Math.pow(lat - s.getLatLng().latitude, 2) + Math.pow(lng - s.getLatLng().longitude, 2));
            }

        }

        public int compareTo(StationComparableByLocation other) {
            if (this.distance > other.distance) {
                return 1;
            } else if (this.distance == other.distance) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Queue<StationComparableByLocation> pq = new PriorityQueue<>();
        for (Station s : stations) {
            pq.add(new StationComparableByLocation(s, location.getLatitude(), location.getLongitude()));
        }
        LinearLayout favoriteListLayout = (LinearLayout) findViewById(R.id.favoriteLocationsListView);
        favoriteListLayout.removeAllViews();
        while (!pq.isEmpty()) {
            Station s = pq.remove().s;
            FavoriteStationListItemActivity t = new FavoriteStationListItemActivity(this, s, favoriteListLayout, returnToParent, this);
            favoriteListLayout.addView(t);
        }
    }

    final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
    /*here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
    */
                ActivityCompat.requestPermissions(this,
                        new String[]{ android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
                return;
            }
            startLocationUpdates();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    startLocationUpdates();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    CheckBox orderByClosenessBox = (CheckBox)findViewById(R.id.orderByClosenessCheckBox);
                    orderByClosenessBox.setChecked(false);
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private GoogleApiClient apiClient;

    @Override
    public void onConnectionSuspended(int x) {
        System.out.println("Connection suspended!");
    }
}
