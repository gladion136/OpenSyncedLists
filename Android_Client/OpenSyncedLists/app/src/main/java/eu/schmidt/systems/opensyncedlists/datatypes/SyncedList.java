package eu.schmidt.systems.opensyncedlists.datatypes;

import static eu.schmidt.systems.opensyncedlists.ListActivity.DEBUG;

import android.util.Log;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.utils.Cryptography;

public class SyncedList {
    String id;
    String name;
    private byte[] secret;
    private ArrayList<SyncedListElement> elementsBuffer = new ArrayList<>();
    ArrayList<SyncedListStep> elementSteps;

    public SyncedList() {
    }

    public SyncedList(String id,
                      String name,
                      byte[] secret,
                      ArrayList<SyncedListStep> elementSteps) {
        this.id = id;
        this.name = name;
        this.secret = secret;
        this.elementSteps = elementSteps;
    }

    public ArrayList<SyncedListElement> getReformatElements() {
        ArrayList<SyncedListElement> result = new ArrayList<>();
        for (int i=0; i < this.elementSteps.size(); i++) {
            SyncedListStep currentStep = this.elementSteps.get(i);
            switch (currentStep.getChangeAction()) {
                case ADD:
                    result.add((SyncedListElement) currentStep.getChangeValue());
                    break;
                case UPDATE:
                    SyncedListElement changeElement =
                            (SyncedListElement) currentStep.getChangeValue();
                    for(int x=0; x < result.size(); x++) {
                        if (result.get(x).getId().equals(currentStep.getChangeId())) {
                            result.set(x, changeElement);
                            break;
                        }
                    }
                    break;
                case SWAP:
                    for(int x=0; x < result.size(); x++) {
                        if (result.get(x).getId().equals(currentStep.getChangeId())) {
                            int swap=0;
                            for(swap=0; swap < result.size(); swap++) {
                                if (result.get(swap).getId().equals(currentStep.getChangeValue())) {
                                    break;
                                }
                            }
                            if(x >= 0 && swap >= 0 && x < result.size() && swap < result
                                    .size()) {
                                SyncedListElement inX = result.get(x);
                                SyncedListElement inSwap = result.get(swap);
                                result.set(x, inSwap);
                                result.set(swap, inX);
                                Log.v(DEBUG, "Swap");
                            }
                            break;
                        }
                    }
                    break;
                case REMOVE:
                    for(int x=0; x < result.size(); x++) {
                        if (result.get(x).getId().equals(currentStep.getChangeId())) {
                            result.remove(x);
                            break;
                        }
                    }
                    break;
                case MOVE:
                    /*for(int x=0; x < result.size(); x++) {
                        if (result.get(x).getId().equals(currentStep.getChangeId())) {

                            if(x > 0 && (int)currentStep.getChangeValue() + x > 0) {
                                SyncedListElement inX = result.get(x);
                                SyncedListElement inSwap = result.get(swap);
                                result.set(x, inSwap);
                                result.set(swap, inX);
                            }
                            break;
                        }
                    }*/
                    break;
                default:

            }
        }

        return result;
    }

    public ArrayList<SyncedListElement> getElements() {
        return elementsBuffer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getSecret() {
        return secret;
    }

    public boolean compareSecret(byte[] target) {
        for (int i=0; i < target.length; i++) {
            if(this.secret[i] != target[i])
            {
                return false;
            }
        }
        return true;
    }

    public void setSecret(String secret) {
        this.secret = Cryptography.getSHA(secret);
    }

    public ArrayList<SyncedListStep> getElementSteps() {
        return elementSteps;
    }

    public void setElementSteps(ArrayList<SyncedListStep> elementSteps) {
        this.elementSteps = elementSteps;
        this.elementsBuffer = this.getReformatElements();
    }

    public void addElementStep(SyncedListStep elementStep) {
        this.elementSteps.add(elementStep);
        this.elementsBuffer = this.getReformatElements();
    }
}
