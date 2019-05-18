package fileTransfer;

public class ByteCaster {

	static byte[] intToByte2(int value) {

		// int는 4바이트 이기 때문에, 2바이트로 int를 모두 나타낼 수는 없음. 2^16 = 65536 까지만 표현 가능
		// (그리고 type이 1 바이트 밖에 안 되기 때문에 보낼 수 있는 최대 바이트 길이는 255 *
		// MESSAGE_FRAGMENTATION_CRITERIA 바이트 둘 중 작은 값으로 제한됨)
		
		if (value > (1 << 16)) {
			System.err.append("Error - Too Big Message Length");
		}

		byte[] temp = new byte[2];

		temp[1] = (byte) ((value & 0x0000FF00) >> 8);
		temp[0] = (byte) ((value & 0x000000FF));

		return temp;
	}

	
	static byte[] intToByte4(int value) {
		byte[] temp = new byte[4];

		temp[0] |= (byte) ((value & 0xFF000000) >> 24);
		temp[1] |= (byte) ((value & 0x00FF0000) >> 16);
		temp[2] |= (byte) ((value & 0x0000FF00) >> 8);
		temp[3] |= (byte) ((value & 0x000000FF));
		return temp;

	}
	
	static int byte2ToInt(byte little_byte, byte big_byte) {

		int little_int = (int) little_byte;
		int big_int = (int) big_byte;

		if (little_int < 0) {
			little_int += 256;
		}

		return (little_int + (big_int << 8));

	}

	static int byte4ToInt(byte[] value) {

		if (value.length != 4) {
			System.out.println("Error In byte4ToInt");
		}

		int temp = 0;

		temp |= (value[3] << 24);
		temp |= (value[2] << 16);
		temp |= (value[1] << 8);
		temp |= (value[0] << 0);

		return temp;

	}
}
