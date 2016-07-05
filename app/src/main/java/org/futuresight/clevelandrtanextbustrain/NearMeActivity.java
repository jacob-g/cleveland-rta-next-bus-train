package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

//Tower City location: 41 29 51; -81 41 37 (DMS)
//https://developer.android.com/training/location/retrieve-current.html#play-services

public class NearMeActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraChangeListener {

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
            mMap.setOnMarkerClickListener(this);
            mMap.setOnCameraChangeListener(this);

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
            paths.add(points);
            return paths;
        }

        protected void onPostExecute(List<List<LatLng>> paths) {
            //TODO: give the lines colors
            for (List<LatLng> path : paths) {
                PolylineOptions polyLineOptions = new PolylineOptions();
                polyLineOptions.addAll(path);
                polyLineOptions.width(4);
                polyLineOptions.color(Color.BLUE);
                mMap.addPolyline(polyLineOptions);
            }
        }
    }

    private void alertDialog(String title, String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
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

    Map<Marker, TempStation> markers = new HashMap<>();

    private class GetStopsTask extends AsyncTask<Void, Void, List<TempStation>> {
        //TODO: cache all of the stops
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
            boolean shouldBeVisible = mMap.getCameraPosition().zoom > minZoomLevel;
            for (TempStation st : stops) {
                Marker m;
                markers.put(m = mMap.addMarker(new MarkerOptions().position(st.pos).title(st.name).snippet(st.line)), st);
                if (!shouldBeVisible) {
                    m.setVisible(false);
                }
            }
            alreadyVisible = shouldBeVisible;
        }
    }

    private boolean alreadyVisible = false;
    private final double minZoomLevel = 13.5;
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if (alreadyVisible && cameraPosition.zoom <= minZoomLevel || !alreadyVisible && cameraPosition.zoom > minZoomLevel) {
            for (Marker m : markers.keySet()) {
                //don't show the icons when zoomed out too much
                m.setVisible(!alreadyVisible);
            }
            alreadyVisible = !alreadyVisible;
        }
    }

    private class ObjectByDistance<E> implements Comparable<ObjectByDistance<E>> {
        E obj;
        double dist;
        Comparable secondary;
        public ObjectByDistance(E o, double d, Comparable s) {
            obj = o;
            dist = d;
            secondary = s;
        }

        public E getObj() {
            return obj;
        }

        public int compareTo(ObjectByDistance<E> other) {
            if (this.dist > other.dist) {
                return 1;
            } else if (this.dist < other.dist) {
                return -1;
            } else {
                return this.secondary.compareTo(other.secondary);
            }
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        TempStation st = markers.get(marker);
        if (st != null) {
            Queue<ObjectByDistance<TempStation>> closeStations = new PriorityQueue<>();

            for (Marker m : markers.keySet()) {
                double d = PersistentDataController.distance(marker.getPosition(), m.getPosition());
                if (d < 200) {
                    TempStation otherStation = markers.get(m);
                    closeStations.add(new ObjectByDistance<>(otherStation, d, otherStation.name));
                }
            }

            String[] options = new String[closeStations.size()];
            final TempStation[] stations = new TempStation[closeStations.size()];
            int i = 0;
            while (!closeStations.isEmpty()) {
                //TODO: make this show the direction as text and not a number
                TempStation s = closeStations.remove().getObj();
                options[i] = s.name + " (" + s.line + ", " + s.dir + ")";
                stations[i] = s;
                i++;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Possible stations (within 200m of where you clicked)").setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //TODO: open the next bus/train activity
                    TempStation station = stations[i];
                    Intent intent = new Intent(NearMeActivity.this, NextBusTrainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("lineId", station.lineId);
                    intent.putExtra("lineName", station.line);
                    intent.putExtra("dirId", station.dir);
                    intent.putExtra("stopId", station.id);
                    intent.putExtra("stopName", station.name);
                    startActivity(intent);
                    if (apiClient != null) {
                        apiClient.disconnect();
                    }
                    finish();
                }
            });
            builder.create();
            builder.show();
            return true;
        } else {
            System.out.println("Couldn't find the marker");
            return false;
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
        //TODO: make this a decent way to follow the user
        //mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
    }


    //call this once we have permission to track the current user's location
    private void initializeMap() {
        try {
            mMap.setMyLocationEnabled(true);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.4975, -81.6939), 10)); //focus on Cleveland
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
