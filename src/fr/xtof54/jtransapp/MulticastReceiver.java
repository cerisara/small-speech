package fr.xtof54.jtransapp;

import java.net.MulticastSocket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;

/**
 * I don't use it anymore as a Multicast or Broadcast, because both are not reliable on android,
 * but rather as a standard network class to send data to the JTrans desktop app
 */
public class MulticastReceiver extends Thread {
	public static final int mcport = 4536;
	public static final String mcip = "230.0.0.0";
	protected MulticastSocket socket = null;
	protected byte[] buf = new byte[256];

	public static void connectToJTrans(final String ip) {
		Thread jtransconnecter = new Thread(new Runnable() {
			public void run() {
				try {
					final int port = 4539;
					System.out.println("detjtrapp connecting to "+ip+" "+port);
					Socket so = new Socket(ip, port);
					DataOutputStream f = new DataOutputStream(so.getOutputStream());
					// get list of files
					if (JTransapp.main.fdir==null) {
						JTransapp.main.alert("ERROR: no PATH to files found");
						return;
					}
					final File[] fs = JTransapp.main.fdir.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String nom) {
							return nom.startsWith("recwav_");
						}	
					});
					f.writeInt(fs.length);
					byte[] buf = new byte[1024];
					for (int i=0;i<fs.length;i++) {
						f.writeUTF(fs[i].getName());
						FileInputStream g = new FileInputStream(fs[i]);
						for (;;) {
							int nread = g.read(buf);
							f.writeInt(nread);
							if (nread<=0) break;
							f.write(buf,0,nread);
						}
						g.close();
					}
					f.close();
					JTransapp.main.alert("Finished transfer");
				} catch (Exception e) {
					e.printStackTrace();
					JTransapp.main.alert("error connection");
				}
			}
		});
		jtransconnecter.start();
	}

	public void run() {
		try {
			String clientip = null;
			System.out.println("detjtrapp now listening to broadcast message...");

			DatagramSocket serverSocketUDP = new DatagramSocket(1000);
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocketUDP.receive(receivePacket);
			System.out.println("detjtrapp android2jtrans received "+receivePacket.getAddress().getHostAddress());
			

			/*
			// using multicast here, but in fact, we'll get a broadcast UDP paquet
			socket = new MulticastSocket(mcport);
			InetAddress group = InetAddress.getByName(mcip);
			socket.joinGroup(group);
			while (true) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				String received = new String(packet.getData(), 0, packet.getLength());
				System.out.println("detjtrapp android2jtrans received "+received);
				if (received.startsWith("detjtrapp 192.168.1.")) {
					clientip = received.substring(10).trim();
					break;
				}
			}
			socket.leaveGroup(group);
			socket.close();

			// Now connect in TCP to send audio files
			if (clientip!=null) {
				final int port = 4539;
				System.out.println("detjtrapp connecting to "+clientip+" "+port);
				Socket so = new Socket(clientip, port);
				DataInputStream fs = new DataInputStream(so.getInputStream());
				for (;;) {
					String s = fs.readUTF();
					s=s.trim();
					if (s.startsWith("fini")) break;
					System.out.println("detjtrapp got file "+s);
				}
				fs.close();
			}
			*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendMC(String s) {
		try {
			DatagramSocket socket = new DatagramSocket();
			byte[] buffer = s.getBytes();
			InetAddress group = InetAddress.getByName(mcip);
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, mcport);
			socket.send(packet);
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

