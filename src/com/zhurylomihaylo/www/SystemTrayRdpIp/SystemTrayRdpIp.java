package com.zhurylomihaylo.www.SystemTrayRdpIp;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import com.zhurylomihaylo.www.SystemTrayRdpIp.Messages;

public class SystemTrayRdpIp {

	private TrayIcon trayIcon;
	private String ip;

	public static void main(String[] args) {
		try {
			new SystemTrayRdpIp().doAllActions();
		} catch (Throwable e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Ups", JOptionPane.ERROR_MESSAGE);
		}
	}

	void doAllActions() {
		if (!SystemTray.isSupported()) {
			throw new RuntimeException("System tray is not supported.");
		}
		//=========================================
		PopupMenu popup = new PopupMenu();
		
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String optionYes = "Yes";
				String optionNo = "No";
				String [] options = {optionYes, optionNo};
				int result = JOptionPane.showOptionDialog(null, "Are you sure to exit?", null, JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, options, optionYes);
				if (result == 0)
					System.exit(0);
			}
		});
		popup.add(exitItem);
		//=========================================
		Image image = new ImageIcon(getClass().getResource("ip_address.png")).getImage();

		trayIcon = new TrayIcon(image, "Left-click mouse to determine your IP-address", popup);
		trayIcon.setImageAutoSize(true);
		
		trayIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				super.mouseClicked(arg0);
				if (arg0.getButton() == MouseEvent.BUTTON1)
					showIP();
			}
		});
		//=========================================
		SystemTray tray = SystemTray.getSystemTray();		
		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			throw new RuntimeException("Tray icon could not be added");
		}
		//=========================================
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				Process process;
				try {
					process = new ProcessBuilder("netstat").start();
					InputStream is = process.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String line;
					String localPort;
					String remoteHost;
					
					String regexp = ":([0-9]+)\\s+(\\S+):";
					Pattern pattern = Pattern.compile(regexp);

					while ((line = br.readLine()) != null) {
						Matcher matcher = pattern.matcher(line); 
						if (matcher.find()) {
							localPort = matcher.group(1);
							if (localPort.equals("3389")) { 
								remoteHost = matcher.group(2);
								setIp(remoteHost);
								return;
							}
						}
					}
					setIp("undefined"); 
					System.out.println("Done"); 
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(null, e1.getMessage(), "Ups", JOptionPane.ERROR_MESSAGE);
					e1.printStackTrace();
				}
			}
		});
		
		thread.start();
	}

	private void showIP() {
		if (getIp() == null) {
			trayIcon.displayMessage(null, "Your remote IP-address is defining now. Try again later.", TrayIcon.MessageType.INFO); 
		} else if (getIp().equals("undefined")) { 
			trayIcon.displayMessage(null, "Failed to determine your remote IP-address.", TrayIcon.MessageType.INFO); 
		} else {
			trayIcon.displayMessage("Your remote IP-address", getIp(), TrayIcon.MessageType.INFO);  
		};
	}
	
	private synchronized String getIp() {
		return ip;
	}

	private synchronized void setIp(String host) {
		System.out.println(host);
		String regexp = "\"^(25[0-5]|2[0-4]\\\\d|[0-1]?\\\\d?\\\\d)(\\\\.(25[0-5]|2[0-4]\\\\d|[0-1]?\\\\d?\\\\d)){3}$\""; 
		Pattern pattern = Pattern.compile(regexp);
		Matcher matcher = pattern.matcher(host); 
		if (!matcher.find()) {
			try {
				InetAddress address = InetAddress.getByName(host);
				host = address.getHostAddress();
			} catch (UnknownHostException e) {
				System.out.println(e);
			}
		}
		this.ip = host;
	}

}
