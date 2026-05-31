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
package eu.schmidt.systems.opensyncedlists.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import androidx.preference.PreferenceManager;

import eu.schmidt.systems.opensyncedlists.network.ServerWrapper;
import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;

/**
 * Bundles the destructive data/storage operations offered under
 * App settings &gt; Data / Storage:
 *
 * <ul>
 *   <li>reset all local lists,</li>
 *   <li>reset all app settings,</li>
 *   <li>delete all lists from their servers,</li>
 *   <li>reset everything (like Android's "Clear data").</li>
 * </ul>
 */
public class DataStorageManager
{
    private static final String LOG = "DataStorageManager";

    private final Context context;
    private final SecureStorage secureStorage;

    public DataStorageManager(Context context)
    {
        this.context = context.getApplicationContext();
        this.secureStorage = new SecureStorage(this.context);
    }

    /**
     * @return number of locally stored lists (for confirmation dialogs).
     */
    public int countLists()
    {
        try
        {
            return secureStorage.getListsIds().size();
        }
        catch (Exception e)
        {
            Log.e(LOG, "countLists failed: " + e);
            return 0;
        }
    }

    /**
     * Delete every locally stored list. App settings are kept.
     *
     * @return true on success
     */
    public boolean resetAllLists()
    {
        try
        {
            secureStorage.deleteAllLists();
            return true;
        }
        catch (Exception e)
        {
            Log.e(LOG, "resetAllLists failed: " + e);
            return false;
        }
    }

    /**
     * Reset all global app settings to their defaults. Lists are kept.
     */
    public void resetAllSettings()
    {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear()
            .apply();
    }

    /**
     * Ask every list's server to delete the server-side copy, then detach the
     * lists locally (clear hostname, disable auto-sync). Local lists are kept.
     *
     * @return number of lists that had a server and were processed
     */
    public int deleteAllListsOnServer()
    {
        int processed = 0;
        try
        {
            for (String id : secureStorage.getListsIds())
            {
                SyncedList list = secureStorage.getList(id);
                String hostname = list.getHeader().getHostname();
                if (hostname == null || hostname.equals(""))
                {
                    continue;
                }
                ServerWrapper.removeList(hostname, id, list.getSecret(),
                    (jsonResult, exception) ->
                    {
                        if (exception != null)
                        {
                            Log.e(LOG, "Server removal failed for " + id + ": "
                                + exception);
                        }
                    });
                // Detach locally regardless of the (asynchronous) server
                // response so no stale server URL remains.
                try
                {
                    list.getHeader().setHostname("");
                    list.getHeader().setAutoSync(false);
                    secureStorage.setList(list);
                }
                catch (Exception e)
                {
                    Log.e(LOG, "Local detach failed for " + id + ": " + e);
                }
                processed++;
            }
        }
        catch (Exception e)
        {
            Log.e(LOG, "deleteAllListsOnServer failed: " + e);
        }
        return processed;
    }

    /**
     * Wipe ALL app data (lists, settings, files) like the system's
     * "Clear data" action. This also stops and restarts the app process.
     *
     * @return true if the wipe was triggered
     */
    public boolean resetEverything()
    {
        ActivityManager am =
            (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        if (am != null)
        {
            return am.clearApplicationUserData();
        }
        // Fallback: clear what we can manually.
        resetAllSettings();
        resetAllLists();
        return false;
    }
}
