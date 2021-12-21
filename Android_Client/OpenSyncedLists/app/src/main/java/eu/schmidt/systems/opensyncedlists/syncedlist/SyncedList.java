package eu.schmidt.systems.opensyncedlists.syncedlist;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

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

    public SyncedList(JSONObject jsonObject) throws JSONException {
        this.syncedListHeader =
                new SyncedListHeader(jsonObject.getJSONObject("header"));
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
                    result.add(currentStep.getChangeValueElement());
                    break;
                case UPDATE:
                    SyncedListElement changeElement =
                            currentStep.getChangeValueElement();
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
                            int swap;
                            for (swap = 0; swap < result.size(); swap++) {
                                if (result.get(swap).getId().equals(currentStep
                                                                            .getChangeValueString())) {
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
                        dstIndex = currentStep.getChangeValueInt();
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
        if (getHeader().isCheckOption()) {
            checkedElementsBuffer = new ArrayList<>();
            uncheckedElementsBuffer = new ArrayList<>();
            for (SyncedListElement element : elementsBuffer) {
                if (element.getChecked()) {
                    checkedElementsBuffer.add(element);
                } else {
                    uncheckedElementsBuffer.add(element);
                }
            }
            getHeader().setListSize(checkedElementsBuffer.size() + " / " + elementsBuffer.size());
        }else {
            getHeader().setListSize(String.valueOf(elementsBuffer.size()));
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

    public String getSecret() {
        return Cryptography.byteArrayToString(syncedListHeader.getSecret());
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

    public void setLocalSecret(SecretKey keypair) {
        this.syncedListHeader.setLocalSecret(keypair);
    }

    public ArrayList<SyncedListStep> getElementSteps() {
        return elementSteps;
    }

    public void setElementSteps(ArrayList<SyncedListStep> elementSteps) {
        this.elementSteps = elementSteps;
        recalculateBuffers();
    }

    public void addElementStep(SyncedListStep elementStep) {
        if (elementStep.changeAction == ACTION.CLEAR) {
            this.elementSteps.clear();
        }
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
     * @return SyncedList steps
     * @throws JSONException
     */
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray jsonArraySteps = new JSONArray();
            for (SyncedListStep step : elementSteps) {
                jsonArraySteps.put(step.toJSON());
            }
            jsonObject.put("steps", jsonArraySteps);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * Convert to JSON (Header with secrets included)
     *
     * @return SyncedList with secrets
     * @throws JSONException
     */
    public JSONObject toJsonWithHeader() {
        JSONObject jsonObject = toJSON();
        try {
            jsonObject.put("header", getHeader().toJSON());
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return jsonObject;
    }

    public SyncedListHeader getHeader() {
        return syncedListHeader;
    }

    public String getAsReadableString() {
        StringBuilder list = new StringBuilder(getName());
        if (getHeader().isCheckedList()) {
            for (SyncedListElement element : uncheckedElementsBuffer) {
                list.append("\n").append(element.getAsReadableString());
            }

            for (SyncedListElement element : checkedElementsBuffer) {
                list.append("\n").append(element.getAsReadableString());
            }
        } else {
            for (SyncedListElement element : elementsBuffer) {
                list.append("\n").append(element.getAsReadableString());
            }
        }
        return list.toString();
    }

    /**
     * Sync lists and return solution
     *
     * @param syncedList1
     * @param syncedList2
     * @return synchronized List
     */
    public static SyncedList sync(SyncedList syncedList1,
                                  SyncedList syncedList2) {
        ArrayList<SyncedListStep> result =
                (ArrayList<SyncedListStep>) syncedList1.getElementSteps()
                        .clone();
        ArrayList<SyncedListStep> syncWith = syncedList2.getElementSteps();

        for (int i = 0; i < syncWith.size(); i++) {
            SyncedListStep currentStep = syncWith.get(i);
            // Still in result?
            boolean stillInResult = false;

            for (SyncedListStep s : result) {
                if (s.equals(currentStep)) {
                    stillInResult = true;
                    break;
                }
            }

            if (stillInResult) {
                continue;
            }
            // Add between correct time
            boolean added = false;
            for (int x = result.size() - 1; x >= 0; x--) {
                if (result.get(x).timestamp < currentStep.timestamp) {
                    result.add(x + 1, currentStep);
                    added = true;
                    break;
                }
            }
            if (!added) {
                result.add(0, currentStep);
            }
        }
        // Optimize
        for(int x = result.size()-1; x > 0; x--) {
            if(result.get(x).getChangeAction()==ACTION.CLEAR) {
                result.removeAll(result.subList(0, x));
                break;
            }
        }
        syncedList1.setElementSteps(result);
        return syncedList1;
    }

    public void sync(SyncedList syncedList) {
        SyncedList newList = SyncedList.sync(this, syncedList);
        syncedListHeader = newList.getHeader();
        setElementSteps(newList.getElementSteps());
    }

    public String getFullListEncrypted() {
        String data = toJsonWithHeader().toString();
        return Cryptography.encryptRSA(getHeader().getLocalSecret(), data);
    }
}
