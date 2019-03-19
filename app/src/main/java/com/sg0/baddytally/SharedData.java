package com.sg0.baddytally;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

public class SharedData {
    private static final String TAG = "SharedData";
    private static final String SHUFFLE_START = "SH_ST";
    private static final String SHUFFLE_SUCCESS = "SH_E";
    private static final String SHUFFLE_FAIL = "SH_F";
    private static final String NEWUSER_CREATED = "NEW_U";
    private static final String USER_MOVED = "USR_M";
    private static final String USER_DELETED = "USR_D";
    private static final String INNINGS_CREATED = "NEW_I";


    //with volatile variable guarantees happens-before relationship,
    // all the write will happen on volatile sSoleInstance before any read of sSoleInstance variable
    private static volatile SharedData sSoleInstance;

    Integer mNumOfGroups;
    String mUser;
    public String mClub;
    String mRole;
    String mInnings;
    String mRoundName;
    String mAdminCode;
    String mMemCode;
    String mRootCode;
    ArrayList<PlayerData> mGoldPlayers;
    ArrayList<PlayerData> mSilverPlayers;
    Integer mInningsDBKey;
    Integer mWinPercNum;
    private boolean mDBUpdated;
    private boolean mUserNotifyEnabled;
    private boolean mDBConnected;
    private boolean mDBLock;
    private Long mDBLockAcqAttemptTime;
    private String mStoreHistory;
    private ValueEventListener mDBConnectListener;
    ArrayList<ActiveUserDBEntry> mActiveUsers;
    Set<String> mGoldPresentPlayerNames;
    Set<String> mSilverPresentPlayerNames;
    //public boolean mTournaMode;
    HashMap<String, String> mTournaMap;
    public String mTournament;
    List<String> mTeams;
    HashMap<String, TeamInfo> mTeamInfoMap;
    String mTournaType;
    int mTable_view_resid;
    int mCount;   //general purpose count


    public List<String> getTeamPlayers(String team) {
        if(mTeamInfoMap==null) return new ArrayList<>();
        TeamInfo tI = mTeamInfoMap.get(team);
        if(tI==null) return new ArrayList<>();
        return tI.players;
    }

    private SharedData() {
        //Prevent form the reflection api.
        if (sSoleInstance != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }

        mDBConnectListener = null;
        clear();
    }  //private constructor.

    public static SharedData getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (SharedData.class) {
                //Check for the second time and make it thread safe
                //This wont be an issue actually in this app, as getInstance is called the first time for main:onCreate()
                sSoleInstance = new SharedData();
            }
        }
        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    //Unused in this app.
    protected SharedData readResolve() {
        return getInstance();
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
        mWinPercNum = Constants.SHUFFLE_WINPERC_NUM_GAMES;
        mDBUpdated = false;
        mUserNotifyEnabled = true;
        mDBConnected = false;
        mDBLock = false;
        mDBLockAcqAttemptTime = 0L;
        mStoreHistory = "";
        mActiveUsers = null;
        mGoldPresentPlayerNames = new HashSet<>();  //initialize for the first time.
        mSilverPresentPlayerNames = new HashSet<>();  //initialize for the first time.
        //mTournaMode = false;
        mTournaMap = null;
        mTournament = "";
        mTeams = null;
        mTeams = new ArrayList<>();
        mTeamInfoMap = null;
        mTeamInfoMap = new HashMap<>();
        mTournaType = "";
        mTable_view_resid = R.layout.tourna_match_info_tiny;
        mCount = 0;
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

    public void initData(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        String club = prefs.getString(Constants.DATA_CLUB, "");
        if (!club.isEmpty()) {
            Log.d(TAG, "initData for " + club);
            mClub = club;
            mUser = prefs.getString(Constants.DATA_USER, "");
            mRole = prefs.getString(Constants.DATA_ROLE, "");
            //mTournaMode = prefs.getBoolean(Constants.DATA_TMODE, false);
            Log.d(TAG, "initData: " + SharedData.getInstance().toString());
        }
    }

    private void addHistory(final String story) {
        if(story.isEmpty()) return;
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT, Locale.CANADA);
        String dateStr = df.format(c);
            if (isUserNotifyEnabled()) {
                //All the user delete/create log can be added as a single entry.
                final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference().child(SharedData.getInstance().mClub).child(Constants.INTERNALS).child(Constants.HISTORY);
                DatabaseReference newHistory = inningsDBRef.push();
                newHistory.setValue(dateStr + "#" + mUser + "#" + story);
            } else mStoreHistory += story + ",";
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
        else if (tmpStr.contains(USER_MOVED)) resultStr = tmpStr.replace(USER_MOVED, "Moved");
        else if (tmpStr.contains(SHUFFLE_SUCCESS))
            resultStr = tmpStr.replace(SHUFFLE_SUCCESS, "Shuffle Successful");
        else if (tmpStr.contains(SHUFFLE_FAIL))
            resultStr = tmpStr.replace(SHUFFLE_FAIL, "Shuffle Failed");
        else if (tmpStr.contains(INNINGS_CREATED))
            resultStr = tmpStr.replace(INNINGS_CREATED, "New Innings");

        return resultStr;
    }

    public void addShuffleStart2History(final String newInningsName) {
        addHistory(SHUFFLE_START + "=" + newInningsName);
    }

    public void addShuffleSuccess2History(final String newInningsName) {
        addHistory(SHUFFLE_SUCCESS + "=" + newInningsName);
    }

    public void addShuffleFailure2History(final String newInningsName) {
        addHistory(SHUFFLE_FAIL + "=" + newInningsName);
    }

    public void addNewUserCreation2History(final String newUserName) {
        addHistory(NEWUSER_CREATED + "=" + newUserName);
    }

    public void addUserMove2History(final String userName) {
        addHistory(USER_MOVED + "=" + userName);
    }

    public void addUserDeletion2History(final String userName) {
        addHistory(USER_DELETED + "=" + userName);
    }

    public void addInningsCreation2History(final String innings) {
        addHistory(INNINGS_CREATED + "=" + innings);
    }

    public void disableUserNotify(Context context) {

            mUserNotifyEnabled = false;
            mStoreHistory = "";
    }

    public void enableUserNotify(Context context) {
            mUserNotifyEnabled = true;
            addHistory(mStoreHistory);
    }

    public boolean isUserNotifyEnabled() {
        return mUserNotifyEnabled;
    }

    public void showToast(final Context c, final CharSequence s, final int d) {
        if (mUserNotifyEnabled) Toast.makeText(c, s, d).show();
        Log.i(TAG, s.toString());
    }

    public SpannableStringBuilder getColorString(CharSequence text, int color) {
        if(text.length()==0) return new SpannableStringBuilder(text);
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssBuilder;
    }

    public SpannableStringBuilder getBgColorString(CharSequence text, int color) {
        if(text.length()==0) return new SpannableStringBuilder(text);
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new BackgroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssBuilder;
    }

    public SpannableStringBuilder getStyleString(CharSequence text, int style) {
        if(text.length()==0) return new SpannableStringBuilder(text);
        //style=Typeface.ITALIC
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new StyleSpan(style), 0, text.length(), 0);
        return ssBuilder;
    }

    public SpannableStringBuilder getSizeString(CharSequence text, float proportion) {
        if(text.length()==0) return new SpannableStringBuilder(text);
        //style=Typeface.ITALIC
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new RelativeSizeSpan(proportion), 0, text.length(), 0);
        return ssBuilder;
    }

    public SpannableStringBuilder getStrikethroughString(CharSequence text) {
        if(text.length()==0) return new SpannableStringBuilder(text);
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new StrikethroughSpan(), 0, text.length(), 0);
        return ssBuilder;
    }

    public SpannableStringBuilder getTitleStr(String title, Context context) {
        return getColorString(title,   //add some color to it. Use the color scheme color from color.xml
                ResourcesCompat.getColor(context.getResources(), R.color.colorPrimaryDark, null));
    }

    public String getShortRoundName(String round) {
        //show a more human readable round name (date format w/o HH:mm)
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.ROUND_DATEFORMAT, Locale.CANADA);
        try {
            Date d = sdf.parse(round);
            sdf.applyPattern(Constants.ROUND_DATEFORMAT_SHORT);
            round = sdf.format(d);
        } catch (ParseException ex) {
            Log.w(TAG, "setTitle ParseException:" + ex.getMessage());
        }
        return round;
    }

    public void sortPlayers(ArrayList<PlayerData> playersList, final int idx, //innings or season
                            final boolean descending, final Context context, final boolean showToasts) {
        //playersList is obj reference of the actual list being passed in.
        //Any updates to the contents will be visible to the caller.
        //But, you cannot change the obj reference itself (ex: playersList = another_list;).
        final String[] infoLog = {""};  //has to be final to be accessed from inner class, but needs to be updated in inner class. Thus, array.
        Collections.sort(playersList, new Comparator<PlayerData>() {
            @Override
            public int compare(PlayerData p1, PlayerData p2) {
                return compare2Players(p1, p2, idx);
                /*
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
                                //win%, Games Won & gamesPlayed are all equal for 2 players. If the sorting is on innings, then
                                //sort on season points.
                                if(idx == Constants.INNINGS_IDX) {

                                }
                                //Toss: Return a random value +1 or -1
                                //Log.v(TAG, "sortPlayers: Tossing to sort " + p1.getDesc() + " and " + p2.getDesc());
                                //Log.v(TAG, "sortPlayers: p1=" + p1.toString() + " and p2=" + p2.toString());
                                if (!infoLog[0].isEmpty()) infoLog[0] += ", ";
                                infoLog[0] += p1.getDesc() + " & " + p2.getDesc();
                                return randomPick();
                            } else return value4;
                        } else return value3;
                    } else return value2;
                }
                return value1;
                */
            }

            private int compare2Players(final PlayerData p1, final PlayerData p2, final int idx) {
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
                                //win%, Games Won & gamesPlayed are all equal for 2 players.
                                if (idx == Constants.INNINGS_IDX) {
                                    //If the sorting is on innings, then sort on season points.
                                    //Log.v(TAG, "sortPlayers: ALL equal for INNINGS, now on SEASON for " + p1.getName() + " and " + p2.getName());
                                    return compare2Players(p1, p2, Constants.SEASON_IDX);
                                } else {
                                    //If the sorting is on season points, then toss (pick a random winner among the 2 players being compared).
                                    //Toss: Return a random value +1 or -1
                                    //Log.v(TAG, "sortPlayers: Tossing to sort " + p1.getDesc() + " and " + p2.getDesc());
                                    //Log.v(TAG, "sortPlayers: p1=" + p1.toString() + " and p2=" + p2.toString());
                                    if (!infoLog[0].isEmpty()) infoLog[0] += ", ";
                                    infoLog[0] += p1.getName() + " & " + p2.getName();
                                    return randomPick();
                                }

                            } else return value4;
                        } else return value3;
                    } else return value2;
                }
                return value1;
            }

            private int randomPick() {
                int rand = new Random().nextInt(10);
                if ((rand % 2) == 0) {
                    //Log.w(TAG, "sortPlayers: Toss: randomPick returning +1");
                    return 1;
                }
                //Log.w(TAG, "sortPlayers: Toss: randomPick returning -1");
                return -1;
            }
        });
        if (!descending) Collections.reverse(playersList);
        if (showToasts && !infoLog[0].isEmpty()) {
            Toast.makeText(context,
                    "Tossed a coin to sort: " + infoLog[0], Toast.LENGTH_SHORT).show();
        }
        //Log.d(TAG, "sortPlayers: Sorted playersList size: " + Integer.toString(playersList.size()));
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

        /* This routine was first implemented for the below DB node:
        final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference()
                                               .child(mClub).child(Constants.INTERNALS).child("awake");

        But, then it was changed to read user-data:
        Instead of simply doing a mock write to wakeup DB connection, lets keep tab of the last login from this user.
        */
        if(mClub.isEmpty() || mUser.isEmpty()) return;
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("MM-dd", Locale.CANADA);
        final String login_day = df.format(c);
        final String model_name = ""; //android.os.Build.MODEL might be leading to privacy error from google, not sure.


        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub)
                .child(Constants.ACTIVE_USERS).child(mUser);
        dbRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                ActiveUserDBEntry userData = mutableData.getValue(ActiveUserDBEntry.class);
                if (userData == null) return Transaction.success(mutableData);
                else {
                    Log.w(TAG, "wakeUpDBConnection: update DB for user:" + mUser);
                    mutableData.setValue(new ActiveUserDBEntry(mRole, model_name, login_day, BuildConfig.VERSION_NAME));
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
                    try {
                        ActiveUserDBEntry userData = dataSnapshot.getValue(ActiveUserDBEntry.class);
                        Log.w(TAG, "wakeUpDBConnection: onComplete: Success: " + userData.toString());
                    } catch (NullPointerException e) {
                        if(mClub.isEmpty()) return;
                        //java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.Boolean.booleanValue()' on a null object reference
                        //SGO: If INTERNALS/locked node is not present in DB, dataSnapshot is not set to null, but a null pointer exception
                        //     is thrown here. Just gonna catch it here to avoid crash.
                        //This was seen in a case where innings was on going, and the app was upgraded. Newer version of app depended on INTERNALS/locked
                        //but the older version never new about INTERNALS/locked. Workaround was to manually create INTERNALS/locked in the DB for that club.
                        //This issue will not happen if the new app is used to create the innings (INTERNALS/locked node iwll be created in DB).
                        Log.w(TAG, "wakeUpDBConnection: onComplete: Error:" + e.getMessage());
                        dbRef.setValue(new ActiveUserDBEntry(mRole, model_name,
                                login_day, BuildConfig.VERSION_NAME));
                        Log.w(TAG, "New user created in DB:" + mUser);
                    }

                }
            }
        });
    }

    public void wakeUpDBConnection_profile() {
        // Do a mock transaction to wake up the database connection.
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(mClub).child(Constants.PROFILE);

        dbRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {

                //Log.w(TAG, "wakeUpDBConnection_profile: onDataChange");
                for (MutableData child : mutableData.getChildren()) {
                    if(null==child) continue;
                    switch (child.getKey()) {
                        case "admincode":
                            mAdminCode = child.getValue(String.class);
                            break;
                        case "memcode":
                            mMemCode = child.getValue(String.class);
                            break;
                        case "rootcode":
                            mRootCode = child.getValue(String.class);
                            break;
                        case "numgroups":
                            mNumOfGroups = child.getValue(Integer.class);
                            break;
                        case "wake":
                            //just write something into DB to keep the DB connection alive
                            Boolean wake = child.getValue(Boolean.class);
                            child.setValue(!wake);
                            Log.w(TAG, "doTransaction: wake=" + !wake );
                            break;
                    } //switch case
                }
                Log.w(TAG, "wakeUpDBConnection_profile: onDataChange:" + mAdminCode + "/" + mRootCode);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot dataSnapshot) { }
        });
    }

    public Boolean isPermitted(final Context context){
        SharedPreferences prefs = context.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        final String secpd = prefs.getString(Constants.DATA_SEC, "");
        Log.d(TAG, "isPermitted: " + secpd + " " +mRootCode + "," + mAdminCode + "," + mMemCode);
        if(secpd.isEmpty()) return true;
        if (secpd.equals(mAdminCode)) {
            return true;
        } else if (secpd.equals(mMemCode)) {
            return true;
        } else if (secpd.equals(mRootCode)) {
            return true;
        }

        Toast.makeText(context,
                "No permission, login again using new credentials",
                Toast.LENGTH_LONG).show();
        return false;
    }

    public boolean isDBLocked() {
        Log.d(TAG, "isDBLocked: " + mDBLock + "," + mDBLockAcqAttemptTime);
        if(mDBLock) mDBLockAcqAttemptTime = 0L;  //Acquire attempt was successful, so reset.
        return mDBLock;
    }

    //There is no need to synchronize this, as it is never called from different threads.
    //Idea is to synchronize with others users operating on teh same DB.
    private void changeDBLockState(final boolean target, final String tourna) {
        // Why a lock?
        //    -- main reason being Innings creation is done by only one root user.
        //    -- added advantage is that a DB online check is done before the critical operation is performed
        //    -- there are comments saying that performing a write makes sure that the DB is in sync.
        //       (but that might be only for that particular DB tree, anyways!)
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub);
        if(null==tourna || tourna.isEmpty())
            dbRef = dbRef.child(Constants.INTERNALS).child(Constants.LOCK);
        else
            dbRef = dbRef.child(Constants.TOURNA).child(tourna)
                    .child(Constants.INTERNALS).child(Constants.LOCK);
        dbRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Boolean locked = mutableData.getValue(Boolean.class);
                if (locked == null) {
                    //most of the time, this routine is invoked twice: once with null, and then with correct value.
                    //return success first time w/o changing the value. Change the value in the second invocation below.
                    return Transaction.success(mutableData);
                    //Since null cant be handled here to create the very first locked attribute, it should be created elsewhere
                    //once for a club.
                }
                else {
                    if (locked == target) {
                        Log.w(TAG, "changeDBLockState:" + tourna + " Already set to " + target);
                        return Transaction.abort();
                    } else {
                        Log.w(TAG, "changeDBLockState:" + tourna + " lock changed to " + target);
                        mutableData.setValue(target);
                        //SGO: mDBLock was designed to be set from onComplete below.
                        //But, there seems to be cases where onComplete() doesnt seem to be invoked.
                        //I saw this for a success case (lock changed to true), may be it happens on failure cases too.
                        //Below set is added as a workaround.
                        mDBLock = target;
                    }
                    return Transaction.success(mutableData);
                }

            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if (error != null || !committed || dataSnapshot == null) {
                    if (null != error)
                        Log.w(TAG, "changeDBLockState: onComplete: Failed:", error.toException());
                    else Log.w(TAG, "changeDBLockState: onComplete: Failed"); //DB connection might be stale
                    mDBLock = false;
                } else {
                    try {
                        mDBLock = dataSnapshot.getValue(Boolean.class);
                        Log.w(TAG, "changeDBLockState: onComplete: Success:" + mDBLock + "," + mDBLockAcqAttemptTime);
                    } catch (NullPointerException e) {
                        //java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.Boolean.booleanValue()' on a null object reference
                        //SGO: If INTERNALS/locked node is not present in DB, dataSnapshot is not set to null, but a null pointer exception
                        //     is thrown here. Just gonna catch it here to avoid crash.
                        //This was seen in a case where innings was on going, and the app was upgraded. Newer version of app depended on INTERNALS/locked
                        //but the older version never knew about INTERNALS/locked. Workaround was to manually create INTERNALS/locked in the DB for that club.
                        //This issue will not happen if the new app is used to create the innings (INTERNALS/locked node iwll be created in DB).
                        Log.w(TAG, "changeDBLockState: onComplete: Error:" + e.getMessage());
                        mDBLock = false;
                    }

                }
            }
        });
    }


/* In some cases, getting a lock takes more time. See the sequence below.
03-04 15:57:56.081 23935-23935/com.sg0.baddytally I/SharedData: acquireDBLock: acquiring...0
03-04 15:57:56.604 23935-23935/com.sg0.baddytally V/BaseEnterData: After DB lock wait...
03-04 15:57:56.605 23935-23935/com.sg0.baddytally D/BaseEnterData: workToUpdateDB: club1
03-04 15:57:56.605 23935-23935/com.sg0.baddytally D/SharedData: isDBLocked: false,25862217
03-04 15:57:56.605 23935-23935/com.sg0.baddytally E/BaseEnterData: workToUpdateDB: Another update is in progress, please refresh and try again later...
03-04 15:57:57.106 23935-23935/com.sg0.baddytally D/BaseEnterData: releaseLockAndCleanup: fin=true, dbUpd=true
03-04 15:57:57.106 23935-23935/com.sg0.baddytally D/SharedData: isDBLocked: false,25862217
03-04 15:57:57.270 23935-24054/com.sg0.baddytally W/SharedData: wakeUpDBConnection: update DB for user:usr
03-04 15:57:57.295 23935-24054/com.sg0.baddytally W/SharedData: changeDBLockState:DEMO lock changed to true
03-04 15:57:57.321 23935-23935/com.sg0.baddytally I/Choreographer: Skipped 43 frames!  The application may be doing too much work on its main thread.
03-04 15:57:57.384 23935-23935/com.sg0.baddytally W/SharedData: wakeUpDBConnection: onComplete: Success: ActiveUserDBEntry{ver='2.1', role='root', device='C6806', last_login='03-04'}
03-04 15:57:57.429 23935-23935/com.sg0.baddytally W/SharedData: changeDBLockState: onComplete: Success:true,25862217
*/

    public void acquireDBLock() {
        acquireDBLock(null);
    }

    public void acquireDBLock(final String tourna) {
        mDBLock = false;
        Log.i(TAG, "acquireDBLock: acquiring..." + mDBLockAcqAttemptTime);
        changeDBLockState(true, tourna);
        if(mDBLockAcqAttemptTime >0L) {
            Long timeInMins = System.currentTimeMillis() / 60000;
            if ((timeInMins - mDBLockAcqAttemptTime) > 3) {
                //DB is been locked for more than 3 mins.
                //This is mostly a failure to update DB while releasing lock (say the n/w connection was lost
                //after acquiring lock or app crashed after acquiring lock etc). So, release the lock forcefully.
                //There is a very rare chance that when a particular user tried to acquire the
                //lock twice (first attempt when mDBLockAcqAttemptTime was filled and now) and there
                //other users (could be different ones) holding on to the lock at that very moment.
                //Even in this case, lock is forcefully released. Could be an issue when the user base increases!
                Log.w(TAG, "isDBLocked: force release lock: " + (timeInMins - mDBLockAcqAttemptTime) +
                        " timeInMins=" + timeInMins + " mDBLockAcqAttemptTime=" + mDBLockAcqAttemptTime);
                releaseDBLock(true, tourna);
            }
        } else mDBLockAcqAttemptTime = System.currentTimeMillis() / 60000;
    }

    public void releaseDBLock() {
        //public API: Lock is released only if locked by this device.
        releaseDBLock(false, null);
    }

    public void releaseDBLock(final String tourna) {
        //public API: Lock is released only if locked by this device.
        releaseDBLock(false, tourna);
    }

    private void releaseDBLock(final Boolean force, final String tourna) {

        if(force || isDBLocked()) {
            //SGO: If force flag is set, Lock is released even if not locked by this device.
            changeDBLockState(false, tourna);
            mDBLockAcqAttemptTime = 0L;
            Log.i(TAG, "releaseDBLock: releasing..." + mDBLockAcqAttemptTime);
        }

    }

    public void createDBLock(final String tourna) {
        DatabaseReference tmpDBRef = FirebaseDatabase.getInstance().getReference()
                .child(mClub);
        if(null==tourna || tourna.isEmpty())
            tmpDBRef = tmpDBRef.child(Constants.INTERNALS);
        else
            tmpDBRef = tmpDBRef.child(Constants.TOURNA).child(tourna)
                    .child(Constants.INTERNALS);
        final DatabaseReference dbRef = tmpDBRef;
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot==null || !dataSnapshot.hasChild(Constants.LOCK)) {
                    // internals attr not found, or locked child not found
                    Log.i(TAG, "createDBLock: lock attr not found, creating...");
                    dbRef.child(Constants.LOCK).setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "createDBLock: databaseError=" + databaseError);
            }
        });
    }

    public void fetchProfile(final CallbackRoutine cb, final Context context, final String club) {
        //club is passed in as SharedData will not be populated yet with club info when invoked from LoginActivity.
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(club).child(Constants.PROFILE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(null==dataSnapshot) return;
                Log.w(TAG, "fetchProfile: onDataChange");
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if(null==child) continue;
                    switch (child.getKey()) {
                        case "admincode":
                            mAdminCode = child.getValue(String.class);
                            break;
                        case "memcode":
                            mMemCode = child.getValue(String.class);
                            break;
                        case "rootcode":
                            mRootCode = child.getValue(String.class);
                            break;
                        case "numgroups":
                            mNumOfGroups = child.getValue(Integer.class);
                            break;

                    } //switch case
                }
                //Log.w(TAG, "fetchProfile: onDataChange:" + mAdminCode + "/" + mRootCode);
                cb.profileFetched();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchProfile: onCancelled", databaseError.toException());
                Toast.makeText(context,
                        "Login Profile DB error:" + databaseError.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public String createNewRoundName(boolean commit, Context context) {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT, Locale.CANADA);
        String rndName = df.format(c);
        if (commit) {
            Log.w(TAG, "createNewRoundName: committing:" + rndName);
            SharedPreferences prefs = context.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.NEWROUND, rndName);
            editor.apply();
        }
        return rndName;
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

    public Boolean isLeagueTournament(final String tourna) {
        String tournaType = getTournamentType(tourna);
        if(tournaType.isEmpty()) return false;

        Log.i(TAG, "isLeagueTournament:" + tourna + "," + tournaType);
        if(tournaType.equals(Constants.LEAGUE)) {
            return true;
        }
        return false;
    }

    public Boolean isEliminationTournament(final String tourna) {
        String tournaType = getTournamentType(tourna);
        if(tournaType.isEmpty()) return false;

        Log.i(TAG, "isEliminationTournament:" + tourna + "," + tournaType);
        if(tournaType.equals(Constants.SE) || tournaType.equals(Constants.DE)) {
            return true;
        }
        return false;
    }

    public Boolean isSETournament(final String tourna) {
        return getTournamentType(tourna).equals(Constants.SE);
    }

    public Boolean isDETournament(final String tourna) {
        return getTournamentType(tourna).equals(Constants.DE);
    }

    public String getTournamentType(final String tourna) {
        if(null==tourna || tourna.isEmpty()) return "";
        if(null==mTournaMap || mTournaMap.size()==0) return "";

        String tournaType = mTournaMap.get(tourna);
        if(null==tournaType) return "";

        return tournaType;
    }

    public void readDBTeam(final String tournament, final Context context, final CallbackRoutine cb) {
        if (tournament.isEmpty()) return;
        final DatabaseReference teamScoreDBRef = FirebaseDatabase.getInstance().getReference().child(mClub)
                .child(Constants.TOURNA)
                .child(tournament).child(Constants.TEAMS);
        teamScoreDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mTeamInfoMap.clear();
                mTeams.clear();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {

                    final String team = childSnapshot.getKey();
                    Log.i(TAG, "onDataChange Got:" + team);

                    TeamInfo tI = new TeamInfo(team);
                    mTeams.add(team);  //still need array to get a list of teams in order

                    DataSnapshot descDS = childSnapshot.child(Constants.DESCRIPTION);
                    String desc = descDS.getValue(String.class);
                    if (desc != null) tI.desc = desc;
                    else tI.desc = "";

                    DataSnapshot scoreData = childSnapshot.child(Constants.SCORE);
                    TeamScoreDBEntry scoreDBEntry = scoreData.getValue(TeamScoreDBEntry.class);
                    if (scoreDBEntry != null) tI.score = scoreDBEntry;
                    else tI.score = new TeamScoreDBEntry();
                    Log.i(TAG, "onDataChange, scoreDBEntry:" + tI.score.toString());

                    mTeamInfoMap.put(team, tI);
                }

                if (mTeams.size() > 0) {
                    cb.completed(tournament, true);
                } else {
                    cb.completed(tournament, false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showToast(context, "DB error on read: " + databaseError.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }


    public TeamInfo getTeamInfo(final String team) {
        TeamInfo tI = null;
        if (mTeamInfoMap == null) return tI;
        for (Map.Entry<String, TeamInfo> entry : mTeamInfoMap.entrySet()) {
            if (team.equals(entry.getKey())) {
                tI = entry.getValue();
                break;
            }
        }
        return tI;
    }
/*
        Iterator it = mTeamInfoMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if(team.equals(pair.getKey())) {
                tI = (TeamInfo) pair.getValue();
                break;
            }
        }
*/

    public Boolean checkIfPlayerAlreadyExists(final Context context, final String pShort) {
        for (Map.Entry<String, TeamInfo> entry : mTeamInfoMap.entrySet()) {
            TeamInfo tI = entry.getValue();
            for(String p_nick: tI.p_nicks) {
                if(p_nick.equals(pShort)) {
                    Log.d(TAG, pShort + " already exists in " + tI.name);
                    //showToast(context, pShort + " already exists in " + tI.name, Toast.LENGTH_SHORT);
                    return true;
                }
            }
        }
        return false;
    }



    public void sortTeams() {
        //playersList is obj reference of the actual list being passed in.
        //Any updates to the contents will be visible to the caller.
        //But, you cannot change the obj reference itself (ex: playersList = another_list;).
        Collections.sort(mTeams, new Comparator<String>() {
            @Override
            public int compare(String team1, String team2) {
                TeamInfo tInfo1 = getTeamInfo(team1);
                TeamInfo tInfo2 = getTeamInfo(team2);
                int value1 = Integer.valueOf(tInfo2.score.getPts()).compareTo(tInfo1.score.getPts()); //descending
                if (value1 == 0) {
                    int value2 = Integer.valueOf(tInfo2.score.getmW()).compareTo(tInfo1.score.getmW());
                    if (value2 == 0) {
                        int value3 = Integer.valueOf(tInfo2.score.getgW()).compareTo(tInfo1.score.getgW());
                        if (value3 == 0) {
                            return Integer.valueOf(tInfo2.score.getgPts()).compareTo(tInfo1.score.getgPts());
                        } else return value3;
                    } else return value2;
                }
                return value1;
            }
        });
        Log.d(TAG, "sortTeams: Sorted mTeams: " + mTeams.toString());
    }

    public void showAlert(final CallbackRoutine cb, final Context context, final String title, final String msg) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setTitle(title);
        alertBuilder.setMessage(msg);
        alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(null!=cb) cb.alertResult(title, true, false);
            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(null!=cb) cb.alertResult(title, false, true);
            }
        });
        alertBuilder.show();
    }

    public static String truncate(String str, int len) {
        String retStr;
        if (str.length() > len) {
            retStr = str.substring(0, len);
        } else {
            return str;
        }
        if(len>5) retStr += "..";
        return retStr;
    }

    // Get a MemoryInfo object for the device's current memory status.
    // Note: This is device's overall memory status, not specific to this app
    public ActivityManager.MemoryInfo getAvailableMemory(final Activity mActivity) {
        ActivityManager activityManager = (ActivityManager) mActivity.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        Log.v(TAG, "getAvailableMemory: avail=" + memoryInfo.availMem/(1048576)  +
                "MB thres=" + memoryInfo.threshold/(1048576) +
                "MB total=" + memoryInfo.totalMem/(1048576));
        return memoryInfo;
    }

    public void printHeapUsage(final String msg) {
        // Get current size of heap in bytes
        long heapSize = Runtime.getRuntime().totalMemory();

        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        // Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();

        // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
        long heapFreeSize = Runtime.getRuntime().freeMemory();

        Log.d(TAG, msg + " printHeapUsage: size=" + heapSize/1024 +
                " max=" + heapMaxSize/1024 + " free=" + heapFreeSize/1024);
    }

    public void killActivity(final Activity activity, final int res_code) {
        Log.d(TAG, "killActivity: " + activity.getLocalClassName());
        activity.setResult(res_code);
        activity.finish();
    }

    public void killApplication(final Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            Log.w(TAG, "+++++++++++++ killApplication(finishAndRemoveTask) +++++++++++++");
            activity.finishAndRemoveTask();
        } else {
            Log.w(TAG, "+++++++++++++ killApplication(exit) +++++++++++++");
            activity.finishAffinity();
            //android.os.Process.killProcess(android.os.Process.myPid());
            //System.exit(0);
        }
    }

    public String getAlbumStorageDirStr() {
        return Environment.DIRECTORY_DOCUMENTS;
    }

    public File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                getAlbumStorageDirStr()), Constants.APPNAME);
        if (!file.mkdirs()) {
            if (file.isDirectory()) {
                Log.d(TAG, "getAlbumStorageDir: Directory has already been created:" +
                        file.getAbsolutePath());
            } else {
                Log.d(TAG, "getAlbumStorageDir: Directory could not be created:" +
                        file.getAbsolutePath());
                return null;
            }
        }
        return file;
    }


    public static final int READ_REQUEST_CODE = 42;
    public static final int STORAGE_PERMISSION_CODE = 43;

    /**
     * Checks if external storage is available for read and write
     *
     * @return boolean
     */
    public boolean isExternalStorageWritable(final Activity activity) {
        if (isStoragePermissionGranted(activity)) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStoragePermissionGranted(final Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //Log.v(TAG,"Permission is granted");
                return true;
            } else {
                //Log.v(TAG,"Permission is revoked:" +Build.VERSION.SDK_INT);
                activity.requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            //Log.v(TAG,"Permission is granted:" +Build.VERSION.SDK_INT);
            return true;
        }
    }

    public void showFileChooser(final Activity activity, final int FILE_SELECT_CODE) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.setType("text/plain");
        Uri uri = Uri.parse(getAlbumStorageDir().getPath());
        Log.d(TAG, "showFileChooser: " + uri.toString());
        intent.setDataAndType(uri, "text/csv");
        try {
            activity.startActivityForResult(  Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (Exception e) {
            Log.e(TAG, " choose file error "+e.toString());
        }
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch(final Activity activity) {
        Log.d(TAG, "performFileSearch: ");

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        //intent.setType("text/plain");
        intent.setType("application/vnd.ms-excel");

        activity.startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public String readTextFromUri(final Activity activity, Uri uri) throws IOException {
        InputStream inputStream = activity.getContentResolver().openInputStream(uri);
        if(null==inputStream) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        reader.close();
        Log.e(TAG, "readTextFromUri: " + stringBuilder.toString());
        return stringBuilder.toString();
    }

    public ArrayList<TeamInfo> readExcel(final Activity activity, Uri uri) {
        ArrayList<TeamInfo> retList = new ArrayList<>();
        try
        {
            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
            if(null==inputStream) {
                Log.e(TAG, "readExcel: null stream");
                return retList;
            }
            Workbook m_workBook = Workbook.getWorkbook(inputStream);
            //p_sheetNo is excel sheet no which u want to read
            Sheet sheet = m_workBook.getSheet(0);

            if(!sheet.getCell(0, 0).getContents().contains("TID")) {
                Log.e(TAG, "readExcel: row=0, col=0: TID not found");
                return retList;
            }
            if(!sheet.getCell(1, 0).getContents().contains("TEAM")) {
                Log.e(TAG, "readExcel: row=0, col=1: TEAM not found");
                return retList;
            }
            if(!sheet.getCell(2, 0).getContents().contains("PID")) {
                Log.e(TAG, "readExcel: row=0, col=2: PID not found");
                return retList;
            }
            if(!sheet.getCell(3, 0).getContents().contains("NAME")) {
                Log.e(TAG, "readExcel: row=0, col=3: NAME not found");
                return retList;
            }

            int rowsAdded = 0;
            for (int row = 1; row < sheet.getRows(); row++)
            {
                String errMsg = "";
                Cell tid = sheet.getCell(0, row);
                //if(null==tid) continue;
                Cell teamName = sheet.getCell(1, row);
                //if(null==teamName) continue;
                TeamInfo tI = new TeamInfo();
                tI.name = tid.getContents();
                tI.desc = teamName.getContents();
                if(tI.name.isEmpty() || tI.desc.isEmpty()) {
                    Log.w(TAG, "readExcel: Bad data:" + tI.name + ":" + tI.desc);
                    continue;
                }

                for(int col=2 ; col < sheet.getColumns(); col+=2)
                {
                    String pid = sheet.getCell(col, row).getContents();
                    String playerName = sheet.getCell(col+1, row).getContents();
                    //if(null==pid) tI.p_nicks.add("");
                    if(pid.isEmpty() && playerName.isEmpty()) continue;
                    tI.p_nicks.add(pid);
                    tI.players.add(playerName);
                }
                Log.d(TAG, row + " readExcel: Adding " + tI.toString());
                retList.add(tI);
                rowsAdded++;
            }
        }
        catch (BiffException e)
        {
            Log.e(TAG, "readExcel: " + e.getMessage() );
            //e.printStackTrace();
        }
        catch (IOException e)
        {
            Log.e(TAG, "readExcel: " + e.getMessage() );
            //e.printStackTrace();
        }

        return  retList;
    }
}

