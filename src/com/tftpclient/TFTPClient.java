package com.tftpclient;

import java.net.InetAddress;

/**
 * A class representing a TFTP client. The Pumpkin server is used to test the 2 primary methods : {@link #sendFile} and {@link #receiveFile}
 */
public class TFTPClient {
	/**
	 * Send a file to the server
	 */
	public void sendFile(InetAddress serverIP,int serverPort,String fileName){}

	/**
	 * Receive a file from the server
	 */
	public void receiveFile(InetAddress serverIP,int serverPort,String fileName){}

}
