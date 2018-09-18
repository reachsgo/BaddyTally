package com.sg0.baddytally;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class SharedData {
    private static final String TAG = "SharedData";
    private static final String SHUFFLE_START = "SH_ST";
    private static final String SHUFFLE_SUCCESS = "SH_E";
    private static final String SHUFFLE_FAIL = "SH_F";
    private static final String NEWUSER_CREATED = "NEW_U";
    private static final String USER_DELETED = "USR_D";
    private static SharedData sSoleInstance;
    public int mNumOfGroups;
    public String mUser;
    public String mClub;
    public String mRole;
    public String mInnings;
    public String mRoundName;
    public String mAdminCode;
    public String mMemCode;
    public String mRootCode;
    public ArrayList<PlayerData> mGoldPlayers;
    public ArrayList<PlayerData> mSilverPlayers;
    public Integer mInningsDBKey;
    private boolean mDBUpdated;
    private boolean mUserNotifyEnabled;
    private boolean mDBConnected;
    private String mStoreHistory;
    private ValueEventListener mDBConnectListener;

    private SharedData() {
        mDBConnectListener = null;
        clear();
    }  //private constructor.

    public static SharedData getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            sSoleInstance = new SharedData();
        }
        return sSoleInstance;
    }

    public void clear() {
        Log.w(TAG, "SharedData: CLEAR");
        mNumOfGroups = Constants.NUM_OF_GROUPS;
        mUser = "";
        mClub = "";
        mRole = "unknown";
        mInnings = "";
        mRoundName = "";
        mAdminCode = "";
        mMemCode = "";
        mRootCode = "";
        mGoldPlayers = null;
        mSilverPlayers = null;
        mInningsDBKey = -1;
        mDBUpdated = false;
        mUserNotifyEnabled = true;
        mDBConnected = false;
        mStoreHistory = "";
    }

    public boolean isRoot() {
        return Constants.ROOT.equals(mRole);
    }

    public boolean isAdmin() {
        return Constants.ADMIN.equals(mRole);
    }

    public boolean isDBUpdated() {
        return mDBUpdated;
    }

    public void setDBUpdated(boolean s) {
        mDBUpdated = s;
    }

    public void resetForNewInnings(final String newInningsName) {
        mInnings = newInningsName;
        mRoundName = "";
        mInningsDBKey = -1;
        setDBUpdated(true);
    }

    private void addHistory(Context context, final String story) {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT, Locale.CANADA);
        String dateStr = df.format(c);
        synchronized (context) {
            //Better to be synchronized. These are called from different firebase-DB-update threads, in case of shuffling scenario.
            if (isUserNotifyEnabled()) {
                //All the user delete/create log can be added as a single entry.
                final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference().child(SharedData.getInstance().mClub).child(Constants.INTERNALS).child(Constants.HISTORY);
                DatabaseReference newHistory = inningsDBRef.push();
                newHistory.setValue(dateStr + "#" + mUser + "#" + story);
            } else mStoreHistory += story + ",";
        }
        Log.w(TAG, "History added: [" + dateStr + ":" + mUser + ":" + story + "]");
    }

    public String parseHistory(String storyLine) {
        if (storyLine.contains(NEWUSER_CREATED) && storyLine.contains(USER_DELETED)) {
            //this is detailed info of Shuffling. Too big to print on phone!
            return "";
        } else if (storyLine.contains(SHUFFLE_START))
            return "";  //ignore

        String resultStr = storyLine.replace("#", "  ");
        String tmpStr = resultStr.replace("=", " : ");

        //only one of the below can appear in a line
        if (tmpStr.contains(NEWUSER_CREATED))
            resultStr = tmpStr.replace(NEWUSER_CREATED, "Created");
        else if (tmpStr.contains(USER_DELETED)) resultStr = tmpStr.replace(USER_DELETED, "Deleted");
        else if (tmpStr.contains(SHUFFLE_SUCCESS))
            resultStr = tmpStr.replace(SHUFFLE_SUCCESS, "Shuffle Successful");
        else if (tmpStr.contains(SHUFFLE_FAIL))
            resultStr = tmpStr.replace(SHUFFLE_FAIL, "Shuffle Failed");

        return resultStr;
    }

    public void addShuffleStart2History(Context context, final String newInningsName) {
        addHistory(context, SHUFFLE_START + "=" + newInningsName);
    }

    public void addShuffleSuccess2History(Context context, final String newInningsName) {
        addHistory(context, SHUFFLE_SUCCESS + "=" + newInningsName);
    }

    public void addShuffleFailure2History(Context context, final String newInningsName) {
        addHistory(context, SHUFFLE_FAIL + "=" + newInningsName);
    }

    public void addNewUserCreation2History(Context context, final String newUserName) {
        addHistory(context, NEWUSER_CREATED + "=" + newUserName);
    }

    public void addUserDeletion2History(Context context, final String userName) {
        addHistory(context, USER_DELETED + "=" + userName);
    }

    public void disableUserNotify(Context context) {
        synchronized (context) {
            mUserNotifyEnabled = false;
            mStoreHistory = "";
        }
    }

    public void enableUserNotify(Context context) {
        synchronized (context) {
            mUserNotifyEnabled = true;
            addHistory(context, mStoreHistory);
        }
    }

    public boolean isUserNotifyEnabled() {
        return mUserNotifyEnabled == true;
    }

    public void showToast(final Context c, final CharSequence s, final int d) {
        if (mUserNotifyEnabled) Toast.makeText(c, s, d).show();
        Log.i(TAG, s.toString());
    }

    public SpannableStringBuilder getColorString(CharSequence text, int color) {
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssBuilder;
    }

    public SpannableStringBuilder getStyleString(CharSequence text, int style) {
        //style=Typeface.ITALIC
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new StyleSpan(style), 0, text.length(), 0);
        return ssBuilder;
    }

    public SpannableStringBuilder getSizeString(CharSequence text, float proportion) {
        //style=Typeface.ITALIC
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new RelativeSizeSpan(proportion), 0, text.length(), 0);
        return ssBuilder;
    }

    public SpannableStringBuilder getTitleStr(String title, Context context) {
        return getColorString(title,   //add some color to it. Use the color scheme color from color.xml
                ResourcesCompat.getColor(context.getResources(), R.color.colorPrimaryDark, null));
    }

    public void sortPlayers(ArrayList<PlayerData> playersList, final int idx, final boolean descending, final Context context, final boolean showToasts) {
        //playersList is obj reference of the actual list being passed in.
        //Any updates to the contents will be visible to the caller.
        //But, you cannot change the obj reference itself (ex: playersList = another_list;).
        final String[] infoLog = {""};  //has to be final to be accessed from inner class, but needs to be updated in inner class. Thus, array.
        Collections.sort(playersList, new Comparator<PlayerData>() {
            @Override
            public int compare(PlayerData p1, PlayerData p2) {
                //Integer.valueOf(p1.getPointsInt_innings()).compareTo(p2.getPointsInt_innings()); //ascending order
                int value1 = Integer.valueOf(p2.getPoints(idx)).compareTo(Integer.valueOf(p1.getPoints(idx)));  //descending order
                if (value1 == 0) {
                    // Sorting order criteria:
                    //If there is a tie, player selection is done on the below criteria in that order:
                    //    0. Points
                    //    1. higher win % (number_of_wins / number_of_games_played x 100).
                    //    2. most number of wins
                    //    3. most number of games played
                    //    4. toss

                    int value2 = Integer.valueOf(p2.getWinPercentage(idx)).compareTo(Integer.valueOf(p1.getWinPercentage(idx)));
                    if (value2 == 0) {
                        int value3 = Integer.valueOf(p2.getGamesWon(idx)).compareTo(Integer.valueOf(p1.getGamesWon(idx)));
                        if (value3 == 0) {
                            int value4 = Integer.valueOf(p2.getGamesPlayed(idx)).compareTo(Integer.valueOf(p1.getGamesPlayed(idx)));
                            if (value4 == 0) {
                                //Toss: Return a random value +1 or -1
                                //Log.v(TAG, "sortPlayers: Tossing to sort " + p1.getName() + " and " + p2.getName());
                                //Log.v(TAG, "sortPlayers: p1=" + p1.toString() + " and p2=" + p2.toString());
                                if (!infoLog[0].isEmpty()) infoLog[0] += ", ";
                                infoLog[0] += p1.getName() + " & " + p2.getName();
                                int rand = new Random().nextInt(10);
                                if ((rand % 2) == 0) {
                                    Log.w(TAG, "sortPlayers: Toss: " + p1.getName() + " smaller!");
                                    return 1;
                                } else if ((rand % 2) == 1) {
                                    Log.w(TAG, "sortPlayers: Toss: " + p2.getName() + " smaller!");
                                    return -1;
                                }
                            } else return value4;
                        } else return value3;
                    } else return value2;
                }
                return value1;
            }
        });
        if (!descending) Collections.reverse(playersList);
        if (showToasts && !infoLog[0].isEmpty()) {
            Toast.makeText(context,
                    "Tossed a coin to sort: " + infoLog[0], Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "sortPlayers: Sorted playersList size: " + Integer.toString(playersList.size()));
    }

    public boolean isDBConnected() {
        return mDBConnected == true;
    }

    public void setUpDBConnectionListener() {
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        if (null != mDBConnectListener) connectedRef.removeEventListener(mDBConnectListener);
        /*
        /.info/connected is a boolean value which is not synchronized between Realtime Database clients because the value is dependent
         on the state of the client. In other words, if one client reads /.info/connected as false, this is no guarantee that a separate
         client will also read false.
         On Android, Firebase automatically manages connection state to reduce bandwidth and battery usage. When a client has no active
         listeners, no pending write or onDisconnect operations, and is not explicitly disconnected by the goOffline method, Firebase
         closes the connection after 60 seconds of inactivity.
        */
        mDBConnectListener = connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(null==snapshot) return;
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.w(TAG, "isDBConnected: connected");
                    mDBConnected = true;
                } else {
                    Log.w(TAG, "isDBConnected: not connected");
                    mDBConnected = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "isDBConnected: Listener was cancelled");
                mDBConnected = false;
            }
        });
    }

    public void wakeUpDBConnection() {
        // Do a mock transaction to wake up the database connection.
        // Note that "awake" child is never created in the DB.
        /*
        Logs from testing:
        09-17 21:13:38.351 27677-27677/com.sg0.baddytally W/SharedData: isDBConnected: connected
        09-17 21:14:28.721 27677-27677/com.sg0.baddytally W/SharedData: isDBConnected: not connected
        09-17 21:17:57.467 27677-27677/com.sg0.baddytally W/SharedData: isDBConnected: not connected
        09-17 21:18:05.109 27677-27677/com.sg0.baddytally W/SharedData: isDBConnected: connected
        09-17 21:18:05.124 27677-27677/com.sg0.baddytally W/SharedData: wakeUpDBConnection: onComplete: Success
        09-17 21:18:12.354 27677-27677/com.sg0.baddytally W/SharedData: isDBConnected: connected
        09-17 21:19:05.118 27677-27677/com.sg0.baddytally W/SharedData: isDBConnected: not connected
         */


        final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.INTERNALS).child("awake");
        inningsDBRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Boolean awake = mutableData.getValue(Boolean.class);
                if (awake == null) return Transaction.success(mutableData);
                else {
                    //This is never hit, as the "awake" child is never created in DB
                    mutableData.setValue(!awake);
                    Log.w(TAG, "wakeUpDBConnection: awake set to " + awake);
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if (error != null || !committed || dataSnapshot == null) {
                    if (null != error)
                        Log.w(TAG, "wakeUpDBConnection: onComplete: Failed:", error.toException());
                    else Log.w(TAG, "wakeUpDBConnection: onComplete: Failed");
                } else {
                    Log.w(TAG, "wakeUpDBConnection: onComplete: Success");
                }
            }
        });
    }


    @Override
    public String toString() {
        String str = "SharedData{" +
                "mUser='" + mUser + '\'' +
                ", mRole='" + mRole + '\'' +
                ", mClub='" + mClub + '\'' +
                ", mInnings='" + mInnings + '\'' +
                ", mRoundName='" + mRoundName + '\'';
        //", mAdminCode='" + mAdminCode + '\'' +
        //", mMemCode='" + mMemCode + '\'';
        if (mGoldPlayers != null) str += ", mGoldPlayers=" + mGoldPlayers.size();
        if (mSilverPlayers != null) str += ", mSilverPlayers=" + mSilverPlayers.size();
        str += '}';
        return str;
    }
}