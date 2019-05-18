package fileTransfer;

import javax.swing.JOptionPane;

public class Main {

	private static LayerManager m_LayerMgr = LayerManager.getInstance();
	
	static {

		try {
			System.loadLibrary("jnetpcap");
			// System.out.println(new File("jnetpcap.dll").getAbsolutePath());
		} catch (UnsatisfiedLinkError e) {
			JOptionPane.showMessageDialog(null, "Native code library failed to load.\n" + e, "Library Link Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	public static void main(String[] args) {

		m_LayerMgr.AddLayer(new NILayer("Network_Interface"));
		m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
		m_LayerMgr.AddLayer(new ChatAppLayer("ChatApp"));
		m_LayerMgr.AddLayer(FileAppLayer.getInstance());
		m_LayerMgr.AddLayer(StopAndWaitDlg.getInstance());
		
		m_LayerMgr.ConnectLayers(" Network_Interface ( *Ethernet ( *ChatApp ( *GUI ) ) ) ");
		m_LayerMgr.ConnectLayers(" Ethernet ( *FileApp ( *GUI ) ) ");
		
		System.out.println("FileApp Layer Layer: " + ((FileAppLayer)(m_LayerMgr.GetLayer("FileApp"))).p_UnderLayer);
		System.out.println("ChatApp Under Layer: " + ((ChatAppLayer)(m_LayerMgr.GetLayer("ChatApp"))).p_UnderLayer);
		
	}

}
