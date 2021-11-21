package eu.schmidt.systems.opensyncedlists.utils;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;

/**
 * Handle server connection
 */
public abstract class ServerConnection {

    /**
     * Check connection to a server
     *
     * @param callback Callback
     */
    public static void check_connection(Callback callback) {
        new HandleRequestTask((jsonObject, exception) -> {
            callback.callback(jsonObject, exception);
        }).execute("http://192.168.0.204:3300/test");
    }

    public static void getList(String name, String secret, Callback callback) {

    }

    public static void setList(SyncedList syncedList, Callback callback) {

    }

    public static void removeList(String name,
                                  String secret,
                                  Callback callback) {

    }

    public static void addList(SyncedList syncedList, Callback callback) {

    }

    /**
     * Handle a http request inside a AsyncTask
     */
    static class HandleRequestTask extends AsyncTask<String, Void, JSONObject> {

        private Exception exception;
        private Callback callback;

        public HandleRequestTask(Callback callback) {
            this.callback = callback;
        }

        protected JSONObject doInBackground(String... urls) {
            try {
                HttpURLConnection urlConnection = null;
                java.net.URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(5000 /* milliseconds */);
                urlConnection.setConnectTimeout(5000 /* milliseconds */);
                urlConnection.setDoOutput(true);
                urlConnection.connect();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(url.openStream()));
                StringBuilder sb = new StringBuilder();

                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                String jsonString = sb.toString();
                return new JSONObject(jsonString);
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(JSONObject jsonObject) {
            this.callback.callback(jsonObject, this.exception);
        }
    }

    /**
     * Callback
     */
    public interface Callback {
        void callback(JSONObject jsonObject, Exception exception);
    }
}
