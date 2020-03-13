package MulticonnectClient;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class MulticonnectClient extends JFrame implements ActionListener {
	static Socket conn;
	JPanel panel;
	JTextField NewMsg;
	JTextArea ChatHistory;
	JTextArea Message;
	JTextField username;
	JTextArea usernameDisplay;
	JTextField password;
	JTextArea passwordDisplay;
	JButton Send;
	JButton login;
	JButton signup;
	int semaphore = 0;
	
	public MulticonnectClient() throws UnknownHostException, IOException {
		panel = new JPanel();
		username = new JTextField();
		usernameDisplay = new JTextArea("Username:");
		passwordDisplay = new JTextArea("Password:");
		password = new JPasswordField();
		ChatHistory = new JTextArea();
		Message = new JTextArea();
		this.setTitle("Welcome!");
		this.setSize(500, 500);
		this.setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		panel.setLayout(null);
		this.add(panel);
		username.setBounds(175, 150, 150, 25);
		panel.add(username);
		usernameDisplay.setBounds(95, 153, 75, 25);
		usernameDisplay.setOpaque(false);
		usernameDisplay.setBackground(new Color(0, 0, 0, 0));
		panel.add(usernameDisplay);
		password.setBounds(175, 180, 150, 25);
		panel.add(password);
		passwordDisplay.setBounds(95, 183, 75, 25);
		passwordDisplay.setOpaque(false);
		passwordDisplay.setBackground(new Color(0, 0, 0, 0));
		panel.add(passwordDisplay);
		Message.setOpaque(false);
		Message.setBackground(new Color(0, 0, 0, 0));
		panel.add(Message);
		login = new JButton("Log in");
		login.setBounds(105, 220, 95, 30);
		panel.add(login);
		signup = new JButton("Sign up");
		signup.setBounds(215, 220, 95, 30);
		panel.add(signup);
		login.addActionListener(this);
		signup.addActionListener(this);
		Send = new JButton("Send");
		Send.addActionListener(this);
		String serverHost = "127.0.0.1";
		conn = new Socket(InetAddress.getByName(serverHost), 2000);
	}
	String[] clientList = new String[100];
	int numOnlineClient = 0;
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == login) {
			try {
				DataOutputStream dostype = new DataOutputStream(
						conn.getOutputStream());
				dostype.writeUTF("login");
				DataOutputStream dosusername = new DataOutputStream(
						conn.getOutputStream());
				dosusername.writeUTF(username.getText());
				DataOutputStream dospassword = new DataOutputStream(
						conn.getOutputStream());
				dospassword.writeUTF(password.getText());
				//Wait for server to authenticate the login
				//Receive a list of clients online
				while(true) {
					DataInputStream dis = new DataInputStream(conn.getInputStream());
					String content = dis.readUTF();
					if (content.equals("Online client list is sent!")) {
						Message.setBounds(100, 260, 200, 30);
						Message.setText("Online client list is sent!");
						this.setTitle("Welcome " + username.getText() + "!");
						panel.repaint();
						break;					}
					if (content.equals("Wrong username or password!")) {
						Message.setBounds(100, 260, 200, 30);
						Message.setText("Wrong username or password!");
						panel.repaint();
						break;
					}
					if (content.equals("Login denied!")) {
						Message.setBounds(100, 260, 200, 30);
						Message.setText("Login denied!");
						panel.repaint();
						break;
					}
					clientList[numOnlineClient++] = content;//Append online clients to clientList 
				}
				//Print list of online client on the board
				if (Message.getText().equals("Wrong username or password!")) throw new Exception();
				if (Message.getText().equals("Login denied!")) throw new Exception();
				panel.removeAll();
				NewMsg = new JTextField();
				ChatHistory = new JTextArea();
				this.setSize(500, 500);
				this.setVisible(true);
				setDefaultCloseOperation(EXIT_ON_CLOSE);
				panel.setLayout(null);
				this.add(panel);
				ChatHistory.setBounds(20, 20, 450, 360);
				panel.add(ChatHistory);
				NewMsg.setBounds(20, 400, 340, 30);
				panel.add(NewMsg);
				Send.setBounds(375, 400, 95, 30);
				panel.add(Send);
				ChatHistory.setText("These users are online. Choose one to connect with:");
				for (int i = 0; i < numOnlineClient; i++) {
					ChatHistory.setText(ChatHistory.getText() + "\n" + i + ". " + clientList[i]);
				}
				panel.repaint();
				new ListenThread() {
					public void run() {
						//Listening to the server for new connection
						while(true) {
							try {
								DataInputStream disUpdate = new DataInputStream(conn.getInputStream());
								String content = disUpdate.readUTF();
								//If there is a demand, connect to it
								if (content.equals("Someone wants to connect!")) {
									String address = disUpdate.readUTF();
									String user = disUpdate.readUTF();
									Socket newsocket = new Socket(InetAddress.getByName(address),user.hashCode()%100 + 1124);
									new ClientThread(newsocket,user){}.start();
								}
								if (content.equals("Regular Update!")) {
									numOnlineClient = 0;
									ChatHistory.setText("These users are online, choose one to connect with:");
									while (true) {
										content = disUpdate.readUTF();
										if (content.equals("Update Complete!")) {
											break;
										}
										clientList[numOnlineClient] = content;
										ChatHistory.setText(ChatHistory.getText() + "\n" + 
										numOnlineClient + ". " + clientList[numOnlineClient++]);
									}
								}
								if(content.equals("Online users update!")) {
									numOnlineClient = 0;
									while (true) {
										content = disUpdate.readUTF();
										if (content.equals("Online client list is sent!")) {
											break;
										}
										clientList[numOnlineClient++] = content;
									}
									String notification = disUpdate.readUTF();
									if (notification.equals("User found!")) {
										try {
											String otherUser = disUpdate.readUTF();
											ServerSocket socket = new ServerSocket(username.getText().hashCode()%100 + 1124);
											Socket chat = socket.accept();
											new ClientThread(chat,otherUser){}.start();
											socket.close();
										} catch (IOException e1) {
										}
									}
									else { //notification == "User is offline, please try again!"
										ChatHistory.setText(ChatHistory.getText() + "\nPlease try again!");
									}
								}
							} catch (IOException e) {
								}
						}
					}
				}.start();
				
				new UpdateThread() {
					public void run() {
						while (true) {
							try {
								Thread.sleep(1000);
								if (semaphore > 0) continue;
								semaphore++;
								new DataOutputStream(conn.getOutputStream()).writeUTF("Demand Update!");
								semaphore--;
							} catch (InterruptedException e) {
							} catch (IOException e) {
							}
						}
					}
				}.start();
				
			} catch (Exception e1) {
				Message.setBounds(150, 260, 95, 70);
				Message.setText("Logging in fail:Network Error");
				panel.repaint();
			}
		}
		
		if (e.getSource() == Send) {
			try {
				//This will send the name of user to whom this client wants to connect to the server 
				//Tell server which client to connect
				while (semaphore > 0);
				semaphore++;
				DataOutputStream dosConnect = new DataOutputStream(conn.getOutputStream());
				for (int i = 0; i < numOnlineClient; i++) {
					if((Integer.parseInt(NewMsg.getText()) == i)&&(clientList[i] !=  username.getText())) {
						dosConnect.writeUTF(clientList[i]);
						dosConnect.writeUTF("127.0.0.1");
						break;
					}
				}
				semaphore--;
			} catch (IOException e1) {
				System.exit(0);
			  }
		} 

		if (e.getSource() == signup && !username.getText().contentEquals("") && !password.getText().contentEquals("")) {
			try {
				DataOutputStream dostype = new DataOutputStream(
						conn.getOutputStream());
				dostype.writeUTF("signup");
				DataOutputStream dosUsername = new DataOutputStream(
						conn.getOutputStream());
				dosUsername.writeUTF(username.getText());
				DataOutputStream dosPassword = new DataOutputStream(
						conn.getOutputStream());
				dosPassword.writeUTF(password.getText());
				//Wait for server to accept the sign up
				while (true) {
					DataInputStream dis = new DataInputStream(conn.getInputStream());
					String content = dis.readUTF();
					if (content.equals("Sign up successful!")) {
						Message.setBounds(150, 260, 300, 30);
						Message.setText("Sign up successful!");
						panel.repaint();
						break;
					}
					if (content.equals("This username has been chosen. Please try again!")) {
						Message.setBounds(75, 260, 300, 30);
						Message.setText("This username has been chosen. Please try again!");
						panel.repaint();
						break;
					}
				}
			} catch (Exception e1) {
				Message.setText("Sign up fail:Network Error");//////////hb.exit(0);
				panel.repaint();
			}
		}
	}
	
	public static void main(String[] args) throws UnknownHostException,
			IOException {
		new MulticonnectClient();
	}
}
class ClientThread extends Thread implements ActionListener{
	Socket chat;
	String otherUser;
	JFrame frame;
	JPanel panel;
	JTextField NewMsg;
	JTextPane ChatHistory;
	StyledDocument doc;
	SimpleAttributeSet left;
    SimpleAttributeSet right;
	JButton Send;
	JButton SendFile;
	JButton SmileEmoji;
	JButton LikeEmoji;
	JButton HeartEmoji;
	public ClientThread(Socket chat,String otherUser) {
		this.chat = chat;
		this.otherUser = otherUser;
	}
	public void run() {
		frame = new JFrame();
		panel = new JPanel();
		NewMsg = new JTextField();
		ChatHistory = new JTextPane();
		doc = ChatHistory.getStyledDocument();
		left = new SimpleAttributeSet();
		right = new SimpleAttributeSet();
		StyleConstants.setAlignment(left, StyleConstants.ALIGN_LEFT);
        StyleConstants.setForeground(left, Color.RED);
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setForeground(right, Color.BLUE);
		Send = new JButton("Send");
		SendFile = new JButton("Send File");
		Font font = new Font("Serif",Font.ITALIC,20);
		SmileEmoji = new JButton("Smile");
		LikeEmoji = new JButton("Like");
		HeartEmoji = new JButton("Heart");
		frame.setSize(510, 510);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		panel.setLayout(null);
		frame.add(panel);
		ChatHistory.setBounds(20, 20, 450, 360);
		ChatHistory.setFont(font);
		panel.add(ChatHistory);
		NewMsg.setBounds(20, 390, 450, 30);
		panel.add(NewMsg);
		Send.setBounds(255, 430, 95, 30);
		panel.add(Send);
		SendFile.setBounds(365, 430, 95, 30);
		panel.add(SendFile);
		SmileEmoji.setBounds(160, 430, 80, 30);
		panel.add(SmileEmoji);
		LikeEmoji.setBounds(20, 430, 65, 30);
		panel.add(LikeEmoji);
		HeartEmoji.setBounds(90, 430, 65, 30);
		panel.add(HeartEmoji);
		Send.addActionListener(this);
		SendFile.addActionListener(this);
		SmileEmoji.addActionListener(this);
		LikeEmoji.addActionListener(this);
		HeartEmoji.addActionListener(this);
		frame.setTitle("Connect with " + otherUser);
		panel.repaint();
		try {
		    doc.insertString(doc.getLength(), "You are connected with " + otherUser, left );
            doc.setParagraphAttributes(doc.getLength(), 1, left, false);
		} catch (BadLocationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		while(true) {
			try {
				DataInputStream dis = new DataInputStream(chat.getInputStream());
				String string = dis.readUTF();
				if(string.contentEquals("I am going to send a file to you!")) {
					try {
						final String FILE_TO_RECEIVED = "C:\\Users\\Thanh Cong Nguyen\\Downloads\\" + dis.readUTF();
						final int FILE_SIZE = Integer.parseInt(dis.readUTF()) + 10;
						int bytesRead;
					    int current = 0;
					    FileOutputStream fos = null;
					    BufferedOutputStream bos = null;
					    byte [] mybytearray  = new byte [FILE_SIZE];
					    fos = new FileOutputStream(FILE_TO_RECEIVED);
					    bos = new BufferedOutputStream(fos);
					    bytesRead = chat.getInputStream().read(mybytearray,0,mybytearray.length);
					    current = bytesRead;
					    bos.write(mybytearray, 0 , current);
					    bos.flush();
					    bos.close();
					    fos.close();
					      System.out.print("Receive file successfully!");
					} catch (Exception e) {
						doc.insertString(doc.getLength(), "Receive file fail", left );
			            doc.setParagraphAttributes(doc.getLength(), 1, left, false);
						continue;
					}
				}
				doc.insertString(doc.getLength(), '\n' + otherUser + ": " + string, left );
	            doc.setParagraphAttributes(doc.getLength(), 1, left, false);
	            } catch (Exception e1) {
	            	try {
					    doc.insertString(doc.getLength(), '\n' + "Message receiving fail.", left );
			            doc.setParagraphAttributes(doc.getLength(), 1, left, false);
					} catch (BadLocationException e2) {
					}
				break;
			}
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if ((e.getSource() == Send) && (NewMsg.getText() != "")) {
            try {
				doc.insertString(doc.getLength(), "\nMe: " + NewMsg.getText(), right );
	            doc.setParagraphAttributes(doc.getLength(), 1, right, false);
			} catch (BadLocationException e2) {
			}
			try {
				DataOutputStream dos = new DataOutputStream(chat.getOutputStream());
				dos.writeUTF(NewMsg.getText());
			} catch (Exception e1) {
				try {
					doc.insertString(doc.getLength(), '\n'
							+ "Message sending fail:Network Error", right );
		            doc.setParagraphAttributes(doc.getLength(), 1, right, false);
				} catch (BadLocationException e2) {
				}
			}
			NewMsg.setText("");
		}
		
		if ((e.getSource() == SendFile) && (NewMsg.getText() != "")) {
			try {
				new DataOutputStream(chat.getOutputStream()).writeUTF("I am going to send a file to you!");
				new DataOutputStream(chat.getOutputStream()).writeUTF(NewMsg.getText());
			} catch (IOException e1) {
				try {
					doc.insertString(doc.getLength(), '\n'
							+ "File sending fail:Network Error", right );
		            doc.setParagraphAttributes(doc.getLength(), 1, right, false);
				} catch (BadLocationException e2) {
				}
			}
			FileInputStream fis = null;
		    BufferedInputStream bis = null;
			try {
				final String FILE_TO_SEND = "C:\\Users\\Thanh Cong Nguyen\\Desktop\\" + NewMsg.getText();
				System.out.print(FILE_TO_SEND);
			    File myFile = new File(FILE_TO_SEND);
				new DataOutputStream(chat.getOutputStream()).writeUTF(Integer.toString((int) myFile.length()));
			    byte[] mybytearray = new byte[(int) myFile.length()];
				fis = new FileInputStream(myFile);				
			    bis = new BufferedInputStream(fis);
			    bis.read(mybytearray, 0, mybytearray.length);
			    chat.getOutputStream().write(mybytearray,0,mybytearray.length);
			    bis.close();
			    System.out.print("Send file successfully!");
			    try {
				    doc.insertString(doc.getLength(), '\n' + "File sent.", right );
		            doc.setParagraphAttributes(doc.getLength(), 1, right, false);
				} catch (BadLocationException e2) {
				}
			    NewMsg.setText("");
			} catch (FileNotFoundException e1) {
				try {
				    doc.insertString(doc.getLength(), '\n' + "FILE INVALID", right );
		            doc.setParagraphAttributes(doc.getLength(), 1, right, false);
				} catch (BadLocationException e2) {
				}			} catch (IOException e1) {
					try {
				    doc.insertString(doc.getLength(), '\n' + "File sending fail.", right );
		            doc.setParagraphAttributes(doc.getLength(), 1, right, false);
				} catch (BadLocationException e2) {
				}
			}
		}
		
		if(e.getSource() == LikeEmoji) {
			NewMsg.setText(NewMsg.getText() + "\ud83d\udc4d");
		}
		
		if (e.getSource() == SmileEmoji) {
			NewMsg.setText(NewMsg.getText() + "\ud83d\ude04");
		}
		
		if (e.getSource() == HeartEmoji) {
			NewMsg.setText(NewMsg.getText() + "\u2764");
		}
	}
}

class ListenThread extends Thread {
}

class UpdateThread extends Thread {
}