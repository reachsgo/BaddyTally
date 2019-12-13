package com.sg0.baddytally;


import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;


/**
 *  SGO: not used!
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState,
                                    String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

}
