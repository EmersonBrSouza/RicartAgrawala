package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class Server implements Runnable{
	
	private String groupAddress = "224.7.35.9";
	private Integer groupPort = 5656;
	private MulticastSocket multicastChannel;
	private Thread multicastListener;
	
	
	public Server (String groupAddress, String groupPort) {
		if (this.isValid(groupAddress)) this.groupAddress = groupAddress;
		if (this.isValid(groupPort)) this.groupPort = Integer.parseInt(groupPort);
	}
	
	@Override
	public void run() {
		this.listenMulticast();
		System.out.println("The server is running at " + this.getGroupAddress() + ":" + this.getGroupPort());
	}
	
	/**
	 * Start one thread to listen Multicast communication
	 * 
	 * @param void
	 * @return void
	 * */
	private void listenMulticast () {
		multicastListener = new Thread () {
			public void run () {
				try {
					multicastChannel = new MulticastSocket(getGroupPort());
					multicastChannel.joinGroup(getFormattedGroupAddress());
					while(true){
						byte[] data = new byte[20480];
						DatagramPacket packet = new DatagramPacket(data,data.length);
						multicastChannel.receive(packet);
						new Thread(new ProtocolProcessor(packet)).start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		multicastListener.start();
	}
	
	/**
	 * Check if data is valid
	 * 
	 * @param String data - The data
	 * @return boolean true - If address is valid
	 * */
	private boolean isValid (String data) {
		return data != null && !this.isEmpty(data);
	}
	
	/**
	 * Check if string is empty
	 * 
	 * @param String test - The string
	 * @return boolean true - If string is empty
	 * */
	private boolean isEmpty (String test) {
		return test.trim().length() == 0;
	}
	
	public String getGroupAddress() {
		return groupAddress;
	}

	public Thread getMulticastListener() {
		return multicastListener;
	}
	
	public Integer getGroupPort () {
		return this.groupPort;
	}

	public InetAddress getFormattedGroupAddress() throws UnknownHostException {
		return InetAddress.getByName(getGroupAddress());
	}


}
