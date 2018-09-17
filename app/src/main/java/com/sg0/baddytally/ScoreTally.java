package com.sg0.baddytally;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;

public class ScoreTally extends Application {
    private static final String TAG = "ScoreTally.app";
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        String role = prefs.getString(Constants.DATA_ROLE, "");
        if(Constants.ROOT.equals(role)) {
            //if you are the root, then enable persistence.
            //Note that, this will happen only when the app is restarted after the initial login.
            //Persistence is enabled for root, basically for the "create new innings" use case.
            //Create new innings has many DB operations and if one of them fails in between due to
            //connectivity failure, the DB will not be recoverable w/o manual changes to firebase DB.

            //keepsync-ed not enabled due to issues reported widely about this flag.

            //SGO: Below issue was seen:
            //    -Innings table was deleted from firebase console
            //    -Refresh / Restart of ScoreTally still found entry in the local DB (with persistence enabled).
            //    -Cleared cache, logged in as admin
            //    -DB still state
            //    -Killed the app on phone and started again: but this routine does not get invoked.
            //    -Reinstalled app and the new DB values were seen.
            //    ++++Note: After added exit() to cache-clear code, seems like Application.onCreate is getting invoked.
            //              But, this behavior is also very inconsistent. it is called only sometimes, on clear_cache.
            //So, persistence w/o keepsync is not safe, if manual changes to DB are done.
            //Will keep it for now, as manual updates to DB via firebase console should not be done.

            //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            //Log.d(TAG, "onCreate: DB Persistence enabled");

            /*
            09-16 21:26:21.719 10221-10316/com.sg0.baddytally I/Transaction: runTransaction() usage detected while persistence is enabled.
            Please be aware that transactions *will not* be persisted across database restarts.
            See https://www.firebase.com/docs/android/guide/offline-capabilities.html#section-handling-transactions-offline for more details.
             */
        } else {
            FirebaseDatabase.getInstance().setPersistenceEnabled(false);
            Log.d(TAG, "onCreate: DB Persistence disabled");
        }
    }
}