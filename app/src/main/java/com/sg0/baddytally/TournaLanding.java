package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Map;


public class TournaLanding extends AppCompatActivity implements CallbackRoutine {

    private static final String TAG = "TournaLanding";
    private static final String DELETE_TOURNA = "Delete tournament";

    private TournaUtil mTUtil;
    private SharedData mCommon;
    //private TournaEditTextDialog mCustomDialog;
    private ListView mTournaLV;
    private ArrayAdapter mTournaLA;
    private ArrayList<String> mTournaList;

    private Handler mMainHandler;

    private void killActivity() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.tourna_landing);
        Log.d(TAG, "onCreate: ");
        mTUtil = new TournaUtil(TournaLanding.this, TournaLanding.this);
        mCommon = SharedData.getInstance();
        mMainHandler = new Handler();

        //mCustomDialog = new TournaEditTextDialog(TournaLanding.this, TournaLanding.this);
        mTournaList = new ArrayList<String>();
        mTournaLA = new ArrayAdapter<String>(
                this, R.layout.listitem_bigbold,
                mTournaList);
        mTournaLV = findViewById(R.id.tournaments_lv);
        mTournaLV.setAdapter(mTournaLA);


        mTournaLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mTournaList.size() > i) {
                    Log.d(TAG, "mTournaLV onItemClick: " + mTournaList.get(i));
                    mCommon.mTournament = mTournaList.get(i);
                    selectActivityForTourna();
                }
            }
        });

        mTournaLV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (!mCommon.isRoot()) return false;
                if (i >= mTournaList.size()) return false;
                mCommon.wakeUpDBConnection_profile();


                Log.d(TAG, "mTournaLV onItemClick: " + mTournaList.get(i));
                final String tourna = mTournaList.get(i);


                Context wrapper = new ContextThemeWrapper(TournaLanding.this, R.style.RedPopup);
                final PopupMenu popup = new PopupMenu(wrapper, view);
                popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
                if (Build.VERSION.SDK_INT >= 23) {
                    popup.setGravity(Gravity.END);
                }
                popup.getMenu().clear();
                Menu pMenu = popup.getMenu();
                pMenu.add(DELETE_TOURNA);

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Log.v(TAG, "onMenuItemClick:" + menuItem.getTitle().toString());
                        String choice = menuItem.getTitle().toString();
                        if (choice.equals(DELETE_TOURNA)) deleteTourna(tourna);
                        popup.dismiss();
                        return true;
                    }
                });
                popup.show();//showing popup menu
                return true;
            }
        });

        LinearLayout ll = findViewById(R.id.tlanding_ll);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show a toast message.
                Snackbar.make(mTournaLV, "Select a tournament", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        refresh();
    }

    private void refresh() {

        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TournaLanding.this,
                        "No connection to internet!", Toast.LENGTH_LONG).show();
                killActivity();
            }
        }, 3000);
        mTUtil.fetchActiveTournaments(); //CB_READTOURNA
    }

    private Boolean selectActivityForTourna() {
        Log.d(TAG, "selectActivityForTourna: ");
        if (mCommon.mTournament.isEmpty()) return false;
        if (null == mCommon.mTournaMap || mCommon.mTournaMap.size() == 0) return false;
        for (Map.Entry<String, String> tourna : mCommon.mTournaMap.entrySet()) {
            if (tourna.getKey().equals(mCommon.mTournament)) {
                if (tourna.getValue().equals(Constants.DE) || tourna.getValue().equals(Constants.SE)) {
                    Intent myIntent = new Intent(TournaLanding.this, TournaTableLayout.class);
                    //dont keep this activity in stack to reduce heap usage (mainly due to background image)
                    //history=false set in manifest
                    TournaLanding.this.startActivity(myIntent);
                } else if (tourna.getValue().equals(Constants.LEAGUE)) {
                    Intent myIntent = new Intent(TournaLanding.this, TournaLeague.class);
                    //dont keep this activity in stack to reduce heap usage (mainly due to background image)
                    //history=false set in manifest
                    TournaLanding.this.startActivity(myIntent);
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");

        if (mCommon.isDBUpdated()) {
            refresh();
            mCommon.setDBUpdated(false);
        }
    }

    @Override
    public void onBackPressed() {
        mCommon.killApplication();
    }

    //CallbackRoutine Callback interfaces
    public void profileFetched() {
    }

    public void callback(final String key, final Object inobj) {
    }

    public void alertResult(final String in, final Boolean ok, final Boolean ko) {
        if (in.contains(DELETE_TOURNA)) {
            if (ok) {
                String parts[] = in.split(Constants.COLON_DELIM);
                if (parts.length != 2) {
                    Log.v(TAG, "alertResult Internal error:" + parts.length);
                    mCommon.showToast(TournaLanding.this, "Internal error!", Toast.LENGTH_SHORT);
                    return;
                }
                updateDB_deleteTourna(parts[1]);
            }
        }
    }

    public void completed(final String in, final Boolean ok) {
        Log.w(TAG, "completed: " + in + ":" + ok);
        if (in.equals(Constants.CB_READTOURNA)) {
            mMainHandler.removeCallbacksAndMessages(null);
            if (ok) {
                Log.d(TAG, "completed: " + mCommon.mTournaMap.size());
                ArrayList<String> tournaList = new ArrayList<>();
                for (Map.Entry<String, String> entry : mCommon.mTournaMap.entrySet()) {
                    tournaList.add(entry.getKey());
                }
                if (tournaList.size() > 0) {
                    mTournaLA.clear();
                    mTournaLA.addAll(tournaList);
                    //mTournaList = tournaList;
                    //mTournaLV.setAdapter(mTournaLA);
                    mTournaLA.notifyDataSetChanged();
                }
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.basic_menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                refresh();
                break;
            // action with ID action_settings was selected
            case R.id.action_settings:
                //wake up connection and read profile again from DB to check for password changes
                mCommon.wakeUpDBConnection_profile();
                Intent myIntent = new Intent(TournaLanding.this, TournaSettings.class);
                TournaLanding.this.startActivity(myIntent);
                break;
            case R.id.action_logout:
                SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.commit();
                mCommon.clear();
                Toast.makeText(TournaLanding.this, "Cache cleared!", Toast.LENGTH_SHORT)
                        .show();
                Intent intent = new Intent(TournaLanding.this, MainSigninActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case R.id.action_about:
                //int versionCode = BuildConfig.VERSION_CODE;
                AlertDialog.Builder builder = new AlertDialog.Builder(TournaLanding.this);
                builder.setMessage("Version: " + BuildConfig.VERSION_NAME)
                        .setTitle(SharedData.getInstance().getTitleStr(Constants.APPNAME, TournaLanding.this))
                        .setNeutralButton("Ok", null).show();
                break;
            default:
                break;
        }
        return true;
    }

    private void deleteTourna(final String tourna) {
        if (!mCommon.isPermitted(TournaLanding.this)) {
            return;
        }
        Log.v(TAG, "deleteTourna:" + tourna);
        SharedData.getInstance().wakeUpDBConnection();
        String msg = "Existing tournament data will be lost permanently. Are you sure? ";
        mCommon.showAlert(this, TournaLanding.this, DELETE_TOURNA + Constants.COLON_DELIM + tourna, msg);
    }

    private void updateDB_deleteTourna(final String tourna) {
        if (!mCommon.isDBConnected()) {
            Toast.makeText(TournaLanding.this,
                    "DB connection is stale, refresh and retry...", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "updateDB_deleteTourna:" + tourna);
        DatabaseReference teamsDBRef = FirebaseDatabase.getInstance().getReference()
                .child(mCommon.mClub).child(Constants.TOURNA);
        teamsDBRef.child(tourna).setValue(null);
        teamsDBRef.child(Constants.ACTIVE).child(tourna).setValue(null);
        refresh();
    }

}
