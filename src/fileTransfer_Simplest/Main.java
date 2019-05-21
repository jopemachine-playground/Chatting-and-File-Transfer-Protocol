package fileTransfer_Simplest;

import javax.swing.JOptionPane;

public class Main {

	private static LayerManager m_LayerMgr = LayerManager.getInstance();
	
	static {

		try {
			System.loadLibrary("jnetpcap");
		} catch (UnsatisfiedLinkError e) {
			JOptionPane.showMessageDialog(null, "Native code library failed to load.\n" + e, "Library Link Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		
		// 네트워크 인터페이스 (1) 
		m_LayerMgr.AddLayer(new NILayer("Network_Interface"));
		
		// 이더넷 계층(2)
		m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
		
		// 채팅 전송 계층(3)
		m_LayerMgr.AddLayer(new ChatAppLayer("ChatApp"));
		
		// 파일 전송 계층(3)
		m_LayerMgr.AddLayer(FileAppLayer.getInstance());
		
		// 응용 프로그램 계층(4)
		m_LayerMgr.AddLayer(GUILayer.getInstance());
		
		m_LayerMgr.ConnectLayers(" Network_Interface ( *Ethernet ( *ChatApp ( *GUI ) ) ) ");
		m_LayerMgr.ConnectLayers(" Ethernet ( *FileApp ( *GUI ) ) ");
		
//		System.out.println("FileApp Layer Layer: " + ((FileAppLayer)(m_LayerMgr.GetLayer("FileApp"))).p_UnderLayer);
//		System.out.println("ChatApp Under Layer: " + ((ChatAppLayer)(m_LayerMgr.GetLayer("ChatApp"))).p_UnderLayer);
		
	}

}
