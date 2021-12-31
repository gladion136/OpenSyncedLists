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
 * A AsyncTask to handle the Requests to a REST API.
 */
class RESTRequestTask
    extends AsyncTask<HashMap<String, String>, Void, JSONObject>
{
    
    private Exception exception;
    private final Callback callback;
    
    /**
     * Initialize Variables
     *
     * @param callback Callback to call after execution.
     */
    public RESTRequestTask(Callback callback)
    {
        this.callback = callback;
    }
    
    /**
     * Execute a REST Request.
     *
     * @param param Parameters for the Request 1-3 HashMaps 0: Header Infos
     *              about the request 1: Query parameters 2: Body data for POST
     *              Requests
     * @return JSON result
     */
    @SafeVarargs protected final JSONObject doInBackground(
        HashMap<String, String>... param)
    {
        try
        {
            HashMap<String, String> info = param[0];
            String hostname = info.get("hostname");
            String path = info.get("path");
            String type = info.get("type");
            String[] splitHost = hostname.split("://");
            String protocol = splitHost[0];
            hostname = splitHost[1];
            
            HttpURLConnection urlConnection;
            Uri.Builder uriBuilder =
                new Uri.Builder().scheme(protocol).encodedAuthority(hostname)
                    .path(path);
            if (param.length > 1)
            {
                HashMap<String, String> query = param[1];
                for (String key : query.keySet())
                {
                    uriBuilder.appendQueryParameter(key, query.get(key));
                }
            }
            
            java.net.URL url = new URL(uriBuilder.build().toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(type);
            urlConnection.setReadTimeout(5000 /* milliseconds */);
            urlConnection.setConnectTimeout(5000 /* milliseconds */);
            
            if (param.length > 2)
            {
                urlConnection
                    .setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setDoOutput(true);
                
                HashMap<String, String> data = param[2];
                byte[] body =
                    fromMap(data).toString().getBytes(StandardCharsets.UTF_8);
                
                try (OutputStream os = urlConnection.getOutputStream())
                {
                    os.write(body, 0, body.length);
                }
            }
            
            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(urlConnection.getInputStream(),
                    StandardCharsets.UTF_8)))
            {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null)
                {
                    response.append(responseLine.trim());
                }
                
                JSONObject responseFromServer =
                    new JSONObject(response.toString());
                if (responseFromServer.getString("status").equals("ERROR"))
                {
                    throw new ServerException(
                        responseFromServer.getString("msg"));
                }
                return responseFromServer;
            }
        }
        catch (Exception e)
        {
            this.exception = e;
            return null;
        }
    }
    
    /**
     * After execute, call the callback
     *
     * @param result JSON result
     */
    protected void onPostExecute(JSONObject result)
    {
        this.callback.callback(result, this.exception);
    }
    
    /**
     * Convert a HashMap to JSON
     *
     * @param map Map to convert
     * @return Map as JSON
     */
    private static JSONObject fromMap(HashMap<String, String> map)
    {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            try
            {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            catch (JSONException exception)
            {
                exception.printStackTrace();
            }
        }
        return jsonObject;
    }
    
    /**
     * Interface to handle result from Request.
     */
    public interface Callback
    {
        void callback(JSONObject string, Exception exception);
    }
}

