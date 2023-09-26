package com.uMuv.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.couchbase.lite.Array;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDictionary;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.uMuv.app.MapActivity;
import com.uMuv.app.R;

import org.apache.commons.lang3.mutable.MutableObject;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class uMuvAlgorithm {
    private static final int MAX_POST_POINTS = 2;
    private static final int LOCATION_REQUEST_DEFAULT_INTERVAL = 60;
    private static final int LOCATION_REQUEST_FAST_INTERVAL = 60;
    private static uMuvAlgorithm instance = null;
    private boolean busDetected, stopDetected, startedFollowUp = false;
    private Bus bus;
    private Stop startStop, lastStop, closestStop;
    private Date startDate, finishDate;
    private String[] status = {"X", "S", "B", "U", "D"};
    private int statusIndex, positionIndex = 0;
    private List<Position> positionsAfter = new ArrayList<>();
    private DatabaseManager dbMgr = DatabaseManager.getSharedInstance();
    private DatosPublicosManager dpMgr = DatosPublicosManager.getSharedInstance();
    private AwarenessManager awarenessManager = AwarenessManager.getSharedInstance();
    private Dictionary avatar;
    private MutableDocument avatarMutDoc;
    private Array stopList;
    private Document avatarOriginalDoc;
    private Activity currentActivity;
    private Location userPosition;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private LocationCallback locationCallback;

    public static uMuvAlgorithm getSharedInstance(Activity activity) {
        if (instance == null) {
            instance = new uMuvAlgorithm(activity);
        }else{
            instance.setCurrentActivity(activity);
        }

        return instance;
    }

    public uMuvAlgorithm(Activity activity) {
        this.statusIndex = 0;
        this.currentActivity = activity;
        this.setClosestStop();
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public boolean isBusDetected() {
        return busDetected;
    }

    public void setBusDetected(boolean busDetected, Bus bus) {
        this.busDetected = busDetected;
        this.checkStatus(bus, null);
    }

    public boolean isStopDetected() {
        return stopDetected;
    }

    public void setStopDetected(boolean stopDetected, Stop stop) {
        this.stopDetected = stopDetected;
        this.checkStatus(null, stop);
    }
    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public Stop getStartStop() {
        return startStop;
    }

    public void setStartStop(Stop startStop) {
        this.startStop = startStop;
    }

    public Stop getLastStop() {
        return lastStop;
    }

    public void setLastStop(Stop lastStop) {
        this.lastStop = lastStop;
    }

    public void checkStatus(Bus bus, Stop stop){
        String currentStatus = this.status[this.statusIndex];

        switch (currentStatus){
            case "X":
                if(this.stopDetected && stop != null) {
                    this.setStartStop(stop);
                    this.statusIndex++;
                }
                break;
            case "S":
                if(this.busDetected && bus != null) {
                    this.setBus(bus);
                    this.statusIndex++;
                }
                else if(!this.stopDetected) {
                    this.setStartStop(null);
                    this.statusIndex--;
                }
                break;
            case "B":
                if(!busDetected) {
                    this.setBus(null);
                    this.statusIndex--;
                }
                else if(!stopDetected) {
                    this.startDate = new Date();
                    this.statusIndex++;
                }
                break;
            case "U":
                if(!busDetected && stopDetected && stop != null) {
                    this.setLastStop(stop);
                    statusIndex++;
                }
                break;
            case "D":
                if(!startedFollowUp) {
                    Toast.makeText(MyApplication.getAppContext(), "Finished Ride, starting follow up", Toast.LENGTH_LONG).show();
                    this.startedFollowUp = true;
                    this.positionsAfter = new ArrayList<>();
                    this.finishDate = new Date();
                    this.locationAfterStopping();
                }
                break;
            default:
                break;


        }
    }

    private void resetDetections() {
        this.setBus(null);
        this.setStartStop(null);
        this.setLastStop(null);
    }

    public String getCurrentStatus(){
        return this.status[this.statusIndex];
    }

    @Override
    public String toString() {
        String res = "Current Status: ";
        switch (this.getCurrentStatus()){
            case "X":
                res += " No stops have been detected yet.\n";
                res += closestStop == null ? "No rides have been done yet" : ("Most used stop: " + closestStop.getCodStop());
                break;
            case "S":
                res += " Stop " + this.startStop.getIdStop() + " detected. \n" + "Press this URL to get more info: " + this.startStop.getUrl() + ".";
                break;
            case "B":
                res += " Bus #" + this.bus.getCodBus() + " from line " + this.bus.getCodLinea() +  " detected.\n" + "Press the following URL to get more info:\n" + this.startStop.getUrl();
                break;
            case "U":
                res += "Aboard of  Bus #" + this.bus.getCodBus() + " from line " + this.bus.getCodLinea();
                break;
            case "D":
                res += " Bus ride ended, finishing ride.";
                break;
            default:
                res += " Nothing to show";
                break;


        }
        return res;
    }

    public void locationAfterStopping(){
            final Handler handler = new Handler();
            Timer timer = new Timer();

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            Position p = new Position(userPosition.getLatitude(), userPosition.getLongitude());
                            positionsAfter.add(p);
                            Toast.makeText(MyApplication.getAppContext(), "Added position to list", Toast.LENGTH_LONG).show();
                            Log.println(Log.VERBOSE, "FOLLOW UP", "NEW MARK");

                            positionIndex++;

                            if(positionIndex >= MAX_POST_POINTS){
                                saveRide();
                                timer.cancel();
                                timer.purge();
                            }
                        }
                    });
                }
            };
            timer.schedule(task, 0, 60 * 1000);  // interval of two secs

    }

    public void resetStatus(){
        this.statusIndex = 0;
        this.startedFollowUp = false;
        this.positionsAfter = new ArrayList<>();
        this.positionIndex = 0;
        this.finishDate = null;
        this.startDate = null;
        this.bus = null;
        this.startStop = null;
        this.lastStop = null;
    }
    public void saveRide(){
        Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id), SelectResult.all())
                .from(DataSource.database(dbMgr.getDatabase()))
                .where(Expression.property("type").equalTo(Expression.string("avatar")));

        try {
            ResultSet rs = query.execute();
            List<Result> resultList = rs.allResults();
            int size = resultList.size();
            if(size> 0) {
                Result result = resultList.get(0);

                //Get Avatar MutableDocument
                String id = result.getString(0);
                this.avatarOriginalDoc = dbMgr.getDocumentById(id);
                this.avatarMutDoc = this.avatarOriginalDoc.toMutable();

                //Update stopList
                MutableDictionary stopListDic = this.avatarMutDoc.getDictionary("stopList");
                MutableArray stopList = stopListDic == null? new MutableArray() : stopListDic.getArray("list");
                int stopIndex = 0;
                for(Object dictionary : stopList.toList()){
                    Log.e("LIST: ", dictionary.toString());
                    Map dic = (Map) dictionary;

                    if(Math.toIntExact((long)dic.get("stopId")) == this.startStop.getIdStop()){
                        break;
                    }
                    stopIndex++;
                }

                if(stopIndex < stopList.count()){
                    MutableDictionary stopUpdated = stopList.getDictionary(stopIndex);
                    stopUpdated.setInt("count", stopUpdated.getInt("count")+1);
                    stopList.remove(stopIndex);
                    stopList.addDictionary(stopUpdated);
                    Log.e("STOP UPDATED: ", stopUpdated.toString());
                }else{
                    MutableDictionary stopAddition = new MutableDictionary();
                    stopAddition.setInt("stopId", this.startStop.getIdStop());
                    stopAddition.setInt("count", 1);
                    stopList.addDictionary(stopAddition);
                }
                Log.e("stopIndex", ""+stopIndex);

                if(stopListDic == null)
                    stopListDic = new MutableDictionary();

                stopListDic.setArray("list", stopList);
                this.avatarMutDoc.setDictionary("stopList", stopListDic);
                Log.e("AVATAR: ", this.avatarMutDoc.toString());


                //Update rideList
                MutableDictionary rideListDic = this.avatarMutDoc.getDictionary("rideList");
                MutableArray rideList = rideListDic == null ? new MutableArray() : rideListDic.getArray("list");
                MutableDictionary ride = new MutableDictionary();
                ride.setInt("stop0", this.startStop.getIdStop());
                ride.setInt("stop1", this.lastStop.getIdStop());
                ride.setInt("busId", this.bus.getCodBus());
                ride.setInt("lineId", this.bus.getCodLinea());
                ride.setDate("timeStart", this.startDate);
                ride.setDate("timeFinish", this.finishDate);
                MutableArray positionsAfter = new MutableArray();
                for(Position p : this.positionsAfter){
                    MutableDictionary posForArray = new MutableDictionary();
                    posForArray.setDouble("lat", p.latitude);
                    posForArray.setDouble("lon", p.longitude);
                    positionsAfter.addDictionary(posForArray);
                }
                ride.setArray("positionsAfter", positionsAfter);
                rideList.addDictionary(ride);
                if(rideListDic == null)
                    rideListDic = new MutableDictionary();
                rideListDic.setArray("list", rideList);
                this.avatarMutDoc.setDictionary("rideList", rideListDic);

                //Save Doc
                dbMgr.getDatabase().save(this.avatarMutDoc);
                Toast.makeText(MyApplication.getAppContext(), "Ride saved!", Toast.LENGTH_LONG).show();
                MapActivity.resetStatusAfterRideSaved();
                this.resetStatus();

            }

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public void setUserPosition(Location location){
        this.userPosition = location;
    }
    public void setClosestStop(){
        Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id), SelectResult.all())
                .from(DataSource.database(dbMgr.getDatabase()))
                .where(Expression.property("type").equalTo(Expression.string("avatar")));

        try {
            ResultSet rs = query.execute();
            List<Result> resultList = rs.allResults();
            int size = resultList.size();
            if(size> 0) {
                Result result = resultList.get(0);

                //Get Avatar MutableDocument
                String id = result.getString(0);
                this.avatarOriginalDoc = dbMgr.getDocumentById(id);
                this.avatarMutDoc = this.avatarOriginalDoc.toMutable();

                //Get stopList
                Dictionary stopListDic = this.avatarOriginalDoc.getDictionary("stopList");
                if(stopListDic == null){
                    this.closestStop = null;
                } else{
                    this.stopList = stopListDic.getArray("list");

                    List<Object> stops = this.stopList.toList();
                    int count = 0;
                    for(Object stopObject : stops){
                        HashMap stopInArray = (HashMap) stopObject;
                        int stopCode = Math.toIntExact((long)stopInArray.get("stopId"));
                        Stop stop = dpMgr.getParadaInfo(stopCode);

                        if(this.closestStop == null && stop != null) {
                            this.closestStop = stop;
                            count = Math.toIntExact((long)stopInArray.get("count"));
                        }else if(stop != null && count < Math.toIntExact((long)stopInArray.get("count"))){
//                                && currentDistance > dpMgr.getDistance(userPosition.getLatitude(), userPosition.getLongitude(), stop.getLat(), stop.getLon())){
                            this.closestStop = stop;
                            count = Math.toIntExact((long)stopInArray.get("count"));
                        }
                    }
                }

            }

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

    }
}
