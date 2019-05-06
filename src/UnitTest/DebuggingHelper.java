package UnitTest;

public class DebuggingHelper {
	
	public static void printBinary(byte[] byteArray) {
		for (int i = 0; i < byteArray.length; i++) 
		{
			System.out.println(i + "th value: " + Integer.toBinaryString(byteArray[i] & 0xff)); 
		}	
	}
	
	public static void printString(byte[] byteArray) {
		System.out.println(new String(byteArray));
	}

}
