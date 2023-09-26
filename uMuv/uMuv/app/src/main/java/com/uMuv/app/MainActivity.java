    package com.uMuv.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.tasks.Task;
import com.uMuv.util.AwarenessManager;
import com.uMuv.util.DatabaseManager;
import com.uMuv.util.DatosPublicosManager;
import com.uMuv.util.Stop;
import com.uMuv.util.VolleyCallBack;
import com.uMuv.util.uMuvAlgorithm;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.*;

import java.util.List;


public class MainActivity extends AppCompatActivity {
    public static final String JSON_URL = "https://datosabiertos.malaga.eu/recursos/transporte/EMT/EMTLineasYParadas/lineasyparadas.geojson";
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
    private static final int RESULT_OK_CHANGED_AVATAR = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String USERNAME = "TMGR";
    private DatabaseManager dbMgr;
    private Button submit, search;
    private EditText name, surname;
    public TextView paragraph;
    private JSONObject completeJson;
    private AwarenessManager awarenessMgr;
    private DatosPublicosManager datosPublicosManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        submit = findViewById(R.id.submit);
        search = findViewById(R.id.search);
        name = findViewById(R.id.name);
        surname = findViewById(R.id.surname);
        paragraph = findViewById(R.id.paragraph);
        paragraph.setMovementMethod(new ScrollingMovementMethod());

        dbMgr = DatabaseManager.getSharedInstance();
        dbMgr.initCouchbaseLite(getApplicationContext());
        dbMgr.openOrCreateDatabaseForUser(getApplicationContext(), USERNAME);

        awarenessMgr = AwarenessManager.getSharedInstance();

        datosPublicosManager = DatosPublicosManager.getSharedInstance();
        datosPublicosManager.getUrlParadasLineas();
        datosPublicosManager.getUrlBuses(new VolleyCallBack() {
            @Override
            public void onSuccess() {

            }
        });

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
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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
        }

        return super.onOptionsItemSelected(item);
    }



    public void buttonAdd(View v){
        String name = this.name.getText().toString();
        String surname = this.surname.getText().toString();

        MutableDocument mutableDocument = new MutableDocument();
        mutableDocument.setString("type", "user");
        mutableDocument.setString("name", name);
        mutableDocument.setString("surname", surname);
        try {
            dbMgr.getDatabase().save(mutableDocument);
        } catch (CouchbaseLiteException e) {
            com.couchbase.lite.internal.support.Log.e(LogDomain.valueOf(TAG), e.toString());
        }

        Toast.makeText(getApplicationContext(),"Guardado en BBDD", Toast.LENGTH_LONG).show();
    }

    public void buttonSearch(View v){
        String name = this.name.getText().toString();
        String surname = this.surname.getText().toString();
        this.paragraph.setText("");

        Query query = QueryBuilder
                .select(SelectResult.expression(Meta.id),
                        SelectResult.property("type"),
                        SelectResult.property("name"),
                        SelectResult.property("surname"))
                .from(DataSource.database(dbMgr.getDatabase()))
                .where(Expression.property("type").equalTo(Expression.string("user")));
        Query query2 = QueryBuilder.select(SelectResult.all())
                .from(DataSource.database(dbMgr.getDatabase()))
                .where(Expression.property("type").equalTo(Expression.string("user")));


        try {
            ResultSet rs = query.execute();
            int i = 1;
            this.paragraph.append("Query Result: \n");
            for (Result r: rs) {
                String show = i + ") " + r.getString("name") + " " + r.getString("surname") + "\n";
                this.paragraph.append(show);
                i++;
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public void viewLocation(View v){
        awarenessMgr.getLocation(getApplicationContext(), this, this.paragraph);
    }

    public void viewBeacons(View v){
        startActivity(new Intent(this, MonitoringActivity.class));
    }

    public void getParadasInfo(View v){
        Stop stop = this.datosPublicosManager.getParadaInfo(351);

        this.paragraph.setText(stop == null ? null : stop.toString());
    }

    private String parseJson(String jsonString){
        String result = "";
        try {
            JSONArray jsonArray = new JSONArray(jsonString);

            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject linea = jsonArray.getJSONObject(i);
                result += "Linea: " + linea.getInt("codLinea") + "; Nombre: " + linea.getString("nombreLinea") + "\n";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void deleteAvatar(View v){
        Query query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.database(dbMgr.getDatabase()))
                .where(Expression.property("type").equalTo(Expression.string("avatar")));

        try {
            ResultSet queryResult = query.execute();
            for(Result r : queryResult){
                String id = r.getString(0);

                Document document = dbMgr.getDocumentById(id);

                if(document != null){
                    dbMgr.deleteDocument(document);
                }
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public void getBusesInfo(View v){
        this.paragraph.setText(this.datosPublicosManager.getBuses());
    }


    public void viewMap(View v){
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);

    }
}
