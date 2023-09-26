package com.uMuv.app;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.couchbase.lite.Array;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.uMuv.util.DatabaseManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class RidesActivity extends AppCompatActivity {
    private ListView listView;
    private DatabaseManager dbMgr;
    private Dictionary avatar;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rides);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.ridesListView);

        this.dbMgr = DatabaseManager.getSharedInstance();

        Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id), SelectResult.all())
                .from(DataSource.database(dbMgr.getDatabase()))
                .where(Expression.property("type").equalTo(Expression.string("avatar")));

        try {
            ResultSet rs = query.execute();
            List<Result> resultList = rs.allResults();
            int size = resultList.size();
            if (size > 0) {
                Result result = resultList.get(0);
                this.avatar = result.getDictionary(dbMgr.getDbName());
                Dictionary rideListDic = avatar.getDictionary("rideList");
                if(rideListDic != null) {
                    Array rideListArray = rideListDic.getArray("list");
                    List rideList = rideListArray.toList();
                    ArrayList<String> ridesToShow = new ArrayList<>();
                    int rideNumber = 1;
                    for (Object ride : rideList) {
                        HashMap rideDictionary = (HashMap) ride;
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
                        Instant startInstant = Instant.parse(rideDictionary.get("timeStart").toString());
                        Date startDate = Date.from(startInstant);

                        String rideToAdd = "Ride #" + rideNumber + "\nLine #" + rideDictionary.get("lineId")
                                + ". From " + rideDictionary.get("stop0") + " ( " + rideDictionary.get("timeStart")
                                + ") to " + rideDictionary.get("stop1") + "(" + rideDictionary.get("timeFinish") + ") " + startDate;
                        ridesToShow.add(rideToAdd);
                        rideNumber++;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ridesToShow);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> a, View v, int position,
                                                long id) {
                            Intent intent = new Intent(getApplicationContext(), ViewRideActivity.class);

                            intent.putExtra("selectedRideIndex", position);
                            startActivity(intent);
                        }
                    });
                }else{
                    ArrayList<String> noRides = new ArrayList<>();
                    noRides.add("No rides to show");
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, noRides);
                    listView.setAdapter(adapter);
                }

            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }
}