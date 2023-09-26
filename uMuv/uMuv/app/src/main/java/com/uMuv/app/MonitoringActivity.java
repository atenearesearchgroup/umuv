package com.uMuv.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.uMuv.util.DatabaseManager;
import com.uMuv.util.DatosPublicosManager;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;


public class MonitoringActivity extends AppCompatActivity implements BeaconConsumer {
    protected static final String TAG = "MonitoringActivity";
    private BeaconManager beaconManager;
    private TextView infoMonitoring, infoIntent;
    private static final String hexWorkspace = "0x5e6f0efe3a2fa83d28eb";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);infoMonitoring = findViewById(R.id.infoMonitoring);

        infoIntent = findViewById(R.id.infoIntent);
        String jsonResponse = getIntent().getStringExtra("JSON_RESPONSE");
        if(jsonResponse != null){
            infoIntent.setText(jsonResponse);
        }
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
         beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));

        beaconManager.bind(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Beacon b = beacons.iterator().next();
                    Log.i(TAG, "The first beacon I see is about "+ b.getDistance()+" meters away.");
                    Log.i(TAG, b.toString());
                    infoMonitoring.setText("Distancia Aproximada Actual al Beacon: " + b.getDistance() + "\n" + b.getId1() + "\n" + b.getId2());
                    parseBeacon(b.getId1().toString(), b.getId2().toString());
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
               message = "Congrats you encountered Stop Number " + parsedId.substring(1,parsedId.length());
            }else if(firstLetter.equals("B")){
                int busNumber = Integer.parseInt(parsedId.substring(1,parsedId.length()));
                message = "Congrats you encountered Bus Number " + busNumber;
                message += "\n" + DatosPublicosManager.getSharedInstance().getBusInfo(busNumber).toString();
            }else{
                message = "Neither bus nor stop " + parsedId.substring(1,parsedId.length() );
            }
            infoMonitoring.setText(message);
        }else
            infoMonitoring.setText("Workspace: " + workspace + "\n " + hexWorkspace);
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
}
