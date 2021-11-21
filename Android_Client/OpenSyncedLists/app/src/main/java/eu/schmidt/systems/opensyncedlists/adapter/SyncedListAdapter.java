package eu.schmidt.systems.opensyncedlists.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.R;
import eu.schmidt.systems.opensyncedlists.datatypes.ACTION;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListStep;
import eu.schmidt.systems.opensyncedlists.utils.Constant;

/**
 * RecyclerView Adapter for ListActivity
 */
public abstract class SyncedListAdapter
        extends RecyclerView.Adapter<SyncedListAdapter.ViewHolder> {
    private Context context;
    private RecyclerView recyclerView;
    private ArrayList<SyncedListElement> listData;
    private LayoutInflater layoutInflater;

    /**
     * ViewHolder for on element
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final CheckBox checkBox;
        public final EditText eTName;
        public final TextView tVDescription;
        public final ImageView iVBtnUp, iVBtnDown;
        public final Button btnTop, btnBottom;

        public ViewHolder(View view) {
            super(view);
            checkBox = view.findViewById(R.id.checkBox);
            eTName = view.findViewById(R.id.eTTitle);
            tVDescription = view.findViewById(R.id.tVDescription);
            iVBtnUp = view.findViewById(R.id.btnJumpUp);
            iVBtnDown = view.findViewById(R.id.btnJumpDown);
            btnTop = view.findViewById(R.id.btnJumpTop);
            btnBottom = view.findViewById(R.id.btnJumpBottom);
        }
    }

    /**
     * Adapter constructor
     *
     * @param context      Context
     * @param listData     list elements to display
     * @param recyclerView recyclerview which is connected to this adapter
     */
    public SyncedListAdapter(Context context,
                             ArrayList<SyncedListElement> listData,
                             RecyclerView recyclerView) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.listData = listData;
        layoutInflater = LayoutInflater.from(context);
    }

    /**
     * Update all elements
     *
     * @param listData new elements
     */
    public void updateItems(ArrayList<SyncedListElement> listData) {
        this.listData.clear();
        this.listData.addAll(listData);
        this.recyclerView
                .post(() -> SyncedListAdapter.this.notifyDataSetChanged());
    }

    /**
     * Return the size of elements (invoked by the layout manager)
     *
     * @return size of elements
     */
    //
    @Override public int getItemCount() {
        return listData.size();
    }

    /**
     * Create new views (invoked by the layout manager)
     *
     * @param viewGroup viewGroup
     * @param viewType  viewType
     * @return viewHolder
     */
    @Override public ViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                                   int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_element, viewGroup, false);

        return new ViewHolder(view);
    }

    /**
     * Replace content of a view (override given because it got recycled)
     *
     * @param viewHolder viewHolder
     * @param position   current position
     */
    @Override public void onBindViewHolder(ViewHolder viewHolder,
                                           final int position) {
        // remove old listeners (because of recycling old views)
        viewHolder.checkBox.setOnCheckedChangeListener(null);
        viewHolder.eTName.setOnFocusChangeListener(null);
        viewHolder.eTName.setOnEditorActionListener(null);

        // name edittext
        viewHolder.eTName.setOnFocusChangeListener((v, focused) -> {
            if (!focused) {
                checkNameChangesAndSubmit(viewHolder, position);
            }
        });
        viewHolder.eTName.setText(listData.get(position).getName());
        viewHolder.eTName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkNameChangesAndSubmit(viewHolder, position);
                InputMethodManager imm = (InputMethodManager) context
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(
                        ((Activity) context).getCurrentFocus().getWindowToken(),
                        0);

                return true;
            }
            return false;
        });
        viewHolder.tVDescription
                .setText(listData.get(position).getDescription());

        // Checkbox
        viewHolder.checkBox.setChecked(listData.get(position).getChecked());
        // on checked
        viewHolder.checkBox.setOnCheckedChangeListener((v, checked) -> {
            SyncedListElement updated = listData.get(position);
            updated.setChecked(checked);
            SyncedListStep newStep =
                    new SyncedListStep(listData.get(position).getId(),
                                       ACTION.UPDATE, updated);
            onAddStep(newStep);
        });

        // on move to top
        viewHolder.btnTop.setOnClickListener(vi -> {
            SyncedListStep newStep =
                    new SyncedListStep(listData.get(position).getId(),
                                       ACTION.SWAP, listData.get(0).getId());
            onAddStep(newStep);
            recyclerView.scrollToPosition(0);
        });

        // on move to bottom
        viewHolder.btnBottom.setOnClickListener(vi -> {
            SyncedListStep newStep =
                    new SyncedListStep(listData.get(position).getId(),
                                       ACTION.SWAP,
                                       listData.get(listData.size() - 1)
                                               .getId());
            onAddStep(newStep);
            recyclerView.scrollToPosition(listData.size() - 1);
        });

        // on move up
        viewHolder.iVBtnUp.setOnClickListener(vi -> {
            if (position > 0) {
                SyncedListStep newStep =
                        new SyncedListStep(listData.get(position).getId(),
                                           ACTION.SWAP,
                                           listData.get(position - 1).getId());
                onAddStep(newStep);
                recyclerView.scrollToPosition(position - 4);
            }
        });

        // on move down
        viewHolder.iVBtnDown.setOnClickListener(vi -> {
            if (position < listData.size() - 1) {
                SyncedListStep newStep =
                        new SyncedListStep(listData.get(position).getId(),
                                           ACTION.SWAP,
                                           listData.get(position + 1).getId());
                onAddStep(newStep);
                recyclerView.scrollToPosition(position - 2);
            }
        });
    }

    /**
     * Check element name changed and submit (create new SyncedListStep)
     *
     * @param viewHolder viewHolder with EditText
     * @param position   current position
     */
    public void checkNameChangesAndSubmit(ViewHolder viewHolder, int position) {
        String newName = viewHolder.eTName.getText().toString();
        if (!newName.equals("") &&
                !newName.equals(listData.get(position).getName())) {
            SyncedListElement updated = listData.get(position);
            updated.setName(newName);
            SyncedListStep newStep =
                    new SyncedListStep(listData.get(position).getId(),
                                       ACTION.UPDATE, updated);
            onAddStep(newStep);
        }
    }

    /**
     * Add step to List
     *
     * @param syncedListStep new SyncedListStep
     */
    public abstract void onAddStep(SyncedListStep syncedListStep);
}