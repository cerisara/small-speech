package fr.xtof54.jtransapp;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import java.io.IOException;

import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.MediaExtractor;

/**
 * Also takes care of saving raw audio files
 */
public class Mike extends InputStream {
	private AudioRecord ar = null;
	private int minSize;
	private ByteBuffer buf = null;
	private int curinbuf=0, maxinbuf=0;
	// private MediaRecorder mrec=null;
	private long startRecordTime = 0;
	private boolean contrec = false;

	public static final int SAMPLE_RATE = 16000;

	public Mike() {
	}

	public void startRecord() {
		contrec=true;
		Thread recorder = new Thread(new Runnable() {
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
				int bufsize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
				if (bufsize == AudioRecord.ERROR || bufsize == AudioRecord.ERROR_BAD_VALUE) bufsize=SAMPLE_RATE*2;
				short[] wav = new short[bufsize/2];

				// do not use Builder here to support api 9
				AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, bufsize);
				if (record.getState() != AudioRecord.STATE_INITIALIZED) {
					System.out.println("detjtrapp audio record cannot initialize");
					JTransapp.alert("ERROR AUDIO");
					JTransapp.main.mikeEnded(); // callback to update the GUI (should use a listener here)
					return;
				}
				record.startRecording();
				try {
					startRecordTime = Calendar.getInstance().getTimeInMillis();
					String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+startRecordTime+".raw";
					System.out.println("detjtrapp start recording bufsize= "+wav.length+" "+PATH_NAME);
					FileChannel fout = new FileOutputStream(PATH_NAME).getChannel();
					ByteBuffer myByteBuffer = ByteBuffer.allocate(wav.length*2);
					myByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
					ShortBuffer myShortBuffer = myByteBuffer.asShortBuffer();
					long shortsRead = 0;
					while (contrec) {
						int nbshorts = record.read(wav,0,wav.length);
						if (nbshorts<0) {
							System.out.println("detjtrapp audio recording error "+nbshorts);
						} else {
							shortsRead += nbshorts;
							myByteBuffer.clear();
							myShortBuffer.clear();
							myShortBuffer.put(wav,0,nbshorts);
							fout.write(myByteBuffer);
						}
						// TODO: conv to mfcc
					}
					record.stop();
					record.release();
					System.out.println("detjtrapp stopped recording nread= "+shortsRead);
					fout.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				JTransapp.main.mikeEnded(); // callback to update the GUI (should use a listener here)
			}
		});
		recorder.start();
	}

	public void stopRecord() {
		contrec=false;
	}

	public static short byteArrayToShortBE(final byte[] bytes, final int offset) {
		int r = ((bytes[offset+1] & 0xFF) << 8) + (bytes[offset] & 0xFF);
		return (short)r;
	}
	public static short byteArrayToShortLE(final byte[] b, final int offset)
	{
		short value = 0;
		for (int i = 0; i < 2; i++) value |= (b[i + offset] & 0x000000FF) << (i * 8);
		return value;
	}

	public static void playPCM(final String PATH_NAME) {
		try {
			File inf = new File(PATH_NAME);
			int nbytes = (int)inf.length();
			byte[] buf = new byte[nbytes];
			FileInputStream finf = new FileInputStream(inf);
			int nread = finf.read(buf);
			finf.close();
			System.out.println("detjtrapp play "+buf.length+" "+PATH_NAME+" "+nread);

			final short[] bb = new short[nbytes/2];
			int maxb = 0;
			for (int i=0;i<bb.length;i++) {
				// bb[i] = byteArrayToShortLE(buf, i+i);
				bb[i] = byteArrayToShortBE(buf,i+i);
				if (bb[i]>maxb) maxb=bb[i];
				if (-bb[i]>maxb) maxb=-bb[i];
			}
			final float scalefact = 32000f/(float)maxb;
			System.out.println("detjtrapp maxwav "+maxb+" "+scalefact);
			for (int i=0;i<bb.length;i++) bb[i] = (short)((float)bb[i]*scalefact);

			Thread playth = new Thread(new Runnable() {
				public void run() {
					try {
						AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bb.length*2, AudioTrack.MODE_STATIC);
						mAudioTrack.write(bb,0,bb.length);
						mAudioTrack.play();
						// play is not blocking ! so we don't want to release audio just after...
						// mAudioTrack.release();
					} catch (Exception e) {
						System.out.println("detjtrapp EXCEPTION while playing");
						e.printStackTrace();
					}
				}});
			playth.start();
		} catch (Exception e) {
			System.out.println("detjtrapp EXCEPTION playing");
			e.printStackTrace();
		}
	}


	/*
	public void getRawAudio() {
		System.out.println("detjtrapp mfcc "+startRecordTime);
		if (startRecordTime<=0) return;
		String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+startRecordTime+".3gp";
		MediaDecoder dec = new MediaDecoder(PATH_NAME);
		short[] wav = dec.readShortData();
		System.out.println("detjtrapp shortwav "+wav.length);
	}

	public void replay() {
		if (startRecordTime<=0) return;
		String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+startRecordTime+".3gp";
		try {
			MediaPlayer mplayer = new MediaPlayer();
			mplayer.setDataSource(PATH_NAME);
			mplayer.prepare();
			mplayer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void startRecordInFile() {
		if (mrec!=null) return;
		try {
			mrec = new MediaRecorder();
			mrec.setAudioSource(MediaRecorder.AudioSource.MIC);
			mrec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mrec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			startRecordTime = Calendar.getInstance().getTimeInMillis();
			String PATH_NAME = JTransapp.main.fdir.getAbsolutePath()+"/recwav_"+startRecordTime+".3gp";
			mrec.setOutputFile(PATH_NAME);
			mrec.prepare();
			Thread recorder = new Thread(new Runnable() {
				public void run() {
					mrec.start();   // Recording is now started
				}
			});
			recorder.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopRecordInFile() {
		if (mrec==null) {
			System.out.println("detjtrapp mrec=null in stoprecord");
			return;
		}
		long curtime = Calendar.getInstance().getTimeInMillis();
		if (curtime-startRecordTime<500) {
			// at least 0.5s, otherwise the mediarecorder may crash
			System.out.println("detjtrapp too short in stoprecord");
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mrec.stop();
		mrec.reset();   // You can reuse the object by going back to setAudioSource() step
		mrec.release(); // Now the object cannot be reused
		mrec=null;
		System.out.println("detjtrapp end in stoprecord");
		JTransapp.main.mikeEnded(); // callback to update the GUI (should use a listener here)
	}

	public void startStream() {
		minSize= AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		minSize*=8;
		buf = new byte[minSize];
		curinbuf=0;
		maxinbuf=0;
		ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,minSize);
		ar.startRecording();
	}

	public void stopStream() {
		if (ar != null) {
		    ar.stop();
		}
        }
	*/

	public void resetAudioSource() {
		buf=null;
	}
	@Override
	public int read() {
		if (buf==null) {
			File dir = JTransapp.main.fdir;
			File[] fs = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String nom) {
					return nom.startsWith("recwav_");
				}	
			});
			String[] fss = new String[fs.length];
			for (int i=0;i<fss.length;i++) fss[i]=fs[i].getAbsolutePath();
			java.util.Arrays.sort(fss);
			if (fss.length==0) return -1;
			String PATH_NAME=fss[fss.length-1];
			System.out.println("detjtrapp mike load rawfile "+PATH_NAME);
			ArrayList<Short> sbuf = new ArrayList<Short>();
			DataInputStream fin = null;
			try {
				fin = new DataInputStream(new FileInputStream(PATH_NAME));
				for (;;) sbuf.add(fin.readShort());
			} catch (Exception e) {}
			try {
				if (fin!=null) fin.close();
			} catch (Exception e) {}

			// convert into a ShortBuffer and then a ByteBuffer, because the InputStream must output bytes
			buf = ByteBuffer.allocate(sbuf.size()*2).order(ByteOrder.LITTLE_ENDIAN);
			ShortBuffer shbuf = buf.asShortBuffer();
			for (Short s: sbuf) shbuf.put(s);
			shbuf.clear(); // does not erase data !
			curinbuf=0;
		}
		if (curinbuf>=buf.capacity()) return -1;
		// this is to return an unsigned byte:
		int b = (int)buf.get(curinbuf++) & 0xff;
		return b;
	}

	private static void writeInt(final DataOutputStream output, final int value) throws IOException {
		output.write(value >> 0);
		output.write(value >> 8);
		output.write(value >> 16);
		output.write(value >> 24);
	}

	private static void writeShort(final DataOutputStream output, final short value) throws IOException {
		output.write(value >> 0);
		output.write(value >> 8);
	}

	private static void writeString(final DataOutputStream output, final String value) throws IOException {
		for (int i = 0; i < value.length(); i++) {
			output.write(value.charAt(i));
		}
	}

	/* Converting RAW format To WAV Format*/
	public static void rawToWave(final File rawFile, final File waveFile) {

		byte[] rawData = new byte[(int) rawFile.length()];
		DataInputStream input = null;
		try {
			try {
				input = new DataInputStream(new FileInputStream(rawFile));
				input.read(rawData);
			} finally {
				if (input != null) {
					input.close();
				}
			}
			DataOutputStream output = null;
			try {
				output = new DataOutputStream(new FileOutputStream(waveFile));
				// WAVE header
				writeString(output, "RIFF"); // chunk id
				writeInt(output, 36 + rawData.length); // chunk size
				writeString(output, "WAVE"); // format
				writeString(output, "fmt "); // subchunk 1 id
				writeInt(output, 16); // subchunk 1 size
				writeShort(output, (short) 1); // audio format (1 = PCM)
				writeShort(output, (short) 1); // number of channels
				writeInt(output, 16000); // sample rate
				writeInt(output, 32000); // byte rate
				writeShort(output, (short) 2); // block align
				writeShort(output, (short) 16); // bits per sample
				writeString(output, "data"); // subchunk 2 id
				writeInt(output, rawData.length); // subchunk 2 size
				// Audio data (conversion big endian -> little endian)
				short[] shorts = new short[rawData.length / 2];
				// ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
				ByteBuffer.wrap(rawData).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts);
				ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
				for (short s : shorts) {
					bytes.putShort(s);
				}
				output.write(bytes.array());
			} finally {
				if (output != null) {
					output.close();
					rawFile.delete();
				}
			}
		} catch (IOException e) {
			System.out.println("detjtrapp ERROR in saveWAVE "+e.toString());
		}
	}
}

