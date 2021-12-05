package eu.schmidt.systems.opensyncedlists.datatypes;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.schmidt.systems.opensyncedlists.utils.Constant;
import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

/**
 * A synced list
 */
public class SyncedList {
    SyncedListHeader syncedListHeader;
    public ArrayList<SyncedListElement> elementsBuffer, checkedElementsBuffer,
            uncheckedElementsBuffer;
    ArrayList<SyncedListStep> elementSteps;

    public SyncedList(SyncedListHeader syncedListHeader, JSONObject jsonObject)
            throws JSONException {
        this.syncedListHeader = syncedListHeader;
        elementSteps = new ArrayList<>();
        checkedElementsBuffer = new ArrayList<>();
        uncheckedElementsBuffer = new ArrayList<>();
        JSONArray jsonArraySteps = jsonObject.getJSONArray("steps");
        for (int i = 0; i < jsonArraySteps.length(); i++) {
            JSONObject step = (JSONObject) jsonArraySteps.get(i);
            elementSteps.add(new SyncedListStep(step));
        }
        recalculateBuffers();
    }

    public SyncedList(SyncedListHeader syncedListHeader,
                      ArrayList<SyncedListStep> elementSteps) {
        this.syncedListHeader = syncedListHeader;
        this.elementSteps = elementSteps;
        this.elementsBuffer = new ArrayList<>();
    }

    public ArrayList<SyncedListElement> getReformatElements() {
        Log.d(Constant.LOG_TITLE_BUILDING,
              "Build list with " + this.elementSteps.size() + " Steps");
        ArrayList<SyncedListElement> result = new ArrayList<>();
        for (int i = 0; i < this.elementSteps.size(); i++) {
            SyncedListStep currentStep = this.elementSteps.get(i);
            switch (currentStep.getChangeAction()) {
                case ADD:
                    result.add(
                            (SyncedListElement) currentStep.getChangeValue());
                    break;
                case UPDATE:
                    SyncedListElement changeElement =
                            (SyncedListElement) currentStep.getChangeValue();
                    for (int x = 0; x < result.size(); x++) {
                        if (result.get(x).getId()
                                .equals(currentStep.getChangeId())) {
                            result.set(x, changeElement);
                            break;
                        }
                    }
                    break;
                case SWAP:
                    for (int x = 0; x < result.size(); x++) {
                        if (result.get(x).getId()
                                .equals(currentStep.getChangeId())) {
                            int swap = 0;
                            for (swap = 0; swap < result.size(); swap++) {
                                if (result.get(swap).getId()
                                        .equals(currentStep.getChangeValue())) {
                                    break;
                                }
                            }
                            if (x >= 0 && swap >= 0 && x < result.size() &&
                                    swap < result.size()) {
                                SyncedListElement inX = result.get(x);
                                SyncedListElement inSwap = result.get(swap);
                                result.set(x, inSwap);
                                result.set(swap, inX);
                            }
                            break;
                        }
                    }
                    break;
                case REMOVE:
                    for (int x = 0; x < result.size(); x++) {
                        if (result.get(x).getId()
                                .equals(currentStep.getChangeId())) {
                            result.remove(x);
                            break;
                        }
                    }
                    break;
                case MOVE:
                    int srcIndex = -1;
                    int dstIndex;

                    for (int x = 0; x < result.size(); x++) {
                        if (result.get(x).getId()
                                .equals(currentStep.getChangeId())) {
                            srcIndex = x;
                            break;
                        }
                    }
                    if (srcIndex != -1) {
                        dstIndex = (int) currentStep.getChangeValue();
                        moveItem(srcIndex, dstIndex, result);
                    }
                    break;
                case CLEAR:
                    result.clear();
                    break;
                default:
                    Log.e(Constant.LOG_TITLE_BUILDING,
                          "Cant found step action");
            }
        }

        return result;
    }

    public void recalculateBuffers() {
        elementsBuffer = getReformatElements();
        if (getHeader().isCheckedList()) {
            checkedElementsBuffer = new ArrayList<>();
            uncheckedElementsBuffer = new ArrayList<>();
            for (SyncedListElement element : elementsBuffer) {
                if (element.getChecked()) {
                    checkedElementsBuffer.add(element);
                } else {
                    uncheckedElementsBuffer.add(element);
                }
            }
        }
    }

    public ArrayList<SyncedListElement> getElements() {
        if (elementsBuffer == null) {
            recalculateBuffers();
        }
        return elementsBuffer;
    }

    public ArrayList<SyncedListElement> getCheckedElements() {
        if (elementsBuffer == null) {
            recalculateBuffers();
        }

        return checkedElementsBuffer;
    }

    public ArrayList<SyncedListElement> getUncheckedElements() {
        if (elementsBuffer == null) {
            recalculateBuffers();
        }
        return uncheckedElementsBuffer;
    }

    public String getId() {
        return syncedListHeader.getId();
    }

    public void setId(String id) {
        this.syncedListHeader.setId(id);
    }

    public String getName() {
        return syncedListHeader.getName();
    }

    public void setName(String name) {
        syncedListHeader.setName(name);
    }

    public byte[] getSecret() {
        return syncedListHeader.getSecret();
    }

    public boolean compareSecret(byte[] target) {
        for (int i = 0; i < target.length; i++) {
            if (this.syncedListHeader.getSecret()[i] != target[i]) {
                return false;
            }
        }
        return true;
    }

    public void setSecret(String secret) {
        this.syncedListHeader.setSecret(Cryptography.getSHA(secret));
    }

    public void setLocalSecret(String secret) {
        this.syncedListHeader.setLocalSecret(Cryptography.getSHA(secret));
    }

    public ArrayList<SyncedListStep> getElementSteps() {
        return elementSteps;
    }

    public void setElementSteps(ArrayList<SyncedListStep> elementSteps) {
        this.elementSteps = elementSteps;
        recalculateBuffers();
    }

    public void addElementStep(SyncedListStep elementStep) {
        this.elementSteps.add(elementStep);
        recalculateBuffers();
    }

    public static <T> void moveItem(int sourceIndex,
                                    int targetIndex,
                                    List<T> list) {
        if (sourceIndex <= targetIndex) {
            Collections.rotate(list.subList(sourceIndex, targetIndex + 1), -1);
        } else {
            Collections.rotate(list.subList(targetIndex, sourceIndex + 1), 1);
        }
    }

    public String generateUniqueElementId() {
        String newId = Cryptography.generatingRandomString(50);
        for (int i = 0; i < elementsBuffer.size(); i++) {
            if (newId.equals(elementsBuffer.get(i).getId())) {
                i = -1;
                newId = Cryptography.generatingRandomString(50);
            }
        }
        return newId;
    }

    /**
     * Convert to JSON (HEADER not included)
     *
     * @return
     * @throws JSONException
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArraySteps = new JSONArray();
        for (SyncedListStep step : elementSteps) {
            jsonArraySteps.put(step.toJSON());
        }
        jsonObject.put("steps", jsonArraySteps);
        return jsonObject;
    }

    public SyncedListHeader getHeader() {
        return syncedListHeader;
    }
}
