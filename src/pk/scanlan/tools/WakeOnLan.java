package pk.scanlan.tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;


public class WakeOnLan  extends Thread{

	public static final int PORT = 9; 
	private InetAddress ip;
	private byte[] macBytes;
	public WakeOnLan(String ipStr)
	{
		
		try {
			
			
			NetworkInterface network;
			//todo
			ip = InetAddress.getByName( ipStr);
			
			
				network = NetworkInterface.getByInetAddress(ip);
			
			 macBytes = network.getHardwareAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	public WakeOnLan(InetAddress mIp, byte[] macAddres)
	{
		ip = mIp;
		macBytes = macAddres;
		
	}
	
	

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			
			
			
		      byte[] bytes = new byte[6 + 16 * macBytes.length];
		      for (int i = 0; i < 6; i++) {
		          bytes[i] = (byte) 0xff;
		      }
		      for (int i = 6; i < bytes.length; i += macBytes.length) {
		          System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
		      }

		    
		      
			try {
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ip, PORT);
			      DatagramSocket socket;
				socket = new DatagramSocket();
				  try {
					socket.send(packet);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			      socket.close();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
				
				
			}	
	
}
