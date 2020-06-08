package com.tftpclient;

import org.w3c.dom.ls.LSOutput;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class GUITFTP extends JPanel implements ActionListener {
	JButton send,receive,selectFile;
	JFileChooser fc;
	File currentFile;
	JTextArea log,currentFileName,localPath,toBeReceived;

	public GUITFTP(){
		super(new BorderLayout());

		//Send Panel
		JPanel sendPanel = new JPanel();
		send = new JButton("Send");
		send.addActionListener(this);
		sendPanel.add(send);

		selectFile = new JButton("Select a File");
		selectFile.addActionListener(this);
		sendPanel.add(selectFile);

		JTextArea fileName = new JTextArea("Current selected file : ");
		fileName.setEditable(false);
		sendPanel.add(fileName);

		currentFileName = new JTextArea("No current file");
		currentFileName.setEditable(false);
		sendPanel.add(currentFileName);



		//Receive panel
		JPanel receivePanel = new JPanel();
		receive = new JButton("Receive");
		receive.addActionListener(this);
		receivePanel.add(receive);

		fileName = new JTextArea("Enter the server file name here :");
		fileName.setEditable(false);
		receivePanel.add(fileName);

		toBeReceived = new JTextArea("...",1,20);
		toBeReceived.setEditable(true);
		receivePanel.add(toBeReceived);

		fc = new JFileChooser(System.getProperty("user.dir"));
		add(BorderLayout.NORTH,sendPanel);
		add(BorderLayout.CENTER,receivePanel);


		//Panel with logs
		JPanel logPanel = new JPanel();
		log = new JTextArea(30,80);
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
				currentFileName.setText(currentFile.getName());
			}
		}
		else if(e.getSource() == send){
			if(currentFile==null){
				System.out.println("Please select a file to be sent first.");
			}
			else{
				int err = TFTPClient.sendFile(currentFile.getPath());
				if (err != 0) {
					System.out.println("Error code : " + err);
				}
			}
		}
		else if(e.getSource() == receive){
			if(toBeReceived.getText().length()==0 || toBeReceived.getText().equals("...")){
				System.out.println("Please enter a file name first.");
			}
			else{
				int err = TFTPClient.receiveFile(toBeReceived.getText());
				if (err != 0) {
					System.out.println("Error code : " + err);
				}
			}
		}
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("TFTP Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(950, 650);
		frame.setResizable(false);
		frame.add( new GUITFTP());
		//frame.pack();
		frame.setVisible(true);
	}

}
