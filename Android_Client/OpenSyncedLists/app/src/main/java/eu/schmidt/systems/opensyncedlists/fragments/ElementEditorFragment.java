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
package eu.schmidt.systems.opensyncedlists.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.syncedlist.ACTION;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListStep;

/**
 * Fragment to edit one element. Displayed as BottomSheetDialog
 */
public class ElementEditorFragment extends BottomSheetDialogFragment
{
    EditText eTName, eTDescription;
    Button btnApplyChanges, btnDelete;
    /** Buffered Element */
    SyncedListElement syncedListElement;
    /** Callback to add a SyncedListStep to the List */
    Callback callback;
    
    /**
     * Fragments needs an empty constructor for creation from the android system
     * via newInstance.
     */
    public ElementEditorFragment()
    {
    
    }
    
    /**
     * Inflate the fragment.
     *
     * @param inflater           LayoutInflater
     * @param container          viewGroup
     * @param savedInstanceState not used
     * @return the View
     */
    @Override public View onCreateView(LayoutInflater inflater,
        ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_element_editor, container,
            false);
    }
    
    /**
     * Fills the view with content and listeners.
     *
     * @param view               View to fill
     * @param savedInstanceState Just used for super call.
     */
    @Override public void onViewCreated(@NonNull View view,
        @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        eTName = view.findViewById(R.id.eTName);
        eTDescription = view.findViewById(R.id.eTDescription);
        btnApplyChanges = view.findViewById(R.id.btnApplyChanges);
        btnDelete = view.findViewById(R.id.btnDelete);
        
        eTName.setText(syncedListElement.getName());
        eTDescription.setText(syncedListElement.getDescription());
        
        btnApplyChanges.setOnClickListener(v ->
        {
            String newName = eTName.getText().toString();
            String newDescription = eTDescription.getText().toString();
            if (!newName.equals("") && (
                !newName.equals(syncedListElement.getName())
                    || !newDescription.equals(
                    syncedListElement.getDescription())))
            {
                SyncedListElement updated = syncedListElement.clone();
                updated.setName(newName);
                updated.setDescription(newDescription);
                SyncedListStep newStep =
                    new SyncedListStep(updated.getId(), ACTION.UPDATE, updated);
                callback.addNewStep(newStep);
                dismiss();
            }
        });
        
        btnDelete.setOnClickListener(v ->
        {
            SyncedListStep newStep =
                new SyncedListStep(syncedListElement.getId(), ACTION.REMOVE);
            callback.addNewStep(newStep);
            dismiss();
        });
    }
    
    /**
     * Creates and initialize the Fragment
     *
     * @param syncedListElement buffered SyncedListElement
     * @param callback          Callback to handle new SyncedListSteps
     * @return initialized Fragment
     */
    public static ElementEditorFragment newInstance(
        SyncedListElement syncedListElement, Callback callback)
    {
        ElementEditorFragment elementEditorFragment =
            new ElementEditorFragment();
        elementEditorFragment.syncedListElement = syncedListElement;
        elementEditorFragment.callback = callback;
        return elementEditorFragment;
    }
    
    /**
     * Interface to handle new SyncedListSteps.
     */
    public interface Callback
    {
        void addNewStep(SyncedListStep syncedListStep);
    }
}