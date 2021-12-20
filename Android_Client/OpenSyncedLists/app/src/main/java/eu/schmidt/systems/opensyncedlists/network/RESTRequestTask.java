package eu.schmidt.systems.opensyncedlists.network;

import android.net.Uri;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Handle a http request inside a AsyncTask
 */
class RESTRequestTask
        extends AsyncTask<HashMap<String, String>, Void, JSONObject> {

    private Exception exception;
    private Callback callback;

    public RESTRequestTask(Callback callback) {
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
                urlConnection
                        .setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");
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
                if (responseFromServer.getString("status").equals("ERROR")) {
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

    public interface Callback {
        void callback(JSONObject string, Exception exception);
    }

}

