package stopandwait;

import java.util.ArrayList;
import java.util.StringTokenizer;

import UnitTest.DebuggingHelper;

public class EthernetLayer implements BaseLayer {

	private class _ETHERNET_Frame {

		// Ethernet Address
		private class _ETHERNET_ADDR {
			public byte[] addr = new byte[6];

			public _ETHERNET_ADDR() {
				this.addr[0] = (byte) 0x00;
				this.addr[1] = (byte) 0x00;
				this.addr[2] = (byte) 0x00;
				this.addr[3] = (byte) 0x00;
				this.addr[4] = (byte) 0x00;
				this.addr[5] = (byte) 0x00;
			}
		}

		_ETHERNET_ADDR enet_dstaddr;
		_ETHERNET_ADDR enet_srcaddr;
		byte[] enet_type;
		// type으로 Ack인 지, Data인지 나타냄. type이 01이라면 데이터를, 02라면 Ack 신호를 나타냄
		byte[] enet_data;

		public _ETHERNET_Frame() {
			this.enet_dstaddr = new _ETHERNET_ADDR();
			this.enet_srcaddr = new _ETHERNET_ADDR();
			this.enet_type = new byte[2];
			this.enet_data = null;
		}
	}

	public int nUpperLayerCount = 0;

	public String pLayerName = null;

	public BaseLayer p_UnderLayer = null;

	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	public _ETHERNET_Frame m_Ethernet_Header = new _ETHERNET_Frame();

	public EthernetLayer(String name) {
		pLayerName = name;
	}

	public void ParsingSrcMACAddress(String addr) {
		// 127을 넘는 수는 2의 보수로 바꿔 표현함. 예) ff는 -1로 표현됨
		StringTokenizer tokens = new StringTokenizer(addr, "-");

		for (int i = 0; tokens.hasMoreElements(); i++) {

			String temp = tokens.nextToken();

			try {
				m_Ethernet_Header.enet_srcaddr.addr[i] = Byte.parseByte(temp, 16);
			} catch (NumberFormatException e) {
				int minus = (Integer.parseInt(temp, 16)) - 256;
				m_Ethernet_Header.enet_srcaddr.addr[i] = (byte) (minus);
			}
		}
	}

	public void ParsingDstMACAddress(String addr) {
		// 127을 넘는 수는 2의 보수로 바꿔 표현함. 예) ff는 -1로 표현됨
		StringTokenizer tokens = new StringTokenizer(addr, "-");

		for (int i = 0; tokens.hasMoreElements(); i++) {

			String temp = tokens.nextToken();

			try {
				m_Ethernet_Header.enet_dstaddr.addr[i] = Byte.parseByte(temp, 16);
			} catch (NumberFormatException e) {
				int minus = (Integer.parseInt(temp, 16)) - 256;
				m_Ethernet_Header.enet_dstaddr.addr[i] = (byte) (minus);
			}
		}
	}

	public boolean Send(byte[] input, int length) {
		
		byte[] temp = Addressing(input, length, 0);
		
		if (p_UnderLayer.Send(temp, length + 14) == false) {
			return false;
		}

		return true;
	}

	public byte[] Addressing(byte[] input, int length, int type) {
		byte[] buf = new byte[length + 14];

		buf[0] = m_Ethernet_Header.enet_dstaddr.addr[0];
		buf[1] = m_Ethernet_Header.enet_dstaddr.addr[1];
		buf[2] = m_Ethernet_Header.enet_dstaddr.addr[2];
		buf[3] = m_Ethernet_Header.enet_dstaddr.addr[3];
		buf[4] = m_Ethernet_Header.enet_dstaddr.addr[4];
		buf[5] = m_Ethernet_Header.enet_dstaddr.addr[5];

		buf[6] = m_Ethernet_Header.enet_srcaddr.addr[0];
		buf[7] = m_Ethernet_Header.enet_srcaddr.addr[1];
		buf[8] = m_Ethernet_Header.enet_srcaddr.addr[2];
		buf[9] = m_Ethernet_Header.enet_srcaddr.addr[3];
		buf[10] = m_Ethernet_Header.enet_srcaddr.addr[4];
		buf[11] = m_Ethernet_Header.enet_srcaddr.addr[5];

		buf[12] = (byte) type;
		buf[13] = m_Ethernet_Header.enet_type[1];

		for (int i = 0; i < length; i++)
			buf[14 + i] = input[i];

		return buf;
	}

	public synchronized boolean Receive(byte[] input) {
		
		if ((isRightPacket(input) == false) || isRightAddress(input) == false) {
			return false;
		}

		if (isAckSignal(input)) {
			LayerManager lm = LayerManager.getInstance();
			((ChatAppLayer) lm.GetLayer("ChatApp")).SendThreadNotify();
			return false;
		}

		input = RemoveAddessHeader(input, input.length);

		GetUpperLayer(0).Receive(input);

		p_UnderLayer.Send(Addressing(new byte[0], 0, 1), 14);

		return true;
	}

	private boolean isAckSignal(byte[] input) {
		// input[12], 즉 isAck가 1인 경우 Ack 신호를 나타내므로, false를 리턴하고 Notify를 전달해 송신 쓰레드를 깨움
		if (input[12] == 1) {
			return true;
		}
		return false;
	}

	private boolean isRightPacket(byte[] input) {
		// type이 0x01, 0x00인 경우 Data 신호이다. 그 외엔 모두 더미 패킷으로 처리한다
		if (!(((input[12] == 0x00) && (input[13] == 0x00)) || (input[12] == 0x01) && (input[13] == 0x00))) {
			return false;
		}
		return true;
	}

	private boolean isRightAddress(byte[] input) {

		int ffCount = 0;
		int fitCount = 0;

		for (int i = 0; i < 6; i++) {

			// 패킷이 갖고 있는 목적지의 비트 패턴이 모두 ff이고, 패킷이 갖고 있는 src addr와 EthernetLayer의 dst와 같고,
			// 패킷이 갖고 있는 src addr이 헤더의 src addr과 다를 때 (즉 루프백은 받지 않음) +1
			if (input[i] == -1 && (input[i + 6] == m_Ethernet_Header.enet_dstaddr.addr[i]
					&& (input[i + 6] != m_Ethernet_Header.enet_srcaddr.addr[i]))) {
				ffCount++;
			}

			// 패킷이 갖고 있는 dst addr와 EthernetLayer의 src와 같고, 패킷이 갖고 있는 src addr와
			// EthernetLayer의 dst와 같으면 +1
			if ((input[i] == m_Ethernet_Header.enet_srcaddr.addr[i])
					&& (input[i + 6] == m_Ethernet_Header.enet_dstaddr.addr[i])) {
				fitCount++;
			}
		}

		if (ffCount == 6 || fitCount == 6) {
			return true;
		}
		return false;
	}

	public byte[] RemoveAddessHeader(byte[] input, int length) {

		byte[] temp = new byte[length - 14];

		for (int i = 0; i < length - 14; i++) {
			temp[i] = input[i + 14];
		}

		return temp;
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
