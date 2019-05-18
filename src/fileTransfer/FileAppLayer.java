package fileTransfer;

import java.awt.Label;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

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

	// 들어오고 나간 순서가 보장된다. 즉, 에러가 없다고 가정해, Queue를 사용했다
	public Send_Thread sendingThread = null;

	private static final int FILE_FRAGMENTATION_CRITERIA = 1448;
	private static final byte[] BUFFER_INITIALIZER = new byte[0];

	private byte[] file_fragments_buffer = new byte[0];

	private static LayerManager m_LayerMgr = LayerManager.getInstance();
	
	private FileReceiveDlg fileReceiveDlg;
	
	private class _FAPP_HEADER {
		byte[] fapp_totlen;
		byte[] fapp_type;
		byte fapp_msg_type;
		byte ed;
		byte[] fapp_seq_num;
		byte[] fapp_data;

		public _FAPP_HEADER(int message_length, int nth_frame, byte isAck) {

			this.fapp_totlen = ByteCaster.intToByte4(message_length);

			this.fapp_type = ByteCaster.intToByte2(nth_frame);
			
			// 0x00 으로 쓰이지 않던, msg_type을 isAck로 사용함
			this.fapp_msg_type = isAck;

			// 쓰이지 않음
			this.ed = 0x00;
			this.fapp_seq_num = new byte[4];

			// 쓰이지 않음
			this.fapp_data = null;
		}
	}

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

	private byte[] ObjToByte(_FAPP_HEADER Header, byte[] input, int length) {
		byte[] buf = new byte[length + 12];

		buf[0] = Header.fapp_totlen[0];
		buf[1] = Header.fapp_totlen[1];
		buf[2] = Header.fapp_totlen[2];
		buf[3] = Header.fapp_totlen[3];
		buf[4] = Header.fapp_type[0];
		buf[5] = Header.fapp_type[1];
		buf[6] = Header.fapp_msg_type;
		buf[7] = Header.ed;
		buf[8] = Header.fapp_seq_num[0];
		buf[9] = Header.fapp_seq_num[1];
		buf[10] = Header.fapp_seq_num[2];
		buf[11] = Header.fapp_seq_num[3];

		for (int i = 0; i < length; i++)
			buf[12 + i] = input[i];

		return buf;
	}

	public void SendThreadNotify() {
		synchronized (sendingThread) {
			sendingThread.notify();
		}
	}

	public void SendThreadNotifyArrivingFileNameFrame() {
		synchronized (sendingThread) {
			sendingThread.fileNameFrame_lock.notify();
		}
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

	public boolean Receive(byte[] input) {

		if (input == null) {
			System.err.append("Error - Wrong File Input");
			return false;
		}

		int total_length = ByteCaster.byte4ToInt(new byte[] { input[0], input[1], input[2], input[3] });

		int ith_frame = ByteCaster.byte4ToInt(new byte[] { input[8], input[9], input[10], input[11] });

		byte[] data = RemoveFappHeader(input, input.length);

		//  type이 0인 경우 == 파일 이름 프레임인 경우
		if (ith_frame == 0) {

			fileReceiveDlg = new FileReceiveDlg(new String(data));
			
			return true;
		}

		// 타입이 1 이상의 값을 갖는 경우
		else {
			// 마지막 조각 (버퍼를 올림)
			if ((ith_frame > 0 && (ith_frame == (total_length / FILE_FRAGMENTATION_CRITERIA) + 1))) {

				byte[] buf = new byte[file_fragments_buffer.length + (total_length % FILE_FRAGMENTATION_CRITERIA) + 1];

				for (int i = 0; i < file_fragments_buffer.length; i++) {
					buf[i] = file_fragments_buffer[i];
				}

				for (int i = 0; i < (total_length % FILE_FRAGMENTATION_CRITERIA); i++) {
					buf[i + file_fragments_buffer.length] = data[i];
				}

				file_fragments_buffer = buf;
				
				((StopAndWaitDlg) m_LayerMgr.GetLayer("GUI")).ReceiveFile(file_fragments_buffer, fileReceiveDlg.getName());
				
				file_fragments_buffer = BUFFER_INITIALIZER;
				
				fileReceiveDlg.QuitTransfer();

				return true;

			}
			// 버퍼에 저장
			else {

				byte[] buf = new byte[file_fragments_buffer.length + FILE_FRAGMENTATION_CRITERIA];

				for (int i = 0; i < file_fragments_buffer.length; i++) {
					buf[i] = file_fragments_buffer[i];
				}

				for (int i = 0; i < FILE_FRAGMENTATION_CRITERIA; i++) {
					buf[i + file_fragments_buffer.length] = data[i];
				}

				file_fragments_buffer = buf;

				fileReceiveDlg.AdjustProgressiveBar((float)ith_frame/total_length);
				
				return true;
			}
		}

	}

	public byte[] RemoveFappHeader(byte[] input, int length) {

		byte[] temp = new byte[length - 12];

		for (int i = 0; i < length - 12; i++) {
			temp[i] = input[i + 12];
		}

		return temp;
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

		// 파일 이름의 크기가 1448 바이트를 넘지 않는다고 가정함 (즉, 파일 이름을 보내는 프레임은 단편화하지 않았다.)

		if (sendingThread == null) {
			sendingThread = new Send_Thread();
			Thread thread = new Thread(sendingThread);
			thread.start();
		}
		
		SendFileNameFrame(file.getName());
		
		sendingThread.Wait_FileNameFrame();
		
		// 아래 코드에서 파일의 크기가 int형 변수보다 크면 문제가 발생함.
		byte[] fileBytes = new byte[(int) file.length()];

		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);

			int data;

			// 한 루프에 100 바이트 씩 아래 레이어 (Ethernet Layer) 로 내려 보냄
			// 아닐 수도 있음. => 배열의 크기로 지정해주면 한 번에 내려갈지도 모름
			while ((data = fis.read(fileBytes)) != -1) {
				sendingThread.filesQueue.add(fileBytes);
			}

			synchronized (sendingThread.send_lock) {
				sendingThread.send_lock.notify();
			}

			fis.close();
		}

		catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "File Not Found", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}
	
	private void SendFileNameFrame(String fileName) {
		
		_FAPP_HEADER header = new _FAPP_HEADER(fileName.length(), 0, (byte) 0);
		
		byte[] data = ObjToByte(header, fileName.getBytes(), fileName.length());
		
		System.out.println(p_UnderLayer);
		
		p_UnderLayer.Send(data, data.length);
	} 
	
	class Send_Thread implements Runnable {

		Queue<byte[]> filesQueue = new LinkedList<>();
		Object send_lock = new Object();
		Object fileNameFrame_lock = new Object();

		private void Wait_Ack() {
			try {
				// 한 프레임을 보내고, Ack 신호가 올 때 까지 대기한다.
				synchronized (this) {
					this.wait();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void Wait_Send() {
			try {
				// 메시지큐가 비어 있으면 송신할 메시지가 입력될 때 까지 기다린다.
				synchronized (send_lock) {
					send_lock.wait();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void Wait_FileNameFrame() {
			try {
				// 파일 이름에 대한 응답이 올 때 까지 기다림
				synchronized (fileNameFrame_lock) {
					fileNameFrame_lock.wait();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {

			while (true) {

				if (filesQueue.isEmpty()) {
					Wait_Send();
				}

				byte[] input = filesQueue.poll();

				int file_fragment_length = input.length;

				_FAPP_HEADER header[] = new _FAPP_HEADER[(file_fragment_length / FILE_FRAGMENTATION_CRITERIA) + 1];

				if (file_fragment_length < FILE_FRAGMENTATION_CRITERIA) {

					// 단편화 하지 않음
					header[0] = new _FAPP_HEADER(file_fragment_length, 1, (byte) 0);

					byte[] data = ObjToByte(header[0], input, file_fragment_length);
					
					((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SendFrame(data, (file_fragment_length % FILE_FRAGMENTATION_CRITERIA) + 12, 2090);

					Wait_Ack();

				}

				else {
					for (int i = 0; i < (file_fragment_length / FILE_FRAGMENTATION_CRITERIA) + 1; i++) {

						byte[] split_data = null;

						if (i == file_fragment_length / FILE_FRAGMENTATION_CRITERIA) {
							split_data = new byte[file_fragment_length % FILE_FRAGMENTATION_CRITERIA + 1];

							for (int j = 0; j < (file_fragment_length % FILE_FRAGMENTATION_CRITERIA); j++) {
								split_data[j] = input[FILE_FRAGMENTATION_CRITERIA * i + j];
							}

							header[i] = new _FAPP_HEADER(file_fragment_length, (i + 2), (byte) 0);

							split_data = ObjToByte(header[i], split_data,
									file_fragment_length % FILE_FRAGMENTATION_CRITERIA);
							
							((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SendFrame(split_data, (file_fragment_length % FILE_FRAGMENTATION_CRITERIA) + 12, 2090);

							Wait_Ack();
						}

						else {

							split_data = new byte[FILE_FRAGMENTATION_CRITERIA];

							for (int j = 0; j < FILE_FRAGMENTATION_CRITERIA; j++) {
								split_data[j] = input[FILE_FRAGMENTATION_CRITERIA * i + j];
							}

							// 모든 조각을 보낼 때 까지 반복문을 돌며 헤더를 만들고, 붙여서 Send
							header[i] = new _FAPP_HEADER(file_fragment_length, (i + 2), (byte) 0);

							split_data = ObjToByte(header[i], split_data, FILE_FRAGMENTATION_CRITERIA);

							((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SendFrame(split_data, FILE_FRAGMENTATION_CRITERIA + 12, 2090);

							
							// notify로 들어온 값과 이번에 Send한 frame이 같은 n번째 라면 Ack를 기다리고, 아니라면 반복문을 더 돈다

							Wait_Ack();

						}
					}

				}

			}
		}

	}
}
