package eu.schmidt.systems.opensyncedlists.adapters;

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_DEFAULT;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.activities.ListActivity;
import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.syncedlist.ACTION;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.fragments.ElementEditorFragment;
import eu.schmidt.systems.opensyncedlists.utils.Constant;

/**
 * RecyclerView Adapter for ListActivity
 */
public class SyncedListAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements View.OnClickListener {
    private ListActivity listActivity;
    private SyncedList syncedList;
    private RecyclerView recyclerView;
    private boolean scrollListTopBottom;
    private LayoutInflater layoutInflater;
    private int jumpDistance = 1;

    /**
     * ViewHolder for one element
     */
    public static class ElementViewHolder extends RecyclerView.ViewHolder {
        public final CheckBox checkBox;
        public final EditText eTName;
        public final TextView tVDescription;
        public final ImageView iVBtnUp, iVBtnDown, iVTop, iVBottom;

        public ElementViewHolder(View view) {
            super(view);
            checkBox = view.findViewById(R.id.checkBox);
            eTName = view.findViewById(R.id.eTTitle);
            tVDescription = view.findViewById(R.id.tVDescription);
            iVBtnUp = view.findViewById(R.id.btnJumpUp);
            iVBtnDown = view.findViewById(R.id.btnJumpDown);
            iVTop = view.findViewById(R.id.btnJumpTop);
            iVBottom = view.findViewById(R.id.btnJumpBottom);
        }
    }

    /**
     * ViewHolder for the isolator
     */
    public static class IsolatorViewHolder extends RecyclerView.ViewHolder {

        public IsolatorViewHolder(View view) {
            super(view);
        }
    }

    public SyncedListAdapter(ListActivity listActivity,
                             RecyclerView recyclerView,
                             SyncedList syncedList) {
        this.listActivity = listActivity;
        this.recyclerView = recyclerView;
        this.syncedList = syncedList;
        layoutInflater = LayoutInflater.from(listActivity);

        // Add ItemTouchHelper for drag and drop events
        ItemTouchHelper itemTouchHelper =
                new ItemTouchHelper(new ItemTouchHelper.Callback() {
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        if (viewHolder instanceof ElementViewHolder &&
                                target instanceof ElementViewHolder) {
                            SyncedListElement selectedElement =
                                    getElementOnPosition(
                                            viewHolder.getAdapterPosition());
                            SyncedListElement displacedElement =
                                    getElementOnPosition(
                                            target.getAdapterPosition());
                            if (syncedList.getHeader().isCheckedList() &&
                                    selectedElement.getChecked() !=
                                            displacedElement.getChecked()) {
                                return false;
                            }
                            int newPositionInList = syncedList.getElements()
                                    .indexOf(displacedElement);
                            SyncedListStep newStep =
                                    new SyncedListStep(selectedElement.getId(),
                                                       ACTION.MOVE,
                                                       newPositionInList);
                            listActivity.addElementStepAndSave(newStep, false);

                            notifyItemMoved(viewHolder.getAdapterPosition(),
                                            target.getAdapterPosition());
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder,
                                         int direction) {
                        Log.d(Constant.LOG_TITLE_DEFAULT,
                              "SWIPED: " + direction);
                    }

                    @Override
                    public int getMovementFlags(RecyclerView recyclerView,
                                                RecyclerView.ViewHolder viewHolder) {
                        if (viewHolder instanceof ElementViewHolder) {
                            return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                                            ItemTouchHelper.DOWN |
                                                    ItemTouchHelper.UP);
                        } else {
                            return 0;
                        }
                    }
                });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(listActivity);
        jumpDistance = Integer.parseInt(
                sharedPreferences.getString("jump_range", "1"));
        scrollListTopBottom = sharedPreferences.getBoolean("scrollList", false);
    }

    /**
     * Return the size of elements (invoked by the layout manager)
     *
     * @return size of elements
     */
    @Override public int getItemCount() {
        if (syncedList.getHeader().isCheckedList() &&
                syncedList.getCheckedElements().size() > 0) {
            return syncedList.getElements().size() + 1; // + Isolator
        } else {
            return syncedList.getElements().size();
        }
    }

    /**
     * Create new views (invoked by the layout manager)
     *
     * @param viewGroup viewGroup
     * @param viewType  viewType
     * @return viewHolder
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                                      int viewType) {
        RecyclerView.ViewHolder viewHolder;

        switch (viewType) {
            case R.layout.element_list_invert:
            case R.layout.element_list:
                View view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(viewType, viewGroup, false);
                view.setOnClickListener(this);
                viewHolder = new ElementViewHolder(view);
                break;
            case R.layout.element_list_isolator:
                View isolatorView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(viewType, viewGroup, false);
                viewHolder = new IsolatorViewHolder(isolatorView);
                break;
            default:
                throw new IllegalStateException(
                        "Unexpected value: " + viewType);
        }
        return viewHolder;
    }

    @Override public int getItemViewType(int position) {
        if (syncedList.getHeader().isCheckedList() &&
                syncedList.getUncheckedElements().size() == position) {
            return R.layout.element_list_isolator;
        } else {

            return syncedList.getHeader().isInvertElement()
                   ? R.layout.element_list_invert : R.layout.element_list;
        }
    }

    /**
     * Replace content of a view (override given because it got recycled)
     *
     * @param viewHolder viewHolder
     * @param position   current position
     */
    @Override public void onBindViewHolder(RecyclerView.ViewHolder viewHolder,
                                           final int position) {
        if (viewHolder instanceof ElementViewHolder) {
            ElementViewHolder elementViewHolder =
                    (ElementViewHolder) viewHolder;
            // remove old listeners (because of recycling old views)
            elementViewHolder.checkBox.setOnCheckedChangeListener(null);
            elementViewHolder.eTName.setOnFocusChangeListener(null);
            elementViewHolder.eTName.setOnEditorActionListener(null);

            SyncedListElement currentSyncedListElement =
                    getElementOnPosition(position);

            if (!syncedList.getHeader().isCheckOption()) {
                elementViewHolder.checkBox.setVisibility(View.GONE);
            }

            // name edittext
            elementViewHolder.eTName.setOnFocusChangeListener((v, focused) -> {
                if (!focused) {
                    checkNameChangesAndSubmit(elementViewHolder,
                                              currentSyncedListElement);
                }
            });
            elementViewHolder.eTName
                    .setText(currentSyncedListElement.getName());
            elementViewHolder.eTName
                    .setOnEditorActionListener((v, actionId, event) -> {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            checkNameChangesAndSubmit(elementViewHolder,
                                                      currentSyncedListElement);
                            InputMethodManager imm =
                                    (InputMethodManager) listActivity
                                            .getSystemService(
                                                    Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(
                                    ((Activity) listActivity).getCurrentFocus()
                                            .getWindowToken(), 0);

                            return true;
                        }
                        return false;
                    });
            elementViewHolder.tVDescription
                    .setText(currentSyncedListElement.getDescription());

            // Checkbox
            elementViewHolder.checkBox
                    .setChecked(currentSyncedListElement.getChecked());
            // on checked
            elementViewHolder.checkBox
                    .setOnCheckedChangeListener((v, checked) -> {
                        SyncedListElement updated =
                                currentSyncedListElement.clone();
                        updated.setChecked(checked);
                        SyncedListStep newStep = new SyncedListStep(
                                currentSyncedListElement.getId(), ACTION.UPDATE,
                                updated);
                        listActivity.addElementStepAndSave(newStep, syncedList
                                .getHeader().isCheckedList());
                    });

            // on move to top
            elementViewHolder.iVTop.setOnClickListener(vi -> {
                SyncedListStep newStep =
                        new SyncedListStep(currentSyncedListElement.getId(),
                                           ACTION.MOVE, 0);
                listActivity.addElementStepAndSave(newStep, true);
                if (scrollListTopBottom) {
                    recyclerView.scrollToPosition(0);
                }
            });

            // on move to bottom
            elementViewHolder.iVBottom.setOnClickListener(vi -> {
                SyncedListStep newStep =
                        new SyncedListStep(currentSyncedListElement.getId(),
                                           ACTION.MOVE,
                                           syncedList.getElements().size() - 1);
                listActivity.addElementStepAndSave(newStep, true);
                if (scrollListTopBottom) {
                    recyclerView.scrollToPosition(
                            getPositionOfElement(currentSyncedListElement));
                }
            });

            // on move up
            elementViewHolder.iVBtnUp.setOnClickListener(vi -> {
                moveElementAndSave(currentSyncedListElement, -jumpDistance);
            });

            // on move down
            elementViewHolder.iVBtnDown.setOnClickListener(vi -> {
                moveElementAndSave(currentSyncedListElement, jumpDistance);
            });
        }
    }

    /**
     * Check element name changed and submit (create new SyncedListStep)
     *
     * @param viewHolder
     * @param syncedListElement
     */
    public void checkNameChangesAndSubmit(ElementViewHolder viewHolder,
                                          SyncedListElement syncedListElement) {

        String newName = viewHolder.eTName.getText().toString();
        if (!newName.equals("") &&
                !newName.equals(syncedListElement.getName())) {
            SyncedListElement updated = syncedListElement.clone();
            updated.setName(newName);
            SyncedListStep newStep =
                    new SyncedListStep(updated.getId(), ACTION.UPDATE,
                                       updated);
            listActivity.addElementStepAndSave(newStep, true);
        }
    }

    /**
     * OnClick on Element
     *
     * @param v View of Element
     */
    @Override public void onClick(View v) {
        int itemPosition = recyclerView.getChildLayoutPosition(v);
        SyncedListElement syncedListElement =
                getElementOnPosition(itemPosition);
        BottomSheetDialogFragment bottomSheetDialogFragment =
                new ElementEditorFragment()
                        .newInstance(syncedListElement, syncedListStep -> {
                            listActivity.addElementStepAndSave(syncedListStep,
                                                               true);
                        });
        bottomSheetDialogFragment.show(((AppCompatActivity) listActivity)
                                               .getSupportFragmentManager(),
                                       bottomSheetDialogFragment.getTag());
    }

    /**
     * Get element by position inside recyclerview
     *
     * @param position
     * @return syncedListElement
     */
    public SyncedListElement getElementOnPosition(int position) {
        if (syncedList.getHeader().isCheckedList()) {
            if (position > syncedList.getUncheckedElements().size()) {
                return syncedList.getCheckedElements().get(position - syncedList
                        .getUncheckedElements().size() - 1);
            } else {
                return syncedList.getUncheckedElements().get(position);
            }
        } else {
            return syncedList.getElements().get(position);
        }
    }

    /**
     * Get position of element inside recyclerview
     *
     * @param syncedListElement
     * @return
     */
    public int getPositionOfElement(SyncedListElement syncedListElement) {
        int position = 0;
        if (syncedList.getHeader().isCheckedList()) {
            for (int i = 0; i < syncedList.getUncheckedElements().size(); i++) {
                if (syncedListElement.getId()
                        .equals(syncedList.getUncheckedElements().get(i)
                                        .getId())) {
                    return position;
                }
                position++;
            }
            position++; // Isolator
            for (int i = 0; i < syncedList.getCheckedElements().size(); i++) {
                if (syncedListElement.getId()
                        .equals(syncedList.getCheckedElements().get(i)
                                        .getId())) {
                    return position;
                }
                position++;
            }
        } else {
            for (int i = 0; i < syncedList.getElements().size(); i++) {
                if (syncedListElement.getId()
                        .equals(syncedList.getElements().get(i).getId())) {
                    return position;
                }
                position++;
            }
        }
        Log.e(LOG_TITLE_DEFAULT,
              "Error: Can't find element: " + syncedListElement);
        return position;
    }

    /**
     * Move element inside list (checked and unchecked list separated
     *
     * @param syncedListElement Element to move
     * @param dir               direction and strength
     */
    public void moveElementAndSave(SyncedListElement syncedListElement,
                                   int dir) {
        SyncedListElement displacedElement;

        // Choose list where the element should move
        ArrayList<SyncedListElement> listOfElement;
        if (!syncedList.getHeader().isCheckedList()) {
            listOfElement = syncedList.getElements();
        } else if (syncedListElement.getChecked()) {
            listOfElement = syncedList.getCheckedElements();
        } else {
            listOfElement = syncedList.getUncheckedElements();
        }
        int targetedPosition = listOfElement.indexOf(syncedListElement) + dir;

        if (targetedPosition >= listOfElement.size()) {
            displacedElement = listOfElement.get(listOfElement.size() - 1);
        } else if (targetedPosition < 0) {
            displacedElement = listOfElement.get(0);
        } else {
            displacedElement = listOfElement.get(targetedPosition);
        }

        int newPositionInList =
                syncedList.getElements().indexOf(displacedElement);
        SyncedListStep newStep =
                new SyncedListStep(syncedListElement.getId(), ACTION.MOVE,
                                   newPositionInList);
        listActivity.addElementStepAndSave(newStep, true);
        recyclerView.scrollToPosition(getPositionOfElement(syncedListElement));
    }
}