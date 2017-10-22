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
import android.util.SparseArray;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

//Tower City location: 41 29 51; -81 41 37 (DMS)
//https://developer.android.com/training/location/retrieve-current.html#play-services

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

    private String[] linesWithAllOption;
    private boolean reloading = false;
    private LatLng reloadingPosition;
    private float reloadingZoom;
    private float reloadingBearing;
    private int selectedLine = 0;
    private boolean belowMapDisplayShown = true;
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
            reloading = true;
            followingUser = savedInstanceState.getBoolean("followingUser");
            double[] latLngArray = savedInstanceState.getDoubleArray("latlng");
            reloadingPosition = new LatLng(latLngArray[0], latLngArray[1]);
            reloadingZoom = savedInstanceState.getFloat("zoom");
            reloadingBearing = savedInstanceState.getFloat("bearing");
            selectedLine = savedInstanceState.getInt("selectedLine");
            belowMapDisplayShown = savedInstanceState.getBoolean("belowMapDisplayShown");
        }

        //set the event for the "choose line" button
        (findViewById(R.id.chooseLineBtn)).setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    if (linesWithAllOption == null) {
                        linesWithAllOption = new String[lines.length + 1];
                        linesWithAllOption[0] = getResources().getString(R.string.allroutes);
                        for (int i = 0; i < lines.length; i++) {
                            linesWithAllOption[i + 1] = lines[i];
                        }
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(NearMeActivity.this);
                    builder.setTitle(getResources().getString(R.string.select_route)).setItems(linesWithAllOption, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            showStationsOnLine(i);
                            selectedLine = i;
                        }
                    });
                    builder.create();
                    builder.show();
                }
            }
        );

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
            savedInstanceState.putInt("selectedLine", selectedLine);
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    //only show the stations on a given line, and in this case i is the index number in the line list (not the line ID) since it is triggered by the dropdown
    private int shownLine = 0;
    private void showStationsOnLine(int i) {
        int lineId = i > 0 ? lineIdMap.get(lines[i - 1]) : 0;
        shownLine = lineId;
        if (i == 0) {
            for (int l : pathsByLineId.keySet()) {
                for (Polyline path : pathsByLineId.get(l)) {
                    path.setVisible(true);
                }
            }
            visibleStations.clear();
            for (Station st : stationList) {
                visibleStations.add(st);
            }
            for (Marker m : markers.keySet()) {
                m.setVisible(alreadyVisible);
                Station st = markers.get(m);
                if (st.isTransfer()) {
                    if (favoriteStations.contains(st)) { //mark with a star if it's a favorite
                        m.setIcon(favoritePin);
                        m.setZIndex(3);
                    } else if (st.getType() == railType) {
                        m.setIcon(railPin);
                        m.setZIndex(1);
                    } else {
                        m.setIcon(busPin);
                        m.setZIndex(0);
                    }
                }
            }
        } else {
            //new GetTransfersTask().execute(lineId);
            for (int l : pathsByLineId.keySet()) {
                boolean visible = (l == lineId);
                for (Polyline path : pathsByLineId.get(l)) {
                    path.setVisible(visible);
                }
            }
            visibleStations = new HashSet<>();
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
                    if (markers.get(m).isTransfer()) {
                        m.setIcon(transferPin);
                    }
                }
            }
        }
        for (int key : pathMarkersByLineId.keySet()) {
            for (Marker m : pathMarkersByLineId.get(key)) {
                m.setVisible(alreadyVisible && (shownLine == 0 || shownLine == key));
            }
        }
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

            //Get the points and stops
            new GetStopsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            new GetPointsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            e.printStackTrace();
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

    Map<String, Integer> lineIdMap = new HashMap<>(); //the map of line ids given their string names
    SparseArray<String> linesById = new SparseArray<>(); //the line string name from its numerical id
    SparseArray<Integer> colorsByLineId = new SparseArray<>(); //the color for a line by its numerical id
    String[] lines; //an array of the list of lines
    Map<Integer, List<Polyline>> pathsByLineId = new HashMap<>(); //the list of paths for a line given its id
    Map<Integer, List<Marker>> pathMarkersByLineId = new HashMap<>(); //the markers on a given line given its id


    private class GetPointsTask extends AsyncTask<Void, Integer, List<NearMeActivity.ColoredPointList>> {
        private final int pointMarkerInterval = 20; //how frequently to place the line labels on the paths
        private final int pointMarkerStopInterval = pointMarkerInterval / 2; //where to stop if trying to avoid a collision
        private final int pointMarkerFontSize = 24; //the font size for the line labels
        public GetPointsTask() {
        }

        protected List<ColoredPointList> doInBackground(Void... params) {
            //see if it's expired
            String cfgValue = PersistentDataController.getConfig(NearMeActivity.this, DatabaseHandler.CONFIG_LAST_SAVED_ALL_PATHS);
            boolean expired = false;

            //make sure this hasn't expired in the database, and if it has, download it from the internet anew
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
                        if (httpData == null) {
                            paths = null;
                            break;
                        }
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
                                    String lineName = pathNode.getAttributes().getNamedItem("n").getTextContent();
                                    //store all the points as a path
                                    ColoredPointList path = new ColoredPointList(color, lineId, lineName);
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
                                    //update the progress
                                    int numPages = Integer.parseInt(pathNode.getTextContent());
                                    publishProgress((int)((double) page * 100 / numPages));
                                }
                            }
                        }
                    }
                    if (paths != null) {
                        db.cacheAllPaths(paths);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return paths;
        }

        protected void onProgressUpdate(Integer... progress) {
            if (findViewById(R.id.loadingLinesBar) != null) {
                ((ProgressBar) findViewById(R.id.loadingLinesBar)).setProgress(progress[0]);
            }
        }

        protected void onPostExecute(List<NearMeActivity.ColoredPointList> paths) {
            if (paths == null) {
                //if the paths didn't load properly, give an option to try again or exit
                AlertDialog alertDialog = new AlertDialog.Builder(NearMeActivity.this).create();
                alertDialog.setTitle(getResources().getString(R.string.error));
                alertDialog.setMessage(getResources().getString(R.string.failed_to_load_lines_try_again));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                new GetPointsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        });
                alertDialog.show();
                return;
            }
            //put the paths on the map
            Set<NumberPair> addedSectors = new TreeSet<>();
            final double sectorLatSize = .0005;
            final double sectorLngSize = .002;
            for (ColoredPointList path : paths) {
                //before doing anything, save the color for later use
                if (colorsByLineId.get(path.lineId, -1) == -1) {
                    colorsByLineId.put(path.lineId, path.color);
                }

                //get the path and store it
                PolylineOptions polyLineOptions = new PolylineOptions();
                polyLineOptions.addAll(path.points);
                polyLineOptions.width(4);
                polyLineOptions.color(path.color);
                int lineColor;
                if (Color.red(path.color) + Color.green(path.color) + Color.blue(path.color) < 384) {
                    lineColor = Color.WHITE;
                } else {
                    lineColor = Color.BLACK;
                }

                Polyline newPath = mMap.addPolyline(polyLineOptions);
                if (!pathsByLineId.containsKey(path.lineId)) {
                    pathsByLineId.put(path.lineId, new LinkedList<Polyline>());
                }
                pathsByLineId.get(path.lineId).add(newPath);

                if (linesById.get(path.lineId) == null) {
                    linesById.put(path.lineId, path.lineName);
                }
                if (!lineIdMap.containsKey(path.lineName)) {
                    lineIdMap.put(path.lineName, path.lineId);
                }

                //add markers along the path to label it
                //create the image
                String lineName = path.lineName;

                //paint for the background
                Paint bgPaint = new Paint();
                bgPaint.setColor(path.color);
                bgPaint.setStrokeWidth(1);

                //paint for the text
                Paint textPaint = new Paint();
                textPaint.setColor(lineColor);
                textPaint.setStrokeWidth(1);
                textPaint.setTextSize(pointMarkerFontSize);
                textPaint.setStrokeWidth(1);
                textPaint.setTextSize(pointMarkerFontSize);
                int width = (int)textPaint.measureText(lineName);

                //create the image
                Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                Bitmap bmp = Bitmap.createBitmap(width, pointMarkerFontSize + 4, conf);
                Canvas canvas = new Canvas(bmp);
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), bgPaint);

                //put the text on
                canvas.drawText(lineName, 0, 24, textPaint); // paint defines the text color, stroke width, size
                for (int i = 0; i < path.points.size(); i += pointMarkerInterval) {
                    //prevent collisions, and if there is one, jump to the next possible point
                    NumberPair pos = null;
                    LatLng coords = null;
                    int k = 0;
                    while ((pos == null || addedSectors.contains(pos)) && i + k < path.points.size()) {
                        coords = path.points.get(i + k);
                        pos = new NumberPair((int)(coords.latitude / sectorLatSize), (int)((coords.longitude) / sectorLngSize));
                        k++;
                        if (k > pointMarkerStopInterval || i + k + 1 == path.points.size()) { //if we're already halfway to the next point, just skip it
                            coords = null;
                            break;
                        }
                    }
                    if (coords != null) {
                        addedSectors.add(pos);
                        Marker newMarker = mMap.addMarker(new MarkerOptions()
                                .position(coords)
                                .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                                .anchor(0.5f, 0.5f)
                        );
                        newMarker.setVisible(alreadyVisible);
                        if (!pathMarkersByLineId.containsKey(path.lineId)) {
                            pathMarkersByLineId.put(path.lineId, new LinkedList<Marker>());
                        }
                        pathMarkersByLineId.get(path.lineId).add(newMarker);
                    }
                }
            }
            //save the lines array
            int size = lineIdMap.size();
            lines = new String[size];
            PersistentDataController.LineForSorting[] linesForSorting = new PersistentDataController.LineForSorting[size];
            int i = 0;
            for (String lineName : lineIdMap.keySet()) {
                PersistentDataController.LineForSorting l = new PersistentDataController.LineForSorting(lineName);
                linesForSorting[i] = l;
                i++;
            }
            Arrays.sort(linesForSorting);
            for (i = 0; i < size; i++) {
                lines[i] = linesForSorting[i].toString();
            }
            //mark the task as done by removing the progress bar
            ((TableLayout)findViewById(R.id.belowMapLayout)).removeView(findViewById(R.id.loadingLinesRow));
            //show the buttons that go below the map
            (findViewById(R.id.topButtonsLayout)).setVisibility(View.VISIBLE);
            //if restoring the map and the display was hidden before, hide it again
            if (!belowMapDisplayShown) {
                findViewById(R.id.belowMapLayout).setVisibility(View.GONE);
                ((ImageButton)findViewById(R.id.showHideBtn)).setImageResource(R.drawable.mr_group_collapse);
            }
            loadedLines = true;

            if (loadedStops) {
                showStationsOnLine(selectedLine);
            }
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

    final double sectorSize = 0.004;
    final char railType = 'r';
    private int focusStationId = -1;
    Map<NumberPair, List<Station>> markerSectors = new HashMap<>();
    Map<Marker, Station> markers = new HashMap<>();
    Set<Station> favoriteStations = new HashSet<>();
    List<Station> stationList = new ArrayList<>();
    Set<Station> visibleStations = new HashSet<>();

    private boolean loadedStops = false;
    private boolean loadedLines = false;
    boolean autoFocused = false;
    private class GetStopsTask extends AsyncTask<Void, Void, List<Station>> {

        public GetStopsTask() {
        }
        protected List<Station> doInBackground(Void... params) {
            return PersistentDataController.getMapMarkers(NearMeActivity.this, (ProgressBar)findViewById(R.id.loadingStopsBar));
        }

        protected void onPostExecute(List<Station> stops) {
            if (stops == null || stops.isEmpty()) {
                //if the markers didn't load properly, give an option to try again or exit
                AlertDialog alertDialog = new AlertDialog.Builder(NearMeActivity.this).create();
                alertDialog.setTitle(getResources().getString(R.string.error));
                alertDialog.setMessage(getResources().getString(R.string.failed_to_load_stops_try_again));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                new GetStopsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        });
                alertDialog.show();
                return;
            }
            //initialize pins
            favoritePin = BitmapDescriptorFactory.fromAsset("icons/favoritepin.png");
            busPin = BitmapDescriptorFactory.fromAsset("icons/blackbuspin.png");
            railPin = BitmapDescriptorFactory.fromAsset("icons/blackrailpin.png");
            transferPin = BitmapDescriptorFactory.fromAsset("icons/blacktransferpin.png");

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
            float autoZoom = 17f; //the default automatic zoom
            float autoRotate = 0f;
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
                if (reloading) {
                    autoFocusPosition = reloadingPosition;
                    autoZoom = reloadingZoom;
                    autoRotate = reloadingBearing;
                } else if (st.getStationId() == stationId) {
                    autoFocusPosition = st.getLatLng();
                    focusStationId = stationId;
                    followingUser = false;
                    autoFocused = true;
                }
            }
            alreadyVisible = mMap.getCameraPosition().zoom > minZoomLevel;

            if (autoFocusPosition != null) {
                shouldFocusOnCleveland = false;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(autoFocusPosition, autoZoom));
                CameraPosition pos = CameraPosition.builder(mMap.getCameraPosition()).bearing(autoRotate).build();
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
                hasLocation = true;
            }
            onCameraChange(mMap.getCameraPosition());

            ((TableLayout)findViewById(R.id.belowMapLayout)).removeView(findViewById(R.id.loadingStopsRow));
            loadedStops = true;

            if (loadedLines) {
                showStationsOnLine(selectedLine);
            }
        }
    }

    //a timer to automatically update the nearby arrivals
    private Timer updateStopsNearMeTimer;
    private final int updateInterval = 15;
    private void startTimer(int delay) {
        updateStopsNearMeTimer = new Timer();
        updateStopsNearMeTimer.scheduleAtFixedRate(new GetStopsNearMeTimerTask(), delay, updateInterval * 1000);
    }

    private void cancelTimer() {
        if (updateStopsNearMeTimer != null) {
            updateStopsNearMeTimer.cancel();
        }
    }

    //the task to update the nearby stop arrivals
    class GetStopsNearMeTimerTask extends TimerTask {
        public GetStopsNearMeTimerTask(){
        }

        public void run() {
            //in order to access the map, this has to run on the UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new UpdateNearbyArrivalsTask().execute();
                }
            });
        }
    }

    //the task to update the arrival times at all of the nearby stops
    private class UpdateNearbyArrivalsTask extends AsyncTask<Queue<Integer>, Void, SparseArray<List<String[]>>> {
        private final SparseArray<Station> oldNearbyStops;
        public UpdateNearbyArrivalsTask() {
            oldNearbyStops = new SparseArray<>();
        }

        protected SparseArray<List<String[]>> doInBackground(Queue<Integer>... params) {
            Queue<Integer> q;
            if (params.length == 0) {
                q = new LinkedList<>();
                for (int i = 0; i + 3 < highestNearbyStopIndex; i += 3) {
                    q.add(i + 1);
                    q.add(i + 2);
                }
            } else {
                q = params[0];
            }
            SparseArray<List<String[]>> out = new SparseArray<>();
            while (!q.isEmpty()) {
                try {
                    int index = q.remove();
                    Station st = listedNearbyStops.get(index);
                    oldNearbyStops.put(index, st);
                    if (st != null) {
                        List<String[]> arrivals = NetworkController.getStopTimes(NearMeActivity.this, st.getLineId(), st.getDirId(), st.getStationId());
                        out.put(index, arrivals);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return out;
        }

        protected void onPostExecute(SparseArray<List<String[]>> stopList) {
            for (int i = 0; i < stopList.size(); i++) {
                try {
                    int index = stopList.keyAt(i);
                    List<String[]> arrivals = stopList.get(index);
                    String arrivalText = "N/A";
                    if (!arrivals.isEmpty()) {
                        arrivalText = arrivals.get(0)[1] + "\n" + arrivals.get(0)[2];
                    }
                    final TableLayout belowMapLayout = ((TableLayout) findViewById(R.id.belowMapLayout));
                    if (belowMapLayout.getChildCount() > index && oldNearbyStops.get(index).equals(listedNearbyStops.get(index))) { //make sure the stop is still listed in the same position before updating it
                        ((TextView) ((TableRow) belowMapLayout.getChildAt(index)).getChildAt(1)).setText(arrivalText);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final int MAX_STATION_BOTTOM_DISPLAY_DISTANCE = 800;
    private SparseArray<Station> listedNearbyStops = new SparseArray<>();
    private int highestNearbyStopIndex = 0;
    //the task to populate the list of nearby stops shown below the map, separate from the task that gets the arrivals
    private class GetStopsNearMeTask extends AsyncTask<LatLng, Void, SparseArray<SparseArray<ObjectByDistance<Station>>>> {
        public GetStopsNearMeTask() {
        }

        protected SparseArray<SparseArray<ObjectByDistance<Station>>> doInBackground(LatLng... params) {
            SparseArray<SparseArray<ObjectByDistance<Station>>> closestStationByLine = new SparseArray<>();

            for (Station st : stationList) {
                double d = PersistentDataController.distance(params[0], st.getLatLng());
                if (d < MAX_STATION_BOTTOM_DISPLAY_DISTANCE) {
                    if (closestStationByLine.get(st.getLineId(), null) == null) {
                        closestStationByLine.put(st.getLineId(), new SparseArray<ObjectByDistance<Station>>());
                    }
                    int priority;
                    if (favoriteStations.contains(st)) { //prioritize favorites
                        priority = 2;
                    } else if (st.getType() == 'r') { //prioritize rail over bus
                        priority = 1;
                    } else {
                        priority = 0;
                    }
                    ObjectByDistance<Station> complexPriority = new ObjectByDistance<>(st, priority, d, st.getName());
                    ObjectByDistance<Station> closestOnLine = closestStationByLine.get(st.getLineId()).get(st.getDirId(), null);
                    if (closestOnLine == null || complexPriority.compareTo(closestOnLine) < 0) {
                        closestStationByLine.get(st.getLineId()).put(st.getDirId(), complexPriority);
                    }
                }
            }
            return closestStationByLine;
        }

        private final float lineHeaderFontSize = 14f;
        protected void onPostExecute(SparseArray<SparseArray<ObjectByDistance<Station>>> closestStationByLine) {
            try {
                final TableLayout belowMapLayout = ((TableLayout)findViewById(R.id.belowMapLayout));
                TableRow[] formerRows = new TableRow[belowMapLayout.getChildCount()];
                for (int i = 0; i < belowMapLayout.getChildCount(); i++) {
                    formerRows[i] = (TableRow)belowMapLayout.getChildAt(i);
                }
                belowMapLayout.removeAllViews();

                PriorityQueue<ObjectByDistance<Station>> linePriorities = new PriorityQueue<>();
                for (int i = 0; i < closestStationByLine.size(); i++) {
                    int lineId = closestStationByLine.keyAt(i);
                    SparseArray<ObjectByDistance<Station>> closestStationsOnLine = closestStationByLine.get(lineId);
                    ObjectByDistance<Station> highestPriority = null;
                    for (int j = 0; j < closestStationsOnLine.size(); j++) {
                        int dirId = closestStationsOnLine.keyAt(j);
                        ObjectByDistance<Station> priority = closestStationsOnLine.get(dirId);
                        if (highestPriority == null || priority.compareTo(highestPriority) < 0) {
                            highestPriority = priority;
                        }
                    }
                    linePriorities.add(highestPriority);
                }

                Queue<Integer> indicesToUpdate = new LinkedList<>();
                int index = 0;
                while (!linePriorities.isEmpty()) {
                    int lineId = linePriorities.remove().getObj().getLineId();
                    //add a row to the table with colspan 2 with just the name of the line
                    TableRow lineNameRow = new TableRow(NearMeActivity.this);
                    String lineName = linesById.get(lineId);
                    int color = colorsByLineId.get(lineId);
                    TextView lineNameView = new TextView(NearMeActivity.this);
                    lineNameView.setText(lineName);
                    lineNameView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    lineNameView.setTypeface(null, Typeface.BOLD);
                    lineNameView.setTextSize(lineHeaderFontSize);
                    lineNameView.setBackgroundColor(color);
                    if (Color.red(color) + Color.green(color) + Color.blue(color) < 384) {
                        lineNameView.setTextColor(Color.WHITE);
                    } else {
                        lineNameView.setTextColor(Color.BLACK);
                    }
                    TableRow.LayoutParams lineNameParams = new TableRow.LayoutParams();
                    lineNameParams.span = 2;
                    lineNameParams.width = TableRow.LayoutParams.MATCH_PARENT;
                    lineNameRow.addView(lineNameView, 0, lineNameParams);
                    belowMapLayout.addView(lineNameRow, index);
                    index++;

                    SparseArray<ObjectByDistance<Station>> closestStationsOnLine = closestStationByLine.get(lineId);
                    for (int j = 0; j < closestStationsOnLine.size(); j++) {
                        int dirId = closestStationsOnLine.keyAt(j);
                        final Station st = closestStationsOnLine.get(dirId).getObj();
                        Station oldSt = listedNearbyStops.get(index, null);
                        if (oldSt == null || !st.equals(oldSt) || index >= formerRows.length) {
                            indicesToUpdate.add(index);
                            listedNearbyStops.put(index, st);

                            //for each direction for this line, add another row with the directions and upcoming arrivals
                            TableRow arrivalRow = new TableRow(NearMeActivity.this);

                            LinearLayout stationNameLayout = new LinearLayout(NearMeActivity.this);
                            stationNameLayout.setOrientation(LinearLayout.VERTICAL);
                            TextView stationNameView = new TextView(NearMeActivity.this);
                            String stopName = st.getStationName().replace(" (Published Stop)", "").replace("Station", "").replace(" STATION", "").replace(" Stn", "");
                            stationNameView.setText(st.getDirName() + ":\n" + stopName);
                            stationNameView.setTextColor(Color.BLUE);

                            stationNameLayout.addView(stationNameView);

                            //TODO: force this to display properly - have a minimum width for both the left and right parts and in between, prioritize the left
                            TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                            params.weight = 1;
                            stationNameLayout.setLayoutParams(params);

                            //a layout for the options to see arrivals and the location on the map
                            final LinearLayout stationActionsLayout = new LinearLayout(NearMeActivity.this);
                            LinearLayout.LayoutParams optionButtonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                            optionButtonParams.weight = 1f;

                            //make it so clicking on the station name will show the options
                            stationNameView.setOnClickListener(new TextView.OnClickListener() {
                                public void onClick(View v) {
                                    stationActionsLayout.setVisibility(View.VISIBLE);
                                }
                            });

                            //the button to show the location on the map
                            ImageButton viewBtn = new ImageButton(NearMeActivity.this);
                            viewBtn.setImageResource(R.drawable.places_ic_search);
                            viewBtn.setLayoutParams(optionButtonParams);
                            viewBtn.setOnClickListener(new Button.OnClickListener() {
                                public void onClick(View v) {
                                    if (locationArrowMarker != null) {
                                        locationArrowMarker.remove();
                                    }
                                    locationArrowMarker = mMap.addMarker(new MarkerOptions().position(st.getLatLng()));
                                    locationArrowMarker.setIcon(BitmapDescriptorFactory.fromResource(android.R.drawable.arrow_down_float));
                                    locationArrowMarker.setZIndex(3);

                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(st.getLatLng(), 17));
                                    followingUser = false;
                                }
                            });
                            stationActionsLayout.addView(viewBtn);

                            //a button to view the arrivals
                            ImageButton arrivalsBtn = new ImageButton(NearMeActivity.this);
                            arrivalsBtn.setImageResource(android.R.drawable.ic_menu_recent_history);
                            arrivalsBtn.setLayoutParams(optionButtonParams);
                            arrivalsBtn.setOnClickListener(new Button.OnClickListener() {
                                public void onClick(View v) {
                                    Intent intent = new Intent(NearMeActivity.this, NextBusTrainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.putExtra("lineId", st.getLineId());
                                    intent.putExtra("lineName", st.getLineName());
                                    intent.putExtra("dirId", st.getDirId());
                                    intent.putExtra("stopId", st.getStationId());
                                    intent.putExtra("stopName", st.getStationName());
                                    startActivity(intent);
                                    if (apiClient != null) {
                                        apiClient.disconnect();
                                    }
                                    finish();
                                    startActivity(intent);
                                }
                            });
                            stationActionsLayout.addView(arrivalsBtn);

                            //a button to collapse the display
                            ImageButton collapseBtn = new ImageButton(NearMeActivity.this);
                            collapseBtn.setImageResource(R.drawable.mr_group_collapse);
                            collapseBtn.setLayoutParams(optionButtonParams);
                            collapseBtn.setOnClickListener(new Button.OnClickListener() {
                                public void onClick(View v) {
                                    stationActionsLayout.setVisibility(View.GONE);
                                }
                            });
                            stationActionsLayout.addView(collapseBtn);

                            stationActionsLayout.setVisibility(View.GONE);
                            stationNameLayout.addView(stationActionsLayout);

                            arrivalRow.addView(stationNameLayout, 0);

                            //the box where the arrival info is shown
                            TextView stationArrivalView = new TextView(NearMeActivity.this);
                            stationArrivalView.setMaxWidth(250);
                            String arrivalText = getResources().getString(R.string.loadingellipsis);
                            stationArrivalView.setText(arrivalText);
                            params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
                            params.weight = 0;
                            stationArrivalView.setLayoutParams(params);
                            arrivalRow.addView(stationArrivalView, 1);

                            belowMapLayout.addView(arrivalRow);
                            //set the columns to adjust size appropriately
                            belowMapLayout.setColumnStretchable(0, true);
                            belowMapLayout.setColumnShrinkable(1, false);
                        } else {
                            belowMapLayout.addView(formerRows[index]);
                        }

                        index++;
                    }

                    if (closestStationsOnLine.size() == 1) { //make sure that there are always two table rows for each line
                        belowMapLayout.addView(new TableRow(NearMeActivity.this));
                        listedNearbyStops.put(index, null);
                        index++;
                    }
                }
                highestNearbyStopIndex = index;
                new UpdateNearbyArrivalsTask().execute(indicesToUpdate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void reloadNearbyStops() {
        new GetStopsNearMeTask().execute(mMap.getCameraPosition().target);
    }

    @Override
    public void onCameraMoveStarted(int reason) {

        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            followingUser = false;
        }
    }


    private boolean alreadyVisible = false;
    private final double minZoomLevel = 14;
    Set<NumberPair> spotsAdded = new HashSet<>();
    private Marker mapCenterMarker;
    private Marker locationArrowMarker;
    private BitmapDescriptor favoritePin;
    private BitmapDescriptor busPin;
    private BitmapDescriptor railPin;
    private BitmapDescriptor transferPin;
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if (loadedStops && loadedLines) { //reload the list of nearby stops
            reloadNearbyStops();
            if (updateStopsNearMeTimer == null) {
                startTimer(0);
            }
        }

        if (!followingUser) {
            if (mapCenterMarker != null) {
                mapCenterMarker.remove();
            }
            MarkerOptions options = new MarkerOptions();
            options.position(cameraPosition.target);
            options.icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_add));
            options.anchor(0.5f, 0.5f);
            mapCenterMarker = mMap.addMarker(options);
        } else if (mapCenterMarker != null) {
            mapCenterMarker.remove();
            mapCenterMarker = null;
        }

        if (alreadyVisible && cameraPosition.zoom <= minZoomLevel || !alreadyVisible && cameraPosition.zoom > minZoomLevel) {
            for (Marker m : markers.keySet()) {
                //don't show the icons when zoomed out too much
                m.setVisible((!alreadyVisible) && visibleStations.contains(markers.get(m)));
            }
            for (int key : pathMarkersByLineId.keySet()) {
                for (Marker m : pathMarkersByLineId.get(key)) {
                    m.setVisible(!alreadyVisible && (shownLine == 0 || shownLine == key));
                }
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


            Queue<Station> stationList = new LinkedList<>();
            int minLatInt = (int)Math.floor(minLat / sectorSize);
            int maxLatInt = (int)Math.ceil(maxLat / sectorSize);
            int minLngInt = (int)Math.floor(minLng / sectorSize);
            int maxLngInt = (int)Math.ceil(maxLng / sectorSize);
            for (Iterator it = markerSectors.keySet().iterator(); it.hasNext(); ) {
                NumberPair pos = (NumberPair) it.next();
                if (minLatInt <= pos.first && pos.first <= maxLatInt && minLngInt <= pos.second && pos.second <= maxLngInt) {
                    if (spotsAdded.add(pos)) {
                        for (Station st : markerSectors.get(pos)) {
                            stationList.add(st);
                            Marker m = mMap.addMarker(new MarkerOptions().position(st.getLatLng()));
                            if (st.getStationId() == focusStationId) {
                                locationArrowMarker = mMap.addMarker(new MarkerOptions().position(st.getLatLng()));
                                locationArrowMarker.setIcon(BitmapDescriptorFactory.fromResource(android.R.drawable.arrow_down_float));
                                locationArrowMarker.setZIndex(3);
                            }

                            if (favoriteStations.contains(st)) { //mark with a star if it's a favorite
                                m.setIcon(favoritePin);
                                m.setZIndex(3);
                            } else if (shownLine == st.getLineId() && st.isTransfer()) { //if only showing one line, mark transfers
                                m.setIcon(transferPin);
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
                        it.remove();
                    }
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
