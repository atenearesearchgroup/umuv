package com.uMuv.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.onesignal.OneSignal;
import com.uMuv.util.DatabaseManager;
import com.uMuv.util.DatosPublicosManager;
import com.uMuv.util.MyApplication;

public class CreateAvatarOrMapActivity extends AppCompatActivity {
    private static final String USERNAME = "TMGR";
    private DatabaseManager dbMgr;
    private DatosPublicosManager datosPublicosManager;
    private static final String TAG = "CreateAvatarOrMapActivity";
    private static final String FENCE_KEY = "FENCE_KEY_1998";
    private PendingIntent mPendingIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_avatar_or_main);

        this.datosPublicosManager = DatosPublicosManager.getSharedInstance();
        datosPublicosManager.getUrlParadasLineas();

        dbMgr = DatabaseManager.getSharedInstance();
        dbMgr.initCouchbaseLite(getApplicationContext());
        dbMgr.openOrCreateDatabaseForUser(getApplicationContext(), USERNAME);

        Intent mapIntent = new Intent(this, MapActivity.class);

        Query query = QueryBuilder.select(SelectResult.all())
                .from(DataSource.database(dbMgr.getDatabase()))
                .where(Expression.property("type").equalTo(Expression.string("avatar")));

        try {
            ResultSet queryResult = query.execute();
            if(queryResult.allResults().size() > 0){
                startActivity(mapIntent);
            }else{
                startActivity(new Intent(this, CreateAvatarActivity.class));
            }
            finish();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }
}