package com.tftpclient;

import java.io.CharArrayReader;
import java.io.IOException;

public class Main {
	public static void main(String[] args) throws Exception {
		try{
			TFTPClient.sendFile("testfiles/testFile.txt");
		} catch (TFTPException e){
			System.out.println(e.getMessage());
		}
		try{
			TFTPClient.sendFile("testfiles/ACelleQuonDitFroide.txt");
		} catch (TFTPException e){
			System.out.println(e.getMessage());
		}
	}
}
