package eu.schmidt.systems.opensyncedlists;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;

public class AboutActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle(getString(R.string.menu_about));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}