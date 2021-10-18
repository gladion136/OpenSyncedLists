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

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.ListActivity;
import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedList;

public class ListsAdapater extends ArrayAdapter<SyncedList> {

    ArrayList<SyncedList> syncedLists;
    Context context;

    public ListsAdapater(@NonNull Context context,
                         int resource,
                         @NonNull
                                 ArrayList<SyncedList> objects) {
        super(context, resource, objects);
        syncedLists = objects;
        this.context = context;
    }

    private static class ViewHolder {
        TextView txtName;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SyncedList dataModel = syncedLists.get(position);

        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.lists_element, parent, false);
            viewHolder.txtName = convertView.findViewById(R.id.textView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.txtName.setText(dataModel.getName());
        viewHolder.txtName.setOnClickListener(v -> {
            Intent intent = new Intent(context, ListActivity.class);
            intent.putExtra("Id", dataModel.getId());
            intent.putExtra("Name", dataModel.getName());
            context.startActivity(intent);
        });
        return convertView;
    }
}