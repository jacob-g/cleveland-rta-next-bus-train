package org.futuresight.clevelandrtanextbustrain;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
            //((WebView) rootView.findViewById(R.id.maptype_image)).loadDataWithBaseURL("file:///android_asset/", "<img src=\"" + mItem.imageId + ".svg\" />", "text/html", "utf-8", null);
            final WebView wv = ((WebView) rootView.findViewById(R.id.maptype_image));
            //TODO: make the webview react appropriately when an item is clicked
            /*wv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println(wv.getUrl());
                }
            });*/

            wv.getSettings().setJavaScriptEnabled(true);
            wv.getSettings().setDomStorageEnabled(true);
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
            listChooseDialog(directions, dirIds, lineId, stationId, name);
        }

        private void listChooseDialog(String[] options, final int[] dirIds, final int lineId, final int stationId, String station) {
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
            builder.setTitle(station + "\nWhich direction?").setItems(options, new DialogInterface.OnClickListener() {
               @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                   Intent intent = new Intent(context, NextBusTrainActivity.class);
                   intent.putExtra("stopId",stationId);
                   intent.putExtra("lineId",lineId);
                   intent.putExtra("dirId",dirIds[i]);
                   startActivity(intent);
               }
            });
            builder.create();
            builder.show();
        }
    }
}
