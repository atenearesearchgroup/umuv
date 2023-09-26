package com.uMuv.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.MutableDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.uMuv.util.DatabaseManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;


public class CreateAvatarActivity extends AppCompatActivity {
    private DatabaseManager dbMgr;
    private static final String USERNAME = "TMGR";
    private static final String TAG = "CreateAvatarActivity";
    private EditText completeName, phone, email, birthDate;
    private EditText[] editTexts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_avatar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        completeName = findViewById(R.id.avatarCreateCompleteName);
        phone = findViewById(R.id.avatarCreatePhone);
        email = findViewById(R.id.avatarCreateEmail);
        birthDate = findViewById(R.id.avatarCreateBirthDate);
        editTexts = new EditText[]{completeName, phone, email, birthDate};



        dbMgr = DatabaseManager.getSharedInstance();
        dbMgr.initCouchbaseLite(getApplicationContext());
        dbMgr.openOrCreateDatabaseForUser(getApplicationContext(), USERNAME);

    }

    public void createAvatar(View v) throws ParseException, JSONException {
        JSONObject json = new JSONObject();
        for(EditText editText : editTexts){
            JSONObject jsonStructure = new JSONObject();
            jsonStructure.put("read", getResources().getString(R.string.publico));
            jsonStructure.put("write", getResources().getString(R.string.privado));
            int id = editText.getId();
            switch (id){
                case R.id.avatarCreateCompleteName:
                    jsonStructure.put("value", completeName.getText().toString());
                    json.put("name", jsonStructure);
                    break;
                case R.id.avatarCreatePhone:
                    jsonStructure.put("value", phone.getText().toString());
                    json.put("phone", jsonStructure);
                    break;
                case R.id.avatarCreateEmail:
                    jsonStructure.put("value", email.getText().toString());
                    json.put("email", jsonStructure);
                    break;
                case R.id.avatarCreateBirthDate:
                    jsonStructure.put("value", birthDate.getText().toString());
                    json.put("birthDate", jsonStructure);
                    break;
                default:
                    break;
            }
        }
        JSONObject jsonStructure = new JSONObject();
        jsonStructure.put("read", getResources().getString(R.string.publico));
        jsonStructure.put("write", getResources().getString(R.string.privado));
        json.put("stopList", new JSONArray());
        json.put("rideList", new JSONArray());
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map map = mapper.readValue(json.toString(), Map.class);
            Log.i("JSON", map.toString());
            MutableDocument mutableDocument = new MutableDocument(map);
            mutableDocument.setString("type", "avatar");

            dbMgr.getDatabase().save(mutableDocument );
            startActivity(new Intent(this, MapActivity.class));
            finish();

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        Toast.makeText(getApplicationContext(),"Saved in Database", Toast.LENGTH_LONG).show();
    }
}