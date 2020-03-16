package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class NewClub extends AppCompatActivity {
    private static final String TAG = "NewClub";
    private SharedData mCommon;
    private Handler mMainHandler;
    private ArrayAdapter<Integer> mNumPlayersAdapter;
    private Map<String, ClubDBEntry> mClubs;
    private String mUserId;
    private UserDBEntry mUserDBEntry;
    private String mRadioSubsciptionStr;
    private boolean mInit;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_club);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mClubs = new HashMap<>();
        mMainHandler = new Handler();
        findViewById(R.id.activation_code_ll).setVisibility(View.GONE);
        mCommon = SharedData.getInstance();
        mUserId = mCommon.getUserID(NewClub.this);
        mRadioSubsciptionStr = Constants.SUBSC_FREE;
        mInit = true;

        readDBForUser();
        readDBForNewClubs();  //see if there is a new club created by this user. If yes, highlight activate

        final Spinner numPlayersSpinner = findViewById(R.id.numPlayers_spinner);
        mNumPlayersAdapter = new ArrayAdapter<>(NewClub.this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>(Arrays.asList(16))); //default is free subscription
        mNumPlayersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numPlayersSpinner.setAdapter(mNumPlayersAdapter);
        numPlayersSpinner.setSelection(0);

        RadioGroup rg = findViewById(R.id.radio_subscription);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int selectedId) {
                RadioButton rbtn = findViewById(selectedId);
                mRadioSubsciptionStr = rbtn.getText().toString();
                //Log.d(TAG, selectedId + ":onCheckedChanged:" + mRadioSubsciptionStr);
                if (mRadioSubsciptionStr.equals(Constants.SUBSC_PAID)) {
                    mNumPlayersAdapter.clear();
                    mNumPlayersAdapter.addAll(Arrays.asList(16, 32, 64));
                    mNumPlayersAdapter.notifyDataSetChanged();
                } else if (mRadioSubsciptionStr.equals(Constants.SUBSC_FREE)) {
                    mNumPlayersAdapter.clear();
                    mNumPlayersAdapter.add(16);
                    mNumPlayersAdapter.notifyDataSetChanged();
                }
                //Log.d(TAG, selectedId + ":onCheckedChanged:" + mNumPlayersAdapter.toString());
            }
        });

        Button enter = findViewById(R.id.enter_button);
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mInit = false;
                String club = ((EditText) findViewById(R.id.et_newClub)).getText().toString();
                String des = ((EditText) findViewById(R.id.et_newClub_desc)).getText().toString();
                String email = ((EditText) findViewById(R.id.et_email)).getText().toString();
                String phone = ((EditText) findViewById(R.id.et_phone)).getText().toString();


                Log.d(TAG, "Enter button:[" + club + "][" + des + "][" +
                        email + "][" + phone + "] radio=" + mRadioSubsciptionStr);

                if (club.isEmpty() || email.isEmpty()) {
                    Toast.makeText(NewClub.this, "Fill at least the mandatory fields.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isEmailValid(email)) {
                    Toast.makeText(NewClub.this, "Not a valid email id!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!phone.isEmpty()) {
                    if (!PhoneNumberUtils.isGlobalPhoneNumber(phone)) {
                        Toast.makeText(NewClub.this, "Not a valid phone number!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                Integer playerCount = (Integer) numPlayersSpinner.getSelectedItem();
                if (mRadioSubsciptionStr.equals(Constants.SUBSC_FREE)) {
                    if (playerCount > Constants.MAX_NUM_PLAYERS_DEFAULT) {
                        Toast.makeText(NewClub.this,
                                "Limit exceeded for Free subscription! ["
                                        + Constants.MAX_NUM_PLAYERS_DEFAULT + "]",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                readDBForNewClubs();  //just get the latest, in case somebody has created clubs just now
            }
        });

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCommon.killActivity(NewClub.this, RESULT_OK);
            }
        });

        ((EditText) findViewById(R.id.et_memberPasswd)).setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                //Log.d(TAG, "onEditorAction: et_memberPasswd:" + id);
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_ACTION_NEXT ||
                        id == EditorInfo.IME_NULL) {
                    findViewById(R.id.activate_button).callOnClick();
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.activate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String act_club = ((EditText) findViewById(R.id.et_activation_club)).getText().toString();
                int act_code = -1;
                try {
                    act_code = Integer.valueOf(((EditText) findViewById(R.id.et_activation_code)).getText().toString());
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Wrong format of Activation code");
                }
                //Log.d(TAG, act_club + ": activateBtn button: code=" + act_code);
                if (act_club.isEmpty() || act_code < 0) {
                    Toast.makeText(NewClub.this, "Enter data first!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                final String superPasswd = ((EditText) findViewById(R.id.et_superPasswd)).getText().toString();
                final String adminPasswd = ((EditText) findViewById(R.id.et_adminPasswd)).getText().toString();
                final String memPasswd = ((EditText) findViewById(R.id.et_memberPasswd)).getText().toString();

                if (superPasswd.isEmpty() || adminPasswd.isEmpty() || memPasswd.isEmpty()) {
                    Toast.makeText(NewClub.this, "Enter all 3 passwords for your club!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (superPasswd.length()<4 || adminPasswd.length()<4 || memPasswd.length()<4) {
                    Toast.makeText(NewClub.this, "Passwords are too short!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "activateBtn button: super=" + superPasswd +
                        ", admin=" + adminPasswd + ", mem=" + memPasswd);

                if (!mUserId.isEmpty() && mClubs.size() > 0) {
                    for (Map.Entry<String, ClubDBEntry> entry : mClubs.entrySet()) {
                        final ClubDBEntry clubDBEntry = entry.getValue();
                        if (clubDBEntry.getN().equals(act_club) &&
                                clubDBEntry.getOwnr().equals(mUserId)) {
                            //Log.d(TAG, mUserId + ":activating the club :"
                            //        + clubDBEntry.toString());
                            if (clubDBEntry.getAc() == act_code) {
                                //club and code matches

                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        clubDBEntry.setAc(0);
                                        clubDBEntry.setActive(true);
                                        FirebaseDatabase.getInstance().getReference()
                                                .child(Constants.ACTIVECLUBS).child(act_club).setValue(clubDBEntry);
                                        FirebaseDatabase.getInstance().getReference()
                                                .child(Constants.NEWCLUBS).child(act_club).setValue(null);

                                        ProfileDBEntry profile = new ProfileDBEntry();
                                        profile.setDes(clubDBEntry.getDes());
                                        profile.setRc(superPasswd);
                                        profile.setAc(adminPasswd);
                                        profile.setMc(memPasswd);
                                        profile.setNews("Welcome!");
                                        FirebaseDatabase.getInstance().getReference()
                                                .child(act_club).child(Constants.PROFILE).setValue(profile);

                                        Toast.makeText(NewClub.this,
                                                "Club " + act_club + " activated!\nYou can login now.",
                                                Toast.LENGTH_LONG).show();
                                        Intent myIntent = new Intent(NewClub.this, LoginActivity.class);
                                        myIntent.putExtra(Constants.ACTIVITY, Constants.INITIAL);
                                        myIntent.putExtra(Constants.DATA_CLUB, act_club);
                                        NewClub.this.startActivity(myIntent);
                                        mCommon.killActivity(NewClub.this, RESULT_OK);
                                    }
                                });

                                return;
                            } else {
                                Toast.makeText(NewClub.this, "Wrong activation code!",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }
                } else {
                    Log.e(TAG, mUserId + ":activating the club: no clubs found!");
                }

                Toast.makeText(NewClub.this, "Club '" + act_club + "' not found!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.del_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String act_club = ((EditText) findViewById(R.id.et_activation_club)).getText().toString();

                if(act_club.isEmpty()) {
                    Toast.makeText(NewClub.this, "Enter your club name!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                ClubDBEntry clubInDB = mClubs.get(act_club);
                if(clubInDB==null || mUserId.isEmpty() || !clubInDB.getOwnr().equals(mUserId)) {
                    Toast.makeText(NewClub.this, "Not valid operation!",
                            Toast.LENGTH_SHORT).show();
                    mCommon.killActivity(NewClub.this, RESULT_OK);
                    return;
                }



                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(NewClub.this);
                alertBuilder.setTitle("Delete new club '" + act_club + "'?");
                alertBuilder.setMessage("\nAre you sure?\n");
                alertBuilder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //if act_club is empty, then child(Constants.NEWCLUBS) will be deleted
                        FirebaseDatabase.getInstance().getReference()
                                .child(Constants.NEWCLUBS).child(act_club).setValue(null);

                        Toast.makeText(NewClub.this, "New Club '" + act_club + "' deleted!",
                                Toast.LENGTH_LONG).show();
                        mCommon.killActivity(NewClub.this, RESULT_OK);
                    }
                });

                alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                alertBuilder.show();


            }
        });

        findViewById(R.id.act_cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCommon.killActivity(NewClub.this, RESULT_OK);
            }
        });

        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


    }

    boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    void readDBForUser() {
        if (mUserId.isEmpty()) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.ACTIVE_USERS).child(mUserId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUserDBEntry = dataSnapshot.getValue(UserDBEntry.class);
                if (mUserDBEntry == null) return;
                //Log.d(TAG, "readDBForUser: " + mUserDBEntry.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    void readDBForNewClubs() {

        mCommon.startProgressDialog(NewClub.this, "", "");
        mCommon.showToastAndDieOnTimeout(mMainHandler, NewClub.this,
                "Check your internet connection", true, true, 0);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.NEWCLUBS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String, ClubDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<Map<String, ClubDBEntry>>() {
                        };
                mClubs = dataSnapshot.getValue(genericTypeIndicator);
                if (null == mClubs) {
                    Log.d(TAG, "readDBForNewClubs: no new clubs");
                    mClubs = new HashMap<>();
                }
                readDBForActiveClubs();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    void readDBForActiveClubs() {

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child(Constants.ACTIVECLUBS);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                GenericTypeIndicator<Map<String, ClubDBEntry>> genericTypeIndicator =
                        new GenericTypeIndicator<Map<String, ClubDBEntry>>() {
                        };
                Map<String, ClubDBEntry> actClubs = dataSnapshot.getValue(genericTypeIndicator);
                if (null == actClubs) {
                    Log.d(TAG, "readDBForActClubs: no Active clubs");
                } else {
                    for (Map.Entry<String, ClubDBEntry> entry : actClubs.entrySet()) {
                        ClubDBEntry club = new ClubDBEntry();
                        entry.getValue().copyData(club);
                        //Log.d(TAG, "readDBForActClubs: adding " + club.toString());
                        mClubs.put(entry.getKey(), club);
                    }
                }
                mMainHandler.removeCallbacksAndMessages(null); //remove progress timeout event
                mCommon.stopProgressDialog(NewClub.this);
                if (mInit) {
                    highlightActivateIfNeeded();
                } else postEventForCreateNewClub();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    void highlightActivateIfNeeded() {
        if (!mUserId.isEmpty() && mClubs.size() > 0) {
            for (Map.Entry<String, ClubDBEntry> entry : mClubs.entrySet()) {
                if (entry.getValue().getOwnr().equals(mUserId)) {
                    Log.d(TAG, mUserId + ":highlightActivateIfNeeded :"
                            + entry.getValue().toString());
                    if (!entry.getValue().isActive()) {

                        ((EditText) findViewById(R.id.et_activation_club))
                                .setText(entry.getValue().getN());

                        //new club activation code is generated, but club is not active yet
                        findViewById(R.id.create_club_ll).setVisibility(View.GONE);
                        findViewById(R.id.activation_code_ll).setVisibility(View.VISIBLE);
                        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

                        if(entry.getValue().getAc() > 0)  //activation code is created
                            ((TextView) findViewById(R.id.title_tv)).setText("Activate your new club");
                        else
                            ((TextView) findViewById(R.id.title_tv)).setText("Code is not generated yet!");
                        //Even if the activation code is not set in DB, see if there is a new
                        //club created by this user. If it is there, give the user an option to delete
                        //the club.

                        String msg = "";
                        if(!entry.getValue().getCmt().isEmpty()) //in case there is a comment set by root
                            msg += entry.getValue().getCmt() + "\n";

                        ((TextView) findViewById(R.id.comment_tv))
                                .setText(mCommon.getBgColorString(
                                        msg, Color.CYAN));

                        if(entry.getValue().getAc() > 0) { //activation code is created
                            findViewById(R.id.activate_button).startAnimation(shake);
                        } else { //activation code is not created
                            findViewById(R.id.activate_button).setVisibility(View.GONE);
                            findViewById(R.id.et_activation_code).setVisibility(View.GONE);
                            findViewById(R.id.et_superPasswd).setVisibility(View.GONE);
                            findViewById(R.id.et_adminPasswd).setVisibility(View.GONE);
                            findViewById(R.id.et_memberPasswd).setVisibility(View.GONE);
                            findViewById(R.id.del_button).startAnimation(shake);
                        }

                        return;
                    }
                }
            }
        }
    }

    void postEventForCreateNewClub() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                createNewClub();
            }
        });
    }

    void createNewClub() {

        final String club = ((EditText) findViewById(R.id.et_newClub)).getText().toString();
        final String des = ((EditText) findViewById(R.id.et_newClub_desc)).getText().toString();
        final String email = ((EditText) findViewById(R.id.et_email)).getText().toString();
        final String phone = ((EditText) findViewById(R.id.et_phone)).getText().toString();
        final Integer playerCount = (Integer) ((Spinner) findViewById(R.id.numPlayers_spinner)).getSelectedItem();

        int maxClubCount = Constants.MAXNUM_CLUBS_PER_USER;
        if (null != mUserDBEntry) {
            maxClubCount = mUserDBEntry.getMaxC();
        }

        ClubDBEntry old = mClubs.get(club);
        if (old != null) {
            //Log.d(TAG, "createNewClub: old club found:" + old.toString());
            Toast.makeText(NewClub.this, "Club name " + club + " is already taken!",
                    Toast.LENGTH_LONG).show();
            return;
        }


        if (mUserId.isEmpty()) {
            Toast.makeText(NewClub.this, "Internal error: no user id!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        int count = 1; //1 for the new club being added
        for (Map.Entry<String, ClubDBEntry> entry : mClubs.entrySet()) {
            if (entry.getValue().getOwnr().equals(mUserId)) {
                //Log.d(TAG, count + ":createNewClub [" + mUserId + "]: "
                //        + entry.getValue().toString());
                count++;
            }
        }

        if (count > maxClubCount) {
            Toast.makeText(NewClub.this, "User limit [" + maxClubCount + "] reached!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        //hide keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (null != inputMethodManager)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(NewClub.this);
        alertBuilder.setTitle("New club '" + club + "'");
        alertBuilder.setMessage("\nMake sure that the below email id is correct:\n" + email +
                "\n\nActivation code will be send to this email." +
                "\nOnce you have the code in your inbox, come back to this screen and activate the code.");
        alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ClubDBEntry clubDBEntry = new ClubDBEntry(club, des, email, phone, mUserId, playerCount);
                FirebaseDatabase.getInstance().getReference()
                        .child(Constants.NEWCLUBS).child(club).setValue(clubDBEntry);
                //Log.d(TAG, "createNewClub: done: " + clubDBEntry.toString());

                Toast.makeText(NewClub.this, "Club '" + club + "' created!" +
                                "\nActivation code will be send to your email.",
                        Toast.LENGTH_LONG).show();
                mCommon.killActivity(NewClub.this, RESULT_OK);
            }
        });

        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alertBuilder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "onOptionsItemSelected: ");
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}


