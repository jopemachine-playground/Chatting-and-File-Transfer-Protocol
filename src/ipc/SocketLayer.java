package ipc;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SocketLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public int dst_port;
	public int src_port;

	public SocketLayer(String pName) {

		pLayerName = pName;

	}

	public void setClientPort(int dstAddress) {
		this.dst_port = dstAddress;

	}

	public void setServerPort(int srcAddress) {
		this.src_port = srcAddress;
	}

	// 클라이언트 쪽에서 서버에 접속요청을 Socket 클래스를 이용해서 한다.
	public boolean Send(byte[] input, int length) {

		try (Socket client = new Socket()) {
			// 클라이언트 초기화
			InetSocketAddress ipep = new InetSocketAddress("127.0.0.1", dst_port);// 상대방포트 "127.0.0.1"은 루프백 IP = 본인 IP를 의미
			
			// 접속
			client.connect(ipep);

			try (OutputStream sender = client.getOutputStream()) {
				sender.write(input, 0, length);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return true;
	}

	public boolean Receive() {
		Receive_Thread thread = new Receive_Thread(this.GetUpperLayer(0), src_port);
		Thread obj = new Thread(thread);
		obj.start();

		return false;
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		if (pUnderLayer == null)
			return;
		p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
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
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}

}

class Receive_Thread implements Runnable {
	byte[] data;

	BaseLayer UpperLayer;
	int server_port;

	public Receive_Thread(BaseLayer m_UpperLayer, int src_port) {

		UpperLayer = m_UpperLayer;
		server_port = src_port;
	}

	// 클라이언트의 요청을 받기 위한 준비를 ServerSocket 클래스를 이용해 한다.
	@Override
	public void run() {
		while (true)
			try (ServerSocket server = new ServerSocket();) {
				// 서버 초기화

				InetSocketAddress ipep = new InetSocketAddress("127.0.0.1", server_port);
				server.bind(ipep);
				System.out.println("Initialize complate");

				// LISTEN 대기
				// accept 메서드로 클라이언트의 요청을 받아들인다.
				Socket client = server.accept();
				System.out.println("Connection");

				// receiver 스트림 받아오기
				// 자동 close
				try (InputStream receiver = client.getInputStream();) {
					// 클라이언트로부터 메시지 받기
					// byte 데이터
					data = new byte[1528]; // Ethernet Maxsize + Ethernet Headersize;
					receiver.read(data, 0, data.length);
					UpperLayer.Receive(data);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
	}

}
