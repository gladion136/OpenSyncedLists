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
package eu.schmidt.systems.opensyncedlists.storages;

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_DEFAULT;
import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_STORAGE;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.BuildConfig;
import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;

public class FileStorage {
    /**
     * File Provider Authority to allow access
     */
    public final static String FILE_PROVIDER_AUTHORITY =
            BuildConfig.APPLICATION_ID + ".fileprovider";

    /**
     * Export a list as JSON File
     *
     * @param context    Context
     * @param syncedList List to export
     * @return Filepath
     */
    public static String exportList(Context context, SyncedList syncedList) {
        File file =
                new File(context.getFilesDir(), syncedList.getName() + ".json");
        try {
            FileWriter fileWriter = new FileWriter(file, false);
            fileWriter.write(syncedList.toJsonWithHeader().toString());
            fileWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(LOG_TITLE_STORAGE, "File not found.");
        } catch (IOException e) {
            Log.e(LOG_TITLE_STORAGE, "Can't write file: " + e.toString());
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    /**
     * Export lists to JSON
     *
     * @param context     Context
     * @param syncedLists Lists to export
     * @return Filepath
     */
    public static String exportLists(Context context,
                                     ArrayList<SyncedList> syncedLists) {
        File file = new File(context.getFilesDir(), "lists_export.json");
        try {
            JSONArray jsonArray = new JSONArray();
            for (SyncedList list : syncedLists) {
                jsonArray.put(list.toJsonWithHeader());
            }
            FileWriter fileWriter = new FileWriter(file, false);
            fileWriter.write(jsonArray.toString());
            fileWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(LOG_TITLE_STORAGE, "File not found.");
        } catch (IOException e) {
            Log.e(LOG_TITLE_STORAGE, "Can't write file: " + e.toString());
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    /**
     * Share a File to another app.
     *
     * @param context     Context
     * @param absolutPath File to share
     */
    public static void shareFile(Context context, String absolutPath) {
        Uri path = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY,
                                              new File(absolutPath));
        Intent i = new Intent(Intent.ACTION_SEND);
        i.putExtra(Intent.EXTRA_STREAM, path);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.setType("plain/*");
        try {
            context.startActivity(i);
        } catch (ActivityNotFoundException exception) {
            Log.e(LOG_TITLE_DEFAULT,
                  "No activity found to receive " + "file: " + exception);
            Toast.makeText(context, context.getString(
                    R.string.no_app_for_intent_installed), Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
