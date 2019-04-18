package stopandwait;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	// 실제론 1456을 사용
	private static final int MESSAGE_FRAGMENTATION_CRITERIA = 10;
	private static final byte[] BUFFER_INITIALIZER = new byte[0];
	
	private byte[] message_buffer = new byte[0];
	
	private class _CHAT_APP {
		byte[] capp_totlen;
		byte capp_type;
		byte capp_unused;
		byte[] capp_data;

		// 한글로 할 경우 깨질지도 모름. 테스트 해 볼 것
		public _CHAT_APP(int message_length, byte nth_frame, byte[] message_data) {
			this.capp_unused = 0x00;

			// totlen : 문자열의 길이
			this.capp_totlen = intToByte2(message_length);

			// type : 몇 번 째 단편화 조각인가?
			this.capp_type = nth_frame;
			this.capp_data = message_data;
		}
	}

	public ChatAppLayer(String pName) {
		pLayerName = pName;
	}

	byte[] intToByte2(int value) {

		if (value > (2 << 16)) {
			System.err.append("Error - Too Big Message Length");
		}

		byte[] temp = new byte[2];
		temp[1] = (byte) (value >> 8);
		temp[0] = (byte) value;

		return temp;
	}

	public byte[] ObjToByte(_CHAT_APP Header, byte[] input, int length) {
		byte[] buf = new byte[length + 4];

		buf[0] = Header.capp_totlen[0];
		buf[1] = Header.capp_totlen[1];
		buf[2] = Header.capp_type;
		buf[3] = Header.capp_unused;

		for (int i = 0; i < length; i++)
			buf[4 + i] = input[i];

		return buf;
	}

	public boolean Send(byte[] input, int message_length) {

		_CHAT_APP header[] = new _CHAT_APP[(message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1];

		// 255 보다 큰 값을 byte에 담을 수 없으므로, message_length는 255 *
		// MESSAGE_FRAGMENTATION_CRITERIA 보다 작아야한다. (그래야 단편화가 가능하므로)
		if (message_length < 0 || message_length > 255 * MESSAGE_FRAGMENTATION_CRITERIA) {
			System.err.append("Error - Wrong Message Length");
			return false;
		}

		if (message_length < MESSAGE_FRAGMENTATION_CRITERIA) {
			// 단편화 하지 않음
			header[0] = new _CHAT_APP(message_length, (byte) 0x00, input);

			byte[] data = ObjToByte(header[0], input, message_length);

			if (p_UnderLayer.Send(data, (message_length % MESSAGE_FRAGMENTATION_CRITERIA) + 4) == false) {
				return false;
			}
			
			return true;
			
		} else {
			// 단편화 후 반복 Send
			for (int i = 0; i < (message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1; i++) {

				byte[] split_data = null;
				// 마지막 조각 처리
				if (i == message_length / MESSAGE_FRAGMENTATION_CRITERIA) {
					split_data = new byte[message_length % MESSAGE_FRAGMENTATION_CRITERIA];

					for (int j = i; j < i + message_length % MESSAGE_FRAGMENTATION_CRITERIA; j++) {
						split_data[j] = input[MESSAGE_FRAGMENTATION_CRITERIA * i + j];
					}

					// 모든 조각을 보낼 때 까지 반복문을 돌며 헤더를 만들고, 붙여서 Send
					header[i] = new _CHAT_APP(message_length, (byte) (i + 1), split_data);

					split_data = ObjToByte(header[i], split_data, message_length % MESSAGE_FRAGMENTATION_CRITERIA);

					if (p_UnderLayer.Send(split_data, (message_length % MESSAGE_FRAGMENTATION_CRITERIA) + 4) == false) {
						return false;
					}
				}
				// 10 바이트 조각 처리
				else {
					split_data = new byte[MESSAGE_FRAGMENTATION_CRITERIA];

					for (int j = i; j < i + MESSAGE_FRAGMENTATION_CRITERIA; j++) {
						split_data[j] = input[MESSAGE_FRAGMENTATION_CRITERIA * i + j];
					}

					// 모든 조각을 보낼 때 까지 반복문을 돌며 헤더를 만들고, 붙여서 Send
					header[i] = new _CHAT_APP(message_length, (byte) (i + 1), split_data);

					split_data = ObjToByte(header[i], split_data, MESSAGE_FRAGMENTATION_CRITERIA);

					if (p_UnderLayer.Send(split_data, MESSAGE_FRAGMENTATION_CRITERIA + 4) == false) {
						return false;
					}
				}

			}
		}
		return true;
	}

	public byte[] RemoveCappHeader(byte[] input, int length) {

		byte[] temp = new byte[length - 4];

		for (int i = 0; i < length - 4; i++) {
			temp[i] = input[i + 4];
		}

		return temp;
	}

	public synchronized boolean Receive(byte[] input) {

		if (input == null) {
			System.err.append("Error - Wrong Message Input");
			return false;
		}

		byte[] data = RemoveCappHeader(input, input.length);
		
		// 단편화가 되어 있지 않은 경우
		if (input[2] == 0) {
			this.GetUpperLayer(0).Receive(data);
			return true;
		}
		

		// 단편화가 되어 있는 경우
		else {
			
			int message_length = (input[0] | (input[1] << 8));
			
			// 마지막 조각 (버퍼를 올림)
			if (input[2] == (message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1) {
				
				byte[] buf = new byte[message_buffer.length + (message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1];
				
				for(int i = 0; i < (message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1; i++) {
					
					buf[i + (message_length % MESSAGE_FRAGMENTATION_CRITERIA)] = buf[i];
					buf[i] = data[i];
				}
				
				this.GetUpperLayer(0).Receive(message_buffer);
				
				message_buffer = BUFFER_INITIALIZER;
				
				return true;
			}
			// 버퍼에 저장
			else if(input[2] < (message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1){
				
				byte[] buf = new byte[message_buffer.length + MESSAGE_FRAGMENTATION_CRITERIA];
				
				for(int i = 0; i < MESSAGE_FRAGMENTATION_CRITERIA; i++) {
					
					buf[i + MESSAGE_FRAGMENTATION_CRITERIA] = buf[i];
					buf[i] = data[i];
				
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public String GetLayerName() {
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {

		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {

		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {

		if (pUpperLayer == null)
			return;

		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

}
