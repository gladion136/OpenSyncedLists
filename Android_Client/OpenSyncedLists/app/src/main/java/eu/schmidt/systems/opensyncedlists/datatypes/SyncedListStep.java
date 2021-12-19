package eu.schmidt.systems.opensyncedlists.datatypes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One changes Step for a SyncedList
 */
public class SyncedListStep {
    long timestamp = 0;
    String changeId = "";
    ACTION changeAction = ACTION.CLEAR;
    Object changeValue = "";

    public SyncedListStep(JSONObject jsonObject) throws JSONException {
        this.changeAction = ACTION.values()[jsonObject.getInt("changeAction")];
        this.timestamp = jsonObject.getLong("timestamp");
        if (jsonObject.has("changeId")) {
            this.changeId = jsonObject.getString("changeId");
        }
        if (jsonObject.has("changeValueElement")) {
            this.changeValue = new SyncedListElement(
                    jsonObject.getJSONObject("changeValueElement"));
        } else if (jsonObject.has("changeValue")) {
            this.changeValue = jsonObject.get("changeValue");
        }
    }

    public SyncedListStep(String changeId,
                          ACTION changeAction,
                          Object changeValue) {
        this.timestamp = System.currentTimeMillis();
        this.changeId = changeId;
        this.changeAction = changeAction;
        this.changeValue = changeValue;
    }

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public ACTION getChangeAction() {
        return changeAction;
    }

    public void setChangeAction(ACTION changeAction) {
        this.changeAction = changeAction;
    }

    public Object getChangeValue() {
        return changeValue;
    }

    public void setChangeValue(Object changeValue) {
        this.changeValue = changeValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", this.timestamp);
        jsonObject.put("changeAction", this.changeAction.ordinal());
        if (this.changeId != null) {
            jsonObject.put("changeId", this.changeId);
        }
        if (this.changeValue != null) {
            if (this.changeValue instanceof SyncedListElement) {
                jsonObject.put("changeValueElement",
                               ((SyncedListElement) this.changeValue).toJSON());
            } else {
                jsonObject.put("changeValue", this.changeValue);
            }
        }
        return jsonObject;
    }

    /**
     * Test if step is equal
     *
     * @param step equal to this step?
     * @return equal?
     */
    public boolean equals(SyncedListStep step) {
        if (getChangeId() == null && step.getChangeId() != null) {
            return false;
        }
        if (getChangeId() != null && step.getChangeId() == null) {
            return false;
        }
        if (getChangeId() != null && step.getChangeId() != null &&
                !getChangeId().equals(step.getChangeId())) {
            return false;
        }
        if (!getChangeAction().equals(step.getChangeAction())) {
            return false;
        }
        if (timestamp != step.getTimestamp()) {
            return false;
        }
        return true;
    }
}
