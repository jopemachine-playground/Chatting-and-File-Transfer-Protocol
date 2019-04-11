package stopandwait;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	private class _CHAT_APP {
		byte[] capp_totlen;
		byte capp_type;
		byte capp_unused;
		byte[] capp_data;

		public _CHAT_APP() {
			this.capp_unused = 0x00;
			this.capp_type = 0x00;
			this.capp_totlen = new byte[2];
			this.capp_data = null;
		}
	}

	private _CHAT_APP m_sHeader = new _CHAT_APP();

	public ChatAppLayer(String pName) {
		pLayerName = pName;
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

	public boolean Send(byte[] input, int length) {

		input = ObjToByte(m_sHeader, input, input.length);

		if (p_UnderLayer.Send(input, length + 4) == false) {
			return false;
		};

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

		byte[] data;

		data = RemoveCappHeader(input, input.length);

		this.GetUpperLayer(0).Receive(data);

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
