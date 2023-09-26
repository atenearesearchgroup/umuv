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

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class UsualStopsActivity extends AppCompatActivity {
    private ListView listView;
    private DatabaseManager dbMgr;
    private Dictionary avatar;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usual_stops);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.stopsListView);

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
                Dictionary stopListDic = avatar.getDictionary("stopList");
                if(stopListDic != null) {
                    Array stopListArray = stopListDic.getArray("list");
                    List stopList = stopListArray.toList();
                    ArrayList<String> stopsToShow = new ArrayList<>();
                    int stopNumber = 1;
                    for (Object stop : stopList) {
                        HashMap stopDictionary = (HashMap) stop;

                        String stopToAdd = "Stop Number: " + stopDictionary.get("stopId") + "\nCount: " + stopDictionary.get("count");

                        stopsToShow.add(stopToAdd);
                        stopNumber++;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, stopsToShow);
                    listView.setAdapter(adapter);
                }else{
                    ArrayList<String> noStops = new ArrayList<>();
                    noStops.add("No stops to show");
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, noStops);
                    listView.setAdapter(adapter);
                }
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }
}