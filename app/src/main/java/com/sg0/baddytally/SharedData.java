package com.sg0.baddytally;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

/*
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
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
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static java.lang.Math.log;
import static java.lang.Math.random;

public class SharedData {
    static final int READ_REQUEST_CODE = 42;
    static final int STORAGE_PERMISSION_CODE = 43;
    private static final String TAG = "SharedData";
    private static final String SHUFFLE_START = "SH_ST";
    private static final String SHUFFLE_SUCCESS = "SH_E";
    private static final String SHUFFLE_FAIL = "SH_F";
    private static final String NEWUSER_CREATED = "NEW_U";
    private static final String USER_MOVED = "USR_M";
    private static final String USER_DELETED = "USR_D";
    private static final String INNINGS_CREATED = "NEW_I";
    private static final String RESET_DATA = "RST_D";
    private static final String RESET_DATA_ALL = "RST_DE";
    private static final String USERID_DELM = "#_U_";
    private static final String DATE_DELM = "#_D_";
    //with volatile variable guarantees happens-before relationship,
    // all the write will happen on volatile sSoleInstance before any read of sSoleInstance variable
    private static volatile SharedData sSoleInstance;
    public String mClub;
    public String mTournament;
    Integer mNumOfGroups;
    String mUser;
    String mRole;
    String mInnings;
    String mRoundName;
    ProfileDBEntry mProfile;
    ProfileDBEntry mRootProfile;
    String mReadNews;
    ArrayList<PlayerData> mGoldPlayers;
    ArrayList<PlayerData> mSilverPlayers;
    Integer mInningsDBKey;
    Integer mWinPercNum;
    Set<String> mGoldPresentPlayerNames;
    Set<String> mSilverPresentPlayerNames;
    HashMap<String, String> mTournaMap;
    List<String> mTeams;
    HashMap<String, TeamInfo> mTeamInfoMap;
    String mFlags;
    int mTable_view_resid;
    ArrayList<String> mStrList;  //general purpose Str list for temp use
    boolean mOfflineMode;
    //mCount: general purpose count; used today for:
    //1. Login failure attempts
    int mCount;
    //mTime: general purpose Long variable; used today for:
    //1. No of continuous refreshs done in ClubLeague/TournaLeague/TournaTableLayout
    Long mTime;
    boolean mDemoMode;

    private boolean mDBUpdated;
    private boolean mUserNotifyEnabled;
    private boolean mDBConnected;
    private boolean mDBLock;
    private Long mDBLockAcqAttemptTime;
    private String mStoreHistory;
    private ValueEventListener mDBConnectListener;
    private ValueEventListener mNewClubListener;




    private SharedData() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }

        //Log.d(TAG, "SharedData: ctor");
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

    static String truncate(final String str, final boolean dots, int len) {
        if(str==null) return "";
        String retStr = str;
        if (len <= 0) len = Constants.TINYNAMELENGTH;
        if (str.length() > len) {
            if (dots) retStr = str.substring(0, len - 2) + "..";
            else retStr = str.substring(0, len);
        }
        return retStr;
    }

    static boolean isValidString(final String s) {
        if(s.isEmpty()) return true;
        //alphanumeric or <hyphen> or <underscore> or <space>
        return s.matches("[A-Za-z0-9\\-_ ]+");
    }

    public static void writeStringAsFile(final Activity activity,
                                         final String fileContents,
                                         final File fileDir,
                                         final String fileName) {
        File dir = fileDir;
        if (fileDir == null) dir = activity.getFilesDir();

        try {
            FileWriter out = new FileWriter(new File(dir, fileName));
            out.write(fileContents);
            out.close();
        } catch (IOException e) {
            Log.w(TAG, "writeStringAsFile: ", e);
        }
    }

    static void showAboutAlert(final Activity activity) {
        //int versionCode = BuildConfig.VERSION_CODE;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String msg = "Version: " + BuildConfig.VERSION_NAME;
        String uID = SharedData.getInstance().getUserID(activity);
        if (!uID.isEmpty()) {
            msg += "\n" + "User: " + uID;
        }
        final String msg2Disp = msg;
        builder.setMessage(msg2Disp)
                .setTitle(SharedData.getInstance().getTitleStr(Constants.APPNAME, activity))
                .setNeutralButton("Ok", null)
                .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(
                                Context.CLIPBOARD_SERVICE);
                        if (clipboard != null) {
                            ClipData clip = ClipData.newPlainText("", msg2Disp);
                            clipboard.setPrimaryClip(clip);
                            //a toast is implicitly displayed to say "copied to clipboard"
                        }
                    }
                })
                .show();
    }

    private static PackageInfo getPackageInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "getPackageInfo: NameNotFoundException");
        }
        return null;
    }

    static int getAppVersionCode(Context context) {
        PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo == null) {
            return 0;
        }
        //Log.d(TAG, "getAppVersionCode: " + packageInfo.toString());
        return packageInfo.versionCode;
    }

    public List<String> getTeamPlayers(String team) {
        if (mTeamInfoMap == null) return new ArrayList<>();
        TeamInfo tI = mTeamInfoMap.get(team);
        if (tI == null) return new ArrayList<>();
        return tI.players;
    }

    //Make singleton from serialize and deserialize operation.
    //Unused in this app.
    protected SharedData readResolve() {
        return getInstance();
    }

    private void clearMain() {
        //clear all the critical members.
        //Log.i(TAG, "SharedData: clearMain");

        //Firebase DB members: they should go together.
        removeDBConnectionListener();
        mDBConnectListener = null;
        mNewClubListener = null;
        mDBConnected = false;

        mClub = "";
        mTournament = "";
        mRole = "unknown";
        mInnings = "";
        mRoundName = "";
        mDBLock = false;
        mDBLockAcqAttemptTime = 0L;
        if(mProfile!=null) mProfile.clear();
        if(mRootProfile!=null) mRootProfile.clear();
        mDemoMode = false;
    }

    public void clear() {
        Log.i(TAG, "SharedData: CLEAR");
        clearMain();
        mNumOfGroups = Constants.NUM_OF_GROUPS;
        mUser = "";
        mProfile = new ProfileDBEntry();
        mRootProfile = new ProfileDBEntry();
        mReadNews = "";
        mGoldPlayers = null;
        mSilverPlayers = null;
        mInningsDBKey = -1;
        mWinPercNum = Constants.SHUFFLE_WINPERC_NUM_GAMES;
        mDBUpdated = false;
        mUserNotifyEnabled = true;
        mStoreHistory = "";
        mGoldPresentPlayerNames = new HashSet<>();  //initialize for the first time.
        mSilverPresentPlayerNames = new HashSet<>();  //initialize for the first time.
        mTournaMap = null;
        mTeams = null;
        mTeams = new ArrayList<>();
        mTeamInfoMap = null;
        mTeamInfoMap = new HashMap<>();
        mFlags = "";
        mTable_view_resid = R.layout.tourna_match_info_small;
        mCount = 0;
        mStrList = null;
        mOfflineMode = false;
        mTime = 0L;
    }

    private void clearLocalStoredData(final Context context) {
        //Log.d(TAG, "clearLocalStoredData: ");
        SharedPreferences prefs = context.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();  //using commit instead of apply for immediate write
    }
    private void clearData(final Context context, final boolean showToast) {
        clearLocalStoredData(context);
        clear();
        if(showToast)
            Toast.makeText(context, "Cache cleared!", Toast.LENGTH_SHORT).show();
    }

    void logOut(final Activity activity, final boolean kill_self) {
        Log.i(TAG, "log out: " + activity.getLocalClassName() + ":" + kill_self);
        RootService.stopRepeatingIntent(activity); //stop the service that was started from LoginActivity
        deleteListenerForNewClub(activity.getApplicationContext());
        clearData(activity, true);
        if(kill_self) killActivity(activity, Activity.RESULT_OK);
        Intent intent = new Intent(activity, MainSigninActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    public boolean isRoot() {
        return Constants.ROOT.equals(mRole);
    }

    public boolean isSuperUser() {
        return Constants.SUPERUSER.equals(mRole);
    }

    public boolean isSuperPlus() {
        return isSuperUser() || isRoot();
    }

    boolean isAdmin() {
        return Constants.ADMIN.equals(mRole);
    }

    boolean isAdminPlus() {
        return isAdmin() || isSuperPlus();
    }

    boolean isMemberRole() {
        return Constants.MEMBER.equals(mRole);
    }

    boolean isRootLogin(final String club, final String secpd) {
        if(mRootProfile.isValid()) {
            if(club.equals(mRootProfile.getDes()) &&
                    secpd.equals(mRootProfile.getRc())) {
                mRole = Constants.ROOT;
                return true;
            }
        }
        return false;
    }

    boolean isDBUpdated() {
        return mDBUpdated;
    }

    public void setDBUpdated(boolean s) {
        mDBUpdated = s;
    }

    void resetForNewInnings(final String newInningsName) {
        mInnings = newInningsName;
        mRoundName = "";
        mInningsDBKey = -1;
        setDBUpdated(true);
    }

    void initData(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        String club = prefs.getString(Constants.DATA_CLUB, "");
        if (!club.isEmpty()) {
            //Log.d(TAG, "initData for " + club);
            mClub = club;
            mUser = prefs.getString(Constants.DATA_USER, "");
            mRole = prefs.getString(Constants.DATA_ROLE, "");
            mOfflineMode = prefs.getBoolean(Constants.DATA_OFFLINE_MODE, false);
            mFlags = prefs.getString(Constants.DATA_FLAGS, "");
        }
        Log.d(TAG, "initData: " + toString());
    }

    void addFlag(final Activity activity, final String flag) {
        if (mFlags.contains(flag)) return;
        mFlags += "|" + flag;
        //Log.d(TAG, "addFlag: " + mFlags);
        SharedPreferences prefs = activity.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.DATA_FLAGS, mFlags);
        editor.apply();
    }

    void removeFlag(final Activity activity, final String flag) {
        if (!mFlags.contains(flag)) return;
        String flagStr = "|" + flag;
        mFlags = mFlags.replace(flagStr, "");
        //Log.d(TAG, "removeFlag: " + mFlags);
        SharedPreferences prefs = activity.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.DATA_FLAGS, mFlags);
        editor.apply();
    }

    void saveUserID(final Context context, final String id) {
        //Log.d(TAG, "saveUserID: " + mUser);
        SharedPreferences prefs = context.getSharedPreferences(Constants.USERDATA_ID, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.DATA_USER, mUser);
        editor.apply();
    }

    String getUserID(final Context context) {
        String uID = mUser;
        if (uID.isEmpty()) {
            SharedPreferences prefs = context.getSharedPreferences(Constants.USERDATA_ID, MODE_PRIVATE);
            uID = prefs.getString(Constants.DATA_USER, "");
            //Log.d(TAG, "getUserID: stored:" + mUser);
        }

        if (uID.isEmpty()) {
            //User ID is not saved yet
            //Try to get android_id
            String id = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            if (id.isEmpty()) {
                mUser = Constants.USERID_TMP + UUID.randomUUID().toString();
                Log.d(TAG, "getUSerID: Empty id, Generated tmp:" + mUser);
            } else {
                mUser = id;
                //Log.d(TAG, "getUserID: android id:" + mUser);
            }
            saveUserID(context, mUser);
        } else if (uID.contains(Constants.USERID_TMP)) {
            //Try to get android_id
            String id = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            if (!id.isEmpty()) {
                Log.i(TAG, "getUserID: Overwriting id:" + mUser + " -> " + id);
                mUser = id;
                saveUserID(context, mUser);
            }
        } else {
            mUser = uID;
        }
        //Log.d(TAG, "getUserID: " + mUser);
        return mUser;
    }

    boolean validFlag(final String flag) {
        //Log.d(TAG, "validFlag:" + flag + "/" + mFlags);
        return mFlags.contains(flag);
    }

    String compactUser(final String usr) {
        if(usr.length()>5) return usr.substring(0,4) + "..";
        else return usr;
    }

    private void addHistory(final String story) {
        if (story.isEmpty()) return;
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT, Locale.CANADA);
        String dateStr = df.format(c);
        if (isUserNotifyEnabled()) {
            //All the user delete/create log can be added as a single entry.
            final DatabaseReference inningsDBRef = FirebaseDatabase.getInstance().getReference()
                    .child(SharedData.getInstance().mClub)
                    .child(Constants.INTERNALS).child(Constants.HISTORY);
            DatabaseReference newHistory = inningsDBRef.push();
            newHistory.setValue(dateStr + DATE_DELM + story + USERID_DELM + mUser);
        } else mStoreHistory += story + ",";
        //Log.w(TAG, "History added: [" + dateStr + ":" + mUser + ":" + story + "]");
    }


    void addHistory(final DatabaseReference dbRef, final String story) {
        if (story.isEmpty()) return;
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT, Locale.CANADA);
        String dateStr = df.format(c);
        DatabaseReference newHistory = dbRef.child(Constants.INTERNALS).child(Constants.HISTORY).push();
        //newHistory.setValue(dateStr + "#" + mUser + "#" + story);
        newHistory.setValue(dateStr + DATE_DELM + story + USERID_DELM + mUser);
        //Log.w(TAG, "History added: [" + dateStr + ":" + mUser + ":" + story + "]");
    }

    String parseHistory(String storyLine) {
        if (storyLine.contains(NEWUSER_CREATED) && storyLine.contains(USER_DELETED)) {
            //this is detailed info of Shuffling. Too big to print on phone!
            return "";
        } else if (storyLine.contains(SHUFFLE_START))
            return "";  //ignore

        if(storyLine.contains(USERID_DELM)) {
            String usrId = storyLine.substring(storyLine.indexOf(USERID_DELM) + USERID_DELM.length());
            //remove user id from end
            storyLine = storyLine.substring(0, storyLine.indexOf(USERID_DELM));
            //add user id after date
            storyLine = storyLine.replace(DATE_DELM, " " + compactUser(usrId) + " ");
        }
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
        else if (tmpStr.contains(RESET_DATA_ALL))
            resultStr = tmpStr.replace(RESET_DATA_ALL, "Reset Everything");
        else if (tmpStr.contains(RESET_DATA))
            resultStr = tmpStr.replace(RESET_DATA, "Reset Data");

        return resultStr;
    }


    void addShuffleStart2History(final String newInningsName) {
        addHistory(SHUFFLE_START + "=" + newInningsName);
    }

    void addShuffleSuccess2History(final String newInningsName) {
        addHistory(SHUFFLE_SUCCESS + "=" + newInningsName);
    }

    void addShuffleFailure2History(final String newInningsName) {
        addHistory(SHUFFLE_FAIL + "=" + newInningsName);
    }

    void addNewUserCreation2History(final String newUserName) {
        addHistory(NEWUSER_CREATED + "=" + newUserName);
    }

    void addUserMove2History(final String userName) {
        addHistory(USER_MOVED + "=" + userName);
    }

    void addUserDeletion2History(final String userName) {
        addHistory(USER_DELETED + "=" + userName);
    }

    void addReset2History(final boolean everything) {
        if(everything) addHistory(RESET_DATA_ALL);
        else addHistory(RESET_DATA);
    }

    void addInningsCreation2History(final String innings) {
        addHistory(INNINGS_CREATED + "=" + innings);
    }

    void disableUserNotify(Context context) {
        mUserNotifyEnabled = false;
        mStoreHistory = "";
    }

    void enableUserNotify(Context context) {
        mUserNotifyEnabled = true;
        addHistory(mStoreHistory);
    }

    boolean isUserNotifyEnabled() {
        return mUserNotifyEnabled;
    }

    //data is retrieved in the order of creation as the history key is autogenerated
    //which is chronological.
    void deleteOldHistory() {
        if(mClub.isEmpty()) return;
        final int MAX_HISTORY_COUNT = 250;
        FirebaseDatabase.getInstance().getReference()
                .child(mClub).child(Constants.INTERNALS).child(Constants.HISTORY)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long numChildren = dataSnapshot.getChildrenCount();
                if(numChildren>MAX_HISTORY_COUNT) {
                    Log.i(TAG, " Old history to be deleted, num=" + numChildren +
                            ", max=" + MAX_HISTORY_COUNT);
                    int count = 0;
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        //Log.d(TAG, count + ":" + child.getKey() + ":to be deleted: " + child.getValue());
                        child.getRef().removeValue();
                        count++;
                        if(count>MAX_HISTORY_COUNT) break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });


    }

    void showToast(final Context c, final CharSequence s, final int d) {
        if (mUserNotifyEnabled) Toast.makeText(c, s, d).show();
        //Log.i(TAG, s.toString());
    }

    public SpannableStringBuilder getColorString(CharSequence text, int color) {
        if (text==null || text.length() == 0)
            return new SpannableStringBuilder("");
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssBuilder;
    }

    SpannableStringBuilder getBgColorString(CharSequence text, int color) {
        if (text==null || text.length() == 0)
            return new SpannableStringBuilder("");
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new BackgroundColorSpan(color), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssBuilder;
    }

    SpannableStringBuilder getStyleString(CharSequence text, int style) {
        if (text==null || text.length() == 0)
            return new SpannableStringBuilder("");
        //style=Typeface.ITALIC
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new StyleSpan(style), 0, text.length(), 0);
        return ssBuilder;
    }

    SpannableStringBuilder getSizeString(CharSequence text, float proportion) {
        if (text==null || text.length() == 0)
            return new SpannableStringBuilder("");
        //style=Typeface.ITALIC
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new RelativeSizeSpan(proportion), 0, text.length(), 0);
        return ssBuilder;
    }

    SpannableStringBuilder getStrikethroughString(CharSequence text) {
        if (text==null || text.length() == 0)
            return new SpannableStringBuilder("");
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(text);
        ssBuilder.setSpan(new StrikethroughSpan(), 0, text.length(), 0);
        return ssBuilder;
    }

    SpannableStringBuilder getTitleStr(String title, Context context) {
        return getColorString(title,   //add some color to it. Use the color scheme color from color.xml
                ResourcesCompat.getColor(context.getResources(), R.color.colorPrimaryDark, null));
    }

    String getShortRoundName(String round) {
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

    void sortPlayers(ArrayList<PlayerData> playersList, final int idx, //innings or season
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
03-04 15:57:57.384 23935-23935/com.sg0.baddytally W/SharedData: wakeUpDBConnection: onComplete: Success: ProfileDBEntry{ver='2.1', role='root', device='C6806', last_login='03-04'}
03-04 15:57:57.429 23935-23935/com.sg0.baddytally W/SharedData: changeDBLockState: onComplete: Success:true,25862217
*/

    public boolean isDBConnected() {
        if (SharedData.getInstance().mOfflineMode) {
            Log.d(TAG, "Offline mode");
            return true;
        } else {
            return mDBConnected;
        }
    }

    boolean isDBServerConnected() {
        //real state w/o considering offline mode
        return mDBConnected;
    }

    private void removeDBConnectionListener() {
        //Log.d(TAG, "removeDBConnectionListener: ");
        if (null != mDBConnectListener) {
            FirebaseDatabase.getInstance().getReference(".info/connected")
                    .removeEventListener(mDBConnectListener);
            mDBConnectListener = null;
            Log.d(TAG, "removeDBConnectionListener: removeEventListener");
        }
    }

    //The listener for .info / connected stops listening if there is
    //no active action or listener for the database. For root user, there will be an active
    //listener for the new club. For non-root users, there will be no active listener that runs
    //forever.
    void setUpDBConnectionListener() {
        if (null != mDBConnectListener) return;

        Log.d(TAG, "--------- setUpDBConnectionListener -----------");
        DatabaseReference connectedRef = FirebaseDatabase.getInstance()
                .getReference(".info/connected");
        //if (null != mDBConnectListener) connectedRef.removeEventListener(mDBConnectListener);
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
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if(null==connected || !connected) {
                    Log.d(TAG, "setUpDBConnectionListener: not connected: " + mClub);
                    mDBConnected = false;
                } else {
                    Log.d(TAG, "setUpDBConnectionListener: connected: " + mClub);
                    mDBConnected = true;
                    wakeupdbconnectionProfileRoot(); //init: read root profile
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "setUpDBConnectionListener: Listener was cancelled");
                mDBConnected = false;
            }
        });
    }

    void wakeUpDBConnection() {
        // Do a mock transaction to wake up the database connection.

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

        Log.d(TAG, "wakeUpDBConnection:" + toString());
        if (mClub.isEmpty() || mUser.isEmpty()) return;

        if (null == mDBConnectListener) setUpDBConnectionListener();

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("MM-dd", Locale.CANADA);
        final String login_day = df.format(c);
        final String model_name = android.os.Build.MODEL; // might be leading to privacy error from google, not sure.
        //final String id = mUser + "-" + model_name;
        final String id = mUser;

        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub)
                .child(Constants.ACTIVE_USERS).child(id);
        dbRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                try {
                    String userData = mutableData.getValue(String.class);
                    if (userData == null) return Transaction.success(mutableData);
                    else {
                        //Log.w(TAG, "wakeUpDBConnection: update DB for user:" + id);
                        mutableData.setValue(mRole);
                        return Transaction.success(mutableData);
                    }
                } catch (com.google.firebase.database.DatabaseException e) {
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot dataSnapshot) {
                if (error != null || !committed || dataSnapshot == null) {
                    if (null != error)
                        Log.w(TAG, "wakeUpDBConnection: onComplete: Failed:", error.toException());
                    else Log.w(TAG, "wakeUpDBConnection: onComplete: Failed");
                } else {
                    try {
                        String userData = dataSnapshot.getValue(String.class);
                        //Log.w(TAG, "wakeUpDBConnection: onComplete: Success: " + id);
                        if (userData == null) {
                            //Log.w(TAG, "wakeUpDBConnection: onComplete: Success: " + "null data");
                            dbRef.setValue(mRole);
                            //Log.w(TAG, "New user created in DB:" + id);
                        } else {
                            //Log.w(TAG, "wakeUpDBConnection: onComplete: Success: " + userData);
                        }
                    } catch (NullPointerException e) {
                        if (mClub.isEmpty()) return;
                        //java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.Boolean.booleanValue()' on a null object reference
                        //SGO: If INTERNALS/locked node is not present in DB, dataSnapshot is not set to null, but a null pointer exception
                        //     is thrown here. Just gonna catch it here to avoid crash.
                        //This was seen in a case where innings was on going, and the app was upgraded. Newer version of app depended on INTERNALS/locked
                        //but the older version never new about INTERNALS/locked. Workaround was to manually create INTERNALS/locked in the DB for that club.
                        //This issue will not happen if the new app is used to create the innings (INTERNALS/locked node iwll be created in DB).
                        Log.w(TAG, "wakeUpDBConnection: onComplete: Error:" + e.getMessage());
                        dbRef.setValue(mRole);
                        //Log.w(TAG, "New user created in DB:" + id);
                    } catch (com.google.firebase.database.DatabaseException e) {
                        Log.i(TAG, "wakeUpDBConnection: onComplete:" + e.getMessage());
                        dbRef.setValue(mRole);
                    }

                }
            }
        });
    }

    public void wakeupdbconnectionProfileRoot() {
        wakeUpDBConnection_profile_inner("");
    }

    public void wakeUpDBConnectionProfile() {
        wakeUpDBConnection_profile_inner(mClub);
    }

    private void wakeUpDBConnection_profile_inner(final String club) {

        if (null == mDBConnectListener) setUpDBConnectionListener();

        // Do a mock transaction to wake up the database connection.
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.PROFILE);
        if(!club.isEmpty()) //root profile
            dbRef = FirebaseDatabase.getInstance().getReference()
                    .child(club).child(Constants.PROFILE);

        dbRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {

                //Log.v(TAG, "wakeUpDBConnectionProfile: doTransaction: " + club);
                ProfileDBEntry userData = mutableData.getValue(ProfileDBEntry.class);
                if (userData == null) return Transaction.success(mutableData);
                else {
                    if(club.isEmpty()) userData.copyProfile(mRootProfile);
                    else userData.copyProfile(mProfile);
                    //Log.d(TAG, club + ":wakeUpDBConnectionProfile: doTransaction: "
                    //        + mProfile.toSecretString() + " root:" + mRootProfile.toSecretString());
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot dataSnapshot) {
            }
        });
    }

    void fetchProfile(final CallbackRoutine cb, final Context context, final String club) {
        //Log.d(TAG, club + ":fetchProfile: " + context.getClass().getName());
        //club is passed in as SharedData will not be populated yet with club info
        //when invoked from LoginActivity.

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(club).child(Constants.PROFILE);
        if(club.isEmpty()) //root profile
            dbRef = FirebaseDatabase.getInstance().getReference().child(Constants.PROFILE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ProfileDBEntry userData = dataSnapshot.getValue(ProfileDBEntry.class);
                if (userData == null) {
                    //this is hit if the DB is NOT connected
                    Log.i(TAG, "fetchProfile: null:" + club);
                    return;
                }
                if(club.isEmpty()) userData.copyProfile(mRootProfile);
                else userData.copyProfile(mProfile);
                //Log.v(TAG, club + ":fetchProfile: " + mProfile.toString()
                //        + " root:" + mRootProfile.toString());
                if(null!=cb) cb.profileFetched();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "fetchProfile: onCancelled", databaseError.toException());
                Toast.makeText(context,
                        "Login Profile DB error:" + databaseError.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }


    void auditClub(final Activity activity) {
        Log.d(TAG, mClub + ":auditClub: " + activity.getLocalClassName());
        if(mClub.isEmpty()) return;
        if(mUser.isEmpty()) mUser = getUserID(activity);

        FirebaseDatabase.getInstance().getReference()
                .child(mClub)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    Log.w(TAG, "auditClub: Club does not exist anymore: " + mClub);
                    mClub = "";
                    logOut(activity, true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "auditClub: onCancelled", databaseError.toException());
            }
        });
    }

    //What if the passwords in the DB have changed?
    //What if the club was deleted by root?
    Boolean isPermitted(final Activity activity) {
        Log.d(TAG, "isPermitted: " + activity.getLocalClassName());
        if(mClub.isEmpty()) {
            Toast.makeText(activity,
                    "Invalid club! Login again.",
                    Toast.LENGTH_LONG).show();
            logOut(activity, true);
            //restartApplication(context, MainSigninActivity.class);
            return false;

        }
        if(!mProfile.isValid()) {
            wakeUpDBConnectionProfile();
        } else if(!mRootProfile.isValid()) {
            wakeupdbconnectionProfileRoot();
        }

        SharedPreferences prefs = activity.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        final String secpd = prefs.getString(Constants.DATA_SEC, "");
        //Log.d(TAG, secpd+"isPermitted: " + mProfile.toString() + " root=" +
        //        mRootProfile.toString());
        if (secpd.isEmpty()) return true;

        boolean err = false;
        if(isRoot()) {
            if (secpd.equals(mRootProfile.getRc())) {
                Log.d(TAG, "isPermitted: still root");
                return true;
            }
        }

        if(isSuperUser() && !secpd.equals(mProfile.getRc())) err = true;
        if(isAdmin() && !secpd.equals(mProfile.getAc())) err = true;
        if(isMemberRole() && !secpd.equals(mProfile.getMc())) err = true;

        if(err) {
            Toast.makeText(activity,
                    "No permission, login again using new credentials",
                    Toast.LENGTH_LONG).show();
            logOut(activity, true);
        }
        return !err;
    }

    boolean isDBLocked() {
        Log.d(TAG, "isDBLocked: " + mDBLock + "," + mDBLockAcqAttemptTime);
        if (mDBLock) mDBLockAcqAttemptTime = 0L;  //Acquire attempt was successful, so reset.
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
        if (null == tourna || tourna.isEmpty())
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
                } else {
                    if (locked == target) {
                        //Log.i(TAG, "changeDBLockState:" + tourna + " Already set to " + target);
                        return Transaction.abort();
                    } else {
                        //Log.i(TAG, "changeDBLockState:" + tourna + " lock changed to " + target);
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
                    else
                        Log.w(TAG, "changeDBLockState: onComplete: Failed"); //DB connection might be stale
                    mDBLock = false;
                } else {
                    try {
                        mDBLock = dataSnapshot.getValue(Boolean.class);
                        //Log.i(TAG, "changeDBLockState: onComplete: Success:" + mDBLock + "," + mDBLockAcqAttemptTime);
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

    void acquireDBLock() {
        acquireDBLock(null);
    }

    void acquireDBLock(final String tourna) {
        mDBLock = false;
        //Log.i(TAG, "acquireDBLock: acquiring..." + mDBLockAcqAttemptTime);
        changeDBLockState(true, tourna);
        if (mDBLockAcqAttemptTime > 0L) {
            Long timeInMins = System.currentTimeMillis() / 60000;
            if ((timeInMins - mDBLockAcqAttemptTime) > 3) {
                //DB is been locked for more than 3 mins.
                //This is mostly a failure to update DB while releasing lock (say the n/w connection was lost
                //after acquiring lock or app crashed after acquiring lock etc). So, release the lock forcefully.
                //There is a very rare chance that when a particular user tried to acquire the
                //lock twice (first attempt when mDBLockAcqAttemptTime was filled and now) and there
                //other users (could be different ones) holding on to the lock at that very moment.
                //Even in this case, lock is forcefully released. Could be an issue when the user base increases!
                //Log.w(TAG, "isDBLocked: force release lock: " + (timeInMins - mDBLockAcqAttemptTime) +
                //        " timeInMins=" + timeInMins + " mDBLockAcqAttemptTime=" + mDBLockAcqAttemptTime);
                releaseDBLock(true, tourna);
            }
        } else mDBLockAcqAttemptTime = System.currentTimeMillis() / 60000;
    }

    void releaseDBLock() {
        //public API: Lock is released only if locked by this device.
        releaseDBLock(false, null);
    }

    void releaseDBLock(final String tourna) {
        //public API: Lock is released only if locked by this device.
        releaseDBLock(false, tourna);
    }

    private void releaseDBLock(final Boolean force, final String tourna) {

        if (force || isDBLocked()) {
            //SGO: If force flag is set, Lock is released even if not locked by this device.
            changeDBLockState(false, tourna);
            mDBLockAcqAttemptTime = 0L;
            //Log.i(TAG, "releaseDBLock: releasing..." + mDBLockAcqAttemptTime);
        }

    }

    void createDBLock(final String tourna) {
        DatabaseReference tmpDBRef = FirebaseDatabase.getInstance().getReference()
                .child(mClub);
        if (null == tourna || tourna.isEmpty())
            tmpDBRef = tmpDBRef.child(Constants.INTERNALS);
        else
            tmpDBRef = tmpDBRef.child(Constants.TOURNA).child(tourna)
                    .child(Constants.INTERNALS);
        final DatabaseReference dbRef = tmpDBRef;
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(Constants.LOCK)) {
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

    String createNewRoundName(boolean commit, Context context) {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat(Constants.ROUND_DATEFORMAT, Locale.CANADA);
        String rndName = df.format(c);
        if (commit) {
            //Log.w(TAG, "createNewRoundName: committing:" + rndName);
            SharedPreferences prefs = context.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.NEWROUND, rndName);
            editor.apply();
        }
        return rndName;
    }

    @Override
    @NonNull
    public String toString() {
        String str = "SharedData{" +
                "mUser='" + mUser + '\'' +
                ", mRole='" + mRole + '\'' +
                ", mClub='" + mClub + '\'' +
                ", mInnings='" + mInnings + '\'' +
                ", mRoundName='" + mRoundName + '\'';
        if (mGoldPlayers != null) str += ", mGoldPlayers=" + mGoldPlayers.size();
        if (mSilverPlayers != null) str += ", mSilverPlayers=" + mSilverPlayers.size();
        str += '}';
        return str;
    }

    Boolean isLeagueTournament(final String tourna) {
        String tournaType = getTournamentType(tourna);
        if (tournaType.isEmpty()) return false;
        //Log.i(TAG, "isLeagueTournament:" + tourna + "," + tournaType);
        return tournaType.equals(Constants.LEAGUE);
    }

    Boolean isEliminationTournament(final String tourna) {
        String tournaType = getTournamentType(tourna);
        if (tournaType.isEmpty()) return false;
        //Log.i(TAG, "isEliminationTournament:" + tourna + "," + tournaType);
        return tournaType.equals(Constants.SE) || tournaType.equals(Constants.DE);
    }

    Boolean isSETournament(final String tourna) {
        return getTournamentType(tourna).equals(Constants.SE);
    }

    Boolean isDETournament(final String tourna) {
        return getTournamentType(tourna).equals(Constants.DE);
    }

    String getTournamentType(final String tourna) {
        if (null == tourna || tourna.isEmpty()) return "";
        if (null == mTournaMap || mTournaMap.size() == 0) return "";

        String tournaType = mTournaMap.get(tourna);
        if (null == tournaType) return "";

        return tournaType;
    }

    void readDBTeam(final String tournament, final Context context, final CallbackRoutine cb) {
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
                    //Log.i(TAG, "onDataChange Got:" + team);

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
                    //Log.i(TAG, "onDataChange, scoreDBEntry:" + tI.score.toString());

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

    void propogateTheWinner(final Activity activity, final String fixLabel, final String matchId, final String winner) {
        if (fixLabel.equals(Constants.DE_FINALS)) {
            return;
        }

        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub)
                .child(Constants.TOURNA).child(mTournament).child(fixLabel);
        //Log.d(TAG, "propogateTheWinner: " + fixLabel + matchId + winner);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String, TournaFixtureDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<Map<String, TournaFixtureDBEntry>>() {
                        };
                Map<String, TournaFixtureDBEntry> map = dataSnapshot.getValue(genericTypeIndicator);
                if (null == map) return;
                for (Map.Entry<String, TournaFixtureDBEntry> entry : map.entrySet()) {
                    String mId = entry.getKey();
                    TournaFixtureDBEntry dbEntry = entry.getValue();
                    //Check if the previous link is same as the match just completed.
                    //If yes, update the team name in DB
                    if (matchId.equals(dbEntry.getPr1(true))) {
                        //prev1 is the match which just completed. Update team1.
                        dbEntry.setW("");  //reset winner; This might be root doing a correction.
                        dbEntry.setT1(true, winner);
                        dbEntry.setWinnerString();  //If one is bye, set the other as winner
                        dbRef.child(mId).setValue(dbEntry);
                        Log.i(TAG, "propogateTheWinner(team1):" + mId + "=" + dbEntry.toString());
                    } else if (matchId.equals(dbEntry.getPr2(true))) {
                        //prev2 is the match which just completed. Update team2.
                        dbEntry.setW("");  //reset winner; This might be root doing a correction.
                        dbEntry.setT2(true, winner);
                        dbEntry.setWinnerString(); //If one is bye, set the other as winner
                        dbRef.child(mId).setValue(dbEntry);
                        Log.i(TAG, "propogateTheWinner(team2):" + mId + "=" + dbEntry.toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "propogateTheWinner", databaseError.toException());
                Toast.makeText(activity, "DB error while updating DB: " + databaseError.toString(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    TeamInfo getTeamInfo(final String team) {
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

    Boolean checkIfPlayerAlreadyExists(final Context context, final String pShort,
                                       final String pName) {
        for (Map.Entry<String, TeamInfo> entry : mTeamInfoMap.entrySet()) {
            TeamInfo tI = entry.getValue();
            for (String p_nick : tI.p_nicks) {
                if (p_nick.equals(pShort)) {
                    //Log.d(TAG, pShort + " already exists in " + tI.name);
                    //showToast(context, pShort + " already exists in " + tI.name, Toast.LENGTH_SHORT);
                    return true;
                }
            }
            for (String p : tI.players) {
                if (p.equals(pName)) {
                    //Log.d(TAG, pShort + " already exists in " + tI.name);
                    //showToast(context, pShort + " already exists in " + tI.name, Toast.LENGTH_SHORT);
                    return true;
                }
            }
        }
        return false;
    }

    void sortTeams() {
        //playersList is obj reference of the actual list being passed in.
        //Any updates to the contents will be visible to the caller.
        //But, you cannot change the obj reference itself (ex: playersList = another_list;).
        Collections.sort(mTeams, new Comparator<String>() {
            @Override
            public int compare(String team1, String team2) {
                TeamInfo tInfo1 = getTeamInfo(team1);
                TeamInfo tInfo2 = getTeamInfo(team2);
                int value1 = Integer.compare(tInfo2.score.getPts(), tInfo1.score.getPts()); //descending
                if (value1 == 0) {
                    int value2 = Integer.compare(tInfo2.score.getmW(), tInfo1.score.getmW());
                    if (value2 == 0) {
                        int value3 = Integer.compare(tInfo2.score.getgW(), tInfo1.score.getgW());
                        if (value3 == 0) {
                            return Integer.compare(tInfo2.score.getgPts(), tInfo1.score.getgPts());
                        } else return value3;
                    } else return value2;
                }
                return value1;
            }
        });
        //Log.d(TAG, "sortTeams: Sorted mTeams: " + mTeams.toString());
    }

    public void showAlert(final CallbackRoutine cb, final Context context,
                          final String title, final String msg) {
        showAlert(cb, context, new SpannableStringBuilder(title), msg);
    }

    public void showAlert(final CallbackRoutine cb, final Context context,
                          final SpannableStringBuilder title, final String msg) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setTitle(title);
        alertBuilder.setMessage(msg);
        alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (null != cb) cb.alertResult(title.toString(), true, false);
            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (null != cb) cb.alertResult(title.toString(), false, true);
            }
        });
        alertBuilder.show();
    }

    // Get a MemoryInfo object for the device's current memory status.
    // Note: This is device's overall memory status, not specific to this app
    ActivityManager.MemoryInfo getAvailableMemory(final Activity mActivity) {
        ActivityManager activityManager = (ActivityManager) mActivity.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        Log.v(TAG, "getAvailableMemory: avail=" + memoryInfo.availMem / (1048576) +
                "MB thres=" + memoryInfo.threshold / (1048576) +
                "MB total=" + memoryInfo.totalMem / (1048576));
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

        Log.d(TAG, msg + " printHeapUsage: size=" + heapSize / 1024 +
                " max=" + heapMaxSize / 1024 + " free=" + heapFreeSize / 1024);
    }

    public void killActivity(final Activity activity, final int res_code) {
        Log.d(TAG, activity.getLocalClassName() +">++++++ killActivity ++++++");
        activity.setResult(res_code);
        activity.finish();
    }

    void killApplication(final Activity activity) {
        mTournament = "";
        if (Build.VERSION.SDK_INT >= 21) {
            Log.w(TAG, activity.getLocalClassName() +
                    ">+++++++++++++ killApplication(finishAndRemoveTask) +++++++++++++");
            activity.finishAndRemoveTask();
        } else {
            Log.w(TAG, activity.getLocalClassName() +
                    ">+++++++++++++ killApplication(exit) +++++++++++++");
            activity.finishAffinity();
            //android.os.Process.killProcess(android.os.Process.myPid());
            //System.exit(0);
        }
    }

    void restartApplication(final Activity activity, final Class cname) {
        mTournament = "";
        Log.w(TAG, activity.getLocalClassName() +
                ">+++++++++++++ restartApplication +++++++++++++");
        Intent mStartActivity = new Intent(activity, cname);
        int mPendingIntentId = 3331;
        PendingIntent mPendingIntent = PendingIntent.getActivity(activity, mPendingIntentId,
                mStartActivity,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        if (mgr != null)
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    void persistOfflineMode(final boolean enable, final Activity activity) {
        if (SharedData.getInstance().mClub.isEmpty()) {
            Log.w(TAG, "offlineMode: club not known (" + SharedData.getInstance().mClub + ")");
            return;
        }

        if (enable)
            Log.i(TAG, "==== persistOfflineMode: DB Persistence: OFFLINE mode ====");
        else
            Log.i(TAG, "==== persistOfflineMode: DB Persistence: disable offline mode ====");

        SharedPreferences prefs = activity.getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.DATA_OFFLINE_MODE, enable);
        editor.commit(); //using commit instead of apply for immediate write, since we will restart app right after this
        SharedData.getInstance().mOfflineMode = enable;

    }

    void setOfflineMode(final boolean enable, final boolean init) {
        if (SharedData.getInstance().mClub.isEmpty()) {
            Log.w(TAG, "setOfflineMode: club not known (" + SharedData.getInstance().mClub + ")");
            return;
        }

        if (enable)
            Log.i(TAG, "==== setOfflineMode: DB Persistence: OFFLINE mode ====");
        else
            Log.i(TAG, "==== setOfflineMode: DB Persistence: disable offline mode ====");

        if (init) {
            //Calls to setPersistenceEnabled() must be made before any other usage of
            //FirebaseDatabase instance
            FirebaseDatabase.getInstance().setPersistenceEnabled(enable);
        }
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(SharedData.getInstance().mClub);
        dbRef.keepSynced(enable);
    }
    
    private void delayedKillActivity(final Handler mainHandler, final Activity activity,
                                    int timeout) {
        mainHandler.removeCallbacksAndMessages(null);
        if(timeout==0) timeout = Constants.SHOWTOAST_TIMEOUT;
        Log.w(TAG, "delayedKillActivity: " + activity.getLocalClassName());
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Kill Activity only after showing the Toast
                activity.finish();
            }
        }, timeout);
    }
    
    //post an event to show a toast & kill self (activity) after a timeout.
    //Usually, in the success case, this event will be cancelled before timer fires.
    void showToastAndDieOnTimeout(final Handler mainHandler, final Activity activity,
                                  final String msg, final boolean die,
                                  final boolean stopProgress, int timeout) {
        //Log.d(TAG, "showToastAndDieOnTimeout: ");
        if(timeout==0) timeout = Constants.DB_READ_TIMEOUT;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity,
                        msg,
                        Toast.LENGTH_LONG).show();
                if(stopProgress) stopProgressDialog(activity);
                if(die) delayedKillActivity(mainHandler, activity, 0);
            }
        }, timeout);
    }

    String getAlbumStorageDirStr() {
        return Environment.DIRECTORY_DOCUMENTS;
    }

    File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                getAlbumStorageDirStr()), Constants.APPNAME);
        if (!file.mkdirs()) {
            if (file.isDirectory()) {
                //Log.d(TAG, "getAlbumStorageDir: Directory has already been created:" +
                //        file.getAbsolutePath());
            } else {
                Log.d(TAG, "getAlbumStorageDir: Directory could not be created:" +
                        file.getAbsolutePath());
                return null;
            }
        }
        return file;
    }

    /**
     * Checks if external storage is available for read and write
     *
     * @return boolean
     */
    boolean isExternalStorageWritable(final Activity activity) {
        if (isStoragePermissionGranted(activity)) {
            String state = Environment.getExternalStorageState();
            return Environment.MEDIA_MOUNTED.equals(state);
        }
        return false;
    }

    private boolean isStoragePermissionGranted(final Activity activity) {
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
        //Log.d(TAG, "showFileChooser: " + uri.toString());
        intent.setDataAndType(uri, "text/csv");
        try {
            activity.startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (Exception e) {
            Log.e(TAG, " choose file error " + e.toString());
        }
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    void performFileSearch(final Activity activity, String[] mimeTypes) {
        //Log.d(TAG, "performFileSearch: ");

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        //intent.setType("text/plain");


        /* an attempt to read xlsx file: not successful (Feb 2020)
        //intent.setType("application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String[] mimeTypes = {"application/vnd.ms-excel" ,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setType(mimeTypes.length == 1 ? mimeTypes[0] : "*/*");
            if (mimeTypes.length > 0) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
        } else {
            StringBuilder mimeTypesStr = new StringBuilder();
            for (String mimeType : mimeTypes) {
                mimeTypesStr.append(mimeType).append("|");
            }
            intent.setType(mimeTypesStr.substring(0,mimeTypesStr.length() - 1));
        }

        //intent.setType("application/vnd.ms-excel");  //.xls files

        activity.startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /*
    #<groupName>=<player1 name>,<player2 name>,...
    #innings=<inningsName>
    #Example:
    #gold=gPlayer1, gPlayer2, gPlayer3, .....
    #silver=sPlayer1, sPlayer2, sPlayer3, .....
    #innings=FirstInnings
    */
    Map<String, ArrayList<String>> readClubLeagueText(final Activity activity, Uri uri) {
        final Map<String, ArrayList<String>> clubMap = new HashMap<>();
        StringBuilder errStr = new StringBuilder();
        try {
            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
            if (null == inputStream) return clubMap;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                //Log.d(TAG, "readClubLeagueData: [" + line + "]");
                String dataline = line.trim();
                if(dataline.startsWith("#")) continue;
                String[] entries = dataline.split("=");
                if(entries.length<2) continue;
                String key = entries[0].trim();
                //key has to be gold, silver or "innings". Check for this is there in Club settings class
                if(key.isEmpty()) continue;
                String[] valStrings = entries[1].trim().split(",");
                ArrayList<String> values = new ArrayList<>();
                StringBuilder badValues = new StringBuilder();
                for(String value: valStrings) {
                    String player = value.trim();
                    if(player.isEmpty()) continue;
                    if (isValidString(player))
                        values.add(player);
                    else badValues.append(player);
                }
                if(badValues.length()>0) {
                    errStr.append("Ignoring invalid entries: ");
                    errStr.append(badValues); errStr.append("\n");
                }
                clubMap.put(key, values);
                //Log.d(TAG, count + ":readClubLeagueData: " + key + "=" + values.toString());
                count++;
                if(count>Constants.TOO_MANY_ENTRIES) break; //bad huge file! avoid tight loop
            }
            inputStream.close();
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "readClubLeagueData: " + e.getMessage());
            //e.printStackTrace();
        }
        if(errStr.length()>0) {
            //showAlert(null, activity, "Warning", errStr.toString());
            //Cant be AlertDialog here, as the return below will happen even before
            //user response to the Alert.
            Toast.makeText(activity, errStr.toString(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "readClubLeagueText: errStr=" + errStr.toString());
        }
        return clubMap;
    }

    Map<String, ArrayList<String>> readClubLeagueExcel(final Activity activity, Uri uri) {
        final Map<String, ArrayList<String>> clubMap = new HashMap<>();
        StringBuilder errStr = new StringBuilder();
        try {
            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
            if (null == inputStream) {
                Log.e(TAG, "readClubLeagueExcel: null stream");
                return clubMap;
            }
            Workbook m_workBook = Workbook.getWorkbook(inputStream);
            //p_sheetNo is excel sheet no which u want to read
            Sheet sheet = m_workBook.getSheet(0);

            if(sheet.getRows()>Constants.TOO_MANY_ENTRIES || sheet.getColumns()>Constants.TOO_MANY_ENTRIES) {
                Toast.makeText(activity, "Bad file!!", Toast.LENGTH_SHORT).show();
                return clubMap;
            }
            for (int row = 0; row < sheet.getRows(); row++) {
                String content = sheet.getCell(0, row).getContents();
                //if its heading, ignore.
                if(row==0 &&
                        (content.contains("TEAM NAME") || content.contains("POOL NAME") ||
                        content.contains("TEAMNAME") || content.contains("POOLNAME"))) continue;

                final String key = sheet.getCell(0, row).getContents();
                //key has to be gold, silver or "innings". Check for this is there in Club settings class
                if (key.isEmpty()) continue;
                if (!isValidString(key)) {
                    String str = "Invalid team name [" + key + "]\n";
                    errStr.append(str);
                    continue;
                }
                ArrayList<String> values = new ArrayList<>();
                for (int col = 1; col < sheet.getColumns(); col++) {
                    String playerName = sheet.getCell(col, row).getContents();
                    if (playerName.isEmpty()) continue;
                    if (!isValidString(playerName)) {
                        String str = "Ignored invalid player name [" + playerName + "] in " +
                                key + "\n";
                        errStr.append(str);
                        continue;
                    }

                    if(values.contains(playerName)) {
                            String str = "Ignored " + playerName + " (duplicate in " +
                                    key + ")\n";
                            errStr.append(str);
                    } else {
                        values.add(playerName);
                    }
                }
                clubMap.put(key, values);
            }
        } catch (IOException | BiffException e) {
            Log.e(TAG, "readClubLeagueExcel: " + e.getMessage());
            //e.printStackTrace();
        }

        if(errStr.length()>0) {
            Toast.makeText(activity, errStr.toString(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "readClubLeagueExcel: errStr=" + errStr.toString());
        }
        return clubMap;
    }

    /*
    #<teamname>=<player1 name>,<player2 name>,...
    #Example:
    #team1=t1player1, t1player2
    */
    ArrayList<TeamInfo> readTournaText(final Activity activity, Uri uri) {
        final ArrayList<TeamInfo> retList = new ArrayList<>();
        ArrayList<String> existingTeamIDs = new ArrayList<>();
        //Generated teamIds are tracked to avoid duplicate teamIds. But team Names are not checked
        //here, as it will be checked in TournaSettings. Similarly for existingPlayerIDs.
        ArrayList<String> existingPlayerIDs = new ArrayList<>();
        StringBuilder errStr = new StringBuilder();
        try {
            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
            if (null == inputStream) return retList;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                //Log.d(TAG, "readTournaText: [" + line + "]");
                String dataline = line.trim();
                if(dataline.startsWith("#")) continue;
                String[] entries = dataline.split("=");
                if(entries.length!=2) continue; //every row has one team entry
                String desc = entries[0].trim();
                if(desc.isEmpty()) continue;
                if (!isValidString(desc)) {
                    errStr.append("Ignoring invalid team name: ");
                    errStr.append(desc); errStr.append("\n");
                    continue;
                }

                TeamInfo tI = new TeamInfo();
                tI.name = getUniqIDStr(desc, 0, existingTeamIDs);
                tI.desc = desc;
                String[] valStrings = entries[1].trim().split(",");
                StringBuilder badValues = new StringBuilder();
                for(String value: valStrings) {
                    String player = value.trim();
                    if(player.isEmpty()) continue;
                    if (isValidString(player)){
                        //Duplicate player name is checked in TournaSettings
                        boolean ignore = false;
                        for (String existingPlayer : tI.players) {
                            if (existingPlayer.equals(player)) {
                                ignore = true;
                                String str = "Ignored " + player + " (duplicate in " +
                                        tI.name + ")\n";
                                errStr.append(str);
                                break;
                            }
                        }
                        if (!ignore) {
                            String plNickName = getUniqIDStr(player, 0, existingPlayerIDs);
                            tI.p_nicks.add(plNickName);
                            tI.players.add(player);
                            existingPlayerIDs.add(plNickName);
                        }
                    }
                    else badValues.append(player);
                }
                if(badValues.length()>0) {
                    errStr.append("Ignoring invalid entries: ");
                    errStr.append(badValues); errStr.append("\n");
                }
                if(!tI.isValid()) continue;
                Log.d(TAG, count + ":readTournaText: Adding " + tI.toString());
                retList.add(tI);
                existingTeamIDs.add(tI.name); //team ids
                count++;
                if(count>Constants.TOO_MANY_ENTRIES) break; //bad huge file! avoid tight loop
            }
            inputStream.close();
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "readTournaText: " + e.getMessage());
            //e.printStackTrace();
        }
        if(errStr.length()>0) {
            //showAlert(null, activity, "Warning", errStr.toString());
            //Cant be AlertDialog here, as the return below will happen even before
            //user response to the Alert.
            Toast.makeText(activity, errStr.toString(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "readTournaText: errStr=" + errStr.toString());
        }
        return retList;
    }

    ArrayList<TeamInfo> readTournaExcel(final Activity activity, Uri uri) {
        final ArrayList<TeamInfo> retList = new ArrayList<>();
        ArrayList<String> existingTeamIDs = new ArrayList<>();
        ArrayList<String> existingPlayerIDs = new ArrayList<>();
        StringBuilder errStr = new StringBuilder();
        try {
            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
            if (null == inputStream) {
                Log.e(TAG, "readTournaExcel: null stream");
                return retList;
            }
            Workbook m_workBook = Workbook.getWorkbook(inputStream);
            //p_sheetNo is excel sheet no which u want to read
            Sheet sheet = m_workBook.getSheet(0);

            if(sheet.getRows()>Constants.TOO_MANY_ENTRIES || sheet.getColumns()>Constants.TOO_MANY_ENTRIES) {
                Toast.makeText(activity, "Bad file!!", Toast.LENGTH_SHORT).show();
                return retList;
            }
            for (int row = 0; row < sheet.getRows(); row++) {
                String content = sheet.getCell(0, row).getContents();
                //if its heading, ignore.
                if(row==0 &&
                        (content.contains("TEAM NAME") || content.contains("POOL NAME") ||
                        content.contains("TEAMNAME") || content.contains("POOLNAME"))) continue;

                String desc = sheet.getCell(0, row).getContents();
                if (desc.isEmpty()) continue;
                if (!isValidString(desc)) {
                    String str = "Invalid team name [" + desc + "]\n";
                    errStr.append(str);
                    continue;
                }
                TeamInfo tI = new TeamInfo();
                tI.desc = desc;
                tI.name = getUniqIDStr(tI.desc, 0, existingTeamIDs);
                for (int col = 1; col < sheet.getColumns(); col++) {
                    String playerName = sheet.getCell(col, row).getContents();
                    if (playerName.isEmpty()) continue;
                    if (!isValidString(playerName)) {
                        String str = "Ignored invalid player name [" + playerName + "] in " +
                                tI.name + "\n";
                        errStr.append(str);
                        continue;
                    }
                    String pID = getUniqIDStr(playerName, 0, existingPlayerIDs);
                    boolean ignore = false;
                    for(String existingPlayer: tI.players) {
                        if(existingPlayer.equals(playerName)) {
                            ignore = true;
                            String str = "Ignored " + playerName + " (duplicate in " +
                                    tI.name + ")\n";
                            errStr.append(str);
                            break;
                        }
                    }
                    if(!ignore) {
                        tI.p_nicks.add(pID);
                        tI.players.add(playerName);
                        existingPlayerIDs.add(pID);
                    }
                }
                if(!tI.isValid()) continue;
                //Log.d(TAG, row + " readTournaExcel: Adding " + tI.toString());
                retList.add(tI);
                existingTeamIDs.add(tI.name);
            }
        } catch (IOException | BiffException e) {
            Log.e(TAG, "readTournaExcel: " + e.getMessage());
            //e.printStackTrace();
        }

        if(errStr.length()>0) {
            Log.d(TAG, "readTournaExcel: errStr=" + errStr.toString());
            Toast.makeText(activity, errStr.toString(), Toast.LENGTH_SHORT).show();
        }
        return retList;
    }

    static String getUniqIDStr(final String p, int len, final ArrayList<String> currentList) {
        if(p==null) return "null";
        String p1 = p.replaceAll("\\s+", "");  //remove all spaces
        if(len<=0) len = TeamDBEntry.MAX_ID_LEN;
        String id;
        if(p1.length() > len)
            //The substring begins at the specified beginIndex and extends to the character at
            //index endIndex - 1. Thus the length of the substring is endIndex-beginIndex.
            id = p1.substring(0,len); //not playerIdLen-1; see comment above
        else
            id = p1;
        id = id.toLowerCase();
        if(null!=currentList && currentList.contains(id)) {
            for (int i = 1; i < 100; i++) {
                //TeamDBEntry.MAX_STR_LEN = 12
                //%x.ys: The first digit (x) is the minimum length (the string will be left padded
                //if it's shorter), the second digit (y) is the maxiumum length and the string will
                //be truncated if it's longer.
                String fmtStr = "%." + (TeamDBEntry.MAX_ID_LEN-2) + "s%d"; //format = "<name>##"
                String newID = String.format(Locale.getDefault(),fmtStr, id, i)
                        .toLowerCase().trim();
                if(!currentList.contains(newID)) {
                    Log.d(TAG, fmtStr + " getUniqIDStr: uniqueName=" + newID);
                    return newID;
                } else {
                    //Log.d(TAG, "getUniqIDStr: name already taken:" + newID);
                }
            }

            for (int i = 0; i < 10; i++) {
                String newID = UUID.randomUUID().toString(); //nothing worked, just create a random name
                if(!currentList.contains(newID)) {
                    Log.d(TAG, "getUniqIDStr: generated uniqueName=" + newID);
                    return newID;
                }
            }
        } else {
            return id;
        }
        return "random" + random();   //don't think we will ever hit this
    }


    /*
    java.lang.NoClassDefFoundError: Failed resolution of: Ljavax/xml/stream/XMLStreamReader;
        at org.apache.xmlbeans.XmlBeans.buildStreamToNodeMethod(XmlBeans.java:251)

        https://stackoverflow.com/questions/55459623/android-read-xlsx-failed-resolution-of-ljavax-xml-stream-xmlstreamreader?noredirect=1&lq=1
        https://stackoverflow.com/questions/12258145/the-type-javax-xml-stream-xmlstreamreader-cannot-be-resolved


    public ArrayList<TeamInfo> readXLSXFile(final Activity activity, Uri uri) {
        ArrayList<TeamInfo> retList = new ArrayList<>();
        try {
            InputStream inputStream = activity.getContentResolver().openInputStream(uri);
            if (null == inputStream) {
                Log.e(TAG, "readXLSXFile: null stream");
                return retList;
            }
            //XSSFWorkbook m_workBook = Workbook.getWorkbook(inputStream);


            XSSFWorkbook m_workBook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = m_workBook.getSheetAt(0);


            XSSFRow row;
            XSSFCell cell;
            Iterator rows = sheet.rowIterator();

            while (rows.hasNext()) {
                row = (XSSFRow) rows.next();
                Iterator cells = row.cellIterator();
                while (cells.hasNext()) {
                    cell = (XSSFCell) cells.next();
                    if (cell.getCellType() == CellType.STRING) {
                        Log.d(TAG, cell.getRowIndex() + ":readXLSXFile: " + cell.toString());
                        if (cell.getRowIndex() == 0) {
                            Log.d(TAG, "readXLSXFile: rowIndex=0");
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "readXLSXFile: " + e.getMessage());
            //e.printStackTrace();
        }

        return retList;
    }
     */

    //To use the below progress dialog routines, you have to override onResume and onPause to add
    //ScoreTally.activityResumed() and ScoreTally.activityPaused()
    void startProgressDialog(final Activity activity, final String title, final String msg) {
        ProgressBar pgsBar = activity.findViewById(R.id.progressBar);
        if(pgsBar!=null) pgsBar.setVisibility(View.VISIBLE);
        //Log.d(TAG, title + " :Progress: " + msg );

        /*
        Using ProgressDialog leads to issues if the parent activity is minimized (not visible)
        Below error is seen in many situations:
        java.lang.IllegalArgumentException: View=DecorView@c85a80c[Fetching data] not attached to window manager
        Much better to use ProgressBar.
        See more notes in ClubLeagueActivity about behavior when there is no internet.
         */
    }

    void stopProgressDialog(final Activity activity) {
        //Log.d(TAG, "stopProgressDialog: vis="+ ScoreTally.isActivityVisible() +
        //        ",fin=" + activity.isFinishing());
        ProgressBar pgsBar = activity.findViewById(R.id.progressBar);
        if(pgsBar!=null) pgsBar.setVisibility(View.GONE);
    }

    public static void addNotification(final Context context, final String title, final String msg) {

        if(msg.isEmpty()) return;
        Log.d(TAG, "addNotification: [" + title + "][" + msg + "]");

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(context, MainSigninActivity.class);
        intent.putExtra(Constants.INTENT_DATASTR1, Constants.CHANNEL_NEWCLUB);
        intent.putExtra(Constants.INTENT_DATASTR2, msg);

        //FLAG_ACTIVITY_NEW_TASK: Start the activity in a new task. If a task is already running
        //for the activity you are now starting, that task is brought to the foreground with its
        //last state restored and the activity receives the new intent in onNewIntent().

        //FLAG_ACTIVITY_CLEAR_TASK: If set in an Intent passed to Context#startActivity, this flag
        //will cause any existing task that would be associated with the activity to be cleared
        //before the activity is started. That is, the activity becomes the new root of an otherwise
        //empty task, and any old activities are finished.
        //This can only be used in conjunction with FLAG_ACTIVITY_NEW_TASK.

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //Define sound URI
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, Constants.CHANNEL_NEWCLUB)
                        .setSmallIcon(R.drawable.birdie_red_btn)
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        // Set the intent that will fire when the user taps the notification
                        .setContentIntent(pendingIntent)
                        .setSound(soundUri)
                        .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());

    }

    public static void createNotificationChannel(final Context context) {
        Log.d(TAG, "createNotificationChannel: ");
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_NEWCLUB,
                    Constants.CHANNEL_NEWCLUB_NAME, importance);
            channel.setDescription("New Club Notification");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if(notificationManager!=null)
                notificationManager.createNotificationChannel(channel);
        }
    }

    public static void deleteNotificationChannel(final Context context) {
        Log.d(TAG, "deleteNotificationChannel: " + context.getClass().getName());
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if(notificationManager!=null)
                notificationManager.deleteNotificationChannel(Constants.CHANNEL_NEWCLUB);
        }
    }

    void addListenerForNewClub(final Context context) {
        if(mNewClubListener!=null) return;
        mNewClubListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String, ClubDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<Map<String, ClubDBEntry>>() {
                        };
                Map<String, ClubDBEntry> mClubs = dataSnapshot.getValue(genericTypeIndicator);
                if (null == mClubs) {
                    Log.d(TAG, "readDBForNewClubs: no new clubs");
                } else {
                    Log.d(TAG, "onDataChange: readDBForNewClubs: " + mClubs.size());
                    if(mClubs.size()>0) {
                        StringBuilder sb = new StringBuilder();
                        for (Map.Entry<String, ClubDBEntry> entry : mClubs.entrySet()) {
                            if (entry.getValue().getAc() < 0) {
                                sb.append(entry.getValue().getN());
                                sb.append(" ");
                            }
                        }
                        addNotification(context, "New clubs created", sb.toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.NEWCLUBS);
        dbRef.addValueEventListener(mNewClubListener);
        createNotificationChannel(context);
    }

    void deleteListenerForNewClub(final Context context) {
        if(mNewClubListener==null) return;
        Log.d(TAG, "deleteListenerForNewClub: " + context.getClass().getName());
        FirebaseDatabase.getInstance().getReference()
                .child(Constants.NEWCLUBS).removeEventListener(mNewClubListener);
        mNewClubListener = null;
        deleteNotificationChannel(context);
    }
}

