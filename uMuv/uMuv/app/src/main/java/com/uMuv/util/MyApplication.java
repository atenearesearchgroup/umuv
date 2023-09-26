package com.uMuv.util;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.couchbase.lite.ArrayFunction;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onesignal.OSInAppMessageAction;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MyApplication extends Application{

    private static Context context;
    private static final String ONESIGNAL_APP_ID = "31514c7e-3924-4f5f-afe9-8cc957cae51f";
    private static final String BUTTON_SELECTED_YES = "yes";
    private static final String BUTTON_SELECTED_NO = "no";
    private static final String HTTP_METHOD_GET = "GET";
    private static final String HTTP_METHOD_POST = "POST";
    private static final String TYPE_AVATAR = "type";
    private static final String PUBLIC_AVATAR = "Public";
    private static final String HEROKU_URL = "https://umuvserver.herokuapp.com";
    private static final int MINIMUM_OBJECTS = 5;
    private DatabaseManager dbMgr = DatabaseManager.getSharedInstance();
    private Document avatarOriginalDoc;


    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();

        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // OneSignal Initialization
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);
        OneSignal.unsubscribeWhenNotificationsAreDisabled(false);
        OneSignal.setInAppMessageClickHandler(
                new OneSignal.OSInAppMessageClickHandler() {
                    @Override
                    public void inAppMessageClicked(OSInAppMessageAction result) {
                        String resultName = result.getClickName();

                        if (resultName != null) {
                            String[] parsedStringName = resultName.split(Pattern.quote("."));
                            int parsedStringSize = parsedStringName.length;
                            if (parsedStringSize > 0) {
                                String selection = parsedStringName[0];
                                if (selection.equals(BUTTON_SELECTED_YES) && parsedStringSize >= MINIMUM_OBJECTS) {
                                    String entity = parsedStringName[1];
                                    String httpMethod = parsedStringName[2];
                                    String endpoint = HEROKU_URL + parsedStringName[3];
                                    String field = parsedStringName[4];

                                    manageNotification(entity, httpMethod, field, endpoint);
                                    Log.e("NOTIFICATION", "YES: " + httpMethod + " " + field + " " + endpoint);
                                }
                                else if (selection.equals(BUTTON_SELECTED_NO)) {
                                    Log.e("NOTIFICATION", "NO " + parsedStringName + " " + resultName);
                                    Toast.makeText(MyApplication.this, "No information was shared", Toast.LENGTH_SHORT).show();
                                }else {
                                    Log.e("NOTIFICATION", "Action was not formed correctly");
                                    Toast.makeText(MyApplication.this, "Action was not formed correctly", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e("NOTIFICATION", "Request was not formed correctly");
                                Toast.makeText(MyApplication.this, "Request was not formed correctly", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
        //END OneSignal
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }

    private void manageNotification(String entity, String httpMethod, String querySelection, String endpoint){
        if(httpMethod.equals(HTTP_METHOD_GET)){
            Query query = QueryBuilder
                    .select(SelectResult.expression(Meta.id))
                    .from(DataSource.database(dbMgr.getDatabase()))
                    .where(Expression.property("type").equalTo(Expression.string("avatar")));

            try {
                ResultSet resultSet = query.execute();
                List<Result> results = resultSet.allResults();
                if(results.size() > 0){
                    Result result = results.get(0);

                    String id = result.getString(0);
                    this.avatarOriginalDoc = dbMgr.getDocumentById(id);
                    List<String> keys = this.avatarOriginalDoc.getKeys();
                    String entidad = "Ayuntamiento";
                    MutableDocument avatarToRetrieve = new MutableDocument();
                    for(String key : keys){
                        Dictionary dictionary = avatarOriginalDoc.getDictionary(key);
                        if(dictionary == null || dictionary.getString("read").equals(PUBLIC_AVATAR) || (dictionary != null && dictionary.getString("read").equals(entidad))){
                            avatarToRetrieve.setValue(key, avatarOriginalDoc.getValue(key));
                        }
                    }
                    Log.e("Notification Manage Map", avatarToRetrieve.toMap().toString());

                    postToEndpoint(endpoint, avatarToRetrieve.toMap());
                }

            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }else{
            Log.e("Notification Manage", httpMethod + "is currently unsupported");
        }
    }

    private void postToEndpoint(String endpoint, Map<String, Object> jsonMap) {
        try {
            JSONObject jsonBody = new JSONObject(jsonMap);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(endpoint, jsonBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.e("Response POST", response.toString());
                    Toast.makeText(MyApplication.this, "Successfully shared information", Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Response POST", error.toString());
                    Toast.makeText(MyApplication.this, "Error sharing information to " + endpoint, Toast.LENGTH_SHORT).show();
                }
            });
            RequestQueue rQueue = Volley.newRequestQueue(MyApplication.getAppContext());
            rQueue.add(jsonObjectRequest);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}