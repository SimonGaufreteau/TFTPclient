package com.tftpclient;

import java.io.*;
import java.lang.reflect.Array;
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
	private static final String[] serverErrors = {"Not defined, see error message (if any)","File not found.",
			"Access violation (the host may have denied your request or you don't have the right to read/write the file).",
			"Disk full or allocation exceeded.", "Illegal TFTP operation.","Unknown transfer ID.",
			"File already exists.","No such user."};
	private static final String[] localErrors = {"Could not create the socket",
			"Error encountered while trying to open the file","I/O error while sending/receiving a packet",
			"Error encountered while trying to read the file","I/O error while closing the file reader"};

	//Default write mode for the strings
	private static final String defaultMode = "netascii";

	//Default timeout value before sending the packet again : 1 min
	private static final int defaultTimeout = 500;
	private static final int defaultReSend = 3;

	//Default IP address
	private static InetAddress defaultIP;
	static {
		try {
			defaultIP = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			System.out.println("Could not resolve the address linked to \"localhost\" ");
		}
	}

	//Default server port
	private static final int defaultServerPort=69;


	/**
	 * Send a file to the server
	 * @return 0 if the file was sent successfully or -/+(errorCode+1) if an error occurred. Negative value : local error (see {@link TFTPClient#localErrors} for descriptions),
	 * Positive value : server error (see {@link TFTPClient#localErrors} for descriptions).
	 * @apiNote Either check the String array corresponding to your type of error at (+/-yourCode)-1 or use the {@link #getErrorMessage(int)} with the result of this method.
	 */
	public static int sendFile(InetAddress serverIP, int serverPort, String filePath){
		double time = System.currentTimeMillis();

		//Creating the socket for the transmission

		DatagramSocket sc;
		try {
			sc = new DatagramSocket();
			sc.setSoTimeout(defaultTimeout);
		} catch (SocketException e) {
			System.out.println(localErrors[0]);
			return -1;
		}

		//Opening the file
		File file = new File(filePath);
		String fileName = file.getName();
		FileInputStream fs;
		try {
			fs = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println(localErrors[1]);
			return -2;
		}

		System.out.println("\n--------------------");
		System.out.println("Starting TFTP request for : \""+fileName+"\"");

		//Preparing to send the WRQ packet to the server
		byte[] wrqMsg = createWRQ(fileName);
		DatagramPacket dp = new DatagramPacket(wrqMsg,wrqMsg.length,serverIP,serverPort);

		DatagramPacket resPacket;
		try {
			resPacket = sendReceive(sc,dp);
		} catch (IOException e) {
			System.out.println(localErrors[2]);
			return -3;
		}

		byte[] resMsg = resPacket.getData();
		//Checking an error
		if(resMsg[1]!=opcode.ACK.value){
			try{
				throwError(resMsg);
			}catch (TFTPException e){
				System.out.println(e.getMessage());
				return resMsg[3]+1;
			}
		}

		//Updating the communication port (--> the server attributes a port for each communication)
		serverPort=resPacket.getPort();

		//Creating the DATA packet
		int blockN = 1;
		int dataLength = 516;
		byte[] fileData;
		while(dataLength==516) {
			fileData = new byte[512];
			try {
				dataLength = fs.read(fileData, 0, 512)+4;
			} catch (IOException e) {
				System.out.println(localErrors[3]);
				return -4;
			}
			byte[] dataMsg = createDATA(fileData, blockN);

			System.out.println("\nSending the #" + blockN + " block of " + dataLength + " bytes");
			blockN++;
			dp = new DatagramPacket(dataMsg, dataLength, serverIP, serverPort);
			try {
				resPacket = sendReceive(sc, dp);
			} catch (IOException e) {
				System.out.println(localErrors[2]);
				return -3;
			}
			if (resPacket.getData()[1] != opcode.ACK.value) {
				try{
					throwError(resPacket.getData());
				}catch (TFTPException e){
					System.out.println(e.getMessage());
					return resMsg[3]+1;
				}
			}
		}
		//Terminating the communication
		try {
			fs.close();
		} catch (IOException e) {
			System.out.println(localErrors[4]);
			return -5;
		}
		sc.close();
		System.out.println("File \""+fileName+"\" sent successfully in "+(System.currentTimeMillis()-time)+"ms.");
		System.out.println("--------------------\n");
		return 0;
	}

	/**
	 * Works like {@link #sendFile(InetAddress, int, String)} but the address is {@link #defaultIP} i.e. the address at localhost
	 * @see #sendFile(InetAddress, int, String)
	 */
	public static int sendFile(int serverPort,String filePath){
		return sendFile(defaultIP,serverPort,filePath);
	}

	/**
	 * Works like {@link #sendFile(InetAddress, int, String)} but the address is {@link #defaultIP} i.e. the address at localhost and the port is {@link #defaultServerPort} i.e. 69
	 * @see #sendFile(InetAddress, int, String)
	 */
	public static int sendFile(String filePath){
		return sendFile(defaultIP,defaultServerPort,filePath);
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
		int i=0;
		do {
			try{
				sc.receive(rec);
				System.out.println("Received a packet from the server : "+Arrays.toString(rec.getData()));
				received=true;
			}catch (SocketTimeoutException e){
				if(i==defaultReSend){
					System.out.println("No response received in "+defaultReSend+" tries. Cancelling the communication.");
					throw new IOException();
				}
				System.out.println("No response from the server, re-sending the packet.");
				sc.send(dp);
				i++;
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

	// RRQ = 1
	// 2 bytes : Opcode / string : filename / 1 byte : 0 / string : Mode / 1 byte : 0
	private static byte[] createRRQ(String fileName) {
		byte[] fileBytes = fileName.getBytes();
		byte[] modeBytes = defaultMode.getBytes();
		byte[] opBytes = {0, (byte) opcode.RRQ.value};
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
		byte[] messageByte = serverErrors[errorCode].getBytes();
		return BytesUtils.concat(BytesUtils.concat(BytesUtils.concat(opBytes,codeByte),messageByte),zeroByte);
	}

	private static void throwError(byte[] resMsg) throws TFTPException {
		byte[] byteMsg = new byte[resMsg.length-4];
		System.arraycopy(resMsg,4,byteMsg,0,resMsg.length-4);
		String errMsg = new String(byteMsg);
		throw new TFTPException(serverErrors[resMsg[3]]+" Error message : "+errMsg);
	}

	/**
	 * Returns the error message corresponding to the code given. This code should be a return value from the {@link #sendFile(InetAddress, int, String)} method.
	 */
	public static String getErrorMessage(int errorCode){
		if(errorCode>serverErrors.length-1 || -errorCode>localErrors.length-1)
			return "No message was found for this error code.";
		if(errorCode==0)
			return "No problem occurred while sending/receiving the file";
		else if(errorCode>0){
			return serverErrors[errorCode-1];
		}
		else return localErrors[-(errorCode+1)];

	}

	/**
	 * Receive a file from the server
	 */
	public static int receiveFile(InetAddress serverIP,int serverPort,String fileName){
		double time = System.currentTimeMillis();

		//Creating the socket for the transmission
		DatagramSocket sc;
		try {
			sc = new DatagramSocket();
			sc.setSoTimeout(defaultTimeout);
		} catch (SocketException e) {
			System.out.println(localErrors[0]);
			return -1;
		}

		//Opening the local file
		File file = new File("local/"+fileName);
		int i=0;
		String[] nameSplit = fileName.split("[.]");
		String realName;
		String extension="";
		if(nameSplit.length==0){
			realName=fileName;
		}
		else if(nameSplit.length<2)
			realName=nameSplit[0];
		else{
			realName=nameSplit[nameSplit.length-2];
			extension=nameSplit[nameSplit.length-1];
		}
		while(file.isFile())
			file=new File("local/"+realName+"("+(i++)+")."+extension);
		FileOutputStream fs;
		try {
			fs = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println(localErrors[1]);
			return -2;
		}

		System.out.println("\n--------------------");
		System.out.println("Starting TFTP request for : \""+fileName+"\"");

		//Preparing to send the WRQ packet to the server
		byte[] rrqMsg = createRRQ(fileName);
		DatagramPacket dp = new DatagramPacket(rrqMsg,rrqMsg.length,serverIP,serverPort);

		DatagramPacket resPacket;
		try {
			resPacket = sendReceive(sc,dp);
		} catch (IOException e) {
			System.out.println(localErrors[2]);
			try{fs.close();}catch (Exception ignored){}
			file.delete();
			return -3;
		}
		//Checking errors
		if (checkError(fs, resPacket)){
			file.delete();
			return resPacket.getData()[3] + 1;
		}

		//Updating the communication port (--> the server attributes a port for each communication)
		serverPort=resPacket.getPort();

		//Creating the DATA packet
		int blockN = 1;
		boolean lastTime = resPacket.getLength()!=516;
		do{
			//Writing the data to the file
			if (writeToFile(fs, resPacket, blockN)) return -4;

			//Creating an ACK for the block and sending it to the server
			byte[] dataMsg = createACK(blockN);
			blockN++;
			dp = new DatagramPacket(dataMsg, dataMsg.length, serverIP, serverPort);
			if(lastTime){
				try {
					sc.send(dp);
				} catch (IOException e) {
					System.out.println(localErrors[2]);
					try{fs.close();}catch (Exception ignored){}
					return -3;
				}
				break;
			}
			try {
				resPacket = sendReceive(sc, dp);
			} catch (IOException e) {
				System.out.println(localErrors[2]);
				try{fs.close();}catch (Exception ignored){}
				return -3;
			}

			//Checking errors
			if (checkError(fs, resPacket)) return resPacket.getData()[3] + 1;
			if(resPacket.getLength()<516)
				lastTime= true;
		}while(resPacket.getLength()==516 || lastTime);

		//Terminating the communication
		try {
			fs.close();
		} catch (IOException e) {
			System.out.println(localErrors[4]);
			return -5;
		}
		sc.close();
		System.out.println("File \""+fileName+"\" retrieved successfully in "+(System.currentTimeMillis()-time)+"ms.");
		System.out.println("--------------------\n");
		return 0;
	}

	private static boolean checkError(FileOutputStream fs, DatagramPacket resPacket) {
		if (resPacket.getData()[1] != opcode.DATA.value) {
			try{
				throwError(resPacket.getData());
			}catch (TFTPException e){
				System.out.println(e.getMessage());
				try{fs.close();}catch (Exception ignored){}
				return true;
			}
		}
		return false;
	}

	private static boolean writeToFile(FileOutputStream fs, DatagramPacket resPacket, int blockN) {
		byte[] fileData;
		fileData = getDATA(resPacket.getData());
		System.out.println("\nWriting the #" + blockN + " block of " + fileData.length + " bytes");
		try {
			fs.write(fileData, 0, 512);
		} catch (IOException e) {
			System.out.println(localErrors[3]);
			return true;
		}
		return false;
	}

	/**
	 * Works like {@link #receiveFile(InetAddress, int, String)} but the address is {@link #defaultIP} i.e. the address at localhost
	 * @see #receiveFile(InetAddress, int, String)
	 */
	public static int receiveFile(int serverPort,String filePath){
		return receiveFile(defaultIP,serverPort,filePath);
	}

	/**
	 * Works like {@link #receiveFile(InetAddress, int, String)} but the address is {@link #defaultIP} i.e. the address at localhost and the port is {@link #defaultServerPort} i.e. 69
	 * @see #receiveFile(InetAddress, int, String)
	 */
	public static int receiveFile(String filePath){
		return receiveFile(defaultIP,defaultServerPort,filePath);
	}

	private static byte[] getDATA(byte[] data) {
		return Arrays.copyOfRange(data,4,data.length);
	}


}
