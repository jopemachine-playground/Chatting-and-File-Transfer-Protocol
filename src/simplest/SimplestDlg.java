package simplest;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import java.net.*;

import java.util.regex.*;

public class SimplestDlg extends JFrame implements BaseLayer {

	private static SimplestDlg INSTANCE;

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	private String defaultName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	BaseLayer UnderLayer;

	private static LayerManager m_LayerMgr = LayerManager.getInstance();

	private JTextField message_Box;

	private JScrollPane chattingScrollPanel;

	Container contentPane;

	JTextArea ChattingArea;
	JButton send_Button;

	private JMenu mnNewMenu;
	private JMenuItem mnAddressSettingButton;
	private JMenuItem mnExitButton;

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

	public static SimplestDlg getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SimplestDlg("GUI");
		}
		return INSTANCE;
	}

	public SimplestDlg(String pName) {

		pLayerName = pName;

		setTitle("Simplest Protocol");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 250, 643, 473);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		mnNewMenu = new JMenu("Menu");
		menuBar.add(mnNewMenu);

		mnAddressSettingButton = new JMenuItem("Mac Address Setting");

		mnNewMenu.add(mnAddressSettingButton);

		mnExitButton = new JMenuItem("Exit");

		mnNewMenu.add(mnExitButton);
		contentPane = new JPanel();
		((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JPanel chattingPanel = new JPanel();// chatting panel
		chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Chatting",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		chattingPanel.setBounds(10, 5, 586, 381);
		contentPane.add(chattingPanel);
		chattingPanel.setLayout(null);

		JPanel chatting_Window = new JPanel();
		chatting_Window.setBounds(27, 27, 531, 284);
		chattingPanel.add(chatting_Window);
		chatting_Window.setLayout(null);

		ChattingArea = new JTextArea();
		ChattingArea.setForeground(Color.BLACK);
		ChattingArea.setBackground(Color.WHITE);
		ChattingArea.setToolTipText("show you the chatting log");
		ChattingArea.setEditable(false);
		ChattingArea.setFont(new Font("함초롬돋움", Font.PLAIN, 13));
		ChattingArea.setLineWrap(true);

		// chatting write panel
		chattingScrollPanel = new JScrollPane(ChattingArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		chatting_Window.add(chattingScrollPanel);
		chattingScrollPanel.setBounds(0, 0, 531, 284);
		chattingScrollPanel.setLayout(new ScrollPaneLayout());

		JPanel chatting_InputPanel = new JPanel();// chatting write panel
		chatting_InputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		chatting_InputPanel.setBounds(27, 337, 439, 20);
		chattingPanel.add(chatting_InputPanel);
		chatting_InputPanel.setLayout(null);

		message_Box = new JTextField();
		message_Box.setBounds(0, 0, 439, 20);// 249
		chatting_InputPanel.add(message_Box);
		message_Box.setColumns(10);// writing area

		send_Button = new JButton("Send");
		send_Button.setBackground(UIManager.getColor("textHighlight"));
		send_Button.setBounds(478, 337, 80, 20);
		send_Button.addActionListener(new setAddressListener());
		chattingPanel.add(send_Button);// chatting send button

		AddEvent();
		setVisible(true);
	}

	class setAddressListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {

			switch (e.getActionCommand()) {

			case "Send":
				HandleSend();
				break;
			}
		}
	}

	private void AddEvent() {

		mnExitButton.addActionListener(e -> {
			System.exit(0);
		});

		message_Box.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				// 10은 엔터의 리턴값
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					HandleSend();
			}
		});

		mnAddressSettingButton.addActionListener(e -> AddressSettingDlg.getInstance().setVisible(true));
	}

	private boolean HandleSend() {

		if (AddressSettingDlg.getInstance().isSetting() == false) {
			JOptionPane.showMessageDialog(null, "First, Press the Setting Button", "Mac Address Setting Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		ChattingArea.append("[SEND]:" + message_Box.getText() + "\n");

		ChattingArea.setCaretPosition(ChattingArea.getDocument().getLength()); // 맨아래로 스크롤한다.

		byte[] byte_text = null;

		try {
			byte_text = message_Box.getText().getBytes("UTF-8");
		}

		catch (UnsupportedEncodingException exception) {
			exception.printStackTrace();
		}

		if (((ChatAppLayer) p_UnderLayer).Send(byte_text, byte_text.length) == false) {
			return false;
		}

		message_Box.setText("");

		return true;
	}

	public boolean Receive(byte[] input) {

		String showText = "";

		try {
			showText = new String(input, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		ChattingArea.append("[RECV]:" + showText + "\n");

		return true;
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
