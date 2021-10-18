package eu.schmidt.systems.opensyncedlists.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.datatypes.ACTION;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListStep;

public abstract class SyncedListAdapter extends
        RecyclerView.Adapter<SyncedListAdapter.ViewHolder> {
    private RecyclerView recyclerView;
    private ArrayList<SyncedListElement> listData;
    private LayoutInflater layoutInflater;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final EditText uName;
        private final TextView uDescription;
        private final ImageView uBtnUp, uBtnDown;
        private final Button uBtnTop, uBtnBottom;

        public ViewHolder(View view) {
            super(view);
            uName = view.findViewById(R.id.eTTitle);
            uDescription = view.findViewById(R.id.tVDescription);
            uBtnUp = view.findViewById(R.id.btnJumpUp);
            uBtnDown = view.findViewById(R.id.btnJumpDown);
            uBtnTop = view.findViewById(R.id.btnJumpTop);
            uBtnBottom = view.findViewById(R.id.btnJumpBottom);
        }

        public TextView getuName() {
            return uName;
        }

        public TextView getuDescription() {
            return uDescription;
        }

        public ImageView getuBtnUp() {
            return uBtnUp;
        }

        public ImageView getuBtnDown() {
            return uBtnDown;
        }

        public Button getuBtnTop() {
            return uBtnTop;
        }

        public Button getuBtnBottom() {
            return uBtnBottom;
        }
    }


    public SyncedListAdapter(Context aContext,
                             ArrayList<SyncedListElement> listData,
                             RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.listData = listData;
        layoutInflater = LayoutInflater.from(aContext);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return listData.size();
    }


    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_element, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getuName().setText(listData.get(position).getName());
        viewHolder.getuDescription().setText(listData.get(position).getDescription());
        viewHolder.getuBtnTop().setOnClickListener(vi -> {
            SyncedListStep newStep =
                    new SyncedListStep(listData.get(position).getId(),
                            ACTION.SWAP, listData.get(0).getId());
            onAddStep(newStep);
            recyclerView.scrollToPosition(0);
        });
        viewHolder.getuBtnBottom().setOnClickListener(vi -> {
            SyncedListStep newStep =
                    new SyncedListStep(listData.get(position).getId(),
                            ACTION.SWAP,
                            listData.get(listData.size() - 1).getId());
            onAddStep(newStep);
            recyclerView.scrollToPosition(listData.size() - 1);
        });

        viewHolder.getuBtnUp().setOnClickListener(vi -> {
            if(position > 0)
            {
                SyncedListStep newStep =
                        new SyncedListStep(listData.get(position).getId(),
                                ACTION.SWAP,
                                listData.get(position - 1).getId());
                onAddStep(newStep);
                recyclerView.scrollToPosition(position - 4);
            }
        });
        viewHolder.getuBtnDown().setOnClickListener(vi -> {
            if(position < listData.size() - 1)
            {
                SyncedListStep newStep =
                        new SyncedListStep(listData.get(position).getId(),
                                ACTION.SWAP,
                                listData.get(position + 1).getId());
                onAddStep(newStep);
                recyclerView.scrollToPosition(position - 2);
            }
        });
    }

    public abstract void onAddStep(SyncedListStep syncedListStep);
}