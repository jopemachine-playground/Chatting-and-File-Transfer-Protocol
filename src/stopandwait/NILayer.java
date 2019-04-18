package stopandwait;

import java.nio.ByteBuffer;
import java.util.*;
import org.jnetpcap.*;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

// Network Interface Layer

public class NILayer implements BaseLayer {

	public int nUpperLayerCount = 0;

	public String pLayerName = null;

	public BaseLayer p_UnderLayer = null;

	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	
	private Receive_Thread mThread;
	private Thread receive_Thread;

	// 네트워크 어댑터 인덱스
	int m_iNumAdapter;

	// 네트워크 어댑터 객체
	public Pcap m_AdapterObject;

	// 네트워크 인터페이스 객체
	public PcapIf device;

	// 네트워크 인터페이스 목록
	public List<PcapIf> m_pAdapterList;

	// 에러 버퍼
	StringBuilder errbuf = new StringBuilder();

	public NILayer(String pName) {
		pLayerName = pName;
		m_pAdapterList = new ArrayList<PcapIf>();
		m_iNumAdapter = 0;
		SetAdapterList();
	}

	public void SetAdapterList() {
		// 접근 가능한 모든 디바이스를 m_pAdapterList에 담는다.
		int r = Pcap.findAllDevs(m_pAdapterList, errbuf);
		
		// 만약, 접근 가능한 디바이스가 없다면 Pcap.Not_ok가 true가 되거나 m_pAdapterList가 비게 된다.
		if (r == Pcap.NOT_OK || m_pAdapterList.isEmpty()) {
			System.err.printf("Can't read list of devices, error is %s", errbuf.toString());
			return;
		}
	}

	public void SetAdapterNumber(String DstAddr) {

		int iNum = 0;

		try {
			for (int i = 0; i < m_pAdapterList.size(); i++) {
				
				byte[] macAddressBytes = m_pAdapterList.get(i).getHardwareAddress();
				
				StringBuilder macAddressStr = new StringBuilder();

				for (int j = 0; j < macAddressBytes.length; j++) {

					macAddressStr.append(String.format("%02X", macAddressBytes[j]));

					if (j < macAddressBytes.length - 1) {
						macAddressStr.append("-");
					}
				}
				
				if (DstAddr.equals(macAddressStr.toString())) {
					iNum = i;
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		m_iNumAdapter = iNum;
		
		// 인덱스에 해당하는 어댑터 드라이버와 연결
		PacketStartDriver();
		// Receive_Thread를 돌리기 시작함
		Receive();
	}

	public void PacketStartDriver() {
		int snaplen = 64 * 1024;
		// flags == 무차별모드
		int flags = Pcap.MODE_PROMISCUOUS;
		int timeout = 10 * 1000;
		m_AdapterObject = Pcap.openLive(m_pAdapterList.get(m_iNumAdapter).getName(), snaplen, flags, timeout, errbuf);
	}

	
	public boolean Receive() {
		
		if (mThread == null) {
			mThread = new Receive_Thread(m_AdapterObject, this.GetUpperLayer(0));
		}
		
		receive_Thread = new Thread(mThread);
		
		receive_Thread.start();
		
		return false;
	}
	
	public boolean DeleteReceiveThread() {
		
		if (receive_Thread == null) {
			return false;
		}
		
		receive_Thread.stop();
		
		return true;
	}

	class Receive_Thread implements Runnable {
		byte[] data;
		Pcap AdapterObject;
		BaseLayer UpperLayer;

		public Receive_Thread(Pcap m_AdapterObject, BaseLayer m_UpperLayer) {
			AdapterObject = m_AdapterObject;
			UpperLayer = m_UpperLayer;
		}

		@Override
		public synchronized void run() {
			while (true) {
				// 익명 클래스를 이용해 PcapHandler 인터페이스의 구현 방식을 명시.
				PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
					@Override
					public void nextPacket(PcapPacket packet, String user) {
						data = packet.getByteArray(0, packet.size());
						UpperLayer.Receive(data);
					}
				};
				AdapterObject.loop(100000, jpacketHandler, "");
			}
		}
	}

	public boolean Send(byte[] input, int length) {
		ByteBuffer buf = ByteBuffer.wrap(input);
		// sendPacket(buf)가 Pcap.OK가 아닌 경우 에러가 난 경우.
	
		if (m_AdapterObject.sendPacket(buf) != Pcap.OK) {
			System.err.println(m_AdapterObject.getErr());
			return false;
		}
		return true;
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
