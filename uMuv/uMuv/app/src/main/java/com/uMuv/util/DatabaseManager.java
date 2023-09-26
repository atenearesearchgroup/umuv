package com.uMuv.util;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseChange;
import com.couchbase.lite.DatabaseChangeListener;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.ListenerToken;

public class DatabaseManager {
    private static Database database;
    private static com.uMuv.util.DatabaseManager instance = null;
    private ListenerToken listenerToken;
    public  String currentUser = null;
    private final String dbName = "uMuvDB";

    protected DatabaseManager() {

    }

    public static com.uMuv.util.DatabaseManager getSharedInstance() {
        if (instance == null) {
            instance = new com.uMuv.util.DatabaseManager();
        }
        
        return instance;
    }

    public static Database getDatabase(){
        return database;
    }

    public String getDbName(){
        return this.dbName;
    }

    public void initCouchbaseLite(Context context) {
        CouchbaseLite.init(context);
    }

    public String getCurrentUserDocId() {
        return "user::" + currentUser;
    }

    public void openOrCreateDatabaseForUser(Context context, String username)
    {
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDirectory(String.format("%s/%s", context.getFilesDir(), username));

        currentUser = username;

        try {
            database = new Database(dbName, config);
            registerForDatabaseChanges();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    private void registerForDatabaseChanges()
    {
        listenerToken = database.addChangeListener(new DatabaseChangeListener() {
            @Override
            public void changed(final DatabaseChange change) {
                if (change != null) {
                    for(String docId : change.getDocumentIDs()) {
                        Document doc = database.getDocument(docId);
                        if (doc != null) {
                            Log.i("DatabaseChangeEvent", "Document was added/updated");
                        }
                        else {

                            Log.i("DatabaseChangeEvent","Document was deleted");
                        }
                    }
                }
            }
        });
        // end::addDatabaseChangelistener[]
    }

    // tag::closeDatabaseForUser[]
    public void closeDatabaseForUser()
    // end::closeDatabaseForUser[]
    {
        try {
            if (database != null) {
                deregisterForDatabaseChanges();
                // tag::closeDatabase[]
                database.close();
                // end::closeDatabase[]
                database = null;
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    // tag::deregisterForDatabaseChanges[]
    private void deregisterForDatabaseChanges()
    // end::deregisterForDatabaseChanges[]
    {
        if (listenerToken != null) {
            // tag::removedbchangelistener[]
            database.removeChangeListener(listenerToken);
            // end::removedbchangelistener[]
        }
    }

    public Document getDocumentById(String id){
        return database.getDocument(id);
    }

    public void deleteDocument(Document document){
        try {
            database.delete(document);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }
}
