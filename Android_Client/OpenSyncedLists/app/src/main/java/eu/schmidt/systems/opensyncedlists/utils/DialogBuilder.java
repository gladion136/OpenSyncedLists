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
        editText.setPadding(margin_in_px, margin_in_px, margin_in_px,
            margin_in_px);
        
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
    
    /**
     * Create a multi-line text input dialog for text import.
     *
     * @param context   Context
     * @param title     title of the dialog
     * @param message   message of the dialog
     * @param hint      hint text for the input field
     * @param yesOption text of the yes option
     * @param noOption  text of the no/cancel option
     * @param callback  handle callback
     */
    public static void multiLineTextDialog(Context context, String title,
        String message, String hint, String yesOption, String noOption,
        Callback callback)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        final EditText editText = new EditText(context);
        
        editText.setInputType(
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMinLines(5);
        editText.setMaxLines(10);
        editText.setGravity(
            android.view.Gravity.TOP | android.view.Gravity.START);
        editText.setHint(hint);
        
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin_in_dp = 10;
        int margin_in_px = (int) (margin_in_dp * context.getResources()
            .getDisplayMetrics().density);
        params.setMargins(margin_in_px, margin_in_px, margin_in_px,
            margin_in_px);
        editText.setLayoutParams(params);
        editText.setPadding(margin_in_px, margin_in_px, margin_in_px,
            margin_in_px);
        
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setView(editText);
        alert.setPositiveButton(yesOption,
            (dialog, whichButton) -> callback.callback(
                editText.getText().toString()));
        alert.setNegativeButton(noOption,
            (dialog, whichButton) -> callback.callback(null));
        
        alert.create().show();
    }
    
    public static void tagSelectionDialog(Context context,
        ArrayList<ListTag> tags, String title, String descrp,
        SyncedListHeader listHeader, TagCallback callback)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        // Build a filtered list of assignable tags (excluding the virtual
        // "untagged" entry) and a matching list of CheckBoxes.
        final ArrayList<ListTag> assignableTags = new ArrayList<>();
        final ArrayList<CheckBox> checkBoxes = new ArrayList<>();
        for (ListTag tag : tags)
        {
            if (tag.untagged)
            {
                continue;
            }
            assignableTags.add(tag);
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
        linearLayout.setPadding(margin_in_px, margin_in_px, margin_in_px,
            margin_in_px);

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
                    selectedTags.add(assignableTags.get(i));
                }
            }
            callback.callback(selectedTags);
        });
        // Cancel: do nothing — the caller is not notified so no null is passed.
        alert.setNegativeButton("Cancel", null);

        alert.create().show();
    }
    
    /**
     * Create a confirmation dialog for a dangerous/irreversible action.
     * The callback runs only when the user confirms.
     *
     * @param context   Context
     * @param title     title of the dialog
     * @param message   message of the dialog
     * @param yesOption text of the confirm option
     * @param noOption  text of the cancel option
     * @param callback  runnable executed on confirm
     */
    public static void confirmDialog(Context context, String title,
        String message, String yesOption, String noOption, Runnable callback)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle(title);
        alert.setMessage(message);
        alert.setPositiveButton(yesOption,
            (dialog, whichButton) -> callback.run());
        alert.setNegativeButton(noOption,
            (dialog, whichButton) -> dialog.cancel());

        alert.create().show();
    }

    /**
     * Create a help dialog showing supported text formats.
     *
     * @param context Context
     * @param title   title of the help dialog
     * @param content content/body of the help dialog
     */
    public static void helpDialog(Context context, String title, String content)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        
        alert.setTitle(title);
        alert.setMessage(android.text.Html.fromHtml(content,
            android.text.Html.FROM_HTML_MODE_LEGACY));
        alert.setPositiveButton("OK",
            (dialog, whichButton) -> dialog.dismiss());
        
        alert.create().show();
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


