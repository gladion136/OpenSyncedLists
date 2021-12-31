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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * One change Step for a SyncedList
 */
public class SyncedListStep
{
    long timestamp;
    String changeId = "";
    ACTION changeAction;
    Integer changeValueInt;
    String changeValueString = null;
    SyncedListElement changeValueElement = null;
    
    public SyncedListStep(JSONObject jsonObject) throws JSONException
    {
        this.changeAction = ACTION.values()[jsonObject.getInt("changeAction")];
        this.timestamp = jsonObject.getLong("timestamp");
        if (jsonObject.has("changeId"))
        {
            this.changeId = jsonObject.getString("changeId");
        }
        if (jsonObject.has("changeValueElement"))
        {
            this.changeValueElement = new SyncedListElement(
                jsonObject.getJSONObject("changeValueElement"));
        }
        if (jsonObject.has("changeValueString"))
        {
            this.changeValueString = jsonObject.getString("changeValueString");
        }
        if (jsonObject.has("changeValueInt"))
        {
            this.changeValueInt = jsonObject.getInt("changeValueInt");
        }
    }
    
    public SyncedListStep(String changeId, ACTION changeAction)
    {
        this.timestamp = System.currentTimeMillis();
        this.changeId = changeId;
        this.changeAction = changeAction;
    }
    
    public SyncedListStep(String changeId, ACTION changeAction,
        SyncedListElement changeValueElement)
    {
        this.timestamp = System.currentTimeMillis();
        this.changeId = changeId;
        this.changeAction = changeAction;
        this.changeValueElement = changeValueElement;
    }
    
    public SyncedListStep(String changeId, ACTION changeAction,
        Integer changeValueInt)
    {
        this.timestamp = System.currentTimeMillis();
        this.changeId = changeId;
        this.changeAction = changeAction;
        this.changeValueInt = changeValueInt;
    }
    
    public SyncedListStep(String changeId, ACTION changeAction,
        String changeValueString)
    {
        this.timestamp = System.currentTimeMillis();
        this.changeId = changeId;
        this.changeAction = changeAction;
        this.changeValueString = changeValueString;
    }
    
    public String getChangeId()
    {
        return changeId;
    }
    
    public ACTION getChangeAction()
    {
        return changeAction;
    }
    
    public long getTimestamp()
    {
        return timestamp;
    }
    
    public Integer getChangeValueInt()
    {
        return changeValueInt;
    }
    
    public String getChangeValueString()
    {
        return changeValueString;
    }
    
    public SyncedListElement getChangeValueElement()
    {
        return changeValueElement;
    }
    
    /**
     * Get SyncedListStep as JSON
     *
     * @return JSON of SyncedListStep
     */
    public JSONObject toJSON()
    {
        JSONObject jsonObject = new JSONObject();
        try
        {
            jsonObject.put("timestamp", this.timestamp);
            jsonObject.put("changeAction", this.changeAction.ordinal());
            if (this.changeId != null)
            {
                jsonObject.put("changeId", this.changeId);
            }
            if (this.changeValueInt != null)
            {
                jsonObject.put("changeValueInt", this.changeValueInt);
            }
            if (this.changeValueElement != null)
            {
                jsonObject.put("changeValueElement",
                    this.changeValueElement.toJSON());
            }
        }
        catch (JSONException exception)
        {
            exception.printStackTrace();
        }
        return jsonObject;
    }
    
    /**
     * Test if step is equal to another SyncedListStep
     *
     * @param step equal to this step?
     * @return equal?
     */
    public boolean equals(SyncedListStep step)
    {
        if (getChangeId() == null && step.getChangeId() != null)
        {
            return false;
        }
        if (getChangeId() != null && step.getChangeId() == null)
        {
            return false;
        }
        if (getChangeId() != null && step.getChangeId() != null
            && !getChangeId().equals(step.getChangeId()))
        {
            return false;
        }
        if (getChangeAction() != step.getChangeAction())
        {
            return false;
        }
        if (timestamp != step.getTimestamp())
        {
            return false;
        }
        if (!Objects.equals(this.changeValueInt, step.changeValueInt))
        {
            return false;
        }
        if (this.changeValueString != null)
        {
            if (step.changeValueString != null)
            {
                if (!this.changeValueString.equals(step.changeValueString))
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        if (this.changeValueElement != null)
        {
            if (step.changeValueElement != null)
            {
                return this.changeValueElement.toJSON().toString()
                    .equals(step.changeValueElement.toJSON().toString());
            }
            else
            {
                return false;
            }
        }
        return true;
    }
}
