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

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.crypto.SecretKey;

import eu.schmidt.systems.opensyncedlists.syncedlist.ACTION;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

/**
 * Integration test using real app logic (SyncedList, Cryptography) with
 * synchronous HTTP calls (no AsyncTask). Pass server URL via:
 * -Dserver.url=https://...
 */
@RunWith(RobolectricTestRunner.class)
public class ServerConnectionTest {

    private static final String DEFAULT_URL =
        "https://opensyncedlists.schmidt-systems.eu";
    private String serverUrl;

    @Before
    public void setUp() {
        serverUrl = System.getProperty("server.url", DEFAULT_URL);
        System.out.println("Testing server: " + serverUrl);
    }

    // ---- Synchronous HTTP helpers (mirrors RESTRequestTask logic) -----------

    private JSONObject httpGet(String path) throws Exception {
        HttpURLConnection conn =
            (HttpURLConnection) URI.create(serverUrl + path).toURL()
                .openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return readResponse(conn);
    }

    private JSONObject httpPost(String path, String query,
        HashMap<String, String> data) throws Exception {
        String url = serverUrl + path + (query != null ? "?" + query : "");
        HttpURLConnection conn =
            (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        JSONObject body = new JSONObject();
        for (String key : data.keySet()) body.put(key, data.get(key));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    private JSONObject readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
            code < 400 ? conn.getInputStream() : conn.getErrorStream(),
            StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line.trim());
        JSONObject json = new JSONObject(sb.toString());
        System.out.println("  HTTP " + code + " -> " + json);
        return json;
    }

    // ---- App-logic helpers --------------------------------------------------

    /** Mirrors addList: encrypts full list and POSTs to /list/add */
    private JSONObject addList(SyncedList list) throws Exception {
        String encrypted = list.getFullListEncrypted();
        HashMap<String, String> data = new HashMap<>();
        data.put("data", encrypted);
        data.put("hash", Cryptography.getSHAasString(encrypted));
        return httpPost("/list/add",
            "id=" + list.getId() + "&secret=" + list.getSecret(), data);
    }

    /** Mirrors getList: GETs /list/get and returns raw JSON */
    private JSONObject getList(SyncedList list) throws Exception {
        return httpGet("/list/get?id=" + list.getId()
            + "&secret=" + list.getSecret());
    }

    /** Mirrors setList: encrypts and POSTs to /list/set */
    private JSONObject setList(SyncedList list, String basedOnHash)
        throws Exception {
        String encrypted = list.getFullListEncrypted();
        HashMap<String, String> data = new HashMap<>();
        data.put("data", encrypted);
        data.put("hash", Cryptography.getSHAasString(encrypted));
        data.put("basedOnHash", basedOnHash);
        return httpPost("/list/set",
            "id=" + list.getId() + "&secret=" + list.getSecret(), data);
    }

    /** Mirrors removeList */
    private JSONObject removeList(SyncedList list) throws Exception {
        return httpGet(
            "/list/remove?id=" + list.getId() + "&secret=" + list.getSecret());
    }

    /**
     * Full sync cycle: GET -> decrypt -> merge -> SET.
     * Mirrors syncWithHost + syncAndUpdateListOnServer.
     */
    private SyncedList syncWithServer(SyncedList local) throws Exception {
        System.out.println("  [sync] getList");
        JSONObject getJson = getList(local);
        Assert.assertEquals("getList status", "OK",
            getJson.getString("status"));

        String encryptedData = getJson.getJSONObject("msg").getString("data");
        String receivedHash = Cryptography.getSHAasString(encryptedData);

        String decrypted =
            Cryptography.decryptRSA(local.getHeader().getLocalSecret(),
                encryptedData);
        System.out.println("  [sync] decrypted length: " + decrypted.length());
        Assert.assertFalse("Decrypted data should not be empty",
            decrypted.isEmpty());

        SyncedList received = new SyncedList(new JSONObject(decrypted));
        SyncedList merged = SyncedList.sync(local, received);
        System.out.println("  [sync] merged elements: "
            + merged.getElements().size());

        System.out.println("  [sync] setList");
        JSONObject setJson = setList(merged, receivedHash);
        Assert.assertEquals("setList status", "OK",
            setJson.getString("status"));

        local.sync(merged);
        return local;
    }

    // ---- Tests --------------------------------------------------------------

    @Test
    public void testCheckConnection() throws Exception {
        System.out.println("=== testCheckConnection ===");
        JSONObject result = httpGet("/test");
        Assert.assertEquals("OK", result.getString("status"));
    }

    @Test
    public void testFullSyncCycle() throws Exception {
        System.out.println("=== testFullSyncCycle ===");

        SecretKey localSecret = Cryptography.generateAESKey();
        byte[] accessSecret =
            Cryptography.generatingRandomString(32).getBytes("UTF-8");
        String listId = "junit-sync-" + System.currentTimeMillis();

        SyncedListHeader header = new SyncedListHeader(listId,
            "JUnit Test List", serverUrl, accessSecret, localSecret);
        SyncedList list =
            new SyncedList(header, new JSONObject("{\"steps\":[]}"));

        System.out.println("-- addList --");
        JSONObject addJson = addList(list);
        Assert.assertEquals("addList status", "OK",
            addJson.getString("status"));

        System.out.println("-- sync --");
        list = syncWithServer(list);
        Assert.assertEquals("ID preserved", listId, list.getHeader().getId());

        System.out.println("-- removeList --");
        removeList(list);
    }

    @Test
    public void testTwoDeviceSync() throws Exception {
        System.out.println("=== testTwoDeviceSync ===");

        SecretKey localSecret = Cryptography.generateAESKey();
        byte[] accessSecret =
            Cryptography.generatingRandomString(32).getBytes("UTF-8");
        String listId = "junit-twosync-" + System.currentTimeMillis();

        // Device A: create list with one item and upload
        System.out.println("-- Device A: create & upload --");
        SyncedListHeader headerA = new SyncedListHeader(listId,
            "Two-Device Sync Test", serverUrl, accessSecret, localSecret);
        SyncedList deviceA =
            new SyncedList(headerA, new JSONObject("{\"steps\":[]}"));
        deviceA.addElementStep(new SyncedListStep("item-a", ACTION.ADD,
            new SyncedListElement("item-a", "Item from Device A", "")));
        System.out.println("  Device A elements: "
            + deviceA.getElements().size());

        JSONObject addJson = addList(deviceA);
        Assert.assertEquals("addList status", "OK",
            addJson.getString("status"));

        // Device B: fresh instance, sync to get A's item, add own item, push
        System.out.println("-- Device B: sync & add item --");
        SyncedListHeader headerB = new SyncedListHeader(listId,
            "Two-Device Sync Test", serverUrl, accessSecret, localSecret);
        SyncedList deviceB =
            new SyncedList(headerB, new JSONObject("{\"steps\":[]}"));
        deviceB = syncWithServer(deviceB);
        System.out.println(
            "  Device B elements after first sync: "
                + deviceB.getElements().size());

        deviceB.addElementStep(new SyncedListStep("item-b", ACTION.ADD,
            new SyncedListElement("item-b", "Item from Device B", "")));
        deviceB = syncWithServer(deviceB);
        System.out.println(
            "  Device B elements after push: " + deviceB.getElements().size());
        Assert.assertEquals("Device B should have 2 items", 2,
            deviceB.getElements().size());

        // Device A: sync to get B's item
        System.out.println("-- Device A: sync to get Device B's item --");
        deviceA = syncWithServer(deviceA);
        System.out.println("  Device A elements after sync: "
            + deviceA.getElements().size());
        Assert.assertEquals("Device A should have 2 items", 2,
            deviceA.getElements().size());

        boolean hasA = deviceA.getElements().stream()
            .anyMatch(e -> e.getName().equals("Item from Device A"));
        boolean hasB = deviceA.getElements().stream()
            .anyMatch(e -> e.getName().equals("Item from Device B"));
        Assert.assertTrue("Device A missing own item", hasA);
        Assert.assertTrue("Device A missing Device B's item", hasB);

        System.out.println("  Final elements on Device A:");
        deviceA.getElements()
            .forEach(e -> System.out.println("    - " + e.getName()));

        System.out.println("-- removeList --");
        removeList(deviceA);
    }
}
