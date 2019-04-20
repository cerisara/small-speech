package fr.xtof54.android2jtrans;


import java.net.MulticastSocket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.NetworkInterface;
import java.io.DataInputStream;
import java.util.Enumeration;

public class Main {
	public static final int mcport = 4536;
	public static final String mcip = "230.0.0.0";

	public static class MulticastReceiver extends Thread {
		protected MulticastSocket socket = null;
		protected byte[] buf = new byte[256];

		public void run() {
			try {
				String clientip = null;
				System.out.println("now listening to broadcast message...");

				// using multicast here, but in fact, we'll get a broadcast UDP paquet
				socket = new MulticastSocket(mcport);
				InetAddress group = InetAddress.getByName(mcip);
				socket.joinGroup(group);
				while (true) {
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);
					String received = new String(packet.getData(), 0, packet.getLength());
					System.out.println("android2jtrans received "+received);
					if (received.startsWith("detjtrapp 192.168.1.")) {
						clientip = received.substring(10).trim();
						break;
					}
				}
				socket.leaveGroup(group);
				socket.close();

				// Now connect in TCP to receive audio files
				if (clientip!=null) {
					final int port = 4539;
					System.out.println("connecting to "+clientip+" "+port);
					Socket so = new Socket(clientip, port);
					DataInputStream fs = new DataInputStream(so.getInputStream());
					for (;;) {
						String s = fs.readUTF();
						s=s.trim();
						if (s.startsWith("fini")) break;
						System.out.println("got file "+s);
					}
					fs.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	public static void main(String args[]) throws Exception {
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
		System.out.println("locip "+locip);

		sendMC("detjtrapp "+locip);
		/*
		MulticastReceiver rec = new MulticastReceiver();
		rec.start();

		while (true) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		*/
	}
}

