package org.futuresight.clevelandrtanextbustrain;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ScheduleActivity extends AppCompatActivity {
    private Button.OnClickListener onDatePickerClickedListener = new Button.OnClickListener() {
        public void onClick(View v) {
            int day = Integer.parseInt(new SimpleDateFormat("dd").format(selectedDate));
            int month = Integer.parseInt(new SimpleDateFormat("MM").format(selectedDate));
            int year = Integer.parseInt(new SimpleDateFormat("YYYY").format(selectedDate));

            new DatePickerDialog(ScheduleActivity.this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    selectedDate = new Date(year - 1900, month, dayOfMonth);
                    SimpleDateFormat df = new SimpleDateFormat("MMM dd, YYYY");
                    String formattedDate = df.format(selectedDate);
                    ((Button)findViewById(R.id.selectDateBtn)).setText(formattedDate);
                    new GetScheduleTask(ScheduleActivity.this, false).execute();
                }
            }, year, month - 1, day).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!getIntent().hasExtra("lineId") || !getIntent().hasExtra("dirId") || !getIntent().hasExtra("stationId") || !getIntent().hasExtra("line") || !getIntent().hasExtra("dir") || !getIntent().hasExtra("station")) {
            finish();
        }

        (findViewById(R.id.selectDateBtn)).setOnClickListener(onDatePickerClickedListener);
        new GetScheduleTask(ScheduleActivity.this, true).execute();
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private Date selectedDate;

    private class GetScheduleTask extends AsyncTask<Void, List<String[]>, List<String[]>> {
        private final Context myContext;
        private final boolean starting;
        public GetScheduleTask(Context context, boolean starting) {
            myContext = context;
            this.starting = starting;
            findViewById(R.id.noScheduleBox).setVisibility(View.GONE);
            findViewById(R.id.loadingBox).setVisibility(View.VISIBLE);
        }
        protected List<String[]> doInBackground(Void... params) {
            int lineId = getIntent().getExtras().getInt("lineId");
            int dirId = getIntent().getExtras().getInt("dirId");
            int stationId = getIntent().getExtras().getInt("stationId");

            if (starting) {
                Calendar c = Calendar.getInstance();
                selectedDate = c.getTime();
            }

            int day = Integer.parseInt(new SimpleDateFormat("dd").format(selectedDate));
            int month = Integer.parseInt(new SimpleDateFormat("MM").format(selectedDate));
            int year = Integer.parseInt(new SimpleDateFormat("YYYY").format(selectedDate));

            List<String[]> out = new LinkedList<>();

            String scheduleXML = NetworkController.basicHTTPRequest("https://nexttrain.futuresight.org/api/getschedule?route=" + lineId + "&stop=" + stationId + "&dir=" + dirId + "&date=" + month + "/" + day + "/" + year);
            try {
                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(scheduleXML)));
                Node rootNode = doc.getDocumentElement();

                if (doc.hasChildNodes()) {
                    NodeList nl = rootNode.getChildNodes();
                    for (int i = 0; i < nl.getLength(); i++) {
                        Node curNode = nl.item(i);
                        if (curNode.getNodeName().equals("d")) {
                            String dest = curNode.getAttributes().getNamedItem("dest").getTextContent();
                            String time = curNode.getAttributes().getNamedItem("time").getTextContent();
                            out.add(new String[]{dest, time});
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return out;
        }

        protected void onPostExecute(List<String[]> result) {
            //if starting the activity, then initialize the fields
            if (starting) {
                ((TextView) findViewById(R.id.lineBox)).setText(getIntent().getExtras().getString("line"));
                ((TextView) findViewById(R.id.directionBox)).setText(getIntent().getExtras().getString("dir"));
                ((TextView) findViewById(R.id.stationBox)).setText(getIntent().getExtras().getString("station"));

                SimpleDateFormat df = new SimpleDateFormat("MMM dd, YYYY");
                String formattedDate = df.format(selectedDate);

                ((Button) findViewById(R.id.selectDateBtn)).setText(formattedDate);
            }

            findViewById(R.id.loadingBox).setVisibility(View.GONE);

            ((TableLayout)findViewById(R.id.scheduleListTable)).removeAllViews();
            if (result.isEmpty()) {
                findViewById(R.id.noScheduleBox).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.noScheduleBox).setVisibility(View.GONE);
                for (String[] arr : result) {
                    TableRow newRow = new TableRow(myContext);
                    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                    newRow.setLayoutParams(params);
                    String[] timeParts = arr[1].split(":");
                    int hours = Integer.parseInt(timeParts[0]);
                    String period;
                    if (hours == 0) {
                        hours += 12;
                        period = "am";
                    } else if (hours == 12) {
                        period = "pm";
                    } else if (hours > 12 && hours < 24) {
                        hours -= 12;
                        period = "pm";
                    } else if (hours >= 24) {
                        hours -= 24;
                        period = "am";
                    } else {
                        period = "am";
                    }
                    TextView timeView = new TextView(myContext);
                    timeView.setText(hours + ":" + timeParts[1] + period);
                    timeView.setGravity(Gravity.CENTER_HORIZONTAL);
                    newRow.addView(timeView);

                    TextView destView = new TextView(myContext);
                    destView.setText(arr[0]);
                    destView.setGravity(Gravity.CENTER_HORIZONTAL);
                    newRow.addView(destView);

                    ((TableLayout) findViewById(R.id.scheduleListTable)).addView(newRow);
                }
            }
        }
    }

}
