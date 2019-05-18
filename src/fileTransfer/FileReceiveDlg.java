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

public class FileReceiveDlg extends JFrame{
	
	private String fileName;
	
	private JProgressBar progressBar;
	
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
		chatting_InputPanel.add(progressBar);

		Label transferProgrssiveBarLabel = new Label(fileName);
		transferProgrssiveBarLabel.setBounds(12, 24, 231, 23);
		chatting_InputPanel.add(transferProgrssiveBarLabel);
		
		setVisible(true);
	}
	
	public void AdjustProgressiveBar(float fileTransferringPercent) {
		
	}
	
	// 전송이 종료되면 ( 전송이 100%가 되면 ) 창을 닫음.
	public void QuitTransfer() {
		setVisible(false);
	}
	
	public String getName() {
		return fileName;
	}

}
