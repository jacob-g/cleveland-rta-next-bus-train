package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
            mMap.setOnMarkerClickListener(this);
            mMap.setOnCameraChangeListener(this);

            //BEGIN TEST SECTION
            new GetStopsTask().execute();
            new GetPointsTask().execute();
            //END TEST SECTION
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private ProgressDialog createDialog(String title, String message) {
        ProgressDialog dlg = new ProgressDialog(NearMeActivity.this);
        dlg.setTitle(title);
        dlg.setMessage(message);
        dlg.show();
        return dlg;
    }

    public static class ColoredPointList {
        public final List<LatLng> points = new ArrayList<>();
        public final int color;
        public ColoredPointList(int color) {
            this.color = color;
        }
    }

    private class GetPointsTask extends AsyncTask<Void, Void, List<NearMeActivity.ColoredPointList>> {
        private ProgressDialog pDlg;

        public GetPointsTask() {
            //TODO: update the time period shown to reflect the appropriate setting
            pDlg = createDialog("Loading lines", "This may take a while, but you only have to do this once every two weeks.");
        }
        protected List<ColoredPointList> doInBackground(Void... params) {
            String cfgValue = PersistentDataController.getConfig(NearMeActivity.this, "lastSavedAllPaths");
            boolean expired = false;
            if (cfgValue.equals("") || Integer.parseInt(cfgValue) < PersistentDataController.getCurTime() - PersistentDataController.getFavLocationExpiry(NearMeActivity.this)) {
                expired = true;
            }
            DatabaseHandler db = new DatabaseHandler(NearMeActivity.this);
            List<ColoredPointList> fromDb = db.getAllPaths();

            List<ColoredPointList> paths = new ArrayList<>();
            try {
                if (!expired && fromDb != null) {
                    System.out.println("From cache!");
                    paths = fromDb;
                } else {
                    System.out.println("From internet!");
                    String httpData = NetworkController.basicHTTPRequest("https://nexttrain.futuresight.org/api/coords");
                    DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = dBuilder.parse(new InputSource(new StringReader(httpData)));
                    Node rootNode = doc.getDocumentElement();

                    if (doc.hasChildNodes()) {
                        NodeList nl = rootNode.getChildNodes();
                        for (int i = 0; i < nl.getLength(); i++) {
                            Node pathNode = nl.item(i);
                            if (pathNode.getNodeName().equals("p")) {
                                int color = Color.rgb(Integer.parseInt(pathNode.getAttributes().getNamedItem("r").getTextContent()), Integer.parseInt(pathNode.getAttributes().getNamedItem("g").getTextContent()), Integer.parseInt(pathNode.getAttributes().getNamedItem("b").getTextContent()));
                                ColoredPointList path = new ColoredPointList(color);
                                NodeList pointNodeList = pathNode.getChildNodes();
                                for (int j = 0; j < pointNodeList.getLength(); j++) {
                                    Node pointNode = pointNodeList.item(j);
                                    if (pointNode.getNodeName().equals("n")) {
                                        double lat = Double.parseDouble(pointNode.getAttributes().getNamedItem("lt").getTextContent());
                                        double lng = Double.parseDouble(pointNode.getAttributes().getNamedItem("ln").getTextContent());
                                        path.points.add(new LatLng(lat, lng));
                                    }
                                }
                                paths.add(path);
                            }
                        }
                    }
                    db.cacheAllPaths(paths);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return paths;
        }

        protected void onPostExecute(List<NearMeActivity.ColoredPointList> paths) {
            for (ColoredPointList path : paths) {
                PolylineOptions polyLineOptions = new PolylineOptions();
                polyLineOptions.addAll(path.points);
                polyLineOptions.width(4);
                polyLineOptions.color(path.color);
                mMap.addPolyline(polyLineOptions);
            }
            pDlg.dismiss();
        }
    }

    Map<Marker, Station> markers = new HashMap<>();
    Set<Integer> favIds = new HashSet<>();

    private class GetStopsTask extends AsyncTask<Void, Void, List<Station>> {
        private ProgressDialog pDlg;
        public GetStopsTask() {
            //TODO: update the time period shown to reflect the appropriate setting
            pDlg = createDialog("Loading stops", "This make take a while, but you only have to do this once every two weeks.");
        }
        protected List<Station> doInBackground(Void... params) {
            List<Station> out = new ArrayList<>();
            try {
                String cfgValue = PersistentDataController.getConfig(NearMeActivity.this, "lastSavedAllStops");
                boolean expired = false;
                if (cfgValue.equals("") || Integer.parseInt(cfgValue) < PersistentDataController.getCurTime() - PersistentDataController.getFavLocationExpiry(NearMeActivity.this)) {
                    expired = true;
                }
                DatabaseHandler db = new DatabaseHandler(NearMeActivity.this);
                List<Station> fromDb = db.getCachedStopLocations();

                if (!expired && fromDb != null) {
                    out = fromDb;
                } else {
                    String httpData = NetworkController.basicHTTPRequest("https://nexttrain.futuresight.org/api/getallstops");
                    Map<Integer, String> directions = new HashMap<>();

                    DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = dBuilder.parse(new InputSource(new StringReader(httpData)));
                    Node rootNode = doc.getDocumentElement();

                    if (doc.hasChildNodes()) {
                        NodeList nl = rootNode.getChildNodes();
                        for (int i = 0; i < nl.getLength(); i++) {
                            Node curNode = nl.item(i); //either <ds> or <ls>
                            switch (curNode.getNodeName()) {
                                case "ds":
                                    NodeList dirNodes = curNode.getChildNodes();
                                    for (int j = 0; j < dirNodes.getLength(); j++) {
                                        Node dirNode = dirNodes.item(j);
                                        if (dirNode.getNodeName().equals("d")) {
                                            int id = Integer.parseInt(dirNode.getAttributes().getNamedItem("i").getTextContent());
                                            String name = dirNode.getAttributes().getNamedItem("n").getTextContent();
                                            directions.put(id, name);
                                        }
                                    }
                                    break;
                                case "ls":
                                    NodeList lineNodes = curNode.getChildNodes();
                                    for (int j = 0; j < lineNodes.getLength(); j++) {
                                        Node lineNode = lineNodes.item(j);
                                        if (lineNode.getNodeName().equals("l")) {
                                            int lineId = Integer.parseInt(lineNode.getAttributes().getNamedItem("i").getTextContent());
                                            String lineName = lineNode.getAttributes().getNamedItem("n").getTextContent();
                                            int dirId = Integer.parseInt(lineNode.getAttributes().getNamedItem("d").getTextContent());
                                            NodeList stopNodes = lineNode.getChildNodes();
                                            for (int k = 0; k < stopNodes.getLength(); k++) {
                                                Node stopNode = stopNodes.item(k);
                                                if (stopNode.getNodeName().equals("s")) {
                                                    int id = Integer.parseInt(stopNode.getAttributes().getNamedItem("i").getTextContent());
                                                    String name = stopNode.getAttributes().getNamedItem("n").getTextContent();
                                                    double lat = Double.parseDouble(stopNode.getAttributes().getNamedItem("lt").getTextContent());
                                                    double lng = Double.parseDouble(stopNode.getAttributes().getNamedItem("ln").getTextContent());

                                                    Station st = new Station(name, id, directions.get(dirId), dirId, lineName, lineId, "", lat, lng);
                                                    out.add(st);
                                                }
                                            }
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                    db.cacheAllStops(out);
                }
                db.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


            return out;
        }

        protected void onPostExecute(List<Station> stops) {
            boolean shouldBeVisible = mMap.getCameraPosition().zoom > minZoomLevel; //see if the stations should be visible
            //get favorites
            DatabaseHandler db = new DatabaseHandler(NearMeActivity.this);
            List<Station> favorites = db.getFavoriteLocations();
            db.close();
            for (Station s : favorites) {
                favIds.add(s.getStationId());
            }
            //add the markers to the map
            for (Station st : stops) {
                Marker m;
                markers.put(m = mMap.addMarker(new MarkerOptions().position(st.getLatLng())), st);
                if (favIds.contains(st.getStationId())) { //mark with a star if it's a favorite
                    m.setIcon(BitmapDescriptorFactory.fromResource(android.R.drawable.btn_star_big_off));
                    m.setAnchor(0.5f, 0.5f); //center the icon
                }
                if (!shouldBeVisible) {
                    m.setVisible(false);
                }
            }
            alreadyVisible = shouldBeVisible;
            System.out.println("Markers ready!");
            pDlg.dismiss();
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
        boolean preferred;
        public ObjectByDistance(E o, boolean preferred, double d, Comparable s) {
            obj = o;
            this.preferred = preferred;
            dist = d;
            secondary = s;
        }

        public E getObj() {
            return obj;
        }

        public int compareTo(ObjectByDistance<E> other) {
            if (this.preferred && !other.preferred) {
                return -1;
            } else if (!this.preferred && other.preferred) {
                return 1;
            }
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
        Station st = markers.get(marker);
        if (st != null) {
            Queue<ObjectByDistance<Station>> closeStations = new PriorityQueue<>();

            for (Marker m : markers.keySet()) {
                double d = PersistentDataController.distance(marker.getPosition(), m.getPosition());
                if (d < 200) {
                    Station otherStation = markers.get(m);
                    closeStations.add(new ObjectByDistance<>(otherStation, favIds.contains(otherStation.getStationId()), d, otherStation.getName()));
                }
            }

            String[] options = new String[closeStations.size()];
            final Station[] stations = new Station[closeStations.size()];
            int i = 0;
            while (!closeStations.isEmpty()) {
                Station s = closeStations.remove().getObj();
                options[i] = s.getStationName() + " (" + s.getLineName() + ", " + s.getDirName() + ")";
                stations[i] = s;
                i++;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Possible stations (within 200m of where you clicked)").setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Station station = stations[i];
                    Intent intent = new Intent(NearMeActivity.this, NextBusTrainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("lineId", station.getLineId());
                    intent.putExtra("lineName", station.getLineName());
                    intent.putExtra("dirId", station.getDirId());
                    intent.putExtra("stopId", station.getStationId());
                    intent.putExtra("stopName", station.getStationName());
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
