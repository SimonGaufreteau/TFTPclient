package com.tftpclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class GUITFTP extends JPanel implements ActionListener {
	JButton send,receive,selectFile;
	JFileChooser fc;
	File currentFile;
	JTextArea log,currentFileName;

	public GUITFTP(){
		super(new BorderLayout());

		//Panel with buttons
		JPanel panel = new JPanel();
		send = new JButton("Send");
		send.addActionListener(this);
		panel.add(send);

		/*receive = new JButton("Receive");
		panel.add(receive);*/

		selectFile = new JButton("Select a File");
		selectFile.addActionListener(this);
		panel.add(selectFile);

		currentFileName = new JTextArea();
		panel.add(currentFileName);

		fc = new JFileChooser(System.getProperty("user.dir"));
		add(BorderLayout.NORTH,panel);

		//Panel with logs
		JPanel logPanel = new JPanel();
		log = new JTextArea(50,80);
		log.setEditable(false);
		Font consola = Font.getFont("Consola");
		if(consola!=null)
			log.setFont(consola);
		logPanel.add(new JScrollPane(log));
		MessageConsole mc = new MessageConsole(log);
		mc.redirectOut();
		mc.redirectErr(Color.RED,null);
		mc.setMessageLines(2000);
		add(BorderLayout.SOUTH,logPanel);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == selectFile){
			//Handle selectFile action.
			int returnVal = fc.showOpenDialog(GUITFTP.this);
			if (returnVal == JFileChooser.APPROVE_OPTION){
				currentFile = fc.getSelectedFile();
				System.out.println("Current file selected : "+currentFile.getName());
				currentFileName.setText("Current file :"+currentFile.getName());
			}
		}
		else if(e.getSource() == send){
			if(currentFile==null){
				System.out.println("Please select a file first.");
			}
			else
				TFTPClient.sendFile(currentFile.getPath());
		}
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("TFTP Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(900, 900);
		frame.add( new GUITFTP());
		//frame.pack();
		frame.setVisible(true);
	}

}
