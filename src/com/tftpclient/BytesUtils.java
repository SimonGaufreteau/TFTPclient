package com.tftpclient;

import java.util.Arrays;

public class BytesUtils {
	private BytesUtils(){}

	/**
	 * Concatenates the 2 given arrays into one
	 */
	public static byte[] concat(byte[] first, byte[] second){
		int firstLen = first.length;
		int secondLen = second.length;
		byte[] result = new byte[firstLen + secondLen];
		System.arraycopy(first, 0, result, 0, firstLen);
		System.arraycopy(second, 0, result, firstLen, secondLen);
		return result;
	}


	public static byte[] removeTrailingZeros(byte[] fileData) {
		int i=fileData.length-1;
		while(i>=0 && fileData[i]==0)
			i--;
		return Arrays.copyOf(fileData,i+1);
	}
}
