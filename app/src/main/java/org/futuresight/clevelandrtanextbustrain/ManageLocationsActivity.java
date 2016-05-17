package org.futuresight.clevelandrtanextbustrain;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class ManageLocationsActivity extends AppCompatActivity {
    private class FavoriteStationListItemActivity extends RelativeLayout {
        Button mainBtn;
        ImageButton editBtn, deleteBtn;
        Station station;
        Context context;
        LinearLayout parent;
        public FavoriteStationListItemActivity(Context c, Station s, LinearLayout p) {
            super(c);

            station = s;
            context = c;
            parent = p;

            mainBtn = new Button(context);
            mainBtn.setText(station.getName());
            mainBtn.setOnClickListener(new AdapterView.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent(context, NextBusTrainActivity.class);
                    intent.putExtra("lineId", station.getLineId());
                    intent.putExtra("lineName", station.getLineName());
                    intent.putExtra("dirId", station.getDirId());
                    intent.putExtra("dirName", station.getDirName());
                    intent.putExtra("stopId", station.getStationId());
                    intent.putExtra("stopName", station.getStationName());
                    startActivity(intent);
                }
            });
            mainBtn.setId(3000);

            RelativeLayout.LayoutParams lp;
            editBtn = new ImageButton(context);
            editBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_edit));
            editBtn.setId(3001);

            deleteBtn = new ImageButton(context);
            deleteBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_delete));
            deleteBtn.setOnClickListener(new AdapterView.OnClickListener() {
                public void onClick(View view) {
                    final AlertDialog.Builder inputAlert = new AlertDialog.Builder(view.getContext());
                    inputAlert.setTitle("Delete Favorite");
                    inputAlert.setMessage("Are you sure you want to delete the station \"" + station.getName() + "\"?");
                    inputAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DatabaseHandler db = new DatabaseHandler(context);
                            db.deleteFavoriteStation(station);
                            db.close();
                            destroyMyself();
                            dialog.dismiss();
                        }
                    });
                    inputAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
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
            editBtn.setLayoutParams(lp);
            this.addView(editBtn, lp);

            lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_END);
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

        //populate the layout with the items
        LinearLayout favoriteListLayout = (LinearLayout) findViewById(R.id.favoriteLocationsListView);
        DatabaseHandler db = new DatabaseHandler(this);
        List<Station> stl = db.getFavoriteLocations();
        for (Station s : stl) {
            FavoriteStationListItemActivity t = new FavoriteStationListItemActivity(this, s, favoriteListLayout);
            favoriteListLayout.addView(t);
        }
        db.close();
    }
}
