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
package eu.schmidt.systems.opensyncedlists.syncedlist;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

/**
 * Header for a SyncedList, contains all information about a list (included
 * secrets)
 */
public class SyncedListHeader
{
    private String id;
    private String name;
    private boolean checkOption;
    private boolean checkedList;
    private boolean overviewActive;
    private boolean autoSync;
    private boolean invertElement;
    private boolean jumpButtons;
    private String hostname;
    private byte[] secret;
    private SecretKey localSecret;
    private String listSize;
    
    private ArrayList<ListTag> tagList;
    
    /**
     * Load a header from JSON.
     *
     * @param jsonObject header as JSON
     * @throws JSONException
     */
    public SyncedListHeader(JSONObject jsonObject) throws JSONException
    {
        id = jsonObject.getString("id");
        name = jsonObject.getString("name");
        checkOption = jsonObject.getBoolean("checkOption");
        checkedList = jsonObject.getBoolean("checkedList");
        autoSync = jsonObject.getBoolean("autoSync");
        invertElement = jsonObject.getBoolean("invertElement");
        hostname = jsonObject.getString("hostname");
        listSize = jsonObject.getString("listSize");
        tagList = new ArrayList<>();
        
        ///  WARNING !! Add new properties only with checking for compatibility
        if (jsonObject.has("tags"))
        {
            for (int i = 0; i < jsonObject.getJSONArray("tags").length(); i++)
            {
                ListTag new_tag = new ListTag(
                    jsonObject.getJSONArray("tags").getJSONObject(i));
                if (tagList.stream()
                    .noneMatch(t -> t.name.equals(new_tag.name)))
                {
                    tagList.add(new_tag);
                }
            }
        }
        if (jsonObject.has("jumpButtons"))
        {
            jumpButtons = jsonObject.getBoolean("jumpButtons");
        }
        else
        {
            jumpButtons = true;
        }
        if (jsonObject.has("overviewActive"))
        {
            overviewActive = jsonObject.getBoolean("overviewActive");
        }
        else
        {
            overviewActive = false;
        }
        secret = Cryptography.stringToByteArray(jsonObject.getString("secret"));
        byte[] encodedKey =
            Cryptography.stringToByteArray(jsonObject.getString("localSecret"));
        localSecret =
            new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
    }
    
    /**
     * Create a SyncedListHeader
     *
     * @param id          id of the list
     * @param name        name of the list
     * @param secret      access secret of the list
     * @param localSecret localSecret to encrypt the list
     */
    public SyncedListHeader(String id, String name, String hostname,
        byte[] secret, SecretKey localSecret)
    {
        this.id = id;
        this.name = name;
        this.hostname = hostname;
        this.secret = secret;
        this.localSecret = localSecret;
        this.checkOption = true;
        this.checkedList = true;
        this.overviewActive = false;
        this.invertElement = false;
        this.autoSync = true;
        this.listSize = "0 / 0";
        this.tagList = new ArrayList<>();
    }
    
    public ArrayList<ListTag> getTagList()
    {
        return tagList;
    }
    
    public void setTagList(ArrayList<ListTag> tagList)
    {
        ArrayList<ListTag> uniqueTags = new ArrayList<>();
        for (ListTag tag : tagList)
        {
            if (!uniqueTags.contains(tag))
            {
                uniqueTags.add(tag);
            }
            else
            {
                Log.e("SyncedListHeader", "Duplicate tag found: " + tag.name);
            }
        }
        this.tagList = uniqueTags;
    }
    
    public String getId()
    {
        return id;
    }
    
    public void setId(String id)
    {
        this.id = id;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public boolean isCheckOption()
    {
        return checkOption;
    }
    
    public void setCheckOption(boolean checkOption)
    {
        this.checkOption = checkOption;
    }
    
    public boolean isJumpButtons()
    {
        return jumpButtons;
    }
    
    public void setJumpButtons(boolean jumpButtons)
    {
        this.jumpButtons = jumpButtons;
    }
    
    public boolean isCheckedList()
    {
        return checkOption && checkedList;
    }
    
    public void setCheckedList(boolean checkedList)
    {
        this.checkedList = checkedList;
    }
    
    public boolean isAutoSync()
    {
        return autoSync;
    }
    
    public void setAutoSync(boolean autoSync)
    {
        this.autoSync = autoSync;
    }
    
    public String getHostname()
    {
        return hostname;
    }
    
    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }
    
    public byte[] getSecret()
    {
        return secret;
    }
    
    public void setSecret(byte[] secret)
    {
        this.secret = secret;
    }
    
    public SecretKey getLocalSecret()
    {
        return localSecret;
    }
    
    public void setLocalSecret(SecretKey localSecret)
    {
        this.localSecret = localSecret;
    }
    
    public boolean isInvertElement()
    {
        return invertElement;
    }
    
    public void setInvertElement(boolean invertElement)
    {
        this.invertElement = invertElement;
    }
    
    public String getListSize()
    {
        return listSize;
    }
    
    public void setListSize(String listSize)
    {
        this.listSize = listSize;
    }
    
    public boolean isOverviewActive()
    {
        return overviewActive;
    }
    
    public void setOverviewActive(boolean overviewActive)
    {
        this.overviewActive = overviewActive;
    }
    
    /**
     * Get the SyncedListHeader as JSON
     *
     * @return SyncedListHeader as JSON
     */
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        try
        {
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            jsonObject.put("hostname", hostname);
            jsonObject.put("overviewActive", overviewActive);
            jsonObject.put("checkOption", checkOption);
            jsonObject.put("checkedList", checkedList);
            jsonObject.put("autoSync", autoSync);
            jsonObject.put("invertElement", invertElement);
            jsonObject.put("listSize", listSize);
            jsonObject.put("jumpButtons", jumpButtons);
            jsonObject.put("secret", Cryptography.byteArrayToString(secret));
            jsonObject.put("localSecret",
                Cryptography.byteArrayToString(localSecret.getEncoded()));
            
            JSONArray tagArray = new JSONArray();
            for (ListTag tag : tagList)
            {
                tagArray.put(tag.toJSON());
            }
            if (tagList.size() == 0)
            {
                jsonObject.put("tags", new JSONArray());
            } else {
                jsonObject.put("tags", tagArray);
            }
        }
        catch (JSONException exception)
        {
            exception.printStackTrace();
        }
        return jsonObject;
    }
}
