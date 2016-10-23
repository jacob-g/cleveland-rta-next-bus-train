package org.futuresight.clevelandrtanextbustrain;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * A fragment representing a single MapType detail screen.
 * This fragment is either contained in a {@link MapTypeListActivity}
 * in two-pane mode (on tablets) or a {@link MapTypeDetailActivity}
 * on handsets.
 */
public class MapTypeDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private MapType.Item mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MapTypeDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = MapType.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(mItem.content);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.maptype_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.maptype_detail)).setText(mItem.details);
            final WebView wv = ((WebView) rootView.findViewById(R.id.maptype_image));
            //enable JS and allow the JS to communicate with this file
            wv.getSettings().setJavaScriptEnabled(true);
            wv.getSettings().setDomStorageEnabled(true);
            wv.getSettings().setBuiltInZoomControls(true); //allow zooming
            wv.addJavascriptInterface(new WebAppInterface(this.getContext(), this), "appInterface");
            wv.loadUrl("file:///android_asset/" + mItem.imageId + ".html");

        }

        return rootView;
    }

    public class WebAppInterface {
        Context context;
        Fragment fragment;
        public WebAppInterface(Context c, Fragment f) {
            context = c;
            fragment = f;
        }

        @JavascriptInterface
        public void out(String out) {
            System.out.println(out);
        }

        @JavascriptInterface
        public void goToStation(String name, String[] directions, int[] dirIds, int stationId, int lineId) {
            listChooseDialog(directions, dirIds, new int[]{lineId, lineId}, new int[]{stationId, stationId}, name);
        }

        @JavascriptInterface
        public void goToStationMultipleDirs(String name, String[] directions, int[] dirIds, int[] stationIds, int lineId) {
            listChooseDialog(directions, dirIds, new int[]{lineId, lineId}, stationIds, name);
        }

        @JavascriptInterface
        public void goToStationMultipleLines(String name, String[] directions, int[] dirIds, int[] stationIds, int[] lineIds) {
            listChooseDialog(directions, dirIds, lineIds, stationIds, name);
        }

        private void listChooseDialog(String[] options, final int[] dirIds, final int[] lineIds, final int[] stationIds, String station) {
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
            builder.setTitle(station + "\n" + getResources().getText(R.string.whichdir)).setItems(options, new DialogInterface.OnClickListener() {
               @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                   Intent intent = new Intent(context, NextBusTrainActivity.class);
                   intent.putExtra("stopId",stationIds[i]);
                   intent.putExtra("lineId",lineIds[i]);
                   intent.putExtra("dirId",dirIds[i]);
                   startActivity(intent);
               }
            });
            builder.create();
            builder.show();
        }
    }
}
