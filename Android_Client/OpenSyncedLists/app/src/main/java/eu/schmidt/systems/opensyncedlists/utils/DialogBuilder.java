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
package eu.schmidt.systems.opensyncedlists.utils;

import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import eu.schmidt.systems.opensyncedlists.syncedlist.ListTag;
import eu.schmidt.systems.opensyncedlists.syncedlist.SyncedListHeader;

/**
 * DialogBuilder to easily create simple dialogs.
 */
public class DialogBuilder
{
    public static void editTextDialog(Context context, String title,
        String message, String yesOption, String noOption, Callback callback)
    {
        DialogBuilder.editTextDialog(context, "", title, message, yesOption,
            noOption, callback);
    }
    
    /**
     * Create a simple text dialog
     *
     * @param context   Context
     * @param title     title of the dialog
     * @param message   message of the dialog
     * @param yesOption text of the yes option
     * @param noOption  text of the no/cancel option
     * @param callback  handle callback
     */
    public static void editTextDialog(Context context, String default_txt,
        String title, String message, String yesOption, String noOption,
        Callback callback)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        final EditText editText = new EditText(context);
        
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setText(default_txt);
        
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        int marign_in_dp = 10;
        int margin_in_px = (int) (marign_in_dp * context.getResources()
            .getDisplayMetrics().density);
        params.setMargins(margin_in_px, margin_in_px, margin_in_px,
            margin_in_px);
        editText.setLayoutParams(params);
        editText.setPadding(margin_in_px, margin_in_px, margin_in_px, margin_in_px);
        
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setView(editText);
        alert.setPositiveButton(yesOption,
            (dialog, whichButton) -> callback.callback(
                editText.getText().toString()));
        alert.setNegativeButton(noOption,
            (dialog, whichButton) -> callback.callback(null));
        
        AlertDialog dialog = alert.create();
        
        editText.setOnEditorActionListener((v, actionId, event) ->
        {
            if (actionId == EditorInfo.IME_ACTION_DONE)
            {
                callback.callback(editText.getText().toString());
                dialog.cancel();
                return true;
            }
            return false;
        });
        dialog.show();
    }
    
    public static void tagSelectionDialog(Context context,
        ArrayList<ListTag> tags, String title, String descrp,
        SyncedListHeader listHeader, TagCallback callback)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        final ArrayList<CheckBox> checkBoxes = new ArrayList<>();
        for (ListTag tag : tags)
        {
            if (tag.untagged)
            {
                continue;
            }
            CheckBox checkBox = new CheckBox(context);
            checkBox.setText(tag.name);
            checkBox.setChecked(listHeader.getTagList().stream()
                .anyMatch(t -> t.name.equals(tag.name)));
            checkBoxes.add(checkBox);
        }
        LinearLayout linearLayout = new LinearLayout(context);
        for (CheckBox checkBox : checkBoxes)
        {
            linearLayout.addView(checkBox);
        }
        
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        int marign_in_dp = 10;
        int margin_in_px = (int) (marign_in_dp * context.getResources()
            .getDisplayMetrics().density);
        params.setMargins(margin_in_px, margin_in_px, margin_in_px,
            margin_in_px);
        linearLayout.setLayoutParams(params);
        linearLayout.setPadding(margin_in_px, margin_in_px, margin_in_px, margin_in_px);
        
        alert.setTitle(title);
        alert.setMessage(descrp);
        
        alert.setView(linearLayout);
        alert.setPositiveButton("OK", (dialog, whichButton) ->
        {
            ArrayList<ListTag> selectedTags = new ArrayList<>();
            for (int i = 0; i < checkBoxes.size(); i++)
            {
                if (checkBoxes.get(i).isChecked())
                {
                    selectedTags.add(tags.get(i + 1));
                }
            }
            callback.callback(selectedTags);
        });
        alert.setNegativeButton("Cancel",
            (dialog, whichButton) -> callback.callback(null));
        
        AlertDialog dialog = alert.create();
        
        dialog.show();
    }
    
    /**
     * Simple Callback interface
     */
    public interface Callback
    {
        void callback(String result);
    }
    
    public interface TagCallback
    {
        void callback(ArrayList<ListTag> result);
    }
}


