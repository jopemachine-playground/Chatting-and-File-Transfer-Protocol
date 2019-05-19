package fileTransfer;

import java.awt.EventQueue;
import java.awt.Label;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import java.awt.Font;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class FileReceiveDlg extends JFrame{
	
	private String fileName;
	
	private JProgressBar progressBar;
	
	private JButton quitButton;
	
	public FileReceiveDlg(String transferringFileName) {
		
		fileName = transferringFileName;
		
		setTitle("File Receiving..");

		setBounds(500, 500, 460, 168);

		JPanel chatting_InputPanel = new JPanel();// chatting write panel
		chatting_InputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		chatting_InputPanel.setBounds(27, 100, 100, 20);
		getContentPane().add(chatting_InputPanel);
		chatting_InputPanel.setLayout(null);

		progressBar = new JProgressBar();
		progressBar.setBounds(12, 64, 420, 23);
		progressBar.setStringPainted(true);
		chatting_InputPanel.add(progressBar);
		System.out.println(fileName);
		JLabel transferProgrssiveBarLabel = new JLabel(fileName);
	
		transferProgrssiveBarLabel.setBounds(12, 24, 420, 23);
		chatting_InputPanel.add(transferProgrssiveBarLabel);
		
		quitButton = new JButton("OK");
		quitButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		quitButton.setEnabled(false);
		quitButton.setBounds(335, 96, 97, 23);
		chatting_InputPanel.add(quitButton);
		
		setVisible(true);
	}
	
	public void AdjustProgressiveBar(int fileTransferringPercent) {
		
		progressBar.setValue(fileTransferringPercent);
		
	}
	
	// 전송이 종료되면 ( 전송이 100%가 되면 ) 창을 닫을 수 있게함.
	public void QuitTransfer() {
		this.setTitle("File Transfer Completed");
		quitButton.setEnabled(true);
	}
	
	public String getName() {
		return fileName;
	}
}
