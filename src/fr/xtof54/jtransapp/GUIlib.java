package fr.xtof54.jtransapp;

import android.app.AlertDialog;
import android.app.Activity; // inherits from Context
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.webkit.WebView;
import java.io.File;

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
	public static void showWebview(final File jscode, final Activity main) {
		AlertDialog.Builder builder = new AlertDialog.Builder(main);
		LinearLayout layout = new LinearLayout(main);
		layout.setOrientation(LinearLayout.VERTICAL);
                final WebView wv = new WebView(main);
                layout.addView(wv);
		layout.setPadding(50, 40, 50, 10);
		builder.setView(layout);
		builder.setCancelable(true)
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
                                }
                        });
                builder.setPositiveButton("Run TF.js", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        wv.loadUrl("file://"+jscode.getAbsolutePath());
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
        }
}

