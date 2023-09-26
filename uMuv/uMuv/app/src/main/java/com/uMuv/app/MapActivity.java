package com.uMuv.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.ArrayFunction;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Function;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.onesignal.OSInAppMessageAction;
import com.onesignal.OneSignal;
import com.uMuv.util.AwarenessManager;
import com.uMuv.util.Bus;
import com.uMuv.util.DatabaseManager;
import com.uMuv.util.DatosPublicosManager;
import com.uMuv.util.MyApplication;
import com.uMuv.util.Stop;
import com.uMuv.util.VolleyCallBack;
import com.uMuv.util.uMuvAlgorithm;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.jetbrains.annotations.NotNull;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity implements BeaconConsumer {
    public static final int LOCATION_REQUEST_DEFAULT_INTERVAL = 8;
    private static final String hexWorkspace = "0x5e6f0efe3a2fa83d28eb";
    private static final int TIME_TO_STOP_DETECTING = 10000;
    private static final int RESULT_OK_CHANGED_AVATAR = 1;
    private static final String FENCE_KEY = "FENCE_KEY_1998";
    public static final int LOCATION_REQUEST_FAST_INTERVAL = 5;
    private PendingIntent mPendingIntent;
    protected static final String TAG = "MapActivity";
    private MapView mapView;
    private MapController mapController;
    private AwarenessManager awarenessManager;
    private GeoPoint posicionActual;
    private DatosPublicosManager datosPublicosManager;
    private Drawable busDrawable, busStop, position;
    public static Marker positionMarker;
    private List<Marker> busMarkers;
    private boolean notTheFirstTime;
    private BeaconManager beaconManager;
    private static TextView textViewMap;
    private static com.uMuv.util.uMuvAlgorithm uMuvAlgorithm;

    private Timer stopTimer, busTimer;
    private TimerTask stopTask, busTask;
    private final Handler handler = new Handler();

    private boolean stopTimerStarted, busTimerStarted;
    private String fenceStateStr;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private LocationCallback locationCallback;

    private DatabaseManager dbMgr = DatabaseManager.getSharedInstance();
    private MutableDocument avatarMutDoc;
    private Document avatarOriginalDoc;
    private static final String TYPE_AVATAR = "type";
    private static final String PUBLIC_AVATAR = "Public";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        notTheFirstTime = false;
        Toast.makeText(MyApplication.getAppContext(), "Charging map...", Toast.LENGTH_SHORT).show();

        // Datos publicos e iconos
        this.datosPublicosManager = DatosPublicosManager.getSharedInstance();
        this.busStop = ResourcesCompat.getDrawable(getResources(), R.drawable.bus_stop_config, null);
        this.position = ResourcesCompat.getDrawable(getResources(), R.drawable.user_position, null);
        this.busDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.bus_config, null);
        this.busMarkers = new ArrayList<>();
        datosPublicosManager.getUrlParadasLineasWithCallback(new VolleyCallBack() {
            @Override
            public void onSuccess() {
                uMuvAlgorithm.setClosestStop();
                setMapStops();
            }
        });
        datosPublicosManager.getUrlBuses(new VolleyCallBack() {
            @Override
            public void onSuccess() {
                setMapBuses();
            }
        });


        // Algoritmo
        this.uMuvAlgorithm = uMuvAlgorithm.getSharedInstance(this);
        this.textViewMap = findViewById(R.id.textViewMap);
        this.textViewMap.setMovementMethod(new ScrollingMovementMethod());
        this.textViewMap.setText(uMuvAlgorithm.toString());

        //Awareness
        awarenessManager = AwarenessManager.getSharedInstance();

        //Map setup
        Configuration.getInstance().setUserAgentValue("TMGR"); // CAMBIAR POR AVATAR
        this.mapView = (MapView) findViewById(R.id.openmapview);
        this.mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        this.mapView.setMultiTouchControls(true);
        this.mapController = (MapController) this.mapView.getController();
        this.mapController.setZoom(18);
        this.setRepeatingAsyncTask();

        // Beacons
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.bind(this);

        //Timers
        this.stopTimer = new Timer();
        this.stopTimerStarted = false;
        this.stopTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        uMuvAlgorithm.setStopDetected(false, null);
                        textViewMap.setText(uMuvAlgorithm.toString() );
                        Log.i(TAG, "Stopped Detecting any Stops");
                        stopTimerStarted = false;
//                        stopTimer = new Timer();
                    }
                });
            }
        };
        this.busTimer = new Timer();
        this.busTimerStarted = false;
        this.busTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        uMuvAlgorithm.setBusDetected(false, null);
                        textViewMap.setText(uMuvAlgorithm.toString());
                        Log.i(TAG, "Stopped Detecting any Buses");
                        busTimerStarted = false;
//                        stopTimer = new Timer();
                    }
                });
            }
        };

        // GPS
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * LOCATION_REQUEST_DEFAULT_INTERVAL);
        locationRequest.setFastestInterval(1000 * LOCATION_REQUEST_FAST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                setCurrentLocation(locationResult.getLastLocation());
                uMuvAlgorithm.setUserPosition(locationResult.getLastLocation());
                textViewMap.setText(uMuvAlgorithm.toString());
            }
        };
        updateGPS();


    }

    private void updateGPS(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }else{
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        }
    }

    public void centerMap(View view) {
        this.mapController.setCenter(this.posicionActual);
        this.mapController.setZoom(18);
    }

    private void setCurrentLocation(Location location){
        if(this.posicionActual == null) {
            this.posicionActual = new GeoPoint(location);
            this.mapController.setCenter(this.posicionActual);
        }else
            this.posicionActual.setCoords(location.getLatitude(), location.getLongitude());

        if(isDestroyed())
            return;
        if(this.positionMarker == null){
            this.positionMarker = new Marker(this.mapView);
            this.positionMarker.setPosition(this.posicionActual);
            this.positionMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            this.positionMarker.setIcon(this.position);
        }else
            this.positionMarker.setPosition(this.posicionActual);


        mapView.getOverlays().add(this.positionMarker);
        mapView.invalidate();
    }

    private void setMapStops(){
        for(Stop stop : this.datosPublicosManager.getStopList()){
            if(!isDestroyed()) {
                try {
                    Marker marker = new Marker(this.mapView);

                    double latitud = stop.getLat();
                    double longitud = stop.getLon();
                    GeoPoint position = new GeoPoint(latitud, longitud);


                    marker.setPosition(position);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setIcon(this.busStop);
                    marker.setTitle(stop.toString());
                    this.mapView.getOverlays().add(marker);
                } catch (NumberFormatException e) {
                    Log.d("MapActivity", e.toString());

                } finally {
                }
            }
        }
    }

    public void setMapBuses() {
        if(notTheFirstTime) {
            this.busMarkers = new ArrayList<>();
            this.datosPublicosManager.getUrlBuses(new VolleyCallBack() {
                @Override
                public void onSuccess() {

                    for(Bus bus : datosPublicosManager.getBusList()){
                        if(!isDestroyed()) {
                            try {
                                Marker marker = new Marker(mapView);
                                GeoPoint position = new GeoPoint(bus.getLat(), bus.getLon());

                                marker.setPosition(position);
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                marker.setIcon(busDrawable);
                                marker.setTitle(bus.toString());
                                busMarkers.add(marker);
                                mapView.getOverlays().add(marker);
                            } catch (NumberFormatException e) {
                                Log.d("MapActivity", e.toString());

                            }
                        }
                    }
                    mapView.invalidate();
                    Log.e("SETMAPBUSES", datosPublicosManager.getBusList().size() + " buses se han actualizado");
                }
            });
        }else{
            for (Bus bus : this.datosPublicosManager.getBusList()) {
                if(!isDestroyed()) {
                    try {
                        Marker marker = new Marker(this.mapView);
                        GeoPoint position = new GeoPoint(bus.getLat(), bus.getLon());

                        marker.setPosition(position);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        marker.setIcon(this.busDrawable);
                        marker.setTitle(bus.toString());
                        this.busMarkers.add(marker);
                        mapView.getOverlays().add(marker);
                    } catch (NumberFormatException e) {
                        Log.d("MapActivity", e.toString());

                    }
                }
            }
            Log.e("SETMAPBUSES", datosPublicosManager.getBusList().size() + "");
        }
    }

    private void setRepeatingAsyncTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if(notTheFirstTime) {
                            try {

                                Log.e("MapActivity", "Refresh de los Buses tras 1 min");

                                deleteMarkers();
                                setMapBuses();

                            } catch (Exception e) {
                                // error, do something
                                Toast.makeText(MyApplication.getAppContext(), "There was some error", Toast.LENGTH_LONG);
                            }
                        }else
                            notTheFirstTime = true;
                    }
                });
            }
        };
        timer.schedule(task, 0, 60*1000);  // interval of one minute
    }

    public static void resetStatusAfterRideSaved(){
        textViewMap.setText(uMuvAlgorithm.toString());
    }

    public void deleteMarkers(){
        mapView.getOverlays().removeAll(this.busMarkers);
        Log.e("DELETEMARKERS", "Done deleting");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }else{
                    Toast.makeText(this, "The app requires location permission to be granted to work", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }

    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    for(Beacon b : beacons){
                        Log.i(TAG, "The first beacon I see is about "+ b.getDistance()+" meters away.");
                        Log.i(TAG, b.toString());
//                    infoMonitoring.setText("Distancia Aproximada Actual al Beacon: " + b.getDistance() + "\n" + b.getId1() + "\n" + b.getId2());
                        parseBeacon(b.getId1().toString(), b.getId2().toString());
                    }
//                    Beacon b = beacons.iterator().next();

                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }

    }

    private void parseBeacon(String workspace, String id){
        if(workspace.equals(hexWorkspace)){
            String parsedId = parseId(id);
            String firstLetter = parsedId.substring(0,1);
            String message = "";

            if (firstLetter.equals("P")){
                String stopId = parsedId.substring(1,parsedId.length());
                Stop stopDetected = this.datosPublicosManager.getParadaInfo(Integer.parseInt(stopId));
                Log.e("PARADA DETECTED: ", ""+stopDetected);
                this.uMuvAlgorithm.setStopDetected(true, stopDetected);
                this.resetStopTimer();

            }else if(firstLetter.equals("B")){
                int busNumber = Integer.parseInt(parsedId.substring(1,parsedId.length()));
                Bus busDetected = this.datosPublicosManager.getBusInfo(busNumber);

                this.uMuvAlgorithm.setBusDetected(true, busDetected);
                this.resetBusTimer();
            }else{
                message = "Neither bus nor stop " + parsedId.substring(1,parsedId.length() );
            }
            message += "" + this.uMuvAlgorithm.toString();
            textViewMap.setText(message);
        }else
            textViewMap.setText("Workspace: " + workspace + "\n " + hexWorkspace);
    }

    private String parseId(String id){
        String[] parsed = new String[id.length()/2];
        int parsedCounter = 0;
        for(int i = 2; i < id.length(); i+=2){
            String substringAux = id.substring(i, i+2);
            if(!substringAux.equals("00")){
                parsed[parsedCounter] = substringAux;
                parsedCounter++;
            }
        }
        String prueba = new String();
        int i = 0;
        while(i < parsedCounter){
            prueba += (char)Integer.parseInt(parsed[i], 16);
            i++;
        }

        return prueba;
    }

    private void resetStopTimer(){
        if(this.stopTimerStarted) {
            try {
                this.stopTimer.cancel();
                this.stopTimer.purge();
                this.stopTimer = new Timer();
            }catch (IllegalStateException e){
                Log.e("Reset Stop Timer", "Timer already cancelled");
            }
        }
        try {
            this.stopTask = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            uMuvAlgorithm.setStopDetected(false, null);
                            textViewMap.setText(uMuvAlgorithm.toString());
                            Log.i(TAG, "Stopped Detecting any Stop");
                            stopTimerStarted = false;
                            stopTimer.cancel();
                            stopTimer.purge();
                            stopTimer = new Timer();
//                        stopTimer = new Timer();
                        }
                    });
                }
            };
            this.stopTimer.schedule(this.stopTask, TIME_TO_STOP_DETECTING,1000);
        }catch (IllegalStateException e){
            Log.e("Reset Stop Timer", "Schedule error");
        }
        this.stopTimerStarted = true;
    }

    private void resetBusTimer(){
        if(this.busTimerStarted) {
            try {
                this.busTimer.cancel();
                this.busTimer.purge();
                this.busTimer = new Timer();
            }catch (IllegalStateException e){
                Log.e("Reset Stop Timer", "Timer already cancelled");
            }
//            this.stopTimer = new Timer();

        }
        try {
            this.busTask = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            uMuvAlgorithm.setBusDetected(false, null);
                            textViewMap.setText(uMuvAlgorithm.toString());
                            Log.i(TAG, "Stopped Detecting any Bus");
                            busTimerStarted = false;
                            busTimer.cancel();
                            busTimer.purge();
                            busTimer = new Timer();
                        }
                    });
                }
            };
            this.busTimer.schedule(this.busTask, TIME_TO_STOP_DETECTING,1000);
        }catch (IllegalStateException e){
            Log.e("Reset Stop Timer", "Schedule error");
        }
        this.busTimerStarted = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivityForResult(intent, RESULT_OK_CHANGED_AVATAR);

            return true;
        }else if(id == R.id.action_rides){
            Intent intent = new Intent(this, RidesActivity.class);
            startActivityForResult(intent, RESULT_OK_CHANGED_AVATAR);
        }else if(id == R.id.action_usual_stops){
            Intent intent = new Intent(this, UsualStopsActivity.class);
            startActivityForResult(intent, RESULT_OK_CHANGED_AVATAR);
        }

        return super.onOptionsItemSelected(item);
    }


}