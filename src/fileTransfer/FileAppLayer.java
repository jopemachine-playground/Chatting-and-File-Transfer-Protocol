package fileTransfer;

import java.awt.Label;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileSystemView;

public class FileAppLayer extends JFrame implements BaseLayer {

	public int nUpperLayerCount = 0;

	public String pLayerName = null;

	public BaseLayer p_UnderLayer = null;

	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	BaseLayer UnderLayer;

	private static FileAppLayer INSTANCE;
	private JTextField sendingFilePath;

	public FileAppLayer(String pName) {

		pLayerName = pName;

		setTitle("File Transfer");

		setBounds(500, 500, 460, 211);

		JPanel chatting_InputPanel = new JPanel();// chatting write panel
		chatting_InputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		chatting_InputPanel.setBounds(27, 100, 100, 20);
		getContentPane().add(chatting_InputPanel);
		chatting_InputPanel.setLayout(null);

		sendingFilePath = new JTextField();
		sendingFilePath.setBounds(12, 92, 420, 23);
		chatting_InputPanel.add(sendingFilePath);
		sendingFilePath.setColumns(10);

		JButton fileSelectButton = new JButton("File Select");
		fileSelectButton.addActionListener(e -> {
			HandleFileSelect();
		});

		fileSelectButton.setBounds(335, 125, 97, 23);
		chatting_InputPanel.add(fileSelectButton);

		JButton transferButton = new JButton("Transfer");
		transferButton.addActionListener(e -> {
			if (AddressSettingDlg.getInstance().isSetting() == false) {
				JOptionPane.showMessageDialog(null, "First, Set Oppenent Computer's Mac Address",
						"Mac Address Setting Error", JOptionPane.ERROR_MESSAGE);
			} else {
				if (HandleSend(new File(sendingFilePath.getText())) == false) {
					JOptionPane.showMessageDialog(null, "Error Occured while Sending File", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		transferButton.setBounds(12, 125, 323, 23);
		chatting_InputPanel.add(transferButton);

		Label filePathLabel = new Label("Selected File Path");
		filePathLabel.setBounds(12, 65, 133, 23);
		chatting_InputPanel.add(filePathLabel);

		JProgressBar progressBar = new JProgressBar();
		progressBar.setBounds(12, 39, 420, 23);
		chatting_InputPanel.add(progressBar);

		Label transferProgrssiveBarLabel = new Label("File Transfer Progrssive Bar");
		transferProgrssiveBarLabel.setBounds(12, 10, 231, 23);
		chatting_InputPanel.add(transferProgrssiveBarLabel);

	}

	public static FileAppLayer getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new FileAppLayer("FileApp");
		}
		return INSTANCE;
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

	private boolean HandleFileSelect() {

		JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

		int returnValue = jfc.showOpenDialog(null);

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			// Debugging: System.out.println(selectedFile.getAbsolutePath());
			sendingFilePath.setText(selectedFile.getAbsolutePath());
		}

		return true;
	}

	private boolean HandleSend(File file) {
		
		// 아래 코드에서 파일의 크기가 int형 변수보다 크면 문제가 발생함.
		byte[] fileBytes = new byte[(int) file.length()];
		
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(file);
			
			int data;
			
			// 한 루프에 100 바이트 씩 아래 레이어 (Ethernet Layer) 로 내려 보냄
			while((data = fis.read(fileBytes)) != -1) {
				
			}
			
			
			fis.close();
		}

		catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "File Not Found", "Error", JOptionPane.ERROR_MESSAGE);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}
}
