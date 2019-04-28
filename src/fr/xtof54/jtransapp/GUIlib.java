package fr.xtof54.jtransapp;

import android.app.AlertDialog;
import android.app.Activity; // inherits from Context
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * This is my own generic class to perform app-independent GUI operations
 */
public class GUIlib {
	public static interface StringHandler {
		public void handleString(String s);
	}
	public static void showTextInputDialog(final String msg, final String txt0, final StringHandler handler, final Activity main) {
		AlertDialog.Builder builder = new AlertDialog.Builder(main);
		LinearLayout layout = new LinearLayout(main);
		layout.setOrientation(LinearLayout.VERTICAL);
		final EditText tv = new EditText(main);
		tv.setSingleLine();
		tv.setText(txt0);
		layout.addView(tv);
		layout.setPadding(50, 40, 50, 10);
		/*
		tv.setLayoutParameters(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT));
		*/
		builder.setView(layout);
		builder.setCancelable(true)
			.setMessage(msg)
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}
			});
		builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					handler.handleString(tv.getText().toString());
					dialog.dismiss();
				}
			});

		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
}

