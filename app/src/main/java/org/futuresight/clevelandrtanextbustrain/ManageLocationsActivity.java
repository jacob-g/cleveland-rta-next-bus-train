package org.futuresight.clevelandrtanextbustrain;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class ManageLocationsActivity extends AppCompatActivity {
    private class FavoriteStationListItemActivity extends RelativeLayout {
        Button mainBtn;
        ImageButton editBtn, deleteBtn;
        Station station;
        Context context;
        public FavoriteStationListItemActivity(Context c, Station s) {
            super(c);

            station = s;
            context = c;

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

            RelativeLayout.LayoutParams lp;
            editBtn = new ImageButton(context);
            editBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_edit));

            lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_END, editBtn.getId());
            lp.addRule(RelativeLayout.LEFT_OF, editBtn.getId());
            lp.addRule(RelativeLayout.ALIGN_PARENT_START);
            this.addView(mainBtn, lp);

            lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_START, mainBtn.getId());
            lp.addRule(RelativeLayout.RIGHT_OF, mainBtn.getId());
            lp.addRule(RelativeLayout.ALIGN_PARENT_END);
            this.addView(editBtn, lp);
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
            FavoriteStationListItemActivity t = new FavoriteStationListItemActivity(this, s);
            favoriteListLayout.addView(t);
        }
        db.close();
    }
}
