package com.uMuv.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

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
import com.uMuv.util.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ViewRideActivity extends AppCompatActivity {
    private DatabaseManager dbMgr;
    private Dictionary avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_ride);

        int index = getIntent().getIntExtra("selectedRideIndex", -1);

        dbMgr = DatabaseManager.getSharedInstance();

        TextView textView = findViewById(R.id.textViewPruebaRide);

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
                Array rideListArray = rideListDic.getArray("list");
                Dictionary rideToShow = rideListArray.getDictionary(index);
                Array pathAfter = rideToShow.getArray("positionsAfter");
                ArrayList<Position> positionsArray = new ArrayList<>();
                List positionList = pathAfter.toList();
                for(Object object : positionList){
                    HashMap positionParsed = (HashMap) object;
                    positionsArray.add(new Position((Double)positionParsed.get("lat"), (Double)positionParsed.get("lon")));
                }
                String rideToAdd = "Ride #" + index + "\nLine #" + rideToShow.getInt("lineId") + ". From " +
                        rideToShow.getInt("stop0") + " to " + rideToShow.getInt("stop1") + ".\nStart Time: " +
                        rideToShow.getDate("timeStart") + ". Finish Time: " + rideToShow.getDate("timeFinish") +
                        ".\nLocation After: " + positionsArray.toString();
                textView.setText(rideToAdd);
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

    }
}