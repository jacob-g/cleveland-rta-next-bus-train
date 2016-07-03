package org.futuresight.clevelandrtanextbustrain;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

//Tower City location: 41 29 51; -81 41 37 (DMS)
//https://developer.android.com/training/location/retrieve-current.html#play-services

public class NearMeActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient apiClient;
    private LocationRequest mLocationRequest;

    @Override
    public void onConnectionSuspended(int x) {
        System.out.println("Connection suspended!");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_near_me);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {

        apiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        apiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {

        apiClient.disconnect();
        super.onStop();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        try {
            //placeholder to show how to mark stations
            LatLng twrCity = new LatLng(41.4975, -81.6939);
            //TODO: add all stations
            mMap.addMarker(new MarkerOptions().position(twrCity).title("Tower City").snippet("Transfer between all rail lines"));

            //BEGIN TEST SECTION
            new GetPointsTask().execute();
            new GetStopsTask().execute();
            //END TEST SECTION
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private class GetPointsTask extends AsyncTask<Void, Void, List<List<LatLng>>> {
        public GetPointsTask() {
        }
        protected List<List<LatLng>> doInBackground(Void... params) {
            List<List<LatLng>> paths = new ArrayList<>();
            List<LatLng> points = new ArrayList<>();
            String httpData = NetworkController.basicHTTPRequest("https://nexttrain.futuresight.org/api/coords");
            String[] lines = httpData.split("\n");
            for (String line : lines) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    points.add(new LatLng(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
                } else if (line.equals("")) {
                    paths.add(points);
                    points = new ArrayList<>();
                }
            }
            System.out.println(points);
            paths.add(points);
            return paths;
        }

        protected void onPostExecute(List<List<LatLng>> paths) {
            for (List<LatLng> path : paths) {
                PolylineOptions polyLineOptions = new PolylineOptions();
                polyLineOptions.addAll(path);
                polyLineOptions.width(2);
                polyLineOptions.color(Color.BLUE);
                mMap.addPolyline(polyLineOptions);
            }
        }
    }

    private class TempStation {
        public final LatLng pos;
        public final String line;
        public final String name;
        public final int lineId;
        public final int dir;
        public final int id;
        public TempStation(int id, String name, double lat, double lng, String line, int lineId, int dir) {
            this.id = id;
            this.name = name;
            pos = new LatLng(lat, lng);
            this.line = line;
            this.lineId = lineId;
            this.dir = dir;
        }
    }

    private class GetStopsTask extends AsyncTask<Void, Void, List<TempStation>> {
        public GetStopsTask() {
        }
        protected List<TempStation> doInBackground(Void... params) {
            List<TempStation> out = new ArrayList<>();
            String httpData = NetworkController.basicHTTPRequest("https://nexttrain.futuresight.org/api/getallstops");
            String[] lines = httpData.split("\n");
            for (String line : lines) {
                String[] parts = line.split("\\|");
                if (parts.length == 7) {
                    out.add(new TempStation(Integer.parseInt(parts[0]), parts[1], Double.parseDouble(parts[5]), Double.parseDouble(parts[6]), parts[3], Integer.parseInt(parts[4]), Integer.parseInt(parts[2])));
                } else {
                    System.out.println(parts.length);
                    System.out.println(line);
                }
            }
            System.out.println(out);
            return out;
        }

        protected void onPostExecute(List<TempStation> stops) {
            for (TempStation st : stops) {
                mMap.addMarker(new MarkerOptions().position(st.pos).title(st.name).snippet(st.line));
            }
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        try {
            createLocationRequest();
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    apiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        System.out.println(location.getLatitude() + " " + location.getLongitude());
    }


    //call this once we have permission to track the current user's location
    private void initializeMap() {
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        startLocationUpdates();
    }

    final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

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
            initializeMap();
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
                    initializeMap();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
