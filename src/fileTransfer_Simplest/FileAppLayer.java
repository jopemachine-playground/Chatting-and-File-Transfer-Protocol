package fileTransfer_Simplest;

import static org.junit.Assert.assertArrayEquals;

import java.awt.Label;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

	private _FAPP_HEADER_BUILDER Fapp_Builder = new _FAPP_HEADER_BUILDER(new _FAPP_HEADER());

	private JProgressBar progressBar;

	private class _FAPP_HEADER {

		// 전체 파일 크기
		byte[] fapp_totlen;
		// 처음, 중간, 끝을 나타냄 (각각 00, 01, 02)
		byte[] fapp_type;
		// 0이면 nameFrame, 1이면 dataFrame, 2이면 데이터 전송 취소 프레임
		byte fapp_msg_type;
		// 쓰이지 않음
		byte ed;
		// 몇 번째 프레임인지를 나타냄
		byte[] fapp_seq_num;
		// 쓰이지 않음
		byte[] fapp_data;
	}

	private class _FAPP_HEADER_BUILDER {

		private _FAPP_HEADER m_Header;

		public _FAPP_HEADER_BUILDER(_FAPP_HEADER header) {
			m_Header = header;
		}

		public _FAPP_HEADER build() {
			return m_Header;
		}

		public _FAPP_HEADER_BUILDER setFileTotalLength(int message_length) {
			// 전체 파일 크기
			m_Header.fapp_totlen = ByteCaster.intToByte4(message_length);
			return this;
		}

		public _FAPP_HEADER_BUILDER setFappType(int setting_fapp_type) {
			m_Header.fapp_type = ByteCaster.intToByte2(setting_fapp_type);
			return this;
		}

		public _FAPP_HEADER_BUILDER setSequenceNumber(int nth_frame) {
			m_Header.fapp_seq_num = ByteCaster.intToByte4(nth_frame);
			return this;
		}

		public _FAPP_HEADER_BUILDER setMsgType(byte setting_fapp_msg_type) {
			m_Header.fapp_msg_type = setting_fapp_msg_type;
			return this;
		}
	}

	private FileAppLayer(String pName) {

		pLayerName = pName;

		setTitle("File Transfer");

		setBounds(500, 500, 460, 217);

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

		fileSelectButton.setBounds(163, 137, 120, 23);
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

		transferButton.setBounds(12, 137, 120, 23);
		chatting_InputPanel.add(transferButton);

		Label filePathLabel = new Label("Selected File Path");
		filePathLabel.setBounds(12, 65, 133, 23);
		chatting_InputPanel.add(filePathLabel);

		progressBar = new JProgressBar();
		progressBar.setBounds(12, 39, 420, 23);
		progressBar.setStringPainted(true);
		chatting_InputPanel.add(progressBar);

		Label transferProgrssiveBarLabel = new Label("File Transfer Progrssive Bar");
		transferProgrssiveBarLabel.setBounds(12, 10, 231, 23);
		chatting_InputPanel.add(transferProgrssiveBarLabel);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setBounds(312, 137, 120, 23);
		chatting_InputPanel.add(cancelButton);

		cancelButton.addActionListener(e -> {
			HandleTransferCancelButtonClicked();
		});

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

		int fapp_type = ByteCaster.byte2ToInt(input[4], input[5]);

		int ith_frame = ByteCaster.byte4ToInt(new byte[] { input[8], input[9], input[10], input[11] });

		byte[] data = RemoveFappHeader(input, input.length);

		// type이 0인 경우 == 파일 이름 프레임인 경우, FileReceiveDlg를 띄워, 전송률을 확인할 수 있게함
		if (fapp_type == 0) {

			try {
				fileReceiveDlg = new FileReceiveDlg(new String(data, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			return true;
		}

		// 타입이 1 이상의 값을 갖는 경우
		else {
			// 마지막 조각 (버퍼를 올림)
			if (fapp_type == 2) {

				fileReceiveDlg.AdjustProgressiveBar(100);

				// System.out.println("마지막 조각!");

				byte[] buf = new byte[file_fragments_buffer.length + (total_length % FILE_FRAGMENTATION_CRITERIA)];

				for (int i = 0; i < file_fragments_buffer.length; i++) {
					buf[i] = file_fragments_buffer[i];
				}

				for (int i = 0; i < (total_length % FILE_FRAGMENTATION_CRITERIA); i++) {
					buf[i + file_fragments_buffer.length] = data[i];
				}

				file_fragments_buffer = buf;

				((GUILayer) m_LayerMgr.GetLayer("GUI")).ReceiveFile(file_fragments_buffer,
						fileReceiveDlg.getName());

				file_fragments_buffer = BUFFER_INITIALIZER;

				fileReceiveDlg.QuitTransfer();

				return true;

			}
			// fapp_type이 1인 경우, 데이터 프레임이므로 순서에 맞게 버퍼에 저장
			else if (fapp_type == 1) {

				// System.out.println("버퍼에 저장!");

//				System.out.println((float)ith_frame);
//				System.out.println(((float)ith_frame/((total_length) / FILE_FRAGMENTATION_CRITERIA)));
//				System.out.println(((float)ith_frame/((total_length) / FILE_FRAGMENTATION_CRITERIA)) * 100);
//				System.out.println(Math.round(((float)ith_frame/((total_length) / FILE_FRAGMENTATION_CRITERIA)) * 100));

				fileReceiveDlg.AdjustProgressiveBar(
						Math.round(((float) ith_frame / ((total_length) / FILE_FRAGMENTATION_CRITERIA)) * 100));

				byte[] buf = new byte[file_fragments_buffer.length + FILE_FRAGMENTATION_CRITERIA];

				for (int i = 0; i < file_fragments_buffer.length; i++) {
					buf[i] = file_fragments_buffer[i];
				}

				for (int i = 0; i < FILE_FRAGMENTATION_CRITERIA; i++) {
					buf[i + file_fragments_buffer.length] = data[i];
				}

				file_fragments_buffer = buf;

				return true;
			}
		}

		assert (false);

		return false;

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
			sendingThread.mThread = new Thread(sendingThread);
			sendingThread.mThread.start();
		}

		SendFileNameFrame(file.getName());

		// sendingThread.Wait_FileNameFrame();
		sendingThread.Wait_Ack();

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

		_FAPP_HEADER header = Fapp_Builder.setFileTotalLength(fileName.length()).setFappType(0).setSequenceNumber(0)
				.setMsgType((byte) 0).build();

		byte[] data = null;

		try {
			data = ObjToByte(header, fileName.getBytes("UTF-8"), fileName.getBytes().length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SendFrame(data, data.length, 2090);

	}

	public void HandleTransferCancelButtonClicked() {

		byte[] data = null;

		_FAPP_HEADER header = Fapp_Builder.setFileTotalLength(0).setFappType(0).setSequenceNumber(0)
				.setMsgType((byte) 1).build();

		data = ObjToByte(header, data, 0);

		// 상대 측에 전송 취소 프레임을 보내고 쓰레드의 전송을 취소함
		((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SendFrame(data, 12, 2020);

		sendingThread.TransferCancel();

	}

	public void ReceiveTransferCanceledFrame() {

		file_fragments_buffer = BUFFER_INITIALIZER;
		JOptionPane.showMessageDialog(null, "Tranfer quit in compulsion", "Error", JOptionPane.ERROR_MESSAGE);

	}

	class Send_Thread implements Runnable {

		// 파일들의 큐. 파일 하나하나
		Queue<byte[]> filesQueue = new LinkedList<>();
		Object send_lock = new Object();
		Thread mThread;

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
				// 파일 전송 큐가 비어 있으면 송신할 파일이 선택될 때 까지 기다린다.
				synchronized (send_lock) {
					send_lock.wait();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings("deprecation")
		public void TransferCancel() {
			mThread.stop();
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
					header[0] = Fapp_Builder.setFileTotalLength(file_fragment_length).setFappType(2)
							.setSequenceNumber(1).setMsgType((byte) 1).build();

					byte[] data = ObjToByte(header[0], input, file_fragment_length);

					((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SendFrame(data,
							(file_fragment_length % FILE_FRAGMENTATION_CRITERIA) + 12, 2090);

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

							header[i] = Fapp_Builder.setFileTotalLength(file_fragment_length).setFappType(2)
									.setSequenceNumber(i + 1).setMsgType((byte) 1).build();

							split_data = ObjToByte(header[i], split_data,
									file_fragment_length % FILE_FRAGMENTATION_CRITERIA);

							((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SendFrame(split_data,
									(file_fragment_length % FILE_FRAGMENTATION_CRITERIA) + 12, 2090);

							progressBar.setValue(100);

							Wait_Ack();
						}

						else {

							split_data = new byte[FILE_FRAGMENTATION_CRITERIA];

							for (int j = 0; j < FILE_FRAGMENTATION_CRITERIA; j++) {
								split_data[j] = input[FILE_FRAGMENTATION_CRITERIA * i + j];
							}

							// 모든 조각을 보낼 때 까지 반복문을 돌며 헤더를 만들고, 붙여서 Send

							header[i] = Fapp_Builder.setFileTotalLength(file_fragment_length).setFappType(1)
									.setSequenceNumber(i + 1).setMsgType((byte) 1).build();

							split_data = ObjToByte(header[i], split_data, FILE_FRAGMENTATION_CRITERIA);

							((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SendFrame(split_data,
									FILE_FRAGMENTATION_CRITERIA + 12, 2090);

							progressBar.setValue(Math
									.round(((float) i / ((file_fragment_length) / FILE_FRAGMENTATION_CRITERIA)) * 100));

							// notify로 들어온 값과 이번에 Send한 frame이 같은 n번째 라면 Ack를 기다리고, 아니라면 반복문을 더 돈다

							Wait_Ack();

						}
					}

				}
			}

		}

	}
}
