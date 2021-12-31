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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

/**
 * DialogBuilder to easily create simple dialogs.
 */
public class DialogBuilder
{
    
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
    public static void editTextDialog(Context context, String title,
        String message, String yesOption, String noOption, Callback callback)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        final EditText editText = new EditText(context);
        
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setView(editText);
        alert.setPositiveButton(yesOption, (dialog, whichButton) -> callback
            .callback(editText.getText().toString()));
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
     * Simple Callback interface
     */
    public interface Callback
    {
        void callback(String result);
    }
}


