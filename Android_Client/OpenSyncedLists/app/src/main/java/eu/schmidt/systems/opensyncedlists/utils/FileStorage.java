package eu.schmidt.systems.opensyncedlists.utils;

import static eu.schmidt.systems.opensyncedlists.utils.Constant.FILE_PROVIDER_AUTHORITY;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;

public class FileStorage {

    public static String exportList(Context context, SyncedList syncedList) {
        File file =
                new File(context.getFilesDir(), syncedList.getName() + ".json");
        try {
            FileWriter fileWriter = new FileWriter(file, false);
            fileWriter.write(syncedList.toJSONwithHeader().toString());
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

    public static String exportLists(Context context,
                                     ArrayList<SyncedList> syncedLists) {
        File file = new File(context.getFilesDir(), "lists_export.json");
        try {
            JSONArray jsonArray = new JSONArray();
            for (SyncedList list : syncedLists) {
                jsonArray.put(list.toJSONwithHeader());
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
            Toast.makeText(context, context.getString(R.string.no_app_for_intent_installed),
                           Toast.LENGTH_SHORT).show();
        }
    }
}
