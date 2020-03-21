package com.sg0.baddytally;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class RootOptions extends AppCompatActivity {

    private static final String TAG = "RootOptions";

    private SharedData mCommon;
    private Map<String, ClubDBEntry> mClubs;
    private Handler mMainHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_root_options);

        mCommon = SharedData.getInstance();
        mClubs = new HashMap<>();
        mMainHandler = new Handler();

        mCommon.wakeupdbconnectionProfileRoot();

        findViewById(R.id.clubs_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showClubs();
            }
        });

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        //Log.d(TAG, "onCreate: check for root:" + SharedData.getInstance().toString());
        mCommon.startProgressDialog(RootOptions.this,"", "");
        mCommon.showToastAndDieOnTimeout(mMainHandler, RootOptions.this,
                "Check your internet connection!", false, true, 2000);
        readDBForNewClubs();
    }



    void readDBForNewClubs() {
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

                if(mClubs.size()>0) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            createActivateButtons();
                        }
                    });
                }

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
                mMainHandler.removeCallbacksAndMessages(null);
                mCommon.stopProgressDialog(RootOptions.this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    void createActivateButtons() {
        int count = 0;
        for (Map.Entry<String, ClubDBEntry> entry : mClubs.entrySet()) {
            final ClubDBEntry club = entry.getValue();

            LinearLayout layout = findViewById(R.id.root_options_ll);

            //set the properties for button
            Button btnTag = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            //params.setMargins(30, 5, 30, 5);
            btnTag.setLayoutParams(params);
            String btnTitle = "Activate code for " + club.getN();
            btnTag.setText(btnTitle);
            btnTag.setId(count);
            if(club.getAc()>0) btnTag.setBackgroundColor(Color.LTGRAY);

            btnTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String msg = String.format(Locale.getDefault(),
                            "desc: %s\nemail: %s\nowner: %s\nphone: %s\ncode: %d\nmax players:%d\n",
                            club.getDes(), club.getEmail(), club.getOwnr(), club.getPh(),
                            club.getAc(), club.getPlN());
                    mCommon.showAlert(null, RootOptions.this,
                            club.getN(), msg);
                }
            });

            btnTag.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(!mCommon.isPermitted(RootOptions.this)) {
                        return false;
                    }
                    final int random = new Random().nextInt(9000) + 1000; //1000-9999
                    FirebaseDatabase.getInstance().getReference()
                            .child(Constants.NEWCLUBS).child(club.getN())
                            .child("ac").setValue(random);
                    Toast.makeText(RootOptions.this,
                            "Activation code [" + random + "] set in DB!",
                            Toast.LENGTH_SHORT).show();
                    sendEmail(club.getEmail(), random, club.getN());
                    return true;
                }
            });

            //add button to the layout
            layout.addView(btnTag);
        }
    }

    void showClubs() {
        if(mClubs.size()==0) return;
        final ArrayList<String> clubList = new ArrayList<>();
        for(Map.Entry<String,ClubDBEntry> entry : mClubs.entrySet()) {
            clubList.add(entry.getKey());
        }
        Collections.sort(clubList);  //sort the tournament list before adding to pop menu

        final CharSequence[] items = new CharSequence[clubList.size()];
        int i = 0; for (String t: clubList) {items[i] = t; i++;}

        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setIcon(R.drawable.birdie02);
        alt_bld.setTitle(mCommon.getStyleString("Select the club", Typeface.BOLD));
        alt_bld.setSingleChoiceItems(items, -1, new DialogInterface
                .OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                String club = items[item].toString();
                ClubDBEntry clubDBEntry = mClubs.get(club);
                dialog.dismiss();
                if(clubDBEntry==null) return;
                showClubPopup(club);

            }
        });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    private void showClubPopup(final String club) {
        final Button view = findViewById(R.id.clubs_btn);
        Context wrapper = new ContextThemeWrapper(RootOptions.this, R.style.RegularPopup);
        final PopupMenu popup = new PopupMenu(wrapper, view);
        popup.getMenuInflater().inflate(R.menu.summary_popup_menu, popup.getMenu());
        popup.getMenu().clear();
        Menu pMenu = popup.getMenu();
        pMenu.add(Constants.ACCESS);
        pMenu.add(Constants.DELETE);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String action = menuItem.getTitle().toString();
                //Log.v(TAG, "showOptions showGroupPopup:" + group);
                popup.dismiss();
                if(action.equals(Constants.ACCESS)) {
                    SharedData.getInstance().mClub = club;
                    SharedData.getInstance().mRole = Constants.ROOT;
                    Intent myIntent = new Intent(RootOptions.this, MainSelection2.class);
                    RootOptions.this.startActivity(myIntent);
                } else if(action.equals(Constants.DELETE)) {
                    deleteClub(club);
                }
                return true;
            }
        });
        popup.show();//showing popup menu
    }

    private void deleteClub(final String club) {
        if(club.isEmpty()) return;
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(RootOptions.this);
        alertBuilder.setTitle("Delete club");
        alertBuilder.setMessage("Sure about " + club + "?");
        alertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                        .child(Constants.ACTIVECLUBS).child(club);
                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            dataSnapshot.getRef().removeValue();
                            Toast.makeText(RootOptions.this,
                                    "Club [active/" + club + "] deleted!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w(TAG, "DB error for: active/"+ club);
                    }
                });

                dbRef = FirebaseDatabase.getInstance().getReference().child(club);
                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            dataSnapshot.getRef().removeValue();
                            Toast.makeText(RootOptions.this,
                                    "Club [" + club + "] deleted!",
                                    Toast.LENGTH_SHORT).show();
                            mMainHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //not the best impl, but will do for root options.
                                    //give 1.5s for active/club node to be delete, which in fact should be faster
                                    Toast.makeText(RootOptions.this,
                                            "Refreshing...",
                                            Toast.LENGTH_SHORT).show();
                                    recreate();
                                }
                            }, 1500);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.w(TAG, "DB error for: "+ club);
                    }
                });

            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        alertBuilder.show();
    }

    private void sendEmail(final String emailid, final int act_code, final String club) {
        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{emailid});
        email.putExtra(Intent.EXTRA_SUBJECT, "ScoreTally: activation of your club " + club);
        //html hyperlink doesnt work: It works only when the alias string is same as the url string
        //        .append("<a href='http://www.facebook.com'> facebook</a>")  //doesnt work
        //        .append("<a href=http://www.facebook.com> facebook</a>")  //doesnt work
        //        .append("<a href='http://www.facebook.com'> http://www.facebook.com</a>")  //works
        //        .append("<a href=http://www.facebook.com> http://www.facebook.com</a>")  //works

        //bold/strong etc formatting below doesnt work for gmail.
        email.putExtra(Intent.EXTRA_TEXT,
                Html.fromHtml(new StringBuilder()
                    .append("<html><body><p>Thanks for using ScoreTally app.</p>")
                    .append("<p>Activation code for your new club is <mark>")
                    .append(act_code)
                    .append("</mark>.</p>")
                    .append("<p>Go back to the ScoreTally app and activate your club '")
                    .append(club)
                    .append("'. ")
                    .append("You will have to enter <strong>3 different passwords</strong> for the below 3 roles. " +
                        "App users of your club can login to the app as either one of these <strong>3 roles</strong>.<br/><br/>")
                    .append("&#8226; <em>super-user</em>: password for this user should not be shared. " +
                        "This user has all the permissions (ex: create/delete/update players/tournaments/scores).<br/><br/>")
                    .append("&#8226; <em>admin</em>: password for this user can be shared with club administrators and match officials. " +
                        "This user has limited permissions (ex: enter scores).<br/><br/>")
                    .append("&#8226; <em>member</em>: password for this user can be shared with public. " +
                            "This is view-only mode, no changes can be made.<br/><br/></p>")
                    .append("<p><strong>Useful links:</strong><br/><br/>")
                    .append("&#8226; <em>User Guide</em>: <a href='https://sites.google.com/view/scoretally/home'>https://sites.google.com/view/scoretally/home</a><br/><br/>")
                    .append("&#8226; <em>Demo video1</em>: <a href='https://www.youtube.com/watch?v=ePddeJr9x-U'>https://www.youtube.com/watch?v=ePddeJr9x-U</a><br/><br/>")
                    .append("&#8226; <em>Demo video2</em>: <a href='https://www.youtube.com/watch?v=JD8Emv8o9Q0'>https://www.youtube.com/watch?v=JD8Emv8o9Q0</a><br/><br/></p>")
                    .append("<p>Don't hesitate to get back to us, in case of queries/issues.</p>")
                    .append("<p>Enjoy your games!</p></body></html>")
                    .toString()));

        //email.setType("message/rfc822");
        email.setType("text/html");
        startActivity(Intent.createChooser(email, "Choose an Email client :"));
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.basic_menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(RootOptions.this);
        switch (item.getItemId()) {
            case R.id.action_refresh:
                mCommon.addListenerForNewClub(getApplicationContext()); //in case if listener is not active
                recreate();
                break;
            case R.id.action_logout:
                SharedData.getInstance().logOut(RootOptions.this, true);
                break;
            case R.id.action_about:
                SharedData.showAboutAlert(RootOptions.this);
                break;
            case android.R.id.home:
                //MainSigninActivity does not have history
                mCommon.killActivity(this, RESULT_OK);
                Intent intent = new Intent(RootOptions.this, MainSigninActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                break;
        }
        return true;
    }

}