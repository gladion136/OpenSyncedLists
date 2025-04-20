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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.activities.ListActivity;
import eu.schmidt.systems.opensyncedlists.activities.ListsActivity;
import eu.schmidt.systems.opensyncedlists.storages.SecureStorage;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;
import eu.schmidt.systems.opensyncedlists.utils.Constant;

/**
 * ListView Adapter for ListsActivity.
 */
public class ListsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    final ArrayList<SyncedListHeader> syncedListsHeaders;
    final ListsActivity listsActivity;
    
    public ListsAdapter(ListsActivity listsActivity,
        ArrayList<SyncedListHeader> syncedListsHeaders,
        RecyclerView recyclerView)
    {
        this.listsActivity = listsActivity;
        this.syncedListsHeaders = syncedListsHeaders;
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
            SyncedListHeader header = syncedListsHeaders.get(position);
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
        }
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
        
        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            view = itemView;
            tVName = itemView.findViewById(R.id.tVName);
            tVSize = itemView.findViewById(R.id.tVSize);
            imgBtnListMenu = itemView.findViewById(R.id.imgBtnListMenu);
        }
    }
}