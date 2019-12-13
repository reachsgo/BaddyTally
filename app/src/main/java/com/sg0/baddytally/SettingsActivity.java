package com.sg0.baddytally;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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
