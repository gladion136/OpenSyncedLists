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

import static android.view.View.INVISIBLE;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog;
import com.github.dhaval2404.colorpicker.listener.ColorListener;
import com.github.dhaval2404.colorpicker.model.ColorShape;
import com.github.dhaval2404.colorpicker.model.ColorSwatch;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.activities.ListsActivity;
import eu.schmidt.systems.opensyncedlists.syncedlist.ListTag;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedList;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.utils.Constant;
import eu.schmidt.systems.opensyncedlists.utils.DialogBuilder;

/**
 * ListView Adapter for ListsActivity.
 */
public class TagsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    public final ArrayList<ListTag> tagList;
    final ListsActivity listsActivity;
    
    public TagsAdapter(ListsActivity listsActivity, ArrayList<ListTag> tagList,
        RecyclerView recyclerView)
    {
        this.listsActivity = listsActivity;
        this.tagList = tagList;
        LayoutInflater.from(listsActivity);
        
        // Add ItemTouchHelper for drag and drop events
        ItemTouchHelper itemTouchHelper =
            new ItemTouchHelper(new ItemTouchHelper.Callback()
            {
                @Override public int getMovementFlags(RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder)
                {
                    if (((ViewHolder) viewHolder).listTag.untagged)
                    {
                        return makeFlag(ItemTouchHelper.ACTION_STATE_IDLE,
                            ItemTouchHelper.ACTION_STATE_IDLE);
                    }
                    return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                        ItemTouchHelper.DOWN | ItemTouchHelper.UP
                            | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                }
                
                public boolean onMove(RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder target)
                {
                    int fromPosition = viewHolder.getAdapterPosition();
                    int toPosition = target.getAdapterPosition();
                    if (toPosition == 0)
                    {
                        return false;
                    }
                    
                    Collections.swap(tagList, fromPosition, toPosition);
                    
                    try
                    {
                        listsActivity.secureStorage.saveAllTags(tagList, false);
                    }
                    catch (Exception e)
                    {
                        Log.e("TagsAdapter", "Error saving tags: " + e);
                    }
                    
                    notifyItemMoved(fromPosition, toPosition);
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
        if (viewType == R.layout.nav_drawer_element)
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
    
    @SuppressLint("NotifyDataSetChanged") @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
        int position)
    {
        if (holder instanceof ViewHolder viewHolder)
        {
            ListTag listTag = tagList.get(position);
            if (listTag == null)
            {
                return;
            }
            if (listTag.untagged)
            {
                viewHolder.imgBtnTagMenu.setVisibility(INVISIBLE);
            }
            viewHolder.listTag = listTag;
            viewHolder.tVName.setText(listTag.name);
            viewHolder.cBoxFilterTag.setChecked(listTag.filterEnabled);
            viewHolder.cBoxFilterTag.setOnClickListener(v ->
            {
                listTag.filterEnabled = viewHolder.cBoxFilterTag.isChecked();
                try
                {
                    listsActivity.secureStorage.saveAllTags(tagList, false);
                }
                catch (Exception e)
                {
                    Log.e("TagsAdapater", "Error saving tags: " + e);
                }
                
                listsActivity.listsAdapter.getFilter().filter(null);
            });
            viewHolder.imgBtnTagMenu.setOnClickListener(v ->
            {
                Log.d("TagsAdapater",
                    "Clicked on menu button for tag: " + listTag.name);
                
                PopupMenu popup = new PopupMenu(viewHolder.view.getContext(),
                    viewHolder.view);
                popup.getMenuInflater()
                    .inflate(R.menu.edit_tag_menu, popup.getMenu());
                
                popup.setOnMenuItemClickListener(item ->
                {
                    switch (item.getItemId())
                    {
                        case R.id.remove_tag:
                            if (!listTag.untagged)
                            {
                                tagList.remove(listTag);
                                try
                                {
                                    listsActivity.secureStorage.saveAllTags(
                                        tagList, false);
                                }
                                catch (Exception e)
                                {
                                    Log.e("TagsAdapater",
                                        "Error saving tags: " + e);
                                }
                                // Remove from attached lists
                                try
                                {
                                    for (SyncedListHeader listHeader :
                                        listsActivity.secureStorage.getListsHeaders())
                                    {
                                        
                                        if (listHeader.getTagList().stream()
                                            .anyMatch(t -> t.name.equals(
                                                listTag.name)))
                                        {
                                            SyncedList syncedList =
                                                listsActivity.secureStorage.getList(
                                                    listHeader.getId());
                                            syncedList.getHeader().getTagList()
                                                .removeIf(t -> t.name.equals(
                                                    listTag.name));
                                            listsActivity.secureStorage.setList(
                                                syncedList);
                                        }
                                    }
                                    
                                    this.listsActivity.listsAdapter.updateItems(
                                        listsActivity.secureStorage.getListsHeaders(),
                                        true);
                                }
                                catch (Exception e)
                                {
                                    Log.e("TagsAdapater",
                                        "Error removing tag from lists: " + e);
                                }
                                
                                this.notifyDataSetChanged();
                            }
                            return true;
                        case R.id.rename_tag:
                            if (!listTag.untagged)
                            {
                                Log.d("TagsAdapater",
                                    "Clicked on rename tag: " + listTag.name);
                                DialogBuilder.Callback callback =
                                    new DialogBuilder.Callback()
                                    {
                                        @Override
                                        public void callback(String result)
                                        {
                                            
                                            if (result != null)
                                            {
                                                if (result.equals(""))
                                                {
                                                    Toast.makeText(
                                                            listsActivity,
                                                            listsActivity.getString(
                                                                R.string.no_name_entered),
                                                            Toast.LENGTH_SHORT)
                                                        .show();
                                                    return;
                                                }
                                                String old_name = listTag.name;
                                                listTag.name = result;
                                                // Update all attached lists
                                                try
                                                {
                                                    for (SyncedListHeader listHeader : listsActivity.secureStorage.getListsHeaders())
                                                    {
                                                        if (listHeader.getTagList()
                                                            .stream().anyMatch(
                                                                t -> t.name.equals(
                                                                    old_name)))
                                                        {
                                                            SyncedList
                                                                syncedList =
                                                                listsActivity.secureStorage.getList(
                                                                    listHeader.getId());
                                                            syncedList.getHeader()
                                                                .getTagList()
                                                                .replaceAll(t ->
                                                                {
                                                                    if (t.name.equals(
                                                                        old_name))
                                                                    {
                                                                        t.name =
                                                                            result;
                                                                    }
                                                                    return t;
                                                                });
                                                            listsActivity.secureStorage.setList(
                                                                syncedList);
                                                        }
                                                    }
                                                    
                                                    listsActivity.secureStorage.saveAllTags(
                                                        tagList, false);
                                                    listsActivity.listsAdapter.updateItems(
                                                        listsActivity.secureStorage.getListsHeaders(),
                                                        true);
                                                }
                                                catch (Exception e)
                                                {
                                                    Log.e("TagsAdapater",
                                                        "Error renaming tag "
                                                            + "in lists: " + e);
                                                }
                                                TagsAdapter.this.notifyDataSetChanged();
                                            }
                                        }
                                    };
                                DialogBuilder.editTextDialog(listsActivity,
                                    listTag.name, listsActivity.getString(
                                        R.string.rename_tag_dialog_title),
                                    listsActivity.getString(
                                        R.string.rename_tag_dialog_message),
                                    listsActivity.getString(
                                        R.string.rename_tag_dialog_yes_option),
                                    listsActivity.getString(
                                        R.string.rename_tag_dialog_no_option),
                                    callback);
                            }
                            return true;
                        case R.id.change_color:
                            Log.d("TagsAdapater",
                                "Clicked on change color tag: " + listTag.name);
                            // TODO: Implement color change
                            new MaterialColorPickerDialog.Builder(
                                listsActivity).setTitle(listsActivity.getString(
                                    R.string.change_tag_color_picker_title))
                                .setColorShape(ColorShape.SQAURE)
                                .setColorSwatch(ColorSwatch._300)
                                .setColorListener(new ColorListener()
                                {
                                    @Override
                                    public void onColorSelected(int color,
                                        @NotNull String colorHex)
                                    {
                                        // Handle Color Selection
                                        Log.d("TagsAdapater",
                                            "Set new tag color: " + colorHex);
                                        listTag.colorHex = colorHex;
                                        try
                                        {
                                            listsActivity.secureStorage.saveAllTags(
                                                tagList, true);
                                        }
                                        catch (Exception e)
                                        {
                                            Log.e("TagsAdapater",
                                                "Error saving tags: " + e);
                                        }
                                        
                                        try
                                        {
                                            listsActivity.listsAdapter.updateItems(
                                                listsActivity.secureStorage.getListsHeaders(),
                                                true);
                                        }
                                        catch (Exception e)
                                        {
                                            Log.e("TagsAdapater",
                                                "Error updating list after "
                                                    + "color change: " + e);
                                        }
                                        
                                        TagsAdapter.this.notifyDataSetChanged();
                                    }
                                }).show();
                        default:
                            return false;
                    }
                });
                popup.show();
            });
        }
    }
    
    @Override public int getItemViewType(int position)
    {
        return R.layout.nav_drawer_element;
    }
    
    @Override public int getItemCount()
    {
        return tagList.size();
    }
    
    /**
     * Update all list elements.
     *
     * @param listData updated elements
     */
    public void updateItems(ArrayList<ListTag> listData, boolean notify)
    {
        this.tagList.clear();
        this.tagList.addAll(listData);
        Log.d(Constant.LOG_TITLE_DEFAULT, "Update Elements: " + tagList.size());
        if (notify)
        {
            this.notifyDataSetChanged();
        }
    }
    
    public void addTag(ListTag listTag) throws Exception
    {
        if (tagList.stream().noneMatch(t -> t.name.equals(listTag.name)))
        {
            tagList.add(listTag);
            notifyItemInserted(tagList.size() - 1);
        }
        else
        {
            Log.d(Constant.LOG_TITLE_DEFAULT,
                "Tag already exists: " + listTag.name);
            throw new Exception("Tag already exists: " + listTag.name);
        }
    }
    
    public ArrayList<ListTag> getFilterTagList()
    {
        ArrayList<ListTag> filteredTagList = new ArrayList<>();
        for (int i = 0; i < tagList.size(); i++)
        {
            if (tagList.get(i).filterEnabled)
            {
                filteredTagList.add(tagList.get(i));
            }
        }
        return filteredTagList;
    }
    
    /**
     * ViewHolder for one list element.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public final View view;
        public final TextView tVName;
        public final ImageButton imgBtnTagMenu;
        public final CheckBox cBoxFilterTag;
        public ListTag listTag;
        
        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            view = itemView;
            tVName = itemView.findViewById(R.id.txtViewTagName);
            imgBtnTagMenu = itemView.findViewById(R.id.imgBtnTagMenu);
            cBoxFilterTag = itemView.findViewById(R.id.cBoxTagFilter);
        }
    }
}