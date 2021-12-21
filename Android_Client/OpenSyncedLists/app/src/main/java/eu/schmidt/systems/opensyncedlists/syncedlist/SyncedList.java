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

    /**
     * Load the list from JSON.
     *
     * @param jsonObject list as JSON
     * @throws JSONException
     */
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

    /**
     * Load a list from the storage.
     *
     * @param syncedListHeader Header of the list
     * @param jsonObject Data of the list (steps)
     * @throws JSONException
     */
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

    /**
     * Load a list from Header and elementSteps. Called for creating a new
     * list for example.
     *
     * @param syncedListHeader Header
     * @param elementSteps Steps
     */
    public SyncedList(SyncedListHeader syncedListHeader,
                      ArrayList<SyncedListStep> elementSteps) {
        this.syncedListHeader = syncedListHeader;
        this.elementSteps = elementSteps;
        recalculateBuffers();
    }

    /**
     * Calculate the element of a list.
     *
     * @return elements of the list
     */
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

    /**
     * Recalculate all buffers. elementsBuffer, checkedElementsBuffer and uncheckedElementsBuffer
     */
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

    /**
     * Get all elements of a list
     *
     * @return elementsBuffer
     */
    public ArrayList<SyncedListElement> getElements() {
        if (elementsBuffer == null) {
            recalculateBuffers();
        }
        return elementsBuffer;
    }

    /**
     * Get all checked elements
     *
     * @return checkedElementsBuffer
     */
    public ArrayList<SyncedListElement> getCheckedElements() {
        if (elementsBuffer == null) {
            recalculateBuffers();
        }
        return checkedElementsBuffer;
    }

    /**
     * Get alö unchecked elements
     *
     * @return uncheckedElementsBuffer
     */
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

    /**
     * Get all element steps
     *
     * @return elementSteps
     */
    public ArrayList<SyncedListStep> getElementSteps() {
        return elementSteps;
    }

    /**
     * Set all elementSteps
     *
     * @param elementSteps all updated elementSteps
     */
    public void setElementSteps(ArrayList<SyncedListStep> elementSteps) {
        this.elementSteps = elementSteps;
        recalculateBuffers();
    }

    /**
     * Add one elementStep and recalculate the buffers.
     *
     * @param elementStep new elementStep
     */
    public void addElementStep(SyncedListStep elementStep) {
        if (elementStep.changeAction == ACTION.CLEAR) {
            this.elementSteps.clear();
        }
        this.elementSteps.add(elementStep);
        recalculateBuffers();
    }

    /**
     *
     *
     * @param sourceIndex old Index
     * @param targetIndex new Index
     * @param list list to change
     * @param <T> Object type inside the list
     */
    public static <T> void moveItem(int sourceIndex,
                                    int targetIndex,
                                    List<T> list) {
        if (sourceIndex <= targetIndex) {
            Collections.rotate(list.subList(sourceIndex, targetIndex + 1), -1);
        } else {
            Collections.rotate(list.subList(targetIndex, sourceIndex + 1), 1);
        }
    }

    /**
     * Generate a unique element Id in the list
     *
     * @return unique Id
     */
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
     * Convert to JSON (HEADER not included).
     *
     * @return SyncedList steps as JSON
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
     * Convert to JSON (Header with secrets included).
     *
     * @return SyncedList with header(secrets included)
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

    /**
     * Get list elements as Markdown.
     *
     * @return list as Markdown formatted String
     */
    public String getAsMarkdown() {
        StringBuilder list = new StringBuilder(getName());
        if (getHeader().isCheckedList()) {
            for (SyncedListElement element : uncheckedElementsBuffer) {
                list.append("\n").append(element.getAsMarkdown());
            }

            for (SyncedListElement element : checkedElementsBuffer) {
                list.append("\n").append(element.getAsMarkdown());
            }
        } else {
            for (SyncedListElement element : elementsBuffer) {
                list.append("\n").append(element.getAsMarkdown());
            }
        }
        return list.toString();
    }

    /**
     * Sync lists and return solution.
     *
     * @param syncedList1 base
     * @param syncedList2 syncWith
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

    /**
     * Synchronize the list.
     *
     * @param syncedList list to sync with.
     */
    public void sync(SyncedList syncedList) {
        SyncedList newList = SyncedList.sync(this, syncedList);
        syncedListHeader = newList.getHeader();
        setElementSteps(newList.getElementSteps());
    }

    /**
     * Get full list with header encrypted.
     *
     * @return encrypted list with header
     */
    public String getFullListEncrypted() {
        String data = toJsonWithHeader().toString();
        return Cryptography.encryptRSA(getHeader().getLocalSecret(), data);
    }
}
