package eu.schmidt.systems.opensyncedlists;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import org.json.JSONException;
import org.json.JSONObject;

import eu.schmidt.systems.opensyncedlists.datatypes.ACTION;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.fragments.ListSettingsFragment;

public class ListSettingsActivity extends AppCompatActivity {

    ListSettingsFragment listSettingsFragment;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        // Get list header from extras and pass to fragment
        listSettingsFragment = new ListSettingsFragment();
        listSettingsFragment.setArguments(getIntent().getExtras());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings, listSettingsFragment).commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed() {
        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtras(getIntent().getExtras());
        startActivity(intent);
        finish();
    }
}

