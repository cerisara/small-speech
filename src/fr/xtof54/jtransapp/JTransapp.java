package fr.xtof54.jtransapp;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.graphics.Color;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public class JTransapp extends Activity {
	public Mike mike=null;
	public static JTransapp main = null;
	public File fdir=null;
	private TextView txt = null;

	@Override
	public void onCreate(Bundle s) {
		super.onCreate(s);
		main = this;
		fdir = getExternalFilesDir(null);
		if (fdir==null) fdir = getFilesDir();
		System.out.println("detjtrapp fdir "+fdir.getAbsolutePath());
		mike = new Mike();
		setContentView(R.layout.main);

		txt = (TextView) findViewById(R.id.textid);
		final Button recb = (Button) findViewById(R.id.recb);
		recb.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					recb.setBackgroundColor(Color.RED);
					recb.invalidate();
					System.out.println("detjtrapp startrecord");
					mike.startRecord();
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					mike.stopRecord();
					System.out.println("detjtrapp stoprecord");
					refreshText();
				}

				return true;
			}
		});
		refreshText();

		checkAcmod();
	}

	public void mikeEnded() {
		// called from the mike
		try {
			runOnUiThread(new Runnable() {
				public void run() {
					Button recb = (Button)findViewById(R.id.recb);
					recb.setBackgroundColor(Color.GRAY);
					System.out.println("detjtrapp mikeended");
					recb.invalidate();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void clear(View v) {
		if (fdir!=null) {
			File[] fs = fdir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String nom) {
					return nom.startsWith("recwav_");
				}	
			});
			for (File f: fs) {
				if (f.isFile() && !f.delete()) System.out.println("detjtrapp cannot delete "+f.getAbsolutePath());
			}
		}
		refreshText();
	}
	public void quitte(View v) {
		System.exit(1);
	}
	public void mfcc(View v) {
		mike.resetAudioSource();
		List frames = MFCC.getMFCC(mike);
		System.out.println("detjtrapp MFCC nframes= "+frames.size());

		// test grammatiseur
		Grammatiseur gram = Grammatiseur.getGrammatiseur();
		//String g = gram.getGrammar("(un|deux|trois|quatre)");
		//System.out.println("detjtrapp phonetisation "+g);

		SpeechAlign.align(frames,"trois,gateau");
	}
	public static void alert(final String s) {
		main.runOnUiThread(new Runnable() {
		    public void run() {
			Toast.makeText(main, s, Toast.LENGTH_LONG).show();
		    }
		});
	}

	public void refreshText() {
		if (fdir!=null) {
			File[] fs = fdir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String nom) {
					return nom.startsWith("recwav_");
				}	
			});
			if (fs!=null) {
				String nfs = "n="+Integer.toString(fs.length);
				if (txt!=null) {
					txt.setText(nfs);
					txt.invalidate();
				}
			}
		}
	}

	private ProgressDialog progdialog=null;
	public class ProgressWin implements ProgressDisplay {
		public ProgressWin() {
			try {
				runOnUiThread(new Runnable() {
					public void run() {
						progdialog = new ProgressDialog(main);
						progdialog.setMessage("Loading speech models. Please wait...");
						progdialog.setIndeterminate(false);
						progdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						progdialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener(){
							public void onClick(DialogInterface dialog, int whichButton){
								FileUtils.goeson=false;
							}
						});
						progdialog.setCanceledOnTouchOutside(false);
						progdialog.setProgress(0);
						progdialog.show();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void setIndeterminateProgress(String message) {
		}
		public void setProgress(String message, final float f) {
			try {
				runOnUiThread(new Runnable() {
					public void run() {
						int p = (int)(f*100.);
						if (progdialog!=null) progdialog.setProgress(p);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void setProgressDone() {
			System.out.println("detjtrapp call to setprogressdone");
			try {
				runOnUiThread(new Runnable() {
					public void run() {
						if (progdialog!=null) {
							progdialog.cancel();
							progdialog=null;
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void checkAcmod() {
		if (fdir==null) {
			System.out.println("detjtrapp cannot check acmod - no fdir");
			return;
		}
		File f = new File(fdir+"/acmod");
		if (f.exists())
			System.out.println("detjtrapp found acmod");
		else {

			AlertDialog.Builder builder = new AlertDialog.Builder(main);
			builder.setTitle("Downloader");
			builder.setMessage("I need to download speech models (60 MB)...")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						System.out.println("detjtrapp trying to download acmod");
						FileUtils.downloadFile("https://members.loria.fr/CCerisara/jtrans/acmod.zip",new File(fdir+"/acmod.zip"),new ProgressWin());
						dialog.cancel();
					}
				})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();

		}

	}
}


