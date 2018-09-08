package com.sg0.baddytally;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Random;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    //private AutoCompleteTextView mEmailView;
    private EditText mClubView;
    private EditText mUserView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private RadioGroup mGameTypeRadioGroup;
    private RadioButton mGameTypeRadioButton;
    private RadioGroup mGroupRadioGroup;
    private RadioButton mGroupRadioButton;
    private String mAdminCode;
    private String mMemCode;
    private String mClub;
    private String mUser;
    private String mRole;
    private String mInnings;
    //private boolean mCache2update;
    private boolean mInitialAttempt;
    private CheckBox mCheckNewRound;


    private void killActivity(){
        finish();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Set up the login form.
        mInitialAttempt = false;

        mClubView = findViewById(R.id.clubname);
        mUserView = findViewById(R.id.username);
        mPasswordView =  findViewById(R.id.password);
        mGameTypeRadioGroup = findViewById(R.id.gametype_radiogroup);
        mGroupRadioGroup = findViewById(R.id.gamegroup_radiogroup);
        mCheckNewRound = findViewById(R.id.check_newround);

        SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
        final String secpd = prefs.getString(Constants.DATA_SEC, "");
        if (!secpd.isEmpty()) {
            mPasswordView.setText(secpd);
        }

        final String club = prefs.getString(Constants.DATA_CLUB, "");
        final String user = prefs.getString(Constants.DATA_USER, "");
        if (club.isEmpty()) {
            mInitialAttempt = true;
            mGameTypeRadioGroup.setVisibility(View.GONE);
            mGroupRadioGroup.setVisibility(View.GONE);
            mCheckNewRound.setVisibility(View.GONE);
        }else {
            mClubView.setText(club);
            mUserView.setText(user);
        }

        if(!mInitialAttempt) {
            SharedData data = SharedData.getInstance();
            Log.d(TAG, "onCreate LoginActivity: data = " + data.toString());
        }

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    prepareForLogin(club, secpd);
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareForLogin(club, secpd);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);


        //Toast.makeText(LoginActivity.this,
        //        mGroupRadioButton.getText(), Toast.LENGTH_SHORT).show();


    }

    private void prepareForLogin(String club, String secpd) {
        if(mInitialAttempt) {
            mClub = mClubView.getText().toString();
            fetchInitialData();
            return;
        }
        /*
        if( club.equals(mClubView.getText().toString()) &&
                secpd.equals(mPasswordView.getText().toString())) {
            Log.w(TAG, "cache update not needed");
            mCache2update = false;  //no need to update the cache
        }*/
        mClub = mClubView.getText().toString();
        fetchInitialData();
        //attemptLogin();
    }

    //TODO: This DB fetch is done every time data is entered. This could be optimized to
    //reduce number of DB reads.
    private void fetchInitialData(){
        Log.w(TAG, "fetchInitialData: " + mClub );
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child(mClub).child(Constants.PROFILE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.w(TAG, "fetchInitialData: onDataChange");
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if (child.getKey().equals("admincode")) {
                        mAdminCode = child.getValue(String.class);
                    } else if (child.getKey().equals("memcode")) {
                        mMemCode = child.getValue(String.class);
                    }
                    Log.w(TAG, "fetchInitialData: onDataChange:" + mAdminCode + "/" + mMemCode);
                }
                attemptLogin();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "fetchInitialData: onCancelled", databaseError.toException());
                Toast.makeText(LoginActivity.this,
                        "DB error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        //if (mAuthTask != null) {
        //   return;
        //}
        Log.w(TAG, "attemptLogin:" + mAdminCode + ":" + mMemCode);
        // Reset errors.
        //mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String club = mClubView.getText().toString();
        String secpd = mPasswordView.getText().toString();
        mUser = mUserView.getText().toString();

        if(club.isEmpty() || secpd.isEmpty() || mUser.isEmpty()) {
            Toast.makeText(LoginActivity.this,
                    "All fields are mandatory!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(secpd) && !isPasswordValid(secpd)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            //mAuthTask = new UserLoginTask(password);
            //mAuthTask.execute((Void) null);
            if (secpd.equals(mAdminCode)) {
                mRole = Constants.ADMIN;
                successfulLogin(club, secpd);
            } else if (secpd.equals(mMemCode)) {
                mRole = Constants.MEMBER;
                successfulLogin(club, secpd);
            } else {
                showProgress(false);
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }
    }

    private void successfulLogin(String club, String secpd) {
            SharedPreferences prefs = getSharedPreferences(Constants.USERDATA, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.DATA_CLUB, club);
            editor.putString(Constants.DATA_SEC, secpd);
            editor.putString(Constants.DATA_ROLE, mRole);
            editor.putString(Constants.DATA_USER, mUser);
            editor.commit();

        showProgress(false);
        if(mInitialAttempt) {
            killActivity();   //finish was not ending the activity here.
            return;
        }

        int selectedId = mGameTypeRadioGroup.getCheckedRadioButtonId();
        // find the radiobutton by returned id
        mGameTypeRadioButton = findViewById(selectedId);
        //Toast.makeText(LoginActivity.this,
        //        mGameTypeRadioButton.getText(), Toast.LENGTH_SHORT).show();


        selectedId = mGroupRadioGroup.getCheckedRadioButtonId();
        // find the radiobutton by returned id
        mGroupRadioButton = findViewById(selectedId);
        Log.w(TAG, "successfulLogin:" + mGameTypeRadioButton.getText() + ":" + mGroupRadioButton.getText());
        String newRoundFlag = "False";
        if(mCheckNewRound.isChecked()) newRoundFlag = Constants.NEWROUND;
        Log.w(TAG, "successfulLogin, new round flag:" + newRoundFlag);
        Intent myIntent = new Intent(LoginActivity.this, EnterData.class);
        myIntent.putExtra("gametype", mGameTypeRadioButton.getText());
        myIntent.putExtra("group", mGroupRadioButton.getText());
        myIntent.putExtra("new_round", newRoundFlag);
        LoginActivity.this.startActivity(myIntent);
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /*
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }



    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }*/

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mPassword;

        UserLoginTask(String password) {
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            if (mPassword.equals("kbc123456")) {
                return true;
            } else if (mPassword.equals("kbc3331")) {
                return true;
            } else {
                return false;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

