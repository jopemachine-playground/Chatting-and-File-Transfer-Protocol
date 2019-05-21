package Utility;

public class DebuggingHelper {
	
	public static void printBinary(byte[] byteArray) {
		for (int i = 0; i < byteArray.length; i++) 
		{
			System.out.println(i + "th value: " + Integer.toBinaryString(byteArray[i] & 0xFF)); 
		}	
	}
	
	public static void printString(byte[] byteArray) {
		System.out.println(new String(byteArray));
	}

}
