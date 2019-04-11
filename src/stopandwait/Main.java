package stopandwait;

public class Main {

	private static LayerManager m_LayerMgr = LayerManager.getInstance();
	
	public static void main(String[] args) {

		m_LayerMgr.AddLayer(new NILayer("Network_Interface"));
		m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
		m_LayerMgr.AddLayer(new ChatAppLayer("ChatApp"));
		m_LayerMgr.AddLayer(new StopAndWaitDlg("GUI"));

		m_LayerMgr.ConnectLayers(" Network_Interface ( *Ethernet ( *ChatApp ( *GUI ) ) ) ");

	}

}
