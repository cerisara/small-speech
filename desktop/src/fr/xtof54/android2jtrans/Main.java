package fr.xtof54.android2jtrans;


import java.net.MulticastSocket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class Main {
	public static final int mcport = 4536;
	public static final String mcip = "230.0.0.0";

	public static class MulticastReceiver extends Thread {
		protected MulticastSocket socket = null;
		protected byte[] buf = new byte[256];

		public void run() {
			try {
				socket = new MulticastSocket(mcport);
				InetAddress group = InetAddress.getByName(mcip);
				socket.joinGroup(group);
				while (true) {
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);
					String received = new String(packet.getData(), 0, packet.getLength());
					if ("end".equals(received)) break;
					System.out.println("android2jtrans received "+received);
				}
				socket.leaveGroup(group);
				socket.close();
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

	public static void main(String args[]) {
		MulticastReceiver rec = new MulticastReceiver();
		rec.start();

		sendMC("totok");
		sendMC("end");
	}
}

