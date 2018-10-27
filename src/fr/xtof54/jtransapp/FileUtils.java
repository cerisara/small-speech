package fr.xtof54.jtransapp;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Closeable;
import java.net.URL;
import java.net.URLConnection;

public class FileUtils {
	public static void downloadFile(URL url, File target, ProgressDisplay progress) throws IOException, InterruptedException {
		URLConnection con = url.openConnection();
		con.connect();

		long len = con.getContentLength();
		long downloadedLen = 0;

		byte[] buf = new byte[8192];
		int read;

		InputStream in = null;
		FileOutputStream os = null;

		long lastSpeedUpdate = System.currentTimeMillis();
		long accumBytes = 0;
		int bps = 0;

		try {
			in = con.getInputStream();
			os = new FileOutputStream(target);

			while ((read = in.read(buf)) > 0) {
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

				progress.setProgress(
						String.format("Downloading " + url + "... (%d KB/s)", bps/1024),
						(float)downloadedLen/(float)len);

				// Allow cancellation
				Thread.sleep(0);
			}
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
