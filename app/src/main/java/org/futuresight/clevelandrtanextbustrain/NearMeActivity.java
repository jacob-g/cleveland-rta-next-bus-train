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
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

//Tower City location: 41 29 51; -81 41 37 (DMS)
//https://developer.android.com/training/location/retrieve-current.html#play-services

public class NearMeActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraChangeListener {

    private GoogleMap mMap;
    private GoogleApiClient apiClient;
    private LocationRequest mLocationRequest;
    private boolean shouldFocusOnCleveland = true;

    @Override
    public void onConnectionSuspended(int x) {
        System.out.println("Connection suspended!");
    }

    private String[] linesWithAllOption;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_near_me);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        (findViewById(R.id.chooseLineBtn)).setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    if (linesWithAllOption == null) {
                        linesWithAllOption = new String[lines.length + 1];
                        linesWithAllOption[0] = "All routes";
                        for (int i = 0; i < lines.length; i++) {
                            linesWithAllOption[i + 1] = lines[i];
                        }
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(NearMeActivity.this);
                    builder.setTitle(getResources().getString(R.string.select_route)).setItems(linesWithAllOption, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //TODO: optimize this to improve runtime
                            if (i == 0) {
                                for (int l : pathsByLineId.keySet()) {
                                    for (Polyline path : pathsByLineId.get(l)) {
                                        path.setVisible(true);
                                    }
                                    visibleStations.clear();
                                    for (Station st : stationList) {
                                        visibleStations.add(st);
                                    }
                                    for (Marker m : markers.keySet()) {
                                        m.setVisible(alreadyVisible);
                                    }
                                }
                            } else {
                                int lineId = lineIdMap.get(lines[i - 1]);
                                for (int l : pathsByLineId.keySet()) {
                                    boolean visible = (l == lineId);
                                    for (Polyline path : pathsByLineId.get(l)) {
                                        path.setVisible(visible);
                                    }
                                    visibleStations = new ArrayList<>();
                                    for (Station st : stationList) {
                                        if (st.getLineId() == lineId) {
                                            visibleStations.add(st);
                                        }
                                    }
                                    for (Marker m : markers.keySet()) {
                                        Station st = markers.get(m);
                                        boolean stationVisible = visibleStations.contains(st);
                                        if (!stationVisible) {
                                            m.setVisible(false);
                                        } else if (alreadyVisible && stationVisible) {
                                            m.setVisible(true);
                                        }
                                    }
                                }
                            }
                        }
                    });
                    builder.create();
                    builder.show();
                }
            }
        );
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

            //Get the points and stops
            new GetStopsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            new GetPointsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static class ColoredPointList {
        public final int lineId;
        public final List<LatLng> points = new ArrayList<>();
        public final int color;
        public ColoredPointList(int color, int lineId) {
            this.color = color;
            this.lineId = lineId;
        }
    }

    Map<String, Integer> lineIdMap;
    String[] lines;
    Map<Integer, List<Polyline>> pathsByLineId = new HashMap<>();

    private class GetPointsTask extends AsyncTask<Void, Integer, List<NearMeActivity.ColoredPointList>> {
        public GetPointsTask() {
        }

        protected List<ColoredPointList> doInBackground(Void... params) {
            lineIdMap = PersistentDataController.getLineIdMap(NearMeActivity.this);
            lines = PersistentDataController.getLines(NearMeActivity.this);
            String cfgValue = PersistentDataController.getConfig(NearMeActivity.this, DatabaseHandler.CONFIG_LAST_SAVED_ALL_PATHS);
            boolean expired = false;
            if (cfgValue.equals("") || Integer.parseInt(cfgValue) < PersistentDataController.getCurTime() - PersistentDataController.getFavLocationExpiry(NearMeActivity.this)) {
                expired = true;
            }
            DatabaseHandler db = new DatabaseHandler(NearMeActivity.this);
            List<ColoredPointList> fromDb = db.getAllPaths();

            List<ColoredPointList> paths = new ArrayList<>();
            try {
                if (!expired && fromDb != null) {
                    paths = fromDb;
                } else {
                    boolean more = true;
                    int page = 0;
                    while (more) {
                        page++;
                        String httpData = NetworkController.basicHTTPRequest("https://nexttrain.futuresight.org/api/coords?page=" + page + "&version=" + PersistentDataController.API_VERSION);
                        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                        Document doc = dBuilder.parse(new InputSource(new StringReader(httpData)));
                        Node rootNode = doc.getDocumentElement();

                        more = false;
                        if (doc.hasChildNodes()) {
                            NodeList nl = rootNode.getChildNodes();
                            for (int i = 0; i < nl.getLength(); i++) {
                                Node pathNode = nl.item(i);
                                if (pathNode.getNodeName().equals("p")) {
                                    more = true;
                                    int color = Color.rgb(Integer.parseInt(pathNode.getAttributes().getNamedItem("r").getTextContent()), Integer.parseInt(pathNode.getAttributes().getNamedItem("g").getTextContent()), Integer.parseInt(pathNode.getAttributes().getNamedItem("b").getTextContent()));
                                    int lineId = Integer.parseInt(pathNode.getAttributes().getNamedItem("l").getTextContent());
                                    ColoredPointList path = new ColoredPointList(color, lineId);
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
                                } else if (pathNode.getNodeName().equals("pages")) {
                                    int numPages = Integer.parseInt(pathNode.getTextContent());
                                    publishProgress((int)((double) page * 100 / numPages));
                                }
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

        protected void onProgressUpdate(Integer... progress) {
            ((ProgressBar)findViewById(R.id.loadingLinesBar)).setProgress(progress[0]);
        }

        protected void onPostExecute(List<NearMeActivity.ColoredPointList> paths) {
            for (ColoredPointList path : paths) {
                PolylineOptions polyLineOptions = new PolylineOptions();
                polyLineOptions.addAll(path.points);
                polyLineOptions.width(4);
                polyLineOptions.color(path.color);
                Polyline newPath = mMap.addPolyline(polyLineOptions);
                if (!pathsByLineId.containsKey(path.lineId)) {
                    pathsByLineId.put(path.lineId, new ArrayList<Polyline>());
                }
                pathsByLineId.get(path.lineId).add(newPath);
            }
            ((TableLayout)findViewById(R.id.belowMapLayout)).removeView(findViewById(R.id.loadingLinesRow));
            (findViewById(R.id.chooseLineBtn)).setVisibility(View.VISIBLE);
            loadedLines = true;
        }
    }

    private class NumberPair implements Comparable<NumberPair> {
        public final int first, second;
        public NumberPair(int first, int second) {
            this.first = first;
            this.second = second;
        }

        public boolean equals(NumberPair other) {
            return this.first == other.first && this.second == other.second;
        }

        public String toString() {
            return "[" + first + "," + second + "]";
        }

        public int compareTo(NumberPair other) {
            int out = this.first - other.first;
            if (out == 0) {
                out = this.second - other.second;
            }
            return out;
        }
    }

    final double sectorSize = 0.005;
    final char railType = 'r';
    private int focusStationId = -1;
    Map<NumberPair, List<Station>> markerSectors = new HashMap<>();
    Map<Marker, Station> markers = new HashMap<>();
    Set<Station> favoriteStations = new HashSet<>();
    List<Station> stationList = new ArrayList<>();
    List<Station> visibleStations = new ArrayList<>();

    private boolean loadedStops = false;
    private boolean loadedLines = false;
    private class GetStopsTask extends AsyncTask<Void, Void, List<Station>> {

        public GetStopsTask() {
        }
        protected List<Station> doInBackground(Void... params) {
            return PersistentDataController.getMapMarkers(NearMeActivity.this, (ProgressBar)findViewById(R.id.loadingStopsBar));
        }

        protected void onPostExecute(List<Station> stops) {
            //preload bitmap descriptors to improve performance
            BitmapDescriptor favoritePin = BitmapDescriptorFactory.fromAsset("icons/favoritepin.png");
            BitmapDescriptor busPin = BitmapDescriptorFactory.fromAsset("icons/blackbuspin.png");
            BitmapDescriptor railPin = BitmapDescriptorFactory.fromAsset("icons/blackrailpin.png");
            char railType = 'r';

            //get favorites
            DatabaseHandler db = new DatabaseHandler(NearMeActivity.this);
            List<Station> favorites = db.getFavoriteLocations();
            db.close();
            for (Station s : favorites) {
                favoriteStations.add(s);
            }
            //add the markers to the map
            int stationId = -1;
            if (getIntent().hasExtra("stationId")) { //if a station id is sent in, focus on that station
                stationId = getIntent().getExtras().getInt("stationId");
            }
            LatLng autoFocusPosition = null;
            markers = new HashMap<>(stops.size(), 0.5f);
            stationList = new ArrayList<>(stops.size());
            int size = stops.size();
            for (int i = 0; i < size; i++) {
                Station st = stops.get(i);
                int sectorLat = (int)Math.floor(st.getLatLng().latitude / sectorSize);
                int sectorLng = (int)Math.floor(st.getLatLng().longitude / sectorSize);
                NumberPair sectorKey = new NumberPair(sectorLat, sectorLng);
                if (!markerSectors.containsKey(sectorKey)) {
                    markerSectors.put(sectorKey, new ArrayList<Station>());
                }
                markerSectors.get(sectorKey).add(st);
                stationList.add(st);
                visibleStations.add(st);
                if (st.getStationId() == stationId) {
                    autoFocusPosition = st.getLatLng();
                    focusStationId = stationId;
                }
            }
            boolean shouldBeVisible = mMap.getCameraPosition().zoom > minZoomLevel; //see if the stations should be visible
            alreadyVisible = shouldBeVisible;

            if (autoFocusPosition != null) {
                shouldFocusOnCleveland = false;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(autoFocusPosition, 17));
                hasLocation = true;
            }
            onCameraChange(mMap.getCameraPosition());

            ((TableLayout)findViewById(R.id.belowMapLayout)).removeView(findViewById(R.id.loadingStopsRow));
            loadedStops = true;
        }
    }

    final Map<Integer, Boolean> shownLines = new HashMap<>();
    final int MAX_STATION_BOTTOM_DISPLAY_DISTANCE = 800;
    final int STATION_LIST_LIMIT = 15;
    private class GetStopsNearMeTask extends AsyncTask<LatLng, Void, List<Object[]>> {
        public GetStopsNearMeTask() {
        }
        protected List<Object[]> doInBackground(LatLng... params) {
            Queue<ObjectByDistance<Station>> closeStations = new PriorityQueue<>();

            for (Station st : stationList) {
                double d = PersistentDataController.distance(params[0], st.getLatLng());
                if (d < MAX_STATION_BOTTOM_DISPLAY_DISTANCE) {
                    int priority;
                    if (favoriteStations.contains(st)) { //prioritize favorites
                        priority = 2;
                    } else if (st.getType() == 'r') { //prioritize rail over bus
                        priority = 1;
                    } else {
                        priority = 0;
                    }
                    closeStations.add(new ObjectByDistance<>(st, priority, d, st.getName() + " (" + st.getLineName() + ", " + st.getDirName()));
                }
            }

            Set<NumberPair> usedLines = new TreeSet<>();
            List<Object[]> stopList = new ArrayList<>();
            try {
                int i = 0;
                while (!closeStations.isEmpty() && i < STATION_LIST_LIMIT) {
                    i++;
                    ObjectByDistance<Station> o = closeStations.remove();
                    Station st = o.getObj();
                    NumberPair linePair = new NumberPair(st.getLineId(), st.getDirId());
                    if (usedLines.add(linePair)) { //only add one station for each line-direction combination
                        List<String[]> arrivalInfo = NetworkController.getStopTimes(NearMeActivity.this, st.getLineId(), st.getDirId(), st.getStationId());
                        stopList.add(new Object[]{st.getStationName(), st.getLineName() + " (" + st.getDirName() + ")", arrivalInfo.size() > 0 ? arrivalInfo.get(0)[2] : "N/A", arrivalInfo.size() > 0 ? arrivalInfo.get(0)[1] : "N/A", st});
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return stopList;
        }

        protected void onPostExecute(List<Object[]> stopList) {
            try {
                ((TableLayout)findViewById(R.id.belowMapLayout)).removeAllViews();

                for (Object[] stopInfo : stopList) {
                    TableRow arrivalRow = new TableRow(NearMeActivity.this);

                    TextView stationNameView = new TextView(NearMeActivity.this);
                    String stopName = ((String) stopInfo[0]).replace(" (Published Stop)", "").replace(" STATION", "").replace(" Stn", "");
                    stationNameView.setText(stopName + "\n" + stopInfo[1]);
                    stationNameView.setTextColor(Color.BLUE);
                    final Station station = (Station) stopInfo[4];
                    stationNameView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
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
                            startActivity(intent);
                        }
                    });
                    arrivalRow.addView(stationNameView, 0);

                    TextView stationLineView = new TextView(NearMeActivity.this);
                    stationLineView.setMaxWidth(250);
                    stationLineView.setText(stopInfo[3] + "\n" + stopInfo[2]);
                    arrivalRow.addView(stationLineView, 1);

                    ((TableLayout) findViewById(R.id.belowMapLayout)).addView(arrivalRow);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean alreadyVisible = false;
    private final double minZoomLevel = 14;
    Set<NumberPair> spotsAdded = new HashSet<>();
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if (alreadyVisible && cameraPosition.zoom <= minZoomLevel || !alreadyVisible && cameraPosition.zoom > minZoomLevel) {
            for (Marker m : markers.keySet()) {
                //don't show the icons when zoomed out too much
                m.setVisible((!alreadyVisible) && visibleStations.contains(markers.get(m)));
            }
            alreadyVisible = !alreadyVisible;
        }

        if (alreadyVisible) {
            LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            double minLat = bounds.southwest.latitude, minLng = bounds.southwest.longitude, maxLat = bounds.northeast.latitude, maxLng = bounds.northeast.longitude;

            minLat -= sectorSize;
            maxLat += sectorSize;
            minLng -= sectorSize;
            maxLng += sectorSize;

            BitmapDescriptor favoritePin = BitmapDescriptorFactory.fromAsset("icons/favoritepin.png");
            BitmapDescriptor busPin = BitmapDescriptorFactory.fromAsset("icons/blackbuspin.png");
            BitmapDescriptor railPin = BitmapDescriptorFactory.fromAsset("icons/blackrailpin.png");
            int minLatInt = (int)Math.floor(minLat / sectorSize);
            int maxLatInt = (int)Math.ceil(maxLat / sectorSize);
            int minLngInt = (int)Math.floor(minLng / sectorSize);
            int maxLngInt = (int)Math.ceil(maxLng / sectorSize);
            for (Iterator it = markerSectors.keySet().iterator(); it.hasNext(); ) {
                NumberPair pos = (NumberPair)it.next();
                if (minLatInt <= pos.first && pos.first <= maxLatInt && minLngInt <= pos.second && pos.second <= maxLngInt) {
                    if (spotsAdded.add(pos)) {
                        for (Station st: markerSectors.get(pos)) {
                            Marker m = mMap.addMarker(new MarkerOptions().position(st.getLatLng()));
                            if (st.getStationId() == focusStationId) {
                                Marker m2 = mMap.addMarker(new MarkerOptions().position(st.getLatLng()));
                                m2.setIcon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_add));
                                m2.setAnchor(0.5f, 0.5f); //center the icon
                                m2.setZIndex(0);
                            }

                            if (favoriteStations.contains(st)) { //mark with a star if it's a favorite
                                m.setIcon(favoritePin);
                                m.setZIndex(2);
                            } else if (st.getType() == railType) {
                                m.setIcon(railPin);
                                m.setZIndex(1);
                            } else {
                                m.setIcon(busPin);
                                m.setZIndex(0);
                            }
                            markers.put(m, st);
                            if (alreadyVisible && visibleStations.contains(st)) {
                                m.setVisible(true);
                            } else {
                                m.setVisible(false);
                            }
                        }
                    }
                    it.remove();
                }
            }
        }
    }

    private class ObjectByDistance<E> implements Comparable<ObjectByDistance<E>> {
        E obj;
        double dist;
        Comparable secondary;
        int preferred;
        public ObjectByDistance(E o, int preferred, double d, Comparable s) {
            obj = o;
            this.preferred = preferred;
            dist = d;
            secondary = s;
        }

        public E getObj() {
            return obj;
        }

        public int compareTo(ObjectByDistance<E> other) {
            if (this.preferred > other.preferred) {
                return -1;
            } else if (this.preferred < other.preferred) {
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

    final int MAX_STATION_CLICK_DISTANCE = 100; //distance in meters to show stations close to where a user clicks
    @Override
    public boolean onMarkerClick(final Marker marker) {
        Station st = markers.get(marker);
        if (st != null) {
            Queue<ObjectByDistance<Station>> closeStations = new PriorityQueue<>();

            for (Marker m : markers.keySet()) {
                double d = PersistentDataController.distance(marker.getPosition(), m.getPosition());
                if (d < MAX_STATION_CLICK_DISTANCE && m.isVisible()) {
                    Station otherStation = markers.get(m);
                    int priority;
                    if (favoriteStations.contains(otherStation)) { //prioritize favorites
                        priority = 2;
                    } else if (otherStation.getType() == 'r') { //prioritize rail over bus
                        priority = 1;
                    } else {
                        priority = 0;
                    }
                    closeStations.add(new ObjectByDistance<>(otherStation, priority, d, otherStation.getName() + " (" + otherStation.getLineName() + ", " + otherStation.getDirName()));
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
            builder.setTitle(String.format(getResources().getString(R.string.nearbystations), MAX_STATION_CLICK_DISTANCE)).setItems(options, new DialogInterface.OnClickListener() {
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

    boolean hasLocation = false;
    long lastCheckedStops = 0;
    final int getStopsNearMeInterval = 10;
    @Override
    public void onLocationChanged(Location location) {
        if (!hasLocation) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
            hasLocation = true;
        }
        if (loadedLines && loadedStops && lastCheckedStops < PersistentDataController.getCurTime() - getStopsNearMeInterval) {
            lastCheckedStops = PersistentDataController.getCurTime();
            new GetStopsNearMeTask().execute(new LatLng(location.getLatitude(), location.getLongitude()));
        }
        //mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
    }


    //call this once we have permission to track the current user's location
    boolean mapInitialized = false;
    private void initializeMap() {
        try {
            mMap.setMyLocationEnabled(true);
            if (shouldFocusOnCleveland && !mapInitialized) {
                mapInitialized = true;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.4975, -81.6939), 10)); //focus on Cleveland
            }
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
