package eu.schmidt.systems.opensyncedlists.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;

import java.io.IOException;

import eu.schmidt.systems.opensyncedlists.datatypes.ACTION;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListElement;
import eu.schmidt.systems.opensyncedlists.datatypes.SyncedListStep;

/**
 * DialogBuilder to easily create simple dialogs
 */
public class DialogBuilder {

    /**
     * Create a simple text dialog
     *
     * @param context   Context
     * @param title     title of the dialog
     * @param message   message of the dialog
     * @param yesOption text of the yes option
     * @param noOption  text of the no/cancle option
     * @param callback  handle callback
     */
    public static void editTextDialog(Context context,
                                      String title,
                                      String message,
                                      String yesOption,
                                      String noOption,
                                      DialogCallback callback) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        final EditText editText = new EditText(context);

        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);

        alert.setTitle(title);
        alert.setMessage(message);
        alert.setView(editText);
        alert.setPositiveButton(yesOption, (dialog, whichButton) -> callback
                .callback(editText.getText().toString()));
        alert.setNegativeButton(noOption, (dialog, whichButton) -> callback
                .callback(null));

        AlertDialog dialog = alert.create();

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                callback.callback(editText.getText().toString());
                dialog.cancel();
                return true;
            }
            return false;
        });
        dialog.show();
    }

    /**
     * DialogCallback
     */
    public interface DialogCallback {
        void callback(String result);
    }

    /**
     * Convert density pixel to pixel
     *
     * @param dp density pixels
     * @return pixels
     */
    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}


