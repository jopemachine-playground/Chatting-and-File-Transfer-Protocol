package ipc;
import java.util.ArrayList;

interface BaseLayer {
	public final int m_nUpperLayerCount = 0;
	public final String m_pLayerName = null;
	public final BaseLayer mp_UnderLayer = null;
	public final ArrayList<BaseLayer> mp_aUpperLayer = new ArrayList<BaseLayer>();

	public String GetLayerName();

	public BaseLayer GetUnderLayer();

	public BaseLayer GetUpperLayer(int nindex);

	public void SetUnderLayer(BaseLayer pUnderLayer);

	public void SetUpperLayer(BaseLayer pUpperLayer);

	public default void SetUnderUpperLayer(BaseLayer pUULayer) {
	}

	// 호출한 쪽이 위, 인자 쪽이 아래가 됨
	public void SetUpperUnderLayer(BaseLayer pUULayer);

	public default boolean Send(byte[] input, int length) {
		return false;
	}

	public default boolean Send(String filename) {
		return false;
	}

	public default boolean Receive(byte[] input) {
		return false;
	}

	public default boolean Receive() {
		return false;
	}

}
