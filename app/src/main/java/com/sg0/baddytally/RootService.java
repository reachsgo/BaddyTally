package com.sg0.baddytally;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class RootService extends IntentService {
    private static final String TAG = "RootService";
    private static final String ACTION_NEWCLUB = "com.sg0.baddytally.action.NEWCLUB";

    public RootService() {
        super("RootService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startRepeatingIntent(final Context context) {
        //RootService.createNotificationChannel(context);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent=PendingIntent.getService(
                context, 0, getNewClubIntent(context), 0);
        if(mgr!=null) mgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                AlarmManager.INTERVAL_FIFTEEN_MINUTES/3, pendingIntent);
    }

    public static void stopRepeatingIntent(final Context context) {
        Log.d(TAG, "stopRepeatingIntent: ");
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent=PendingIntent.getService(
                context, 0, getNewClubIntent(context), 0);
        if(mgr!=null) mgr.cancel(pendingIntent);
        //deleteNotificationChannel(context);
    }

    public static Intent getNewClubIntent(final Context context) {
        Intent intent = new Intent(context, RootService.class);
        intent.setAction(ACTION_NEWCLUB);
        //intent.putExtra(EXTRA_PARAM1, param1);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_NEWCLUB.equals(action)) {
                Log.d(TAG, "onHandleIntent: calling addListenerForNewClub");
                SharedData.getInstance().addListenerForNewClub(getApplicationContext());
            }
        }
    }

    void readDBForNewClubs() {

        Log.d(TAG, "readDBForNewClubs: ");
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.NEWCLUBS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String, ClubDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<Map<String, ClubDBEntry>>() {
                        };
                Map<String, ClubDBEntry> mClubs = dataSnapshot.getValue(genericTypeIndicator);
                if (null == mClubs) {
                    Log.d(TAG, "readDBForNewClubs: no new clubs");
                    mClubs = new HashMap<>();
                }
                if(mClubs.size()>0) {
                    //addNotification();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
