package com.tftpclient;

public class TFTPException extends Exception {
	public TFTPException(String error) {
		super("TFTP Exception : "+error);
	}
}
