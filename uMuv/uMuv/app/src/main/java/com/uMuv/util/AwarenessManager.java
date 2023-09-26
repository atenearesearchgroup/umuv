package com.uMuv.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.SnapshotClient;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.snapshot.BeaconStateResponse;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.awareness.state.BeaconState;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.uMuv.app.MapActivity;

import java.util.Arrays;
import java.util.List;

public class AwarenessManager {
    private static AwarenessManager instance = null;
    private static final int MY_PERMISSION_LOCATION = 1;
    private static final int EARTH_R = 6371 * 1000;
    private Location loc;
    private Task finished;
    private static final List BEACON_TYPE_FILTERS = Arrays.asList(
            BeaconState.TypeFilter.with(
                    "5e6f0efe3a2fa83d28eb",
                    "5e9c336c212b"));

    protected AwarenessManager() {

    }

    public static com.uMuv.util.AwarenessManager getSharedInstance() {
        if (instance == null) {
            instance = new com.uMuv.util.AwarenessManager();
        }

        return instance;
    }
    public void getLocation(Context context, Activity activity, View view) {
        finished = null;
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_LOCATION
            );
        }
        Awareness.getSnapshotClient(activity).getLocation()
                .addOnSuccessListener(new OnSuccessListener<LocationResponse>() {
                    @Override
                    public void onSuccess(LocationResponse lr) {
                        Location loc = lr.getLocation();
                        double latParada3 = 36.691048;
                        double longParada3 = -4.4532385;
                        String msg = "Latitude: " + loc.getLatitude() + ", Longitude: " + loc.getLongitude() + "\n"
                                + "Distance To Line 3 First Stop: " + AwarenessManager.this.getDistance(loc.getLatitude(), loc.getLongitude(), latParada3, longParada3);
                        Log.println(Log.VERBOSE, "POSITION", msg);
                        if (view instanceof TextView) {
                            TextView textView = (TextView) view;
                            textView.setText(msg);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.println(Log.VERBOSE, "ERROR", e.toString());
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<LocationResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<LocationResponse> task) {
                        finished = task;

                    }
                });
    }

    public SnapshotClient getSnapshotClient(Activity activity){
        return Awareness.getSnapshotClient(activity);
    }


    protected double getDistance(double lat1, double lon1, double lat2, double lon2){

        double dLat = deg2rad(lat2 - lat1);
        double dLon = deg2rad(lon2 - lon1);
        double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2)
                ;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = this.EARTH_R * c;
        return d;
    }

    private double deg2rad(double deg){
        return deg * (Math.PI/180);
    }
    public void getBeacons(Context context, Activity activity, View view) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_LOCATION
            );
        }

        Awareness.getSnapshotClient(activity).getBeaconState(BEACON_TYPE_FILTERS)
                .addOnSuccessListener(new OnSuccessListener<BeaconStateResponse>() {
                    @Override
                    public void onSuccess(BeaconStateResponse beaconStateResponse) {
                        BeaconState beaconStateResult = beaconStateResponse.getBeaconState();
                        Log.println(Log.VERBOSE, "BEACON", beaconStateResult.toString());
                    }
                });

    }

    public void registerWalkingDrivingFence(){
        AwarenessFence walkingFence = DetectedActivityFence.during(DetectedActivityFence.WALKING);
        AwarenessFence drivingFence = DetectedActivityFence.during(DetectedActivityFence.IN_VEHICLE);
        AwarenessFence userActivity = AwarenessFence.or(drivingFence, walkingFence);
    }
}
