package com.tftpclient;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.Arrays;

/**
 * A class representing a TFTP client. The Pumpkin server is used to test the 2 primary methods : {@link #sendFile} and {@link #receiveFile}
 */
public class TFTPClient {
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
	private static int defaultTimeout = 60000;

	//Default IP adress
	private static InetAddress defaultIP;
	static {
		try {
			defaultIP = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	//Default server port
	private static int defaultServerPort=69;


	/**
	 * Send a file to the server
	 */
	public static void sendFile(InetAddress serverIP, int serverPort, String filePath) throws Exception {
		double time = System.currentTimeMillis();

		//Creating the socket for the transmission
		DatagramSocket sc = new DatagramSocket();
		sc.setSoTimeout(defaultTimeout);

		//Opening the file
		File file = new File(filePath);
		String fileName = file.getName();
		FileInputStream fs = new FileInputStream(file);

		System.out.println("\n--------------------");
		System.out.println("Starting TFTP request for : \""+fileName+"\"");

		//Preparing to send the WRQ packet to the server
		byte[] wrqMsg = createWRQ(fileName);
		DatagramPacket dp = new DatagramPacket(wrqMsg,wrqMsg.length,serverIP,serverPort);

		DatagramPacket resPacket = sendReceive(sc,dp);
		byte[] resMsg = resPacket.getData();

		//Checking an error
		if(resMsg[1]!=opcode.ACK.value){
			throwError(resMsg);
		}

		//Updating the communication port (--> the server attributes a port for each communication)
		serverPort=resPacket.getPort();

		//Creating the DATA packet
		int blockN = 1;
		int dataLength = 516;
		byte[] fileData;
		while(dataLength==516) {
			fileData = new byte[512];
			dataLength = fs.read(fileData, 0, 512)+4;
			fileData = BytesUtils.removeTrailingZeros(fileData);
			byte[] dataMsg = createDATA(fileData, blockN);
			System.out.println("\nSending the #" + blockN + " block of " + dataLength + " bytes");
			blockN++;
			dp = new DatagramPacket(dataMsg, dataLength, serverIP, serverPort);
			resPacket = sendReceive(sc, dp);
			if (resPacket.getData()[1] != opcode.ACK.value) {
				throwError(resPacket.getData());
			}
		}
		//Terminating the communication
		fs.close();
		sc.close();
		System.out.println("File \""+fileName+"\" sent successfully in "+(System.currentTimeMillis()-time)+"ms.");
		System.out.println("--------------------\n");

	}

	public void sendFile(int serverPort,String filePath) throws Exception {
		sendFile(defaultIP,serverPort,filePath);
	}
	public static void sendFile(String filePath) throws Exception {
		sendFile(defaultIP,defaultServerPort,filePath);
	}

	/**
	 * Sends a packet to the server and wait for the response.
	 * @param sc The client's socket
	 * @param dp Last packet sent
	 * @return Response of the server
	 */
	private static DatagramPacket sendReceive(DatagramSocket sc, DatagramPacket dp) throws IOException {
		//Send the packet to the server and
		byte[] recMsg = new byte[516];
		DatagramPacket rec = new DatagramPacket(recMsg,516);
		System.out.println("Sending the packet : "+Arrays.toString(dp.getData()));
		sc.send(dp);

		//Try to receive the response from the server, if the timeout exceeded, re-sends the packet
		boolean received = false;
		do {
			try{
				sc.receive(rec);
				System.out.println("Received a packet from the server : "+Arrays.toString(rec.getData()));
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
	private static byte[] createWRQ(String fileName){
		byte[] fileBytes = fileName.getBytes();
		byte[] modeBytes = defaultMode.getBytes();
		byte[] opBytes = {0, (byte) opcode.WRQ.value};
		byte[] zeroByte =new byte[]{(byte) 0};
		return BytesUtils.concat(BytesUtils.concat(BytesUtils.concat(BytesUtils.concat(opBytes,fileBytes), zeroByte),modeBytes),zeroByte);
	}

	// DATA
	// 2 bytes : Opcode / 2 bytes : Block# / n bytes : Data (0 to 512 bytes)
	private static byte[] createDATA(byte[] fileData,int blockN) {
		byte[] opBytes = {0, (byte) opcode.DATA.value};
		byte[] tempNumber = BigInteger.valueOf(blockN).toByteArray();
		byte[] blockNumber;
		if(tempNumber.length<2)
			 blockNumber= new byte[]{0, tempNumber[0]};
		else
			blockNumber = tempNumber;
		return BytesUtils.concat(BytesUtils.concat(opBytes,blockNumber), fileData);
	}

	// ACK
	// 2 bytes : Opcode / 2 bytes : Block#
	private static byte[] createACK(int blockN) {
		byte[] tempNumber = BigInteger.valueOf(blockN).toByteArray();
		byte[] blockNumber;
		if(tempNumber.length<2)
			blockNumber= new byte[]{0, tempNumber[0]};
		else
			blockNumber = tempNumber;
		byte[] res = new byte[]{0, (byte) opcode.ACK.value};
		return BytesUtils.concat(res,blockNumber);
	}

	// ERROR
	// 2 bytes : Opcode / 2 bytes : ErrorCode / string : ErrorMessage / 1 byte : 0
	private static byte[] createError(int errorCode) {
		byte[] opBytes = {0, (byte) opcode.ERROR.value};
		byte[] tempNumber = BigInteger.valueOf(errorCode).toByteArray();
		byte[] codeByte;
		if(tempNumber.length<2)
			codeByte= new byte[]{0, tempNumber[0]};
		else
			codeByte = tempNumber;
		byte[] zeroByte =new byte[]{(byte) 0};
		byte[] messageByte = errors[errorCode].getBytes();
		return BytesUtils.concat(BytesUtils.concat(BytesUtils.concat(opBytes,codeByte),messageByte),zeroByte);
	}

	private static void throwError(byte[] resMsg) throws Exception {
		throw new TFTPException(errors[resMsg[3]]);
	}

	/**
	 * Receive a file from the server
	 */
	public void receiveFile(InetAddress serverIP,int serverPort,String fileName){

	}




}
