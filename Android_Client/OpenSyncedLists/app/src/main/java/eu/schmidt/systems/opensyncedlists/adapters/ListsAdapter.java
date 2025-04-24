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

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.activities.ListActivity;
import eu.schmidt.systems.opensyncedlists.activities.ListsActivity;
import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.syncedlist.ListTag;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.utils.Constant;

/**
 * ListView Adapter for ListsActivity.
 */
public class ListsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements Filterable
{
    final ArrayList<SyncedListHeader> syncedListsHeaders;
    final ArrayList<SyncedListHeader> syncedListsHeadersFiltered;
    final ListsActivity listsActivity;
    
    public ListsAdapter(ListsActivity listsActivity,
        ArrayList<SyncedListHeader> syncedListsHeaders,
        RecyclerView recyclerView)
    {
        this.listsActivity = listsActivity;
        this.syncedListsHeaders = syncedListsHeaders;
        this.syncedListsHeadersFiltered = new ArrayList<>();
        LayoutInflater.from(listsActivity);
        
        // Add ItemTouchHelper for drag and drop events
        ItemTouchHelper itemTouchHelper =
            new ItemTouchHelper(new ItemTouchHelper.Callback()
            {
                @Override public int getMovementFlags(RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder)
                {
                    return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                        ItemTouchHelper.DOWN | ItemTouchHelper.UP
                            | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                }
                
                public boolean onMove(RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder target)
                {
                    SecureStorage secureStorage =
                        new SecureStorage(listsActivity);
                    int fromPosition = viewHolder.getAdapterPosition();
                    int toPosition = target.getAdapterPosition();
                    try
                    {
                        
                        ArrayList<String> ids = secureStorage.getListsIds();
                        if (fromPosition < toPosition)
                        {
                            for (int i = fromPosition; i < toPosition; i++)
                            {
                                Collections.swap(ids, i, i + 1);
                            }
                        }
                        else
                        {
                            for (int i = fromPosition; i > toPosition; i--)
                            {
                                Collections.swap(ids, i, i - 1);
                            }
                        }
                        secureStorage.setListsIds(ids);
                        notifyItemMoved(fromPosition, toPosition);
                        return true;
                    }
                    catch (Exception exception)
                    {
                        exception.printStackTrace();
                    }
                    return false;
                }
                
                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder,
                    int direction)
                {
                    Log.d(Constant.LOG_TITLE_DEFAULT, "SWIPED: " + direction);
                }
            });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }
    
    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
        int viewType)
    {
        RecyclerView.ViewHolder viewHolder;
        if (viewType == R.layout.element_lists
            || viewType == R.layout.element_lists_small_height)
        {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
            viewHolder = new ViewHolder(view);
        }
        else
        {
            throw new IllegalStateException("Unexpected value: " + viewType);
        }
        return viewHolder;
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
        int position)
    {
        if (holder instanceof ViewHolder viewHolder)
        {
            SyncedListHeader header =
                filterActive ? syncedListsHeadersFiltered.get(position)
                    : syncedListsHeaders.get(position);
            
            viewHolder.tVName.setText(header.getName());
            viewHolder.tVSize.setText(header.getListSize());
            viewHolder.imgBtnListMenu.setOnClickListener(v ->
            {
                listsActivity.showListMenu(v, header);
            });
            viewHolder.view.setOnClickListener(v ->
            {
                Intent intent = new Intent(listsActivity, ListActivity.class);
                intent.putExtra("id", header.getId());
                listsActivity.startActivity(intent);
            });
            
            viewHolder.chipGrpTags.removeAllViews();
            
            if (header.getTagList().isEmpty())
            {
                return;
            }
            ListTag firstTag = header.getTagList().get(0);
            try
            {
                ListTag finalFirstTag = firstTag;
                Optional<ListTag> firstTagFromGlobalStorage =
                    listsActivity.secureStorage.getAllTags().stream()
                        .filter(t -> finalFirstTag.name.equals(t.name))
                        .findFirst();
                if (firstTagFromGlobalStorage.isPresent())
                {
                    firstTag = firstTagFromGlobalStorage.get();
                }
            }
            catch (Exception e)
            {
                Log.e("TAG", "Error getting tags: " + e);
            }
            if (firstTag != null)
            {
                Chip chip = new Chip(listsActivity);
                chip.setText(firstTag.name);
                chip.setCloseIconVisible(false);
                chip.setClickable(false);
                chip.setCheckable(false);
                chip.setChipBackgroundColor(getChipColor(firstTag.colorHex));
                Log.d("TAG",
                    "Tag '" + firstTag.name + "' color: " + firstTag.colorHex);
                viewHolder.chipGrpTags.addView(chip);
                
                if (header.getTagList().size() > 1)
                {
                    Chip chip_placeholder = new Chip(listsActivity);
                    chip_placeholder.setText("...");
                    chip_placeholder.setCloseIconVisible(false);
                    chip_placeholder.setClickable(false);
                    chip_placeholder.setCheckable(false);
                    viewHolder.chipGrpTags.addView(chip_placeholder);
                }
            }
        }
    }
    
    public ColorStateList getChipColor(String colorHex)
    {
        int color = Color.parseColor(colorHex);
        
        // Hintergrundfarbe
        int[][] states =
            new int[][]{new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{-android.R.attr.state_enabled}, // disabled
                new int[]{-android.R.attr.state_checked}, // unchecked
                new int[]{android.R.attr.state_pressed}  // pressed
            };
        
        int[] colors = new int[]{color,
            ContextCompat.getColor(listsActivity, android.R.color.darker_gray),
            //disabled color
            color, color};
        
        return new ColorStateList(states, colors);
    }
    
    @Override public int getItemViewType(int position)
    {
        // Check if the list is small height
        if (listsActivity.globalSharedPreferences.getBoolean(
            "list_overview_instead_cards", false))
        {
            return R.layout.element_lists_small_height;
        }
        return R.layout.element_lists;
    }
    
    @Override public int getItemCount()
    {
        if (filterActive)
        {
            return syncedListsHeadersFiltered.size();
        }
        return syncedListsHeaders.size();
    }
    
    /**
     * Update all list elements.
     *
     * @param listData updated elements
     */
    public void updateItems(ArrayList<SyncedListHeader> listData,
        boolean notify)
    {
        this.syncedListsHeaders.clear();
        this.syncedListsHeaders.addAll(listData);
        getFilter().filter(null);
        Log.d(Constant.LOG_TITLE_DEFAULT,
            "Update Elements: " + syncedListsHeaders.size());
        if (notify)
        {
            this.notifyDataSetChanged();
        }
    }
    
    /**
     * ViewHolder for one list element.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public final View view;
        public final TextView tVName;
        public final TextView tVSize;
        public final ImageButton imgBtnListMenu;
        public final ChipGroup chipGrpTags;
        
        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            view = itemView;
            tVName = itemView.findViewById(R.id.tVName);
            tVSize = itemView.findViewById(R.id.tVSize);
            imgBtnListMenu = itemView.findViewById(R.id.imgBtnListMenu);
            chipGrpTags = itemView.findViewById(R.id.chipGrpTags);
        }
    }
    
    @Override public Filter getFilter()
    {
        return syncedListFilter;
    }
    
    Boolean filterActive = false;
    
    private Filter syncedListFilter = new Filter()
    {
        @Override
        protected FilterResults performFiltering(CharSequence constraint)
        {
            ArrayList<SyncedListHeader> filteredList = new ArrayList<>();
            
            ArrayList<ListTag> activeTags = new ArrayList<>();
            try
            {
                for (ListTag tag : listsActivity.secureStorage.getAllTags())
                {
                    if (tag.filterEnabled)
                    {
                        activeTags.add(tag);
                    }
                }
            }
            catch (Exception e)
            {
                Log.e("TAG", "Error getting tags: " + e);
            }
            for (ListTag tag : activeTags)
            {
                Log.d("Filter active:", "Tag: " + tag.name);
            }
            if (activeTags.size() <= 0)
            {
                filteredList.addAll(syncedListsHeaders);
                Log.d("Filter", "No filter");
                filterActive = false;
            }
            else
            {
                filterActive = true;
                
                for (SyncedListHeader item : syncedListsHeaders)
                {
                    for (ListTag tag_s : activeTags)
                    {
                        ArrayList<ListTag> tag_list_of_item = item.getTagList();
                        if (tag_list_of_item.isEmpty() && tag_s.untagged)
                        {
                            filteredList.add(item);
                        }
                        else if (tag_list_of_item.stream()
                            .anyMatch(tag_m -> tag_m.name.equals(tag_s.name)))
                        
                        {
                            filteredList.add(item);
                        }
                    }
                }
            }
            
            // Remove duplicated
            ArrayList<SyncedListHeader> filteredListNoDuplicates =
                new ArrayList<>();
            for (SyncedListHeader item : filteredList)
            {
                if (filteredListNoDuplicates.stream()
                    .noneMatch(item2 -> item2.getId().equals(item.getId())))
                {
                    filteredListNoDuplicates.add(item);
                }
            }
            Log.d("Filter",
                "Filtered list size: " + filteredListNoDuplicates.size());
            Log.d("Filter", "Original list size: " + filteredList.size());
            Log.d("Filter", "All list size: " + syncedListsHeaders.size());
            FilterResults results = new FilterResults();
            results.values = filteredListNoDuplicates;
            results.count = filteredListNoDuplicates.size();
            return results;
        }
        
        @Override protected void publishResults(CharSequence constraint,
            FilterResults results)
        {
            syncedListsHeadersFiltered.clear();
            try
            {
                if (results.values instanceof ArrayList)
                {
                    syncedListsHeadersFiltered.addAll(
                        (ArrayList) results.values);
                }
                else
                {
                    throw new Exception("Results are not an ArrayList");
                }
            }
            catch (Exception e)
            {
                Log.e("Filter", "Error casting results: " + e);
                syncedListsHeadersFiltered.addAll(syncedListsHeaders);
            }
            
            notifyDataSetChanged();
        }
    };
}