package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.Layout;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.futuresight.clevelandrtanextbustrain.StopType.RAIL;

//Tower City location: 41 29 51; -81 41 37 (DMS)
//https://developer.android.com/training/location/retrieve-current.html#play-services

class MapLine {
    final LineType type;
    final String name;

    public MapLine(LineType type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

class ArrivalSet {
    final Map<MapLine, Map<Integer, List<Arrival>>> arrivals; //in the format line -> direction -> arrivals (in ascending order of time)
    //TODO: have a way to prioritize lines

    public ArrivalSet() {
        arrivals = new HashMap<MapLine, Map<Integer, List<Arrival>>>();
    }

    public void addArrival(Arrival arrival) {
        if (!arrivals.containsKey(arrival.line)) {
            arrivals.put(arrival.line, new HashMap<Integer, List<Arrival>>());
        }
        if (!arrivals.get(arrival.line).containsKey(arrival.direction)) {
            arrivals.get(arrival.line).put(arrival.direction, new ArrayList<Arrival>());
        }

        List<Arrival> curArrivals = arrivals.get(arrival.line).get(arrival.direction);
        for (int i = 0; i <= curArrivals.size(); i++) {
            if (i == curArrivals.size() || arrival.compareTo(curArrivals.get(i)) < 0) {
                curArrivals.add(i, arrival);
                break;
            }
        }
    }
}

class Arrival implements Comparable<Arrival> {
    final MapLine line;
    final int direction;
    final String destination;
    final int timeHour;
    final int timeMinute;

    public Arrival(MapLine line, int direction, String destination, int timeHour, int timeMinute) {
        this.line = line;
        this.direction = direction;
        this.destination = destination;
        this.timeHour = timeHour;
        this.timeMinute = timeMinute;

        //TODO: handle times cycling through midnight (like 1am is after 11pm)
    }

    public Arrival(MapLine line, int direction, String destination, String time) throws Exception {
        this.line = line;
        this.direction = direction;
        this.destination = destination;

        Pattern p = Pattern.compile("(\\d+):(\\d+)(am|pm)");
        Matcher m = p.matcher(time);
        if (m.matches()) {
            int hour = Integer.parseInt(m.group(1));
            if (m.group(3).equals("pm")) {
                hour += 12;
            }
            if (hour == 12 || hour == 24) {
                hour -= 12;
            }
            this.timeHour = hour;
            this.timeMinute = Integer.parseInt(m.group(2));
        } else {
            throw new Exception("Invalid time format, must be XX:XXam/pm");
        }
    }

    public int compareTo(Arrival other) {
        if (this.timeHour < other.timeHour) {
            return -1;
        } else if (this.timeHour > other.timeHour) {
            return 1;
        } else if (this.timeMinute < other.timeMinute) {
            return -1;
        } else if (this.timeMinute > other.timeMinute) {
            return 1;
        } else {
            return 0;
        }
    }
}

class MapStation {
    final String id;
    final String name;
    final String parent;

    public MapStation(String id, String name, String parent) {
        this.id = id;
        this.name = name;
        this.parent = parent;
    }

    List<Arrival> getArrivals(Map<String, MapLine> linesById) {
        List<Arrival> outArrivals = new ArrayList<>();

        String httpData = NetworkController.basicHTTPRequest("https://nexttraintest.futuresight.org/api/getarrivals/" + id + "?version=" + PersistentDataController.API_VERSION);
        if (httpData == null) {
            return null;
        }

        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(httpData)));

            if (doc.hasChildNodes()) {
                Node rootNode = doc.getDocumentElement();

                NodeList childNodes = rootNode.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node curNode = childNodes.item(i);
                    if (curNode.getNodeName().equals("a")) {
                        String timeString = curNode.getAttributes().getNamedItem("t").getTextContent();
                        int tripId = Integer.parseInt(curNode.getAttributes().getNamedItem("tr").getTextContent());
                        String destination = curNode.getAttributes().getNamedItem("dst").getTextContent();
                        String route = curNode.getAttributes().getNamedItem("r").getTextContent();
                        int direction = Integer.parseInt(curNode.getAttributes().getNamedItem("dir").getTextContent());

                        //TODO: get the line properly
                        String[] timeParts = timeString.split(":");
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        Arrival arrival = new Arrival(linesById.get(route), direction, destination, hour, minute);
                        outArrivals.add(arrival);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(outArrivals);
        return outArrivals;
    }
}

enum LineType {
    BUS, RAIL
}

enum StopType {
    BUS, RAIL, TRANSFER
}

class MapLocation {
    private Set<MapStation> stations;
    final LatLng position;
    final StopType type;
    final String name;

    public MapLocation(String name, LatLng position, StopType type) {
        this.name = name;
        this.position = position;
        this.type = type;
        stations = new HashSet<>();
    }

    public void addStation(MapStation station) {
        stations.add(station);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(MapLocation other) {
        return this.name.equals(other.name);
    }

    ArrivalSet getArrivals(Map<String, MapLine> linesById) {
        ArrivalSet outArrivals = new ArrivalSet();

        for (MapStation station : stations) {
            List<Arrival> curArrivals = station.getArrivals(linesById);
            for (Arrival arrival : curArrivals) {
                outArrivals.addArrival(arrival);
            }
        }
        return outArrivals;
    }
}

class MapItem {

}

public class NearMeActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraChangeListener, GoogleMap.OnCameraMoveStartedListener {

    private GoogleMap mMap;
    private GoogleApiClient apiClient;
    private LocationRequest mLocationRequest;
    private boolean shouldFocusOnCleveland = true;

    @Override
    public void onConnectionSuspended(int x) {
        System.out.println("Connection suspended!");
    }

    private void alertDialog(String title, String msg, final boolean die) {
        AlertDialog alertDialog = new AlertDialog.Builder(NearMeActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (die) {
                            finish();
                        }
                    }
                });
        alertDialog.show();
    }

    private Marker lastSearchedMarker = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_near_me);

        if (!NetworkController.connected(this)) {
            alertDialog(getResources().getString(R.string.network), getResources().getString(R.string.nonetworkmsg), true);
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //restore saved data if necessary
        if (savedInstanceState != null) {
            hasLocation = true;
            followingUser = savedInstanceState.getBoolean("followingUser");
            double[] latLngArray = savedInstanceState.getDoubleArray("latlng");
        }

        //set the event for the show/hide below map display button
        (findViewById(R.id.showHideBtn)).setOnClickListener(new Button.OnClickListener() {
                                                                  public void onClick(View view) {
                                                                      View belowMapLayout = findViewById(R.id.belowMapLayout);
                                                                      ImageButton sender = (ImageButton)view;
                                                                      if (belowMapLayout.getVisibility() == View.VISIBLE) {
                                                                          //hide the below map display
                                                                          belowMapLayout.setVisibility(View.GONE);
                                                                          sender.setImageResource(R.drawable.mr_group_collapse);
                                                                      } else {
                                                                          //show the below map display
                                                                          belowMapLayout.setVisibility(View.VISIBLE);
                                                                          sender.setImageResource(R.drawable.mr_group_expand);
                                                                      }
                                                                      //also update the height of the whole thing
                                                                      ScrollView belowMapScrollView = (ScrollView)findViewById(R.id.belowMapScrollView);
                                                                      belowMapScrollView.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
                                                                      belowMapScrollView.invalidate();
                                                                      belowMapLayout.requestLayout();
                                                                  }
                                                              }
        );

        (findViewById(R.id.belowMapBackBtn)).setOnClickListener(new Button.OnClickListener() {
                                                                public void onClick(View view) {
                                                                    showStopsNearMeBelowMap();
                                                                }
                                                            }
        );

        //handle events related to the search bar
        final PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete);
        autocompleteFragment.setBoundsBias(new LatLngBounds(
                new LatLng(41.301, -82.023),
                new LatLng(41.686, -81.339))); //roughly the bounds for the RTA's service area
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                followingUser = false;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 17));
                //add a marker to the map for where the user searched
                if (lastSearchedMarker != null) {
                    lastSearchedMarker.remove();
                }
                lastSearchedMarker = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).snippet(place.getName().toString()));
            }

            @Override
            public void onError(Status status) {
                System.err.println("An error occurred: " + status);
            }
        });

        //adjust the height to make sure that the bottom layout isn't more than 50% of the screen height
        ScrollView belowMapScrollView = (ScrollView)findViewById(R.id.belowMapScrollView);
        belowMapScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new OnViewGlobalLayoutListener(belowMapScrollView));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current state
        try {
            CameraPosition pos = mMap.getCameraPosition();
            savedInstanceState.putDoubleArray("latlng", new double[]{pos.target.latitude, pos.target.longitude});
            savedInstanceState.putFloat("bearing", pos.bearing);
            savedInstanceState.putFloat("zoom", pos.zoom);
            savedInstanceState.putBoolean("followingUser", followingUser);
            savedInstanceState.putBoolean("belowMapDisplayShown", findViewById(R.id.belowMapLayout).getVisibility() == View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private static class OnViewGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private View view;

        public OnViewGlobalLayoutListener(View view) {
            this.view = view;
        }

        @Override
        public void onGlobalLayout() {
            LinearLayout principalLayout = (LinearLayout)((NearMeActivity)view.getContext()).findViewById(R.id.mapParentLayout);
            int maxHeight = principalLayout.getHeight() / 2;
            LinearLayout container = (LinearLayout)((NearMeActivity)view.getContext()).findViewById(R.id.belowMapContainer);
            if (container.getHeight() > maxHeight) {
                view.getLayoutParams().height = maxHeight;
            } else {
                view.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
            }
            view.invalidate();
            view.requestLayout();
        }
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
            mMap.setOnCameraMoveStartedListener(this);

            new GetStopsTask().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void showStopsNearMeBelowMap() {
        findViewById(R.id.loadingTable).setVisibility(TableLayout.GONE);
        findViewById(R.id.nearbyStopsTable).setVisibility(TableLayout.VISIBLE);
        findViewById(R.id.stopArrivalsTable).setVisibility(TableLayout.GONE);

        //TODO: show the information for nearby stops
    }

    private void showLocationInfoBelowMap(MapLocation loc) {
        //show just the panel for this stop
        findViewById(R.id.loadingTable).setVisibility(TableLayout.GONE);
        findViewById(R.id.nearbyStopsTable).setVisibility(TableLayout.GONE);
        findViewById(R.id.stopArrivalsTable).setVisibility(TableLayout.VISIBLE);

        ((TextView)findViewById(R.id.belowMapStationNameView)).setText(loc.name); //set the header text to this stop's name

        new GetArrivalsForStopTask().execute(loc);
    }

    class GetArrivalsForStopTask extends AsyncTask<MapLocation, Void, ArrivalSet> {
        protected ArrivalSet doInBackground(MapLocation... params) {
            return params[0].getArrivals(linesById);
        }

        protected void onPostExecute(ArrivalSet arrivals) {
            displayArrivals(arrivals);
        }
    }

    void displayArrivals(ArrivalSet arrivals) {
        TableLayout stopArrivalsTable = ((TableLayout)findViewById(R.id.stopArrivalsTable));
        for (; stopArrivalsTable.getChildCount() > 1;) {
            stopArrivalsTable.removeView(stopArrivalsTable.getChildAt(1));
        }

        for (MapLine line : arrivals.arrivals.keySet()) {
            List<TableRow> rows = new LinkedList<>();

            //create the header with the line name (spans both columns)
            TableRow lineHeader = new TableRow(NearMeActivity.this);
            TextView lineNameText = new TextView(NearMeActivity.this);
            lineNameText.setText(line.name);
            lineNameText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            lineNameText.setTypeface(null, Typeface.BOLD);
            TableRow.LayoutParams lineNameParams = new TableRow.LayoutParams();
            lineNameParams.span = 2;
            lineNameParams.width = TableRow.LayoutParams.MATCH_PARENT;
            lineHeader.addView(lineNameText, 0, lineNameParams);
            stopArrivalsTable.addView(lineHeader);

            //create the header for each direction
            TableRow directionHeader = new TableRow(NearMeActivity.this);

            //TODO: set these directions based on the line
            TextView dir1View = new TextView(NearMeActivity.this);
            dir1View.setText("EAST");
            directionHeader.addView(dir1View);

            TextView dir2View = new TextView(NearMeActivity.this);
            dir2View.setText("WEST");
            directionHeader.addView(dir2View);

            stopArrivalsTable.addView(directionHeader);

            List<List<String>> arrivalTexts = new ArrayList<>();
            for (Integer direction : arrivals.arrivals.get(line).keySet()) {

                List<Arrival> curArrivals = arrivals.arrivals.get(line).get(direction);
                int index = 0;
                for (Arrival arrival : curArrivals) {
                    while (arrivalTexts.size() <= index) {
                        arrivalTexts.add(new ArrayList<String>());
                    }
                    while (arrivalTexts.get(index).size() <= arrival.direction) {
                        arrivalTexts.get(index).add(" ");
                    }
                    arrivalTexts.get(index).set(arrival.direction, arrival.destination + "\n" + String.format("%d:%02d", arrival.timeHour, + arrival.timeMinute));
                    index++;
                }
            }

            for (List<String> rowTexts : arrivalTexts) {
                TableRow row = new TableRow(NearMeActivity.this);
                for (String arrivalText : rowTexts) {
                    TextView arrivalTextView = new TextView(NearMeActivity.this);
                    arrivalTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT));
                    arrivalTextView.setText(arrivalText);
                    row.addView(arrivalTextView);
                }
                stopArrivalsTable.addView(row);
            }
        }
    }

    Map<Marker, MapLocation> mapLocationsByMarker = new HashMap<>();
    Map<String, MapLocation> mapLocationsById = new HashMap<>();
    Map<String, MapLine> linesById = new HashMap<>();
    class GetStopsTask extends AsyncTask<Void, Integer, Set<MapLocation>> {
        protected Set<MapLocation> doInBackground(Void... params) {
            Set<MapLocation> toReturn = new HashSet<>();

            Set<MapStation> stationsWithParents = new HashSet<>();

            try {
                String httpData = NetworkController.basicHTTPRequest("https://nexttraintest.futuresight.org/api/getallroutes?version=" + PersistentDataController.API_VERSION);

                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(httpData)));
                if (doc.hasChildNodes()) {
                    Node rootNode = doc.getDocumentElement();
                    NodeList childNodes = rootNode.getChildNodes();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node curNode = childNodes.item(i);
                        if (curNode.getNodeName().equals("l")) {
                            String id = curNode.getAttributes().getNamedItem("i").getTextContent();
                            String name = curNode.getAttributes().getNamedItem("n").getTextContent();
                            LineType type = curNode.getAttributes().getNamedItem("i").getTextContent().equals("3") ? LineType.BUS : LineType.RAIL;
                            MapLine line = new MapLine(type, name);

                            linesById.put(id, line);
                        }
                    }
                }

                boolean shouldContinue = true;
                int page = 1;
                while (shouldContinue) {
                    shouldContinue = false;

                    httpData = NetworkController.basicHTTPRequest("https://nexttraintest.futuresight.org/api/getallstops?page=" + page + "&version=" + PersistentDataController.API_VERSION);
                    if (httpData == null) {
                        return null;
                    }

                    publishProgress(page);

                    dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    doc = dBuilder.parse(new InputSource(new StringReader(httpData)));

                    if (doc.hasChildNodes()) {
                        Node rootNode = doc.getDocumentElement();

                        NodeList childNodes = rootNode.getChildNodes();
                        for (int i = 0; i < childNodes.getLength(); i++) {
                            Node curNode = childNodes.item(i);
                            if (curNode.getNodeName().equals("s")) {
                                shouldContinue = true;
                                String stopId = curNode.getAttributes().getNamedItem("i").getTextContent();
                                String name = curNode.getAttributes().getNamedItem("n").getTextContent();
                                double lat = Double.parseDouble(curNode.getAttributes().getNamedItem("lt").getTextContent());
                                double lng = Double.parseDouble(curNode.getAttributes().getNamedItem("ln").getTextContent());
                                String parentName = curNode.getAttributes().getNamedItem("p").getTextContent();

                                if (parentName.equals("")) {
                                    MapLocation loc = new MapLocation(name, new LatLng(lat, lng), RAIL);
                                    mapLocationsById.put(stopId, loc);
                                    toReturn.add(loc);

                                    MapStation station = new MapStation(stopId, name, parentName);
                                    loc.addStation(station);
                                } else {
                                    MapStation station = new MapStation(stopId, name, parentName);
                                    stationsWithParents.add(station);
                                }
                            }
                        }

                        page++;
                    }
                }

                for (MapStation station : stationsWithParents) {
                    mapLocationsById.get(station.parent).addStation(station);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return toReturn;
        }

        protected void onProgressUpdate(Integer... progress) {
            if (findViewById(R.id.loadingLinesBar) != null) {
                ((ProgressBar) findViewById(R.id.loadingLinesBar)).setProgress(progress[0]);
            }
        }

        protected void onPostExecute(Set<MapLocation> mapItems) {
            //TODO: put everything on the map

            final BitmapDescriptor favoritePin = BitmapDescriptorFactory.fromAsset("icons/favoritepin.png");
            final BitmapDescriptor busPin = BitmapDescriptorFactory.fromAsset("icons/blackbuspin.png");
            final BitmapDescriptor railPin = BitmapDescriptorFactory.fromAsset("icons/blackrailpin.png");
            final BitmapDescriptor transferPin = BitmapDescriptorFactory.fromAsset("icons/blacktransferpin.png");

            //TODO: use the sector approach from before to avoid adding all several thousand at once

            for (MapLocation loc : mapItems) {
                Marker m = mMap.addMarker(new MarkerOptions().position(loc.position));
                m.setIcon(railPin);

                mapLocationsByMarker.put(m, loc);
            }
            showStopsNearMeBelowMap();
        }
    }

    public static class ColoredPointList {
        public final int lineId;
        public final String lineName;
        public final List<LatLng> points = new ArrayList<>();
        public final int color;
        public ColoredPointList(int color, int lineId, String lineName) {
            this.color = color;
            this.lineId = lineId;
            this.lineName = lineName;
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

        public int hashCode() {
            return first + second;
        }
    }


    private boolean autoFocused = false;


    @Override
    public void onCameraMoveStarted(int reason) {

        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            followingUser = false;
        }
    }



    private Marker mapCenterMarker;
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        //TODO: handle camera moving
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
        //TODO: handle marker clicking

        showLocationInfoBelowMap(mapLocationsByMarker.get(marker));

        return true;
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
    boolean followingUser = false;
    @Override
    public void onLocationChanged(Location location) {
        if (followingUser) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        }
        if (!hasLocation) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
            hasLocation = true;
            if (!autoFocused) {
                followingUser = true;
            }
        }
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

            //make it so that when the user hits the "go to my location" button it resumes following
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener(){
                @Override
                public boolean onMyLocationButtonClick() {
                    followingUser = true;
                    //remove the marker in the middle of the map
                    if (mapCenterMarker != null) {
                        mapCenterMarker.remove();
                        mapCenterMarker = null;
                    }
                    return false;
                }
            });
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
