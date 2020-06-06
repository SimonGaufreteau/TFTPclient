package com.tftpclient;

public class Main {
	public static void main(String[] args) {
		TFTPClient.sendFile("testfiles/testFile.txt");
		TFTPClient.sendFile("testfiles/ACelleQuonDitFroide.txt");
		TFTPClient.sendFile("testfiles/the_times.jpg");
	}
}
