package simplest;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.border.TitledBorder;

import simplest.SimplestDlg.setAddressListener;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Container;

import javax.swing.border.BevelBorder;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JComboBox;

public class AddressSettingDlg extends JFrame {
	
	private static AddressSettingDlg INSTANCE;

	JTextArea srcAddress;
	JTextArea dstAddress;
	JTextArea nic;

	JLabel lblsrc;
	JLabel lbldst;

	JButton Setting_Button;

	Container contentPane;

	static JComboBox<String> NICComboBox;

	private String defaultName = null;
	static ArrayList<String> MACAddrs = new ArrayList<String>();

	private static LayerManager m_LayerMgr = LayerManager.getInstance();
	
	private final static String MacAddress_Pattern = "^\\w{2}(-)\\w{2}(-)\\w{2}(-)\\w{2}(-)\\w{2}(-)\\w{2}$";
	private boolean isSetting;
	
	public static AddressSettingDlg getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new AddressSettingDlg();
		}
		return INSTANCE;
	}
	
	public boolean isSetting() {
		return isSetting;
	}
	
	private AddressSettingDlg() {
		
		setTitle("Address Setting");

		setBounds(500, 500, 460, 458);

		contentPane = new JPanel();
		contentPane.setLayout(null);
		
		JPanel settingPanel = new JPanel();
		settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Setting",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		settingPanel.setBounds(47, 37, 346, 319);

		contentPane.add(settingPanel);
		settingPanel.setLayout(null);

		JPanel srcMACAddressPanel = new JPanel();
		srcMACAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		srcMACAddressPanel.setBounds(20, 148, 190, 20);
		settingPanel.add(srcMACAddressPanel);
		srcMACAddressPanel.setLayout(null);

		lblsrc = new JLabel("Source Mac Address");
		lblsrc.setBounds(20, 122, 170, 20);
		settingPanel.add(lblsrc);

		srcAddress = new JTextArea();
		srcAddress.setBounds(2, 2, 190, 20);
		srcAddress.setEditable(false);
		srcMACAddressPanel.add(srcAddress);// src address

		JPanel dstMACAddressPanel = new JPanel();
		dstMACAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		dstMACAddressPanel.setBounds(20, 210, 190, 20);
		settingPanel.add(dstMACAddressPanel);
		dstMACAddressPanel.setLayout(null);

		lbldst = new JLabel("Destination Mac Address");
		lbldst.setBounds(20, 184, 190, 20);
		settingPanel.add(lbldst);

		dstAddress = new JTextArea();
		dstAddress.setBounds(2, 2, 190, 20);
		dstAddress.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent e) {

				// BackSpace 입력 시 이벤트를 따로 처리하지 않음
				if (e.getKeyChar() == 8) {
					return;
				}
				// 입력한 문자를 대문자로 변환
				if (e.getKeyChar() >= 'a' && e.getKeyChar() <= 'f') {
					e.setKeyChar(Character.toUpperCase(e.getKeyChar()));
				}
				// 입력된 문자열을 무시하는 경우
				if (dstAddress.getText().length() >= 17 || (!(e.getKeyChar() >= '0' && e.getKeyChar() <= '9')
						&& !(e.getKeyChar() >= 'A' && e.getKeyChar() <= 'F'))) {
					e.consume();
				}
				// '-'은 자동으로 입력
				if (dstAddress.getText().length() == 2 || dstAddress.getText().length() == 5
						|| dstAddress.getText().length() == 8 || dstAddress.getText().length() == 11
						|| dstAddress.getText().length() == 14) {
					dstAddress.append("-");
				}

			}
			
			
		});

		dstAddress.setToolTipText("Enter the Oppent Computer's Mac address");

		dstMACAddressPanel.add(dstAddress);// dst address

		Setting_Button = new JButton("Setting");// setting
		Setting_Button.setBackground(UIManager.getColor("textHighlight"));
		Setting_Button.setBounds(138, 272, 100, 20);
		Setting_Button.addActionListener(new setAddressListener());
		settingPanel.add(Setting_Button);// setting

		JLabel NICLabel = new JLabel("Select NIC");
		NICLabel.setBounds(20, 50, 218, 27);
		settingPanel.add(NICLabel);

		JPanel NICPanel = new JPanel();
		NICPanel.setLayout(null);
		NICPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		NICPanel.setBounds(20, 76, 292, 20);
		settingPanel.add(NICPanel);

		NICComboBox = new JComboBox(getAllNetworkName());
		NICComboBox.setBounds(0, 0, 292, 21);
		NICComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				srcAddress.setText(MACAddrs.get(NICComboBox.getSelectedIndex()));
			}
		});

		NICComboBox.setPrototypeDisplayValue(NICComboBox.getSelectedItem().toString());
		NICComboBox.setSelectedItem(defaultName);
		NICPanel.add(NICComboBox);

		getContentPane().add(contentPane);

	}

	private String[] getAllNetworkName() {

		try {
			// get all network interfaces of the current system
			InetAddress presentAddr = InetAddress.getLocalHost();

			Enumeration<NetworkInterface> networkInterface = NetworkInterface.getNetworkInterfaces();

			NetworkInterface presentNI = NetworkInterface.getByInetAddress(presentAddr);

			Vector<String> comboBoxRecords = new Vector<String>();

			// iterate over all interfaces
			while (networkInterface.hasMoreElements()) {
				// get an interface
				NetworkInterface network = networkInterface.nextElement();

				String displayName = network.getDisplayName();

				// get its hardware or mac address
				byte[] macAddressBytes = network.getHardwareAddress();

				if (macAddressBytes != null) {

					// initialize a string builder to hold mac address
					StringBuilder macAddressStr = new StringBuilder();
					// iterate over the bytes of mac address
					for (int i = 0; i < macAddressBytes.length; i++) {
						// convert byte to string in hexadecimal form
						macAddressStr.append(String.format("%02X", macAddressBytes[i]));
						// check if there are more bytes, then add a "-" to make it more readable
						if (i < macAddressBytes.length - 1) {
							macAddressStr.append("-");
						}
					}

					MACAddrs.add(macAddressStr.toString());
					comboBoxRecords.add(displayName);

					if (presentNI.equals(network)) {
						defaultName = displayName;
					}
				}
			}

			return comboBoxRecords.toArray(new String[comboBoxRecords.size()]);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	class setAddressListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {

			switch (e.getActionCommand()) {

			case "Setting":

				HandleSetting();
				break;
				
			case "Reset":
				HandleReset();
				break;
			}
		}
	}

	private void HandleSetting() {

		// Mac Address Setting
		((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).ParsingSrcMACAddress(srcAddress.getText());

		if (Pattern.matches(MacAddress_Pattern, dstAddress.getText())) {
			((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).ParsingDstMACAddress(dstAddress.getText());
		} else {
			JOptionPane.showMessageDialog(null, "Mac Address Format Error.\nWrite the right mac address",
					"Mac Address Input Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		((NILayer) (m_LayerMgr.GetLayer("Network_Interface"))).SetAdapterNumber(srcAddress.getText());

		NICComboBox.setEnabled(false);

		srcAddress.setEnabled(false);
		dstAddress.setEnabled(false);
		isSetting = true;

		setVisible(false);
		
		Setting_Button.setText("Reset");
	}
	
	private void HandleReset() {

		srcAddress.setEnabled(true);
		dstAddress.setEnabled(true);

		((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).ParsingSrcMACAddress("00-00-00-00-00-00");
		((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).ParsingDstMACAddress("00-00-00-00-00-00");

		((NILayer) (m_LayerMgr.GetLayer("Network_Interface"))).DeleteReceiveThread();

		NICComboBox.setEnabled(true);

		isSetting = false;
		Setting_Button.setText("Setting");

	}
	
}
