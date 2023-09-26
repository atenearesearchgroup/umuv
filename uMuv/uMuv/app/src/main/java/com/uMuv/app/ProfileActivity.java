package com.uMuv.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDictionary;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.uMuv.util.DatabaseManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final String USERNAME = "TMGR";
    private DatabaseManager dbMgr;
    private EditText completeName, phone, email, birthDate;
    private TextView editNameLink, editPhoneLink, editEmailLink, editBirthLink;
    private Spinner securityReadName, securityWriteName, securityReadPhone, securityWritePhone,
    securityReadEmail, securityWriteEmail,securityReadBirthDate, securityWriteBirthDate;
    private String[] securityOptionsArray;
    private LinearLayout editSecurityName, editSecurityPhone, editSecurityEmail, editSecurityBirthDate;
    private Dictionary avatar;
    private MutableDocument avatarMutDoc;
    private Document avatarOriginalDoc;
    private TextView textViewProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        securityOptionsArray = getResources().getStringArray(R.array.security);
        List<String> list = Arrays.asList(securityOptionsArray);

        dbMgr = DatabaseManager.getSharedInstance();
        dbMgr.initCouchbaseLite(getApplicationContext());
        dbMgr.openOrCreateDatabaseForUser(getApplicationContext(), USERNAME);

        completeName = findViewById(R.id.avatarViewCompleteName);
        phone = findViewById(R.id.avatarViewPhone);
        email = findViewById(R.id.avatarViewEmail);
        birthDate = findViewById(R.id.avatarViewBirthDate);
        textViewProfile = findViewById(R.id.textViewProfile);

        //DROPDOWNS
        securityReadName = findViewById(R.id.securityReadName);
        securityWriteName = findViewById(R.id.securityWriteName);
        securityReadPhone = findViewById(R.id.securityReadPhone);
        securityWritePhone = findViewById(R.id.securityWritePhone);
        securityReadEmail = findViewById(R.id.securityReadEmail);
        securityWriteEmail = findViewById(R.id.securityWriteEmail);
        securityReadBirthDate = findViewById(R.id.securityReadBirthDate);
        securityWriteBirthDate = findViewById(R.id.securityWriteBirthDate);

        //EDIT LINKS
        editNameLink = findViewById(R.id.editName);
        editSecurityName = findViewById(R.id.editSecurityName);
        editSecurityName.setVisibility(View.GONE);
        editPhoneLink = findViewById(R.id.editPhone);
        editSecurityPhone = findViewById(R.id.editSecurityPhone);
        editSecurityPhone.setVisibility(View.GONE);
        editEmailLink = findViewById(R.id.editEmail);
        editSecurityEmail = findViewById(R.id.editSecurityEmail);
        editSecurityEmail.setVisibility(View.GONE);
        editBirthLink= findViewById(R.id.editBirthDate);
        editSecurityBirthDate= findViewById(R.id.editSecurityBirthDate);
        editSecurityBirthDate.setVisibility(View.GONE);

        ArrayAdapter<CharSequence> adapterNameRead = ArrayAdapter.createFromResource(this,
                R.array.security, R.layout.support_simple_spinner_dropdown_item);
        // Specify the layout to use when the list of choices appears
        adapterNameRead.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner


        // Specify the layout to use when the list of choices appears
        securityReadName.setAdapter(adapterNameRead);
        securityWriteName.setAdapter(adapterNameRead);
        securityReadPhone.setAdapter(adapterNameRead);
        securityWritePhone.setAdapter(adapterNameRead);
        securityReadEmail.setAdapter(adapterNameRead);
        securityWriteEmail.setAdapter(adapterNameRead);
        securityReadBirthDate.setAdapter(adapterNameRead);
        securityWriteBirthDate.setAdapter(adapterNameRead);

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
                Map<String, Object> propertiesF;
                //SHOW AVATAR IN INTERFACE
                this.avatar = result.getDictionary(dbMgr.getDbName());

                // SET NAME
                Dictionary name = avatar.getDictionary("name");
                completeName.setText(name.getString("value"));
                String nameReadOption = name.getString("read");
                String nameWriteOption = name.getString("write");
                if(list.contains(nameReadOption))
                    securityReadName.setSelection(list.indexOf(nameReadOption));
                if(list.contains(nameWriteOption))
                    securityWriteName.setSelection(list.indexOf(nameWriteOption));

                //SET PHONE
                Dictionary phoneDic = avatar.getDictionary("phone");
                phone.setText(phoneDic.getString("value"));
                String phoneReadOption = phoneDic.getString("read");
                String phoneWriteOption = phoneDic.getString("write");
                if(list.contains(phoneReadOption))
                    securityReadPhone.setSelection(list.indexOf(phoneReadOption));
                if(list.contains(phoneWriteOption))
                    securityWritePhone.setSelection(list.indexOf(phoneWriteOption));

                //SET EMAIL
                Dictionary emailDic = avatar.getDictionary("email");
                email.setText(emailDic.getString("value"));
                String emailReadOption = emailDic.getString("read");
                String emailWriteOption = emailDic.getString("write");
                if(list.contains(emailReadOption))
                    securityReadEmail.setSelection(list.indexOf(emailReadOption));
                if(list.contains(emailWriteOption))
                    securityWriteEmail.setSelection(list.indexOf(emailWriteOption));

                //SET BIRTH
                Dictionary birthDic = avatar.getDictionary("birthDate");
                birthDate.setText(birthDic.getString("value"));
                String birthReadOption = birthDic.getString("read");
                String birthWriteOption = birthDic.getString("write");
                if(list.contains(birthReadOption))
                    securityReadBirthDate.setSelection(list.indexOf(birthReadOption));
                if(list.contains(birthWriteOption))
                    securityWriteBirthDate.setSelection(list.indexOf(birthWriteOption));
            }

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public void editName (View v){
        if(this.editSecurityName.getVisibility() == View.VISIBLE) {
            this.editSecurityName.setVisibility(View.GONE);
            this.editNameLink.setText("Edit");
        }else {
            this.editSecurityName.setVisibility(View.VISIBLE);
            this.editNameLink.setText("Hide");
        }
    }
    public void editPhone (View v){
        if(this.editSecurityPhone.getVisibility() == View.VISIBLE) {
            this.editSecurityPhone.setVisibility(View.GONE);
            this.editPhoneLink.setText("Edit");
        }else {
            this.editSecurityPhone.setVisibility(View.VISIBLE);
            this.editPhoneLink.setText("Hide");
        }
    }
    public void editEmail (View v){
        if(this.editSecurityEmail.getVisibility() == View.VISIBLE) {
            this.editSecurityEmail.setVisibility(View.GONE);
            this.editEmailLink.setText("Edit");
        }else {
            this.editSecurityEmail.setVisibility(View.VISIBLE);
            this.editEmailLink.setText("Hide");
        }
    }
    public void editBirth (View v){
        if(this.editSecurityBirthDate.getVisibility() == View.VISIBLE) {
            this.editSecurityBirthDate.setVisibility(View.GONE);
            this.editBirthLink.setText("Edit");
        }else {
            this.editSecurityBirthDate.setVisibility(View.VISIBLE);
            this.editBirthLink.setText("Hide");
        }
    }

    public void changeAvatar (View v){
        MutableDictionary nameDictionary = this.avatarMutDoc.getDictionary("name");
        nameDictionary.setString("value", this.completeName.getText().toString());
        nameDictionary.setString("read", this.securityReadName.getSelectedItem().toString());
        nameDictionary.setString("write", this.securityWriteName.getSelectedItem().toString());
        this.avatarMutDoc.setDictionary("name", nameDictionary);

        MutableDictionary phoneDictionary = this.avatarMutDoc.getDictionary("phone");
        phoneDictionary.setString("value", this.phone.getText().toString());
        phoneDictionary.setString("read", this.securityReadPhone.getSelectedItem().toString());
        phoneDictionary.setString("write", this.securityWritePhone.getSelectedItem().toString());
        this.avatarMutDoc.setDictionary("phone", phoneDictionary);

        MutableDictionary emailDictionary = this.avatarMutDoc.getDictionary("email");
        emailDictionary.setString("value", this.email.getText().toString());
        emailDictionary.setString("read", this.securityReadEmail.getSelectedItem().toString());
        emailDictionary.setString("write", this.securityWriteEmail.getSelectedItem().toString());
        this.avatarMutDoc.setDictionary("email", emailDictionary);

        MutableDictionary birthDateDictionary = this.avatarMutDoc.getDictionary("birthDate");
        birthDateDictionary.setString("value", this.birthDate.getText().toString());
        birthDateDictionary.setString("read", this.securityReadBirthDate.getSelectedItem().toString());
        birthDateDictionary.setString("write", this.securityWriteBirthDate.getSelectedItem().toString());
        this.avatarMutDoc.setDictionary("birthDate", birthDateDictionary);


        try {
            dbMgr.getDatabase().save(this.avatarMutDoc);
            Toast.makeText(getApplicationContext(),"Avatar changed", Toast.LENGTH_LONG).show();
            finish();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public void cancelEdit (View v){
        Toast.makeText(getApplicationContext(),"Avatar not changed", Toast.LENGTH_LONG).show();
        finish();
    }


}