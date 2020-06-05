package com.tftpclient;

import javax.xml.crypto.Data;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.Arrays;

/**
 * A class representing a TFTP client. The Pumpkin server is used to test the 2 primary methods : {@link #sendFile} and {@link #receiveFile}
 */
public class TFTPClient {
	// DATA
	// 2 bytes : Opcode / 2 bytes : Block# / n bytes : Data (0 to 512 bytes)

	// ACK
	// 2 bytes : Opcode / 2 bytes : Block#

	// ERROR
	// 2 bytes : Opcode / 2 bytes : ErrorCode / string : ErrorMessage / 1 byte : 0

	/**
	 * Values of the opcodes used in the packets
	 */
	private enum opcode {RRQ(1),WRQ(2),DATA(3),ACK(4),ERROR(5);
		public final int value;
		opcode(int i) {this.value=i;}
	}

	//Error messages
	private static String[] errors = {"Not defined, see error message (if any)","File not found.","Access violation.","Disk full or allocation exceeded.",
			"Illegal TFTP operation.","Unknown transfer ID.","File already exists.","No such user."};

	//Default write mode for the strings
	private static String defaultMode = "netascii";

	//Default timeout value before sending the packet again : 1 min
	private int defaultTimeout = 60000;

	/**
	 * Send a file to the server
	 */
	public void sendFile(InetAddress serverIP,int serverPort,String filePath) throws IOException {
		//Creating the socket for the transmission
		DatagramSocket sc = new DatagramSocket();
		sc.setSoTimeout(defaultTimeout);

		//Opening the file
		File file = new File(filePath);
		String fileName = file.getName();
		BufferedReader br = new BufferedReader(new FileReader(file));

		//Preparing to send the WRQ packet to the server
		byte[] wrqMsg = createWRQ(fileName);
		DatagramPacket dp = new DatagramPacket(wrqMsg,wrqMsg.length,serverIP,serverPort);

		DatagramPacket resPacket = sendReceive(sc,dp);


	}

	/**
	 * Sends a packet to the server and wait for the response.
	 * @param sc The client's socket
	 * @param dp Last packet sent
	 * @return Response of the server
	 */
	private DatagramPacket sendReceive(DatagramSocket sc, DatagramPacket dp) throws IOException {
		//Send the packet to the server and
		byte[] recMsg = new byte[516];
		DatagramPacket rec = new DatagramPacket(recMsg,516);
		sc.send(dp);

		//Try to receive the response from the server, if the timeout exceeded, re-sends the packet
		boolean received = false;
		do {
			try{
				sc.receive(rec);
				received=true;
			}catch (SocketTimeoutException e){
				System.out.println("No response from the server, re-sending the packet.");
				sc.send(dp);
			}
		}while (!received);
		return rec;
	}

	// WRQ = 2
	// 2 bytes : Opcode / string : filename / 1 byte : 0 / string : Mode / 1 byte : 0
	private byte[] createWRQ(String fileName){
		byte[] fileBytes = fileName.getBytes();
		byte[] modeBytes = defaultMode.getBytes();
		byte[] opBytes = BigInteger.valueOf(opcode.WRQ.value).toByteArray();
		byte[] zeroByte =new byte[]{(byte) 0};
		return BytesUtils.concat(BytesUtils.concat(BytesUtils.concat(BytesUtils.concat(opBytes,fileBytes), zeroByte),modeBytes),zeroByte);

	}

	/**
	 * Receive a file from the server
	 */
	public void receiveFile(InetAddress serverIP,int serverPort,String fileName){}



}
