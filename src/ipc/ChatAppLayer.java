package ipc;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	private class _CAPP_HEADER {
		int capp_src;
		int capp_dst;
		byte[] capp_totlen;
		byte[] capp_data;

		public _CAPP_HEADER() {
			this.capp_src = 0x00000000;
			this.capp_dst = 0x00000000;
			this.capp_totlen = new byte[2];
			this.capp_data = null;
		}
	}

	private _CAPP_HEADER m_sHeader = new _CAPP_HEADER();

	public ChatAppLayer(String pName) {
		pLayerName = pName;
		ResetHeader();
	}

	public void ResetHeader() {
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
		}
		m_sHeader.capp_data = null;
	}

	public byte[] ObjToByte(_CAPP_HEADER Header, byte[] input, int length) {
		byte[] buf = new byte[length + 10];
		byte[] srctemp = intToByte4(Header.capp_src);
		byte[] dsttemp = intToByte4(Header.capp_dst);

		buf[0] = dsttemp[0];
		buf[1] = dsttemp[1];
		buf[2] = srctemp[0];
		buf[3] = srctemp[1];
		buf[4] = dsttemp[0];
		buf[5] = dsttemp[1];
		buf[6] = srctemp[0];
		buf[7] = srctemp[1];
		buf[8] = (byte) (length % 256);
		buf[9] = (byte) (length / 256);

		for (int i = 0; i < length; i++)
			buf[10 + i] = input[i];

		return buf;
	}

	public boolean Send(byte[] input, int length) {

		input = ObjToByte(m_sHeader, input, input.length);
		
		if (p_UnderLayer.Send(input, length + 10)) return true;
		
		else return false;
	}

	public byte[] RemoveCappHeader(byte[] input, int length) {

		for (int i = 0; i < length - 10; i++) {
			input[i] = input[i + 10];
		}

		return input;
	}
	
	public synchronized boolean Receive(byte[] input) {
		byte[] data;
		byte[] temp_src = intToByte2(m_sHeader.capp_src);

		for (int i = 0; i < 2; i++) {
			if (input[i] != temp_src[i]) {
				return false;
			}
		}
		
		data = RemoveCappHeader(input, input.length);

		this.GetUpperLayer(0).Receive(data);

		return true;
	}

	byte[] intToByte2(int value) {
		byte[] temp = new byte[2];
		temp[1] = (byte) (value >> 8);
		temp[0] = (byte) value;

		return temp;
	}

	byte[] intToByte4(int value) {
		byte[] temp = new byte[4];
		
		temp[0] |= (byte) ((value & 0xFF000000) >> 24);
		temp[1] |= (byte) ((value & 0x00FF0000) >> 16);
		temp[2] |= (byte) ((value & 0x0000FF00) >> 8);
		temp[3] |= (byte) ((value & 0x000000FF));
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

	public void SetEnetSrcAddress(int srcAddress) {
		m_sHeader.capp_src = srcAddress;
		
	}

	public void SetEnetDstAddress(int dstAddress) {
		m_sHeader.capp_dst = dstAddress;
	}

}
