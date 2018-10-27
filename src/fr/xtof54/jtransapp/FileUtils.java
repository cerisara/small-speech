package fr.xtof54.jtransapp;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Closeable;
import java.net.URL;
import java.net.URLConnection;

public class FileUtils {
	public static boolean goeson = true;
	public static void downloadFile(final String surl, final File target, final ProgressDisplay progress) {
		Thread downloader = new Thread(new Runnable() {
			public void run() {
				downloadFile0(surl,target,progress);
			}
		});
		downloader.start();
	}
	public static void downloadFile0(String surl, File target, ProgressDisplay progress) {
		InputStream in = null;
		FileOutputStream os = null;
		try {
			URL url = new URL(surl);
			URLConnection con = url.openConnection();
			con.connect();

			long len = con.getContentLength();
			long downloadedLen = 0;

			byte[] buf = new byte[8192];
			int read;

			long lastSpeedUpdate = System.currentTimeMillis();
			long accumBytes = 0;
			int bps = 0;

			in = con.getInputStream();
			os = new FileOutputStream(target);
			
			goeson=true;
			while ((read = in.read(buf)) > 0 && goeson) {
				os.write(buf, 0, read);
				downloadedLen += read;

				long now = System.currentTimeMillis();
				long window = now - lastSpeedUpdate;

				if (window > 50) {
					bps = (int)(accumBytes / (window / 1000f));
					lastSpeedUpdate = now;
					accumBytes = 0;
				} else {
					accumBytes += read;
				}

				if (progress!=null) progress.setProgress(
						String.format("Downloading " + url + "... (%d KB/s)", bps/1024),
						(float)downloadedLen/(float)len);

				// Allow cancellation
				Thread.sleep(0);
			}
			if (goeson) System.out.println("detjtrapp fini download ok");
			if (progress!=null) progress.setProgressDone();
			goeson=true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeQuietly(os);
			closeQuietly(in);
		}
	}

	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}

