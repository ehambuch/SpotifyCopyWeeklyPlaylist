package de.erichambuch.spotify.copyweekly;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class MyPreferencesActivity extends AppCompatActivity {

    public static class MyPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new MyPreferencesActivity.MyPreferencesFragment())
                .commit();
    }

}
