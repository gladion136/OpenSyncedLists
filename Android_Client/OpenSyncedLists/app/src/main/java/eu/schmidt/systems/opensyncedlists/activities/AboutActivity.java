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

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_DEFAULT;
import static eu.schmidt.systems.opensyncedlists.utils.PlayStore.askForPlayStoreReview;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.model.ReviewErrorCode;

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
        String version = "UNKOWN";
        try
        {
            PackageInfo pInfo = this.getPackageManager()
                .getPackageInfo(this.getPackageName(), 0);
            version = pInfo.versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.e("AboutActivity", "Error getting version name");
        }
        
        setContentView(R.layout.activity_about);
        setTitle(getString(R.string.menu_about));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView tv = findViewById(R.id.tVAbout);
        tv.setText(Html.fromHtml(getString(R.string.text_about)));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        
        TextView tvOthers = findViewById(R.id.tVOthers);
        tvOthers.setText(Html.fromHtml(getString(R.string.text_about_others)));
        tvOthers.setMovementMethod(LinkMovementMethod.getInstance());
        
        TextView tvVersion = findViewById(R.id.tVVersion);
        tvVersion.setText("Version: " + version);
    }
    
    /**
     * In onCreateOptionsMenu the menu from the ActionBar is inflated.
     *
     * @param menu Menu to inflate
     * @return true
     */
    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.about_menu, menu);
        return true;
    }
    
    /**
     * onOptionsItemSelected handles the events from the ActionBar.
     *
     * @param item selected item
     * @return action handled?
     */
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                // Back to ListsActivity
                onBackPressed();
                return true;
            case R.id.openInPlayStore:
                try
                {
                    Intent rateIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(String.format("%s?id=%s", "market://",
                            getPackageName())));
                    startActivity(rateIntent);
                }
                catch (ActivityNotFoundException e)
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://play.google.com/store/apps/details?id="
                            + getPackageName())));
                }
                return true;
            case R.id.contact_developer:
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL,
                    new String[]{getString(R.string.dev_mail_address)});
                intent.putExtra(Intent.EXTRA_SUBJECT,
                    getString(R.string.dev_mail_subject));
                if (intent.resolveActivity(getPackageManager()) != null)
                {
                    startActivity(intent);
                }
                else
                {
                    // Open play store feedback
                    Log.e(LOG_TITLE_DEFAULT, "No activity found to send mail");
                    Toast.makeText(this,
                        getString(R.string.no_app_for_intent_installed),
                        Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.openWebpage:
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.app_webpage))));
                return true;
            case R.id.showSourceCode:
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.sourcecode_webpage))));
                return true;
            case R.id.reviewOnPlayStore:
                askForPlayStoreReview(this);
                return true;
        }
        // Item not handled.. pass to super
        return super.onOptionsItemSelected(item);
    }
}

