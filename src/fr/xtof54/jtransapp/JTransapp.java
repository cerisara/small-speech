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
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.text.InputType;
import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Enumeration;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.NetworkInterface;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class JTransapp extends Activity {
	public Mike mike=null;
	public static JTransapp main = null;
	public File fdir=null;
	private TextView txt = null;
	public String ftpserver = null;
	private static final int PLAY_FILE = 0;
	private static final int DEL_FILE = 1;

	public static final int mcport = 4536;
	public static final String mcip = "192.168.1.255";

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
		String ss = PrefUtils.getFromPrefs(getApplicationContext(), "JTRAPPFTP", null);
		if (ss!=null) ftpserver=ss;
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

	public void delFile(String ff) {
		File f = new File(ff);
		if (f.isFile() && !f.delete()) System.out.println("detjtrapp cannot delete "+f.getAbsolutePath());
		refreshText();
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
        public void dlasr(View v) {
            // test d'utilisation de tensorflow.js
            try {
                InputStream f = main.getAssets().open("tfjs.html");
                BufferedReader ff = new BufferedReader(new InputStreamReader(f));
                File outputDir = main.getCacheDir();
                File outputFile = File.createTempFile("tfjs", "html", outputDir);
                PrintWriter g = new PrintWriter(new FileOutputStream(outputFile));
                String s="";
                for (;;) {
                    String l = ff.readLine();
                    if (l == null) break;
                    g.print(l);
                }
                g.close();
                ff.close();
                GUIlib.showWebview(outputFile, main);
            } catch (Exception e) {
                System.out.println("detjtrapp Exception webview");
                e.printStackTrace();
            }
        }
        public void quitte(View v) {
		System.exit(1);
	}
	public void settings(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(main);
		builder.setTitle("Settings");
		final EditText inp = new EditText(main);
		inp.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setView(inp);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ftpserver = inp.getText().toString();
				PrefUtils.saveToPrefs(getApplicationContext(), "JTRAPPFTP", ftpserver);
				dialog.cancel();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
	}

	public void exportWAV(View v) {
		String PATH_NAME = JTransapp.main.fdir.getAbsolutePath(); //+"/recwav_"+startRecordTime+".raw";
		try {
			File sdcard = Environment.getExternalStorageDirectory();
			if (sdcard==null) {
				alert("Export needs an sdcard");
			} else {
				File fd = new File(PATH_NAME);
				File[] fs = fd.listFiles();
				for (File f: fs) {
					if (f.getName().startsWith("recwav_")) {
						String wavf = sdcard.getAbsolutePath()+"/"+f.getName().substring(0,f.getName().length()-4)+".wav";
						System.out.println("detjtrapp towav "+f.getName()+" "+wavf);
						Mike.rawToWave(f,new File(wavf));
					}
				}
				System.out.println("detjtrapp all files towav");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	public void exportFTP(View v) {
		if (ftpserver==null) {
			alert("Enter FTP server name first");
			return;
		}
		String PATH_NAME = JTransapp.main.fdir.getAbsolutePath(); //+"/recwav_"+startRecordTime+".raw";
		File fd = new File(PATH_NAME);
		File[] fs = fd.listFiles();
		try {
			for (File f: fs) {
				if (f.getName().startsWith("recwav_")) {
					System.out.println("JTRAPP upload "+f.getName());
					FTPClient ftpc = new FTPClient();
					ftpc.connect(InetAddress.getByName(ftpserver));
					ftpc.enterLocalPassiveMode();
					ftpc.login("anonymous","");
					ftpc.changeWorkingDirectory("upload");
					ftpc.setFileType(FTP.BINARY_FILE_TYPE);
					BufferedInputStream ff = new BufferedInputStream(new FileInputStream(f));
					ftpc.storeFile(f.getName(),ff);
					ff.close();
					ftpc.logout();
					ftpc.disconnect();
				}
			}
			System.out.println("JTRAPP all files uploaded");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void audiofiles(View v) {
		processFile(PLAY_FILE);
	}
	public void delaudiofiles(View v) {
		processFile(DEL_FILE);
	}
	private void setClipboard(Context context, String text) {
		if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(text);
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
			clipboard.setPrimaryClip(clip);
		}
	}
	private void processFile(final int filemode) {
		if (fdir!=null) {
			final File[] fs = fdir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String nom) {
					return nom.startsWith("recwav_");
				}	
			});
			if (fs!=null) {
				final ArrayAdapter<String> aa = new ArrayAdapter<String>(main,R.layout.choiceinlist);
				for (File ff: fs) aa.add(ff.getName());
				System.out.println("DGSAPP fileslist "+fs.length);

				AlertDialog.Builder builder = new AlertDialog.Builder(main);
				switch(filemode) {
					case PLAY_FILE: builder.setTitle("Play Files"); break;
					case DEL_FILE: builder.setTitle("Delete Files"); break;
				}
				builder.setCancelable(true)
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					})
				.setAdapter(aa, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						String chosenfile = aa.getItem(id);
						setClipboard(getApplicationContext(),fs[id].toString());
						// alert("chosen file "+chosenfile);
						switch(filemode) {
							case PLAY_FILE: 
								Mike.playPCM(fdir+"/"+chosenfile);
								break;
							case DEL_FILE: 
								delFile(fdir+"/"+chosenfile);
								break;
						}
					}	
				});
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			}
		}

	}

	public static void sendMC(String s) {
		try {
			DatagramSocket socket = new DatagramSocket();
			socket.setBroadcast(true);
			byte[] buffer = s.getBytes();
			InetAddress group = InetAddress.getByName(mcip);
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, mcport);
			socket.send(packet);
			socket.close();
			System.out.println("detjtrapp sendMC OK");
		} catch (Exception e) {
			System.out.print("detjtrapp sendMC exception");
			e.printStackTrace();
		}
	}
	public void mfcc(View v) {
		/*
		 *
		 * Old attempt: wanted to do speech recognition embedded
		 *
		mike.resetAudioSource();
		List frames = MFCC.getMFCC(mike);
		System.out.println("detjtrapp MFCC nframes= "+frames.size());

		// test grammatiseur
		Grammatiseur gram = Grammatiseur.getGrammatiseur();
		//String g = gram.getGrammar("(un|deux|trois|quatre)");
		//System.out.println("detjtrapp phonetisation "+g);

		SpeechAlign.align(frames,"trois,gateau");

		* 
		* right now, I prefer first to try and send the audio files to a desktop computer running JTrans:
		*
		*/

		/*
		 * multicast and broadcast seem to work very unreliably... So I'll just popup a window asking for desktop IP
		 
		MulticastReceiver broadcastListener = new MulticastReceiver();
		broadcastListener.start();
		*/

		GUIlib.showTextInputDialog("Enter server IP with JTrans", "192.168.1.", new GUIlib.StringHandler() {
			public void handleString(String s) {
				alert("connecting to "+s);
				MulticastReceiver.connectToJTrans(s);
			}
		}, main);

		try {
		/*
			// opens a port, waiting for a dekstop JTrans app to connect
			listenPort();
			// now broadcasts my IP so that the desktop Jtrans app knows where to connect to
			Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
			String locip = null;
			for (; n.hasMoreElements();)
			{
				NetworkInterface e = n.nextElement();
				System.out.println("Interface: " + e.getName());
				Enumeration<InetAddress> a = e.getInetAddresses();
				for (; a.hasMoreElements();)
				{
					InetAddress addr = a.nextElement();
					String ll = addr.getHostAddress();
					if (ll.startsWith("192.168.1.")) {
						locip=ll;
						break;
					}
				}
			}
			sendMC("detjtrapp "+locip);
			// TODO: repeat sendMC until the server has connected to the listenPort()
			

			// debug
			/*
			Thread aaa = new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(2000);
						Socket tt = new Socket("192.168.1.6",4539);
						System.out.println("detjtrapp debug "+tt);
						Thread.sleep(3000);
						tt.close();
					} catch (Exception e) {
						System.out.print("detjtrapp debug exception");
						e.printStackTrace();
					}
				}});
			aaa.start();
			*/
		} catch (Exception e) {
			System.out.print("detjtrapp sendIP exception");
			e.printStackTrace();
		}
	}
	private void listenPort() {
		Thread listenth = new Thread(new Runnable() {
			public void run() {
				try {
					final int port = 4539;
					ServerSocket serverSocket = new ServerSocket(port);
					System.out.println("detjtrapp waiting for server app ");
					Socket clientSocket = serverSocket.accept();
					// a JTrans app connected !
					System.out.println("detjtrapp got server app ");
					DataOutputStream socketout = new DataOutputStream(clientSocket.getOutputStream());
					// PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
					// BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					alert("uploading files...");

					// now transfer files
					if (fdir!=null) {
						File[] fs = fdir.listFiles(new FilenameFilter() {
							public boolean accept(File dir, String nom) {
								return nom.startsWith("recwav_");
							}	
						});
						for (File f: fs) {
							socketout.writeUTF(f.getName());
							// TODO send data
							DataInputStream ff = new DataInputStream(new FileInputStream(f));
							ff.close();
						}
						socketout.writeUTF("fini");
						socketout.close();
					}
					socketout.close();

				} catch (Exception e) {
					System.out.print("detjtrapp listenPort exception");
					e.printStackTrace();
				}
			}});
		listenth.start();
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


