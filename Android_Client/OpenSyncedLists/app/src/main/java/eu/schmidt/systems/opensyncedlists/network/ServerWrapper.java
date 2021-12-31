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

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_NETWORK;

import android.util.Log;

import java.util.HashMap;

import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

/**
 * ServerWrapper to handle requests.
 */
public abstract class ServerWrapper
{
    
    /**
     * Check connection to a server.
     *
     * @param callback Callback for Request
     */
    public static void checkConnection(String hostname,
        RESTRequestTask.Callback callback)
    {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", hostname);
        info.put("path", "/test");
        info.put("type", "GET");
        
        Log.d(LOG_TITLE_NETWORK,
            "Send Request: checkConnection to " + info.get("hostname"));
        new RESTRequestTask(callback).execute(info);
    }
    
    /**
     * Get a syncedList from a server.
     *
     * @param hostname Server hostname with protocol and port
     * @param id       id of the list
     * @param secret   secret to access
     * @param callback Callback for Request
     */
    public static void getList(String hostname, String id, String secret,
        RESTRequestTask.Callback callback)
    {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", hostname);
        info.put("path", "/list/get");
        info.put("type", "GET");
        
        HashMap<String, String> query = new HashMap<>();
        query.put("id", id);
        query.put("secret", secret);
        
        Log.d(LOG_TITLE_NETWORK,
            "Send Request: getList to " + info.get("hostname"));
        new RESTRequestTask(callback).execute(info, query);
    }
    
    /**
     * Override a list on a server.
     *
     * @param syncedList  new List (contains server info)
     * @param basedOnHash List on server must match to this Hash
     * @param callback    Callback for Request
     */
    public static void setList(SyncedList syncedList, String basedOnHash,
        RESTRequestTask.Callback callback)
    {
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
        
        Log.d(LOG_TITLE_NETWORK,
            "Send Request: setList to " + info.get("hostname"));
        new RESTRequestTask(callback).execute(info, query, data);
    }
    
    /**
     * Remove a list from the server.
     *
     * @param hostname Server hostname with protocol and port
     * @param id       id of the list
     * @param secret   access secret from the list
     * @param callback Callback for Request
     */
    public static void removeList(String hostname, String id, String secret,
        RESTRequestTask.Callback callback)
    {
        HashMap<String, String> info = new HashMap<>();
        info.put("hostname", hostname);
        info.put("path", "/list/remove");
        info.put("type", "GET");
        
        HashMap<String, String> query = new HashMap<>();
        query.put("id", id);
        query.put("secret", secret);
        
        Log.d(LOG_TITLE_NETWORK,
            "Send Request: removeList to " + info.get("hostname"));
        new RESTRequestTask(callback).execute(info, query);
    }
    
    /**
     * Add a list to a server
     *
     * @param syncedList List to add (contains server info)
     * @param callback   Callback for Request
     */
    public static void addList(SyncedList syncedList,
        RESTRequestTask.Callback callback)
    {
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
        
        Log.d(LOG_TITLE_NETWORK,
            "Send Request: addList to " + info.get("hostname"));
        new RESTRequestTask(callback).execute(info, query, data);
    }
}