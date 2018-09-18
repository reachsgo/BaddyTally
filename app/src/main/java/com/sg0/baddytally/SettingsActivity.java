package com.sg0.baddytally;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

//SGO: This class is not used now. Just for trying out Preferences settings menu

@SuppressLint("Registered")
public class SettingsActivity extends AppCompatActivity {
    public static final String
            KEY_PREF_CACHE_CLEAR = "switch_clear";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
