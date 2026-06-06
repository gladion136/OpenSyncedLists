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
package eu.schmidt.systems.opensyncedlists.adapters;

import static eu.schmidt.systems.opensyncedlists.utils.Constant.LOG_TITLE_DEFAULT;

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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.activities.ListActivity;
import eu.schmidt.systems.opensyncedlists.fragments.ElementEditorFragment;
import eu.schmidt.systems.opensyncedlists.syncedlist.ACTION;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.utils.Constant;

/**
 * RecyclerView Adapter for ListActivity.
 */
public class SyncedListAdapter
    extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements View.OnClickListener, Filterable
{
    private final ListActivity listActivity;
    private final SyncedList syncedList;
    private final RecyclerView recyclerView;
    private final boolean scrollListTopBottom;
    private final int jumpDistance;
    private final float fontScale;
    private final int buttonSizePx;
    private boolean filterActive = false;
    /**
     * Drag/drop controller. Kept as a field so it can be detached from the
     * RecyclerView when this adapter is replaced — otherwise each replacement
     * (e.g. reloading list settings) leaves the previous ItemTouchHelper still
     * attached, and the stacked helpers fight over touch events, breaking
     * drag-and-drop.
     */
    private ItemTouchHelper itemTouchHelper;
    private List<SyncedListElement> syncedListElementsFiltered;
    private Filter syncedListFilter = new Filter()
    {
        @Override
        protected FilterResults performFiltering(CharSequence constraint)
        {
            List<SyncedListElement> filteredList = new ArrayList<>();
            
            if (constraint == null || constraint.length() == 0)
            {
                filteredList.addAll(syncedList.getReformatElements());
                Log.d("Filter", "No filter");
                filterActive = false;
            }
            else
            {
                String filterPattern =
                    constraint.toString().toLowerCase().trim();
                Log.d("Filter", "Filter: " + filterPattern);
                filterActive = true;
                
                for (SyncedListElement item : syncedList.getElements())
                {
                    if (item.getName().toLowerCase().contains(filterPattern)
                        || item.getDescription().toLowerCase()
                        .contains(filterPattern))
                    {
                        filteredList.add(item);
                    }
                }
            }
            
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }
        
        @Override protected void publishResults(CharSequence constraint,
            FilterResults results)
        {
            syncedListElementsFiltered.clear();
            syncedListElementsFiltered.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };
    
    /**
     * Initialize the recyclerview
     *
     * @param listActivity ListActivity
     * @param recyclerView RecyclerView to fill
     * @param syncedList   selected SyncedList
     */
    public SyncedListAdapter(ListActivity listActivity,
        RecyclerView recyclerView, SyncedList syncedList)
    {
        this.listActivity = listActivity;
        this.recyclerView = recyclerView;
        this.syncedList = syncedList;
        syncedListElementsFiltered = syncedList.getReformatElements();
        LayoutInflater.from(listActivity);
        
        // Add ItemTouchHelper for drag and drop events
        itemTouchHelper =
            new ItemTouchHelper(new ItemTouchHelper.Callback()
            {
                @Override public int getMovementFlags(RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder)
                {
                    if (filterActive)
                    {
                        return 0;
                    }
                    
                    if (viewHolder instanceof ElementViewHolder)
                    {
                        return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                            ItemTouchHelper.DOWN | ItemTouchHelper.UP);
                    }
                    else
                    {
                        return 0;
                    }
                }
                
                public boolean onMove(RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder target)
                {
                    if (!(viewHolder instanceof ElementViewHolder)
                        || !(target instanceof ElementViewHolder))
                    {
                        return false;
                    }
                    int fromPosition = viewHolder.getAdapterPosition();
                    int toPosition = target.getAdapterPosition();
                    if (fromPosition == RecyclerView.NO_POSITION
                        || toPosition == RecyclerView.NO_POSITION)
                    {
                        return false;
                    }
                    return handleDragMove(fromPosition, toPosition);
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder,
                    int direction)
                {
                    Log.d(Constant.LOG_TITLE_DEFAULT, "SWIPED: " + direction);
                }
            });
        itemTouchHelper.attachToRecyclerView(recyclerView);
        
        SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(listActivity);
        jumpDistance =
            Integer.parseInt(sharedPreferences.getString("jump_range", "1"));
        scrollListTopBottom = sharedPreferences.getBoolean("scrollList", false);
        fontScale = parseFontScale(
            sharedPreferences.getString("font_size", "1.0"));
        buttonSizePx = Math.round(
            parseButtonSize(sharedPreferences.getString("button_size", "35"))
                * listActivity.getResources().getDisplayMetrics().density);
    }

    /**
     * @return the font-size scale factor this adapter was created with.
     */
    public float getFontScale()
    {
        return fontScale;
    }

    /**
     * Detach this adapter's drag/drop ItemTouchHelper from the RecyclerView.
     * Call before replacing the adapter so a stale helper does not keep
     * intercepting touch events alongside the new adapter's helper.
     */
    public void detachDragHelper()
    {
        if (itemTouchHelper != null)
        {
            itemTouchHelper.attachToRecyclerView(null);
        }
    }

    /**
     * Handle one drag step: move the element at recycler position
     * {@code fromPosition} onto {@code toPosition}, persist the change and
     * animate the swap.
     *
     * This mirrors the original (working) behaviour: each step persists a MOVE
     * (which replays + rebuilds the buffers) and then calls
     * {@code notifyItemMoved(fromPosition, toPosition)}. Rebuilding the buffer
     * per step is what keeps {@code getElementOnPosition}, the buffer and the
     * RecyclerView's adapter-position tracking consistent, so the next onMove
     * reports an updated position.
     *
     * Extracted into a public method so instrumented tests can drive the same
     * code path deterministically.
     *
     * @param fromPosition source recycler position
     * @param toPosition   target recycler position
     * @return true if the elements were reordered
     */
    public boolean handleDragMove(int fromPosition, int toPosition)
    {
        SyncedListElement selectedElement = getElementOnPosition(fromPosition);
        SyncedListElement displacedElement = getElementOnPosition(toPosition);

        // In a checked list, dragging across the checked/unchecked boundary is
        // not allowed.
        if (syncedList.getHeader().isCheckedList()
            && selectedElement.getChecked() != displacedElement.getChecked())
        {
            return false;
        }

        // EXACT master behaviour: persist a MOVE step (which replays + rebuilds
        // the buffers) and notifyItemMoved. Restored to verify the known-good
        // baseline before layering any change on top.
        int newPositionInList =
            syncedList.getElements().indexOf(displacedElement);
        SyncedListStep newStep = new SyncedListStep(selectedElement.getId(),
            ACTION.MOVE, newPositionInList);
        listActivity.addElementStepAndSave(newStep, false);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    /**
     * Parse the stored font-size scale factor, falling back to 1.0 on any
     * malformed value.
     *
     * @param value stored preference value
     * @return scale factor, defaulting to 1.0
     */
    public static float parseFontScale(String value)
    {
        try
        {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException e)
        {
            return 1.0f;
        }
    }

    /**
     * Parse the stored button-size value, falling back to 35dp on malformed
     * input.
     *
     * @param value stored preference value
     * @return button size in dp, defaulting to 35
     */
    public static int parseButtonSize(String value)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return 35;
        }
    }
    
    /**
     * Create and choose correct ViewHolder
     *
     * @param viewGroup viewGroup
     * @param viewType  viewType represents layout id
     * @return viewHolder as IsolatorViewHolder or ElementViewHolder
     */
    @Override public RecyclerView.ViewHolder onCreateViewHolder(
        ViewGroup viewGroup, int viewType)
    {
        RecyclerView.ViewHolder viewHolder;
        
        switch (viewType)
        {
            case R.layout.element_list_invert:
            case R.layout.element_list_overview:
            case R.layout.element_list_overview_invert:
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
    
    /**
     * Replace the content of a view (and override given because it got
     * recycled).
     *
     * @param viewHolder viewHolder to fill with content
     * @param position   current position
     */
    @Override public void onBindViewHolder(RecyclerView.ViewHolder viewHolder,
        final int position)
    {
        if (viewHolder instanceof ElementViewHolder elementViewHolder)
        {
            // remove old listeners (because of recycling old views)
            elementViewHolder.checkBox.setOnCheckedChangeListener(null);
            elementViewHolder.eTName.setOnFocusChangeListener(null);
            elementViewHolder.eTName.setOnEditorActionListener(null);
            
            SyncedListElement currentSyncedListElement =
                getElementOnPosition(position);
            
            if (!syncedList.getHeader().isCheckOption())
            {
                elementViewHolder.checkBox.setVisibility(View.GONE);
            }
            // Toggle the jump buttons directly so both the old wrapped layouts
            // and the new direct layout stay in sync.
            int jumpVisibility = (syncedList.getHeader().isOverviewActive() || !syncedList.getHeader().isJumpButtons())
                ? View.GONE : View.VISIBLE;
            elementViewHolder.iVBtnUp.setVisibility(jumpVisibility);
            elementViewHolder.iVBtnDown.setVisibility(jumpVisibility);
            elementViewHolder.iVTop.setVisibility(jumpVisibility);
            elementViewHolder.iVBottom.setVisibility(jumpVisibility);

            applyFontSize(elementViewHolder);
            applyButtonSize(elementViewHolder);
            
            // name edittext
            elementViewHolder.eTName.setOnFocusChangeListener((v, focused) ->
            {
                if (!focused)
                {
                    checkNameChangesAndSubmit(elementViewHolder,
                        currentSyncedListElement);
                }
            });
            elementViewHolder.eTName.setText(
                currentSyncedListElement.getName());
            elementViewHolder.eTName.setOnEditorActionListener(
                (v, actionId, event) ->
                {
                    if (actionId == EditorInfo.IME_ACTION_DONE)
                    {
                        checkNameChangesAndSubmit(elementViewHolder,
                            currentSyncedListElement);
                        InputMethodManager imm =
                            (InputMethodManager) listActivity.getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        View focus = listActivity.getCurrentFocus();
                        if (focus != null) {
                            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                        }

                        return true;
                    }
                    return false;
                });
            elementViewHolder.tVDescription.setText(
                currentSyncedListElement.getDescription());
            
            // Checkbox
            elementViewHolder.checkBox.setChecked(
                currentSyncedListElement.getChecked());
            // on checked
            elementViewHolder.checkBox.setOnCheckedChangeListener(
                (v, checked) ->
                {
                    SyncedListElement updated =
                        currentSyncedListElement.clone();
                    updated.setChecked(checked);
                    SyncedListStep newStep =
                        new SyncedListStep(currentSyncedListElement.getId(),
                            ACTION.UPDATE, updated);
                    listActivity.addElementStepAndSave(newStep,
                        syncedList.getHeader().isCheckedList());
                });
            
            // on move to top
            elementViewHolder.iVTop.setOnClickListener(vi ->
            {
                SyncedListStep newStep =
                    new SyncedListStep(currentSyncedListElement.getId(),
                        ACTION.MOVE, 0);
                listActivity.addElementStepAndSave(newStep, true);
                if (scrollListTopBottom)
                {
                    recyclerView.scrollToPosition(0);
                }
            });
            
            // on move to bottom
            elementViewHolder.iVBottom.setOnClickListener(vi ->
            {
                SyncedListStep newStep =
                    new SyncedListStep(currentSyncedListElement.getId(),
                        ACTION.MOVE, syncedList.getElements().size() - 1);
                listActivity.addElementStepAndSave(newStep, true);
                if (scrollListTopBottom)
                {
                    recyclerView.scrollToPosition(
                        getPositionOfElement(currentSyncedListElement));
                }
            });
            
            // on move up
            elementViewHolder.iVBtnUp.setOnClickListener(
                vi -> moveElementAndSave(currentSyncedListElement,
                    -jumpDistance));
            
            // on move down
            elementViewHolder.iVBtnDown.setOnClickListener(
                vi -> moveElementAndSave(currentSyncedListElement,
                    jumpDistance));

            // overview 3-dot menu: open the same editor as clicking the row
            if (elementViewHolder.iVOverviewMenu != null)
            {
                elementViewHolder.iVOverviewMenu.setOnClickListener(
                    vi -> openElementEditor(currentSyncedListElement));
            }
        }
    }
    
    /**
     * Get correct viewType (layout) for one element.
     *
     * @param position Position of the element
     * @return viewType represents layout id
     */
    @Override public int getItemViewType(int position)
    {
        if (syncedList.getHeader().isCheckedList()
            && syncedList.getUncheckedElements().size() == position)
        {
            return R.layout.element_list_isolator;
        }
        else
        {
            if (syncedList.getHeader().isOverviewActive())
            {
                return syncedList.getHeader().isInvertElement()
                    ? R.layout.element_list_overview_invert
                    : R.layout.element_list_overview;
            }
            return syncedList.getHeader().isInvertElement()
                ? R.layout.element_list_invert : R.layout.element_list;
        }
    }
    
    /**
     * Return the size of elements in the list
     *
     * @return size of elements
     */
    @Override public int getItemCount()
    {
        if (filterActive)
        {
            return syncedListElementsFiltered.size();
        }
        if (syncedList.getHeader().isCheckedList()
            && syncedList.getCheckedElements().size() > 0)
        {
            return syncedList.getElements().size() + 1; // + Isolator
        }
        else
        {
            return syncedList.getElements().size();
        }
    }
    
    /**
     * OnClick on a element
     *
     * @param view View of Element
     */
    @Override public void onClick(View view)
    {
        int itemPosition = recyclerView.getChildLayoutPosition(view);
        openElementEditor(getElementOnPosition(itemPosition));
    }

    /**
     * Open the bottom sheet element editor for one element. Used both by a
     * click on the whole row and by the overview 3-dot menu button.
     *
     * @param syncedListElement element to edit
     */
    public void openElementEditor(SyncedListElement syncedListElement)
    {
        BottomSheetDialogFragment bottomSheetDialogFragment =
            new ElementEditorFragment().newInstance(syncedListElement,
                syncedListStep -> listActivity.addElementStepAndSave(
                    syncedListStep, true));
        bottomSheetDialogFragment.show(listActivity.getSupportFragmentManager(),
            bottomSheetDialogFragment.getTag());
    }
    
    @Override public Filter getFilter()
    {
        return syncedListFilter;
    }
    
    /**
     * Get element by position inside recyclerview.
     *
     * @param position position in recyclerview.
     * @return syncedListElement on this position
     */
    public SyncedListElement getElementOnPosition(int position)
    {
        if (filterActive)
        {
            return syncedListElementsFiltered.get(position);
        }
        if (syncedList.getHeader().isCheckedList())
        {
            if (position > syncedList.getUncheckedElements().size())
            {
                return syncedList.getCheckedElements().get(
                    position - syncedList.getUncheckedElements().size() - 1);
            }
            else
            {
                return syncedList.getUncheckedElements().get(position);
            }
        }
        else
        {
            return syncedList.getElements().get(position);
        }
    }
    
    /**
     * Check if element name changed and submit (create new SyncedListStep).
     *
     * @param viewHolder        viewHolder of element
     * @param syncedListElement the buffered element
     */
    public void checkNameChangesAndSubmit(ElementViewHolder viewHolder,
        SyncedListElement syncedListElement)
    {
        String newName = viewHolder.eTName.getText().toString();
        if (!newName.equals("") && !newName.equals(syncedListElement.getName()))
        {
            SyncedListElement updated = syncedListElement.clone();
            updated.setName(newName);
            SyncedListStep newStep =
                new SyncedListStep(updated.getId(), ACTION.UPDATE, updated);
            listActivity.addElementStepAndSave(newStep, true);
        }
    }
    
    /**
     * Get position of element inside recyclerview.
     *
     * @param syncedListElement element to search
     * @return position in recyclerview
     */
    public int getPositionOfElement(SyncedListElement syncedListElement)
    {
        int position = 0;
        if (syncedList.getHeader().isCheckedList())
        {
            for (int i = 0; i < syncedList.getUncheckedElements().size(); i++)
            {
                if (syncedListElement.getId()
                    .equals(syncedList.getUncheckedElements().get(i).getId()))
                {
                    return position;
                }
                position++;
            }
            position++; // Isolator
            for (int i = 0; i < syncedList.getCheckedElements().size(); i++)
            {
                if (syncedListElement.getId()
                    .equals(syncedList.getCheckedElements().get(i).getId()))
                {
                    return position;
                }
                position++;
            }
        }
        else
        {
            for (int i = 0; i < syncedList.getElements().size(); i++)
            {
                if (syncedListElement.getId()
                    .equals(syncedList.getElements().get(i).getId()))
                {
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
     * Move element inside list (checked and unchecked list separated), map to
     * limits. Move inside SyncedList order, Counting inside Recyclerview order
     *
     * @param syncedListElement Element to move
     * @param dir               direction and strength, positive = down
     */
    public void moveElementAndSave(SyncedListElement syncedListElement, int dir)
    {
        if (filterActive)
        {
            return;
        }
        SyncedListElement displacedElement;
        
        // Choose list where the element should move
        ArrayList<SyncedListElement> listOfElement;
        if (!syncedList.getHeader().isCheckedList())
        {
            listOfElement = syncedList.getElements();
        }
        else if (syncedListElement.getChecked())
        {
            listOfElement = syncedList.getCheckedElements();
        }
        else
        {
            listOfElement = syncedList.getUncheckedElements();
        }
        int targetedPosition = listOfElement.indexOf(syncedListElement) + dir;
        
        if (targetedPosition >= listOfElement.size())
        {
            displacedElement = listOfElement.get(listOfElement.size() - 1);
        }
        else if (targetedPosition < 0)
        {
            displacedElement = listOfElement.get(0);
        }
        else
        {
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
    
    /**
     * Apply the configured font-size scale to an element's text views.
     *
     * In overview mode the row is meant to be compact, so a smaller font also
     * shrinks the row's minimum height. The checkbox and jump buttons are
     * controlled by the separate button-size preference.
     *
     * @param holder element view holder to adjust
     */
    private void applyFontSize(ElementViewHolder holder)
    {
        boolean overview = syncedList.getHeader().isOverviewActive();

        // Base text sizes (sp) mirror the values defined in the layouts.
        float titleBaseSp = overview ? 18f : 20f;
        float descriptionBaseSp = 14f;

        holder.eTName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,
            titleBaseSp * fontScale);
        holder.tVDescription.setTextSize(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            descriptionBaseSp * fontScale);
    }

    /**
     * Apply the configured button size to the checkbox and jump buttons.
     *
     * @param holder element view holder to adjust
     */
    private void applyButtonSize(ElementViewHolder holder)
    {
        applyButtonLayout(holder.iVBtnUp);
        applyButtonLayout(holder.iVBtnDown);
        applyButtonLayout(holder.iVTop);
        applyButtonLayout(holder.iVBottom);
    }

    /**
     * Apply the configured size to one individual jump button.
     *
     * @param button jump button image view
     */
    private void applyButtonLayout(ImageView button)
    {
        ViewGroup.LayoutParams params = button.getLayoutParams();
        if (params != null)
        {
            params.width = buttonSizePx;
            params.height = buttonSizePx;
            button.setLayoutParams(params);
        }
    }

    /**
     * ViewHolder for one element.
     */
    public static class ElementViewHolder extends RecyclerView.ViewHolder
    {
        public final CheckBox checkBox;
        public final EditText eTName;
        public final TextView tVDescription;
        public final ImageView iVBtnUp, iVBtnDown, iVTop, iVBottom;
        /** Overview-only 3-dot menu button; null in non-overview layouts. */
        public final ImageView iVOverviewMenu;
        /** Only present in the overview layouts; null otherwise. */
        public final View elementRow;

        public ElementViewHolder(View view)
        {
            super(view);
            checkBox = view.findViewById(R.id.checkBox);
            eTName = view.findViewById(R.id.eTTitle);
            tVDescription = view.findViewById(R.id.tVDescription);
            iVBtnUp = view.findViewById(R.id.btnJumpUp);
            iVBtnDown = view.findViewById(R.id.btnJumpDown);
            iVTop = view.findViewById(R.id.btnJumpTop);
            iVBottom = view.findViewById(R.id.btnJumpBottom);
            iVOverviewMenu = view.findViewById(R.id.btnOverviewMenu);
            elementRow = view.findViewById(R.id.elementRow);
        }
    }
    
    /**
     * ViewHolder for the isolator. (Isolate checked and unchecked elements)
     */
    public static class IsolatorViewHolder extends RecyclerView.ViewHolder
    {
        public IsolatorViewHolder(View view)
        {
            super(view);
        }
    }
}
