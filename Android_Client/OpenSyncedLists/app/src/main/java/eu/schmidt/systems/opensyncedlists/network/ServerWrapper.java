package eu.schmidt.systems.opensyncedlists.network;

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_NETWORK;

import android.util.Log;

import java.util.HashMap;

import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

/**
 * Handle server connection
 */
public abstract class ServerWrapper {

    /**
     * Check connection to a server
     *
     * @param callback Callback
     */
    public static void checkConnection(String hostname, RESTRequestTask.Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", hostname);
        info.put("path", "/test");
        info.put("type", "GET");

        Log.d(LOG_TITLE_NETWORK, "Send Request: checkConnection to " + info.get(
                "hostname"));
        new RESTRequestTask(callback).execute(info);
    }

    public static void getList(String hostname,
                               String id,
                               String secret,
                               RESTRequestTask.Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", hostname);
        info.put("path", "/list/get");
        info.put("type", "GET");

        HashMap<String, String> query = new HashMap<>();
        query.put("id", id);
        query.put("secret", secret);

        Log.d(LOG_TITLE_NETWORK, "Send Request: getList to " + info.get(
                "hostname"));
        new RESTRequestTask(callback).execute(info, query);
    }

    public static void setList(SyncedList syncedList,
                               String basedOnHash,
                               RESTRequestTask.Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", syncedList.getHeader().getHostname());
        info.put("path", "/list/set");
        info.put("type", "POST");

        HashMap<String, String> query = new HashMap<>();
        query.put("id", syncedList.getId());
        query.put("secret", syncedList.getSecret());

        HashMap<String, String> data = new HashMap<>();
        String fullListEncrypted = syncedList.getFullListEncrypted();
        data.put("data", fullListEncrypted);
        data.put("hash", Cryptography.getSHAasString(fullListEncrypted));
        // Only success if basedOnHash equals hash inside database
        data.put("basedOnHash", basedOnHash);

        Log.d(LOG_TITLE_NETWORK, "Send Request: setList to " + info.get(
                "hostname"));
        new RESTRequestTask(callback).execute(info, query, data);
    }

    public static void removeList(String hostname, String id, String secret,
                                  RESTRequestTask.Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", hostname);
        info.put("path", "/list/remove");
        info.put("type", "GET");

        HashMap<String, String> query = new HashMap<>();
        query.put("id", id);
        query.put("secret", secret);

        Log.d(LOG_TITLE_NETWORK, "Send Request: removeList to " + info.get(
                "hostname"));
        new RESTRequestTask(callback).execute(info, query);
    }

    public static void addList(SyncedList syncedList, RESTRequestTask.Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", syncedList.getHeader().getHostname());
        info.put("path", "/list/add");
        info.put("type", "POST");

        HashMap<String, String> query = new HashMap<>();
        query.put("id", syncedList.getId());
        query.put("secret", syncedList.getSecret());

        HashMap<String, String> data = new HashMap<>();
        String fullListEncrypted = syncedList.getFullListEncrypted();
        data.put("data", fullListEncrypted);
        data.put("hash", Cryptography.getSHAasString(fullListEncrypted));

        Log.d(LOG_TITLE_NETWORK, "Send Request: addList to " + info.get(
                "hostname"));
        new RESTRequestTask(callback).execute(info, query, data);
    }

}