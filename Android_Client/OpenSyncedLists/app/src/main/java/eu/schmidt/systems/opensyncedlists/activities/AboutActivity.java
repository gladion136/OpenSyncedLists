/*
 * Copyright (C) 2021  Etienne Schmidt (eschmidt@schmidt-ti.eu)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package eu.schmidt.systems.opensyncedlists.activities;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import eu.schmidt.systems.opensyncedlists.BuildConfig;
import eu.schmidt.systems.opensyncedlists.R;

/**
 * Activity for showing things about the app like author, licence, version.
 */
public class AboutActivity extends AppCompatActivity
{
    
    /**
     * In onCreate the layout is set and the associated textViews got filled
     * with content.
     *
     * @param savedInstanceState In this case just used for the super call.
     */
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle(getString(R.string.menu_about));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView tv = findViewById(R.id.tVAbout);
        tv.setText(Html.fromHtml(getString(R.string.text_about)));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        
        TextView tvVersion = findViewById(R.id.tVVersion);
        tvVersion.setText("Version: " + BuildConfig.VERSION_NAME);
    }
}