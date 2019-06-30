package org.futuresight.clevelandrtanextbustrain;

import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class LiveMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener {

    private GoogleMap mMap = null;
    private LocationRequest mLocationRequest;
    private GoogleApiClient apiClient;
    private final float initialZoomLevel = 17f; //the default automatic zoom
    private boolean haveAutoFocusedCamera = false; //keeps track of whether or not we have auto-focused the camera on the user's location yet (if not, we need to zoom in)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void initializeMap() {
        try {
            mMap.setMyLocationEnabled(true);
            startTrackingLocation();
        } catch (SecurityException e) {
            finish();
        }
    }

    @Override
    protected void onStart() {
        startAPI();

        super.onStart();
    }

    //if we have a new location for the user, go to it
    @Override
    public void onLocationChanged(Location location) {

        if (haveAutoFocusedCamera) { //if we have already made the first automatic movement, then don't zoom the camera
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        } else { //otherwise, zoom in on the user's current location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), initialZoomLevel));
            haveAutoFocusedCamera = true;
        }
    }

    private void startTrackingLocation() {
        try {
            createLocationRequest();
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    apiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            finish();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //stuff to deal with the Google API Client
    @Override
    public void onConnectionSuspended(int x) {
        System.out.println("Connection suspended!");
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

    private void startAPI() {
        apiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        apiClient.connect();
    }

    //background tasks for downloading map locations

}
