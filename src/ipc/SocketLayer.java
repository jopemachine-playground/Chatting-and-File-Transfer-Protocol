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

	// Ŭ���̾�Ʈ �ʿ��� ������ ���ӿ�û�� Socket Ŭ������ �̿��ؼ� �Ѵ�.
	public boolean Send(byte[] input, int length) {

		try (Socket client = new Socket()) {
			// Ŭ���̾�Ʈ �ʱ�ȭ
			InetSocketAddress ipep = new InetSocketAddress("127.0.0.1", dst_port);// ������Ʈ "127.0.0.1"�� ������ IP = ���� IP�� �ǹ�
			
			// ����
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

	// Ŭ���̾�Ʈ�� ��û�� �ޱ� ���� �غ� ServerSocket Ŭ������ �̿��� �Ѵ�.
	@Override
	public void run() {
		while (true)
			try (ServerSocket server = new ServerSocket();) {
				// ���� �ʱ�ȭ

				InetSocketAddress ipep = new InetSocketAddress("127.0.0.1", server_port);
				server.bind(ipep);
				System.out.println("Initialize complate");

				// LISTEN ���
				// accept �޼���� Ŭ���̾�Ʈ�� ��û�� �޾Ƶ��δ�.
				Socket client = server.accept();
				System.out.println("Connection");

				// receiver ��Ʈ�� �޾ƿ���
				// �ڵ� close
				try (InputStream receiver = client.getInputStream();) {
					// Ŭ���̾�Ʈ�κ��� �޽��� �ޱ�
					// byte ������
					data = new byte[1528]; // Ethernet Maxsize + Ethernet Headersize;
					receiver.read(data, 0, data.length);
					UpperLayer.Receive(data);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
	}

}
