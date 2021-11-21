package eu.schmidt.systems.opensyncedlists.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.json.JSONException;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.ListActivity;
import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListHeader;

/**
 * ListView Adapter for ListsActivity
 */
public class ListsAdapter extends ArrayAdapter<SyncedListHeader> {

    ArrayList<SyncedListHeader> syncedListsHeaders;
    Context context;

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
        TextView txtName;
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
        SyncedListHeader dataModel = syncedListsHeaders.get(position);

        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView =
                    inflater.inflate(R.layout.lists_element, parent, false);
            viewHolder.txtName = convertView.findViewById(R.id.textView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.txtName.setText(dataModel.getName());
        viewHolder.txtName.setOnClickListener(v -> {
            Intent intent = new Intent(context, ListActivity.class);
            try {
                intent.putExtra("header", dataModel.toJSON().toString());
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
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