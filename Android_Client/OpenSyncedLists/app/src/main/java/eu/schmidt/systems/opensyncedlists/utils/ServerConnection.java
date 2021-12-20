package eu.schmidt.systems.opensyncedlists.utils;

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_NETWORK;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.exceptions.ServerException;

/**
 * Handle server connection
 */
public abstract class ServerConnection {

    /**
     * Check connection to a server
     *
     * @param callback Callback
     */
    public static void checkConnection(String hostname, Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", hostname);
        info.put("path", "/test");
        info.put("type", "GET");

        Log.d(LOG_TITLE_NETWORK, "Send Request: checkConnection to " + info.get(
                "hostname"));
        new HandleRequestTask((jsonObject, exception) -> {
            callback.callback(jsonObject, exception);
        }).execute(info);
    }

    public static void getList(String hostname,
                               String id,
                               String secret,
                               Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", hostname);
        info.put("path", "/list/get");
        info.put("type", "GET");

        HashMap<String, String> query = new HashMap<>();
        query.put("id", id);
        query.put("secret", secret);

        Log.d(LOG_TITLE_NETWORK, "Send Request: getList to " + info.get(
                "hostname"));
        new HandleRequestTask((jsonObject, exception) -> {
            callback.callback(jsonObject, exception);
        }).execute(info, query);
    }

    public static void setList(SyncedList syncedList,
                               String basedOnHash,
                               Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", syncedList.getHeader().getHostname());
        info.put("path", "/list/set");
        info.put("type", "POST");

        HashMap<String, String> query = new HashMap<>();
        query.put("id", syncedList.getId());
        query.put("secret", syncedList.getSecret().toString());

        HashMap<String, String> data = new HashMap<>();
        String fullListEncrypted = syncedList.getFullListEncrypted();
        data.put("data", fullListEncrypted);
        data.put("hash", Cryptography.getSHAasString(fullListEncrypted));
        // Only success if basedOnHash equals hash inside database
        data.put("basedOnHash", basedOnHash);

        Log.d(LOG_TITLE_NETWORK, "Send Request: setList to " + info.get(
                "hostname"));
        new HandleRequestTask((jsonObject, exception) -> {
            callback.callback(jsonObject, exception);
        }).execute(info, query, data);
    }

    public static void removeList(String hostname, String id, String secret,
                                  Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", hostname);
        info.put("path", "/list/remove");
        info.put("type", "GET");

        HashMap<String, String> query = new HashMap<>();
        query.put("id", id);
        query.put("secret", secret);

        Log.d(LOG_TITLE_NETWORK, "Send Request: removeList to " + info.get(
                "hostname"));
        new HandleRequestTask((jsonObject, exception) -> {
            callback.callback(jsonObject, exception);
        }).execute(info, query);
    }

    public static void addList(SyncedList syncedList, Callback callback) {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", syncedList.getHeader().getHostname());
        info.put("path", "/list/add");
        info.put("type", "POST");

        HashMap<String, String> query = new HashMap<>();
        query.put("id", syncedList.getId());
        query.put("secret", syncedList.getSecret().toString());

        HashMap<String, String> data = new HashMap<>();
        String fullListEncrypted = syncedList.getFullListEncrypted();
        data.put("data", fullListEncrypted);
        data.put("hash", Cryptography.getSHAasString(fullListEncrypted));

        Log.d(LOG_TITLE_NETWORK, "Send Request: addList to " + info.get(
                "hostname"));
        new HandleRequestTask((jsonObject, exception) -> {
            callback.callback(jsonObject, exception);
        }).execute(info, query, data);
    }

    /**
     * Handle a http request inside a AsyncTask
     */
    static class HandleRequestTask
            extends AsyncTask<HashMap<String, String>, Void, JSONObject> {

        private Exception exception;
        private Callback callback;

        public HandleRequestTask(Callback callback) {
            this.callback = callback;
        }

        protected JSONObject doInBackground(HashMap<String, String>... param) {
            try {
                HashMap<String, String> info = param[0];
                String hostname = info.get("hostname");
                String path = info.get("path");
                String type = info.get("type");
                String[] splitHost = hostname.split("://");
                String protocoll = splitHost[0];
                hostname = splitHost[1];

                HttpURLConnection urlConnection = null;
                Uri.Builder uriBuilder = new Uri.Builder().scheme(protocoll)
                        .encodedAuthority(hostname).path(path);
                if (param.length > 1) {
                    HashMap<String, String> query = param[1];
                    for (String key : query.keySet()) {
                        uriBuilder.appendQueryParameter(key, query.get(key));
                    }
                }

                java.net.URL url = new URL(uriBuilder.build().toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(type);
                urlConnection.setReadTimeout(5000 /* milliseconds */);
                urlConnection.setConnectTimeout(5000 /* milliseconds */);

                if (param.length > 2) {
                    urlConnection.setRequestProperty("Content-Type",
                                                     "application/json");
                    urlConnection
                            .setRequestProperty("Accept", "application/json");
                    urlConnection.setDoOutput(true);

                    HashMap<String, String> data = param[2];
                    byte[] body = fromMap(data).toString()
                            .getBytes(StandardCharsets.UTF_8);

                    try (OutputStream os = urlConnection.getOutputStream()) {
                        os.write(body, 0, body.length);
                    }
                }

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream(),
                                              "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JSONObject responseFromServer =
                            new JSONObject(response.toString());
                    if (responseFromServer.getString("status")
                            .equals("ERROR")) {
                        throw new ServerException(
                                responseFromServer.getString("msg"));
                    }
                    return responseFromServer;
                }
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(JSONObject string) {
            this.callback.callback(string, this.exception);
        }
    }

    /**
     * Callback
     */
    public interface Callback {
        void callback(JSONObject string, Exception exception);
    }

    private static JSONObject fromMap(HashMap<String, String> map) {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            try {
                jsonObject.put(entry.getKey(), entry.getValue());
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
        }
        return jsonObject;
    }
}