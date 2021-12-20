package eu.schmidt.systems.opensyncedlists.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.datatypes.ACTION;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListStep;

public class ElementEditorFragment extends BottomSheetDialogFragment {
    EditText eTName, eTDescription;
    Button btnApplyChanges, btnDelete;

    SyncedListElement syncedListElement;
    Callback callback;

    public ElementEditorFragment() {

    }

    public static ElementEditorFragment newInstance(SyncedListElement syncedListElement,
                                                    Callback callback) {
        ElementEditorFragment elementEditorFragment =
                new ElementEditorFragment();
        elementEditorFragment.syncedListElement = syncedListElement;
        elementEditorFragment.callback = callback;
        return elementEditorFragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override public View onCreateView(LayoutInflater inflater,
                                       ViewGroup container,
                                       Bundle savedInstanceState) {
        return inflater
                .inflate(R.layout.fragment_element_editor, container, false);
    }

    @Override public void onViewCreated(@NonNull View view,
                                        @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        eTName = view.findViewById(R.id.eTName);
        eTDescription = view.findViewById(R.id.eTDescription);
        btnApplyChanges = view.findViewById(R.id.btnApplyChanges);
        btnDelete = view.findViewById(R.id.btnDelete);

        eTName.setText(syncedListElement.getName());
        eTDescription.setText(syncedListElement.getDescription());

        btnApplyChanges.setOnClickListener(v -> {
            String newName = eTName.getText().toString();
            String newDescription = eTDescription.getText().toString();
            if (!newName.equals("") &&
                    (!newName.equals(syncedListElement.getName()) ||
                            !newDescription.equals(syncedListElement
                                                           .getDescription()))) {
                syncedListElement.setName(newName);
                syncedListElement.setDescription(newDescription);
                SyncedListStep newStep =
                        new SyncedListStep(syncedListElement.getId(),
                                           ACTION.UPDATE, syncedListElement);
                callback.addNewStep(newStep);
                dismiss();
            }
        });

        btnDelete.setOnClickListener(v -> {
            SyncedListStep newStep =
                    new SyncedListStep(syncedListElement.getId(), ACTION.REMOVE,
                                       null);
            callback.addNewStep(newStep);
            dismiss();
        });
    }

    public interface Callback {
        void addNewStep(SyncedListStep syncedListStep);
    }
}