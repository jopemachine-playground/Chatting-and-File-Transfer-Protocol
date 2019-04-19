package stopandwait;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	// 실제론 1456을 사용
	private static final int MESSAGE_FRAGMENTATION_CRITERIA = 10;
	private static final byte[] BUFFER_INITIALIZER = new byte[0];

	private byte[] message_buffer = new byte[0];

	// 들어오고 나간 순서가 보장된다. 즉, 에러가 없다고 가정해, Queue를 사용했다
	public Send_Thread sendingThread = null;

	private class _CHAT_APP {
		byte[] capp_totlen;
		byte capp_type;
		byte capp_isAck;
		byte[] capp_data;

		// 한글로 할 경우 깨질지도 모름. 테스트 해 볼 것
		public _CHAT_APP(int message_length, byte nth_frame, byte[] message_data, byte isAck) {
			this.capp_isAck = isAck;

			// totlen : 문자열의 길이
			this.capp_totlen = intToByte2(message_length);

			// type : 몇 번 째 단편화 조각인가?
			this.capp_type = nth_frame;

			// data가 메시지라면 위 레이어로 byte[]이 아니라 _CHAT_APP 객체를 보내야함? 왜 이렇게 함?
			this.capp_data = message_data;
		}
	}

	public ChatAppLayer(String pName) {
		pLayerName = pName;
	}

	// Ack가 분실되어도, 처리할 수 있도록 type으로 몇 번째 프레인인지 값을 전달
	public void SendThreadNotify(byte type) {
		sendingThread.notified_nth_frame = type;
		synchronized (sendingThread) {
			sendingThread.notify();
		}
	}

	byte[] intToByte2(int value) {

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

	int byte2ToInt(byte little_byte, byte big_byte) {

		int little_int = (int) little_byte;
		int big_int = (int) big_byte;

		if (little_int < 0) {
			little_int += 256;
		}

		return (little_int + (big_int << 8));

	}

	public byte[] ObjToByte(_CHAT_APP Header, byte[] input, int length) {
		byte[] buf = new byte[length + 4];

		buf[0] = Header.capp_totlen[0];
		buf[1] = Header.capp_totlen[1];
		buf[2] = Header.capp_type;
		buf[3] = Header.capp_isAck;

		for (int i = 0; i < length; i++)
			buf[4 + i] = input[i];

		return buf;
	}

	public boolean Send(byte[] input, int message_length) {

		Thread thread = null;

		if (sendingThread == null) {
			sendingThread = new Send_Thread();
			thread = new Thread(sendingThread);
			thread.start();
		}

		sendingThread.messageQueue.add(input);

		synchronized (sendingThread.send_lock) {
			sendingThread.send_lock.notify();
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

	public boolean Receive(byte[] input) {

		if (input == null) {
			System.err.append("Error - Wrong Message Input");
			return false;
		}

		if (input[3] == 1) {
			System.out.println("Ack 도착");
			SendThreadNotify(input[2]);
			return false;
		}

		byte little_length = input[0];
		byte big_length = input[1];
		int type = input[2];

		byte[] data = RemoveCappHeader(input, input.length);

		Send_Ack(input[2]);

		// 단편화가 되어 있지 않은 경우
		if (type == 0) {
			this.GetUpperLayer(0).Receive(data);
			return true;
		}

		// 단편화가 되어 있는 경우
		else {

			int message_length = byte2ToInt(little_length, big_length);

			// 마지막 조각 (버퍼를 올림)
			if ((type > 0 && (type == (message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1))
					|| (type < 0 && (type + 256 == (message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1))) {

				byte[] buf = new byte[message_buffer.length + (message_length % MESSAGE_FRAGMENTATION_CRITERIA) + 1];

				for (int i = 0; i < message_buffer.length; i++) {
					buf[i] = message_buffer[i];
				}

				for (int i = 0; i < (message_length % MESSAGE_FRAGMENTATION_CRITERIA); i++) {
					buf[i + message_buffer.length] = data[i];
				}

				message_buffer = buf;

				this.GetUpperLayer(0).Receive(message_buffer);

				message_buffer = BUFFER_INITIALIZER;

				return true;
			}

			// 버퍼에 저장
			else {

				byte[] buf = new byte[message_buffer.length + MESSAGE_FRAGMENTATION_CRITERIA];

				for (int i = 0; i < message_buffer.length; i++) {
					buf[i] = message_buffer[i];
				}

				for (int i = 0; i < MESSAGE_FRAGMENTATION_CRITERIA; i++) {
					buf[i + message_buffer.length] = data[i];
				}

				message_buffer = buf;

				return true;
			}
		}

	}

	public void Send_Ack(byte type) {

		_CHAT_APP ack = new _CHAT_APP(0, type, null, (byte) 1);

		p_UnderLayer.Send(ObjToByte(ack, new byte[0], 0), 4);
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

	class Send_Thread implements Runnable {

		Queue<byte[]> messageQueue = new LinkedList<>();
		Object send_lock = new Object();

		byte notified_nth_frame;

		private void Wait_Ack() {
			try {
				// 한 프레임을 보내고, Ack 신호가 올 때 까지 대기한다.
				synchronized (this) {
					this.wait();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void Wait_Send() {
			try {
				// 메시지큐가 비어 있으면 송신할 메시지가 입력될 때 까지 기다린다.
				synchronized (send_lock) {
					send_lock.wait();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public synchronized void run() {

			// 전송이 끝난 후 while 루프로 인해, 돌아와 큐에서 새 메시지를 꺼냄, 메시지가 없을 경우 Wait.
			while (true) {

				if (messageQueue.isEmpty()) {
					Wait_Send();
				}

				byte[] input = messageQueue.poll();

				int message_length = input.length;

				_CHAT_APP header[] = new _CHAT_APP[(message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1];

				// 255 보다 큰 값을 byte에 담을 수 없으므로, message_length는 255 *
				// MESSAGE_FRAGMENTATION_CRITERIA 보다 작아야한다. (그래야 단편화가 가능하므로)
				if (message_length < 0 || message_length > 255 * MESSAGE_FRAGMENTATION_CRITERIA) {
					System.err.append("Error - Wrong Message Length");
				}

				if (message_length < MESSAGE_FRAGMENTATION_CRITERIA) {
					// 단편화 하지 않음
					header[0] = new _CHAT_APP(message_length, (byte) 0x00, input, (byte) 0);

					byte[] data = ObjToByte(header[0], input, message_length);

					p_UnderLayer.Send(data, (message_length % MESSAGE_FRAGMENTATION_CRITERIA) + 4);

				} else {
					// 단편화 후 반복 Send
					for (int i = 0; i < (message_length / MESSAGE_FRAGMENTATION_CRITERIA) + 1; i++) {

						byte[] split_data = null;
						// 마지막 조각 처리
						if (i == message_length / MESSAGE_FRAGMENTATION_CRITERIA) {
							split_data = new byte[message_length % MESSAGE_FRAGMENTATION_CRITERIA + 1];

							for (int j = 0; j < (message_length % MESSAGE_FRAGMENTATION_CRITERIA); j++) {
								split_data[j] = input[MESSAGE_FRAGMENTATION_CRITERIA * i + j];
							}

							// System.out.print(new String(split_data));

							// 모든 조각을 보낼 때 까지 반복문을 돌며 헤더를 만들고, 붙여서 Send
							header[i] = new _CHAT_APP(message_length, (byte) (i + 1), split_data, (byte) 0);

							split_data = ObjToByte(header[i], split_data,
									message_length % MESSAGE_FRAGMENTATION_CRITERIA);

							p_UnderLayer.Send(split_data, (message_length % MESSAGE_FRAGMENTATION_CRITERIA) + 4);
							
							Wait_Ack();
						}
						// 10 바이트 조각 처리
						else {
							split_data = new byte[MESSAGE_FRAGMENTATION_CRITERIA];

							for (int j = 0; j < MESSAGE_FRAGMENTATION_CRITERIA; j++) {
								split_data[j] = input[MESSAGE_FRAGMENTATION_CRITERIA * i + j];
							}

							// 모든 조각을 보낼 때 까지 반복문을 돌며 헤더를 만들고, 붙여서 Send
							header[i] = new _CHAT_APP(message_length, (byte) (i + 1), split_data, (byte) 0);

							// System.out.print(new String(split_data));

							split_data = ObjToByte(header[i], split_data, MESSAGE_FRAGMENTATION_CRITERIA);

							p_UnderLayer.Send(split_data, MESSAGE_FRAGMENTATION_CRITERIA + 4);

							// notify로 들어온 값과 이번에 Send한 frame이 같은 n번째 라면 Ack를 기다리고, 아니라면 반복문을 더 돈다
							
							Wait_Ack();
//							if (i + 1 == notified_nth_frame) {
//								System.out.println("nth_frame: " + i + 1);
//								System.out.println("notified_nth_frame" + notified_nth_frame);
//								Wait_Ack();
//							}

						}

					}
				}

			}
		}
	}
}
