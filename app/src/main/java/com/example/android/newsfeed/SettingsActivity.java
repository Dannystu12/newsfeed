package com.example.android.newsfeed;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Created by Daniel on 11/07/2017.
 */

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
    }

    public static class NewsPreferenceFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener{

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);

            //Populate fragment with preferences defined in settings_main
            addPreferencesFromResource(R.xml.settings_main);

            Preference section = findPreference(getString(R.string.settings_section_key));
            bindPreferenceSummaryToValue(section);
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object value){

            String stringValue = value.toString();

            if(pref instanceof MultiSelectListPreference){
                MultiSelectListPreference multiSelectListPreference =
                        (MultiSelectListPreference) pref;

                if(value instanceof Set){
                    Set<String> selectedPrefs = (Set<String>) value;

                    //Convert the set to an array list so it can be alphabetised
                    ArrayList<String> selectedPrefsList = new ArrayList<String>(selectedPrefs);
                    Collections.sort(selectedPrefsList, String.CASE_INSENSITIVE_ORDER);

                    StringBuilder summary = new StringBuilder();

                    //Get a list of labels
                    CharSequence[] labels = multiSelectListPreference.getEntries();

                    boolean firstIteration = true;

                    //Loop through selected keys and retrieve their label
                    for(String key : selectedPrefsList){
                        // Get position of key in the list
                        int prefIndex = multiSelectListPreference.findIndexOfValue(key);

                        //Only add comma seperation on each iteration after the first
                        if(!firstIteration){
                            summary.append(", ");
                        } else {
                            firstIteration = false;
                        }

                        //If we find a valid index
                        if(prefIndex >= 0){
                            summary.append(labels[prefIndex]);
                        }
                    }

                    pref.setSummary(summary.toString());
                }

                int prefIndex = multiSelectListPreference.findIndexOfValue(stringValue);
                if (prefIndex >= 0){
                    CharSequence[] labels = ((MultiSelectListPreference) pref).getEntries();
                    pref.setSummary(labels[prefIndex]);
                }
            } else {
                pref.setSummary(stringValue);
            }

            return true;
        }


        /**
         * This function sets up each preference with a change listener and also forces
         * summary to display on first load
         * @param pref
         */
        private void bindPreferenceSummaryToValue(Preference pref){
            //Set listener so we can update summary when preference changes
            pref.setOnPreferenceChangeListener(this);

            //Here we simulate an on preference change with existing values to get them to display
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(pref.getContext());

            //Need to get set of selected options
            Set<String> selectedPreferences = preferences.getStringSet(pref.getKey(), null);
            onPreferenceChange(pref, selectedPreferences);
        }

    }

}
