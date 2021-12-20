package eu.schmidt.systems.opensyncedlists.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import eu.schmidt.systems.opensyncedlists.R;

public class AboutActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle(getString(R.string.menu_about));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}