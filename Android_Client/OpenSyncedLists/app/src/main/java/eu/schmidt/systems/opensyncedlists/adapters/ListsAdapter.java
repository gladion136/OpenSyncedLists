package eu.schmidt.systems.opensyncedlists.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.activities.ListActivity;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;

/**
 * ListView Adapter for ListsActivity
 */
public class ListsAdapter extends ArrayAdapter<SyncedListHeader> {

    final ArrayList<SyncedListHeader> syncedListsHeaders;
    final Context context;

    /**
     * Adapter constructor
     *
     * @param context  Context
     * @param resource Resource
     * @param objects  List elements
     */
    public ListsAdapter(@NonNull Context context,
                        int resource,
                        @NonNull ArrayList<SyncedListHeader> objects) {
        super(context, resource, objects);
        syncedListsHeaders = objects;
        this.context = context;
    }

    /**
     * ViewHolder for on list element
     */
    private static class ViewHolder {
        TextView tVName;
        TextView tVSize;
    }

    /**
     * getView for one element in list
     *
     * @param position    current position
     * @param convertView convertView
     * @param parent      parent
     * @return convertView
     */
    @Override public View getView(int position,
                                  View convertView,
                                  ViewGroup parent) {
        SyncedListHeader header = syncedListsHeaders.get(position);

        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView =
                    inflater.inflate(R.layout.element_lists, parent, false);
            viewHolder.tVName = convertView.findViewById(R.id.tVName);
            viewHolder.tVSize = convertView.findViewById(R.id.tVSize);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.tVName.setText(header.getName());
        viewHolder.tVSize.setText(header.getListSize());
        convertView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ListActivity.class);
            intent.putExtra("id", header.getId());
            context.startActivity(intent);
        });
        return convertView;
    }

    /**
     * Update all list elements
     *
     * @param listData new data
     */
    public void updateItems(ArrayList<SyncedListHeader> listData) {
        this.syncedListsHeaders.clear();
        this.syncedListsHeaders.addAll(listData);
        this.notifyDataSetChanged();
    }
}