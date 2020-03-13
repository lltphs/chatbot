package MulticonnectServer;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class MulticonnectServer extends JFrame{
	public static ServerSocket server;
	public static Socket conn;
	public static JPanel panel;
	public static JTextArea ChatHistory;
	public static DataInputStream dis;
	public static DataOutputStream dos;
	public static String[] userOnline = new String[100];	//Store the username of client who is online at the moment
	public static int numOnline = 0; //show how many users are online
	public static String[][] userToConnect = new String[100][3]; 	//Store the username of client whom someone else want to chat with
	public static int numOfCommand = 0; //Show how many command are made
	public static int numUsers = 0;//This store the total number of users
	public static String[][] users = new String[100][2];//This store the name of all users
	int semaphore = 0;
	public MulticonnectServer() throws UnknownHostException, IOException {
		panel = new JPanel();
		ChatHistory = new JTextArea();
		this.setSize(500, 500);
		this.setVisible(true);
		this.setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		panel.setLayout(null);
		this.add(panel);
		ChatHistory.setBounds(0, 0, 500, 500);
		panel.add(ChatHistory);
		this.setTitle("Multi Connect Server");
		server = new ServerSocket(2000);
		ChatHistory.setText("Waiting for Client");
		//initialize the name of user being connected to
		for (int i = 0; i < 100; i++) {
			userToConnect[i][0] = userToConnect[i][1] = userToConnect[i][2] = userOnline[i] = "";
		}
		while(true){
			AtomicReference<String> thisuser = new AtomicReference<String>();
			try{
				conn = server.accept();
				new ServerThread(conn) {
					public void run() {
						while(true) {
							try {
								DataInputStream disType = new DataInputStream(socket.getInputStream());
								type = disType.readUTF();
								DataInputStream disusername = new DataInputStream(socket.getInputStream());
								username = disusername.readUTF();
								DataInputStream dispassword = new DataInputStream(socket.getInputStream());
								password = dispassword.readUTF();
							} catch (IOException e1) {
								break;
							  }
							
							try {
								BufferedReader txtReader = null;
								txtReader = new BufferedReader(new FileReader("C:\\Users\\Thanh Cong Nguyen\\Desktop\\clientdata.txt"));
								String user;
								while ((user = txtReader.readLine()) != null) {
								    users[numUsers][0] = user.split(",")[0];
								    users[numUsers][1] = user.split(",")[1];
								    numUsers++;
								}
								txtReader.close();
							} catch (Exception e) {
								break;
							}
							if(type.equals("login")) {
								try {
									for (int userNo = 0; userNo < numUsers; userNo++) {
										if (username.equals(users[userNo][0])&&password.equals(users[userNo][1])) {
											for (int i = 0; i < numOnline; i++) {
												if (username.equals(userOnline[i])) {
													DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
													dos.writeUTF("Login denied!");
													throw new Exception("Login denied!");
												}
											}
											ChatHistory.setText(ChatHistory.getText() + '\n' + username + " has logged in.");
											//Add client to online list
											userOnline[numOnline++] = username;
											thisuser.set(username);
											//Return list of clients whenever a new client connect to server
											DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
											for (int i = 0; i < numOnline; i++) {
												// Send the name of the i user to client
												dos.writeUTF(userOnline[i]);
											}
											//Send a message announce that all online clients are sent
											dos.writeUTF("Online client list is sent!");
											break;
										}
										if (userNo == numUsers - 1) {
											DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
											dos.writeUTF("Wrong username or password!");
											throw new Exception("Wrong username or password!");
										}
								    }
								} catch (Exception e) {
									if (e.getMessage().equals("Login denied!")||e.getMessage().equals("Wrong username or password!")) {
										continue;
									}
									for (int i = 0; i < numOnline; i++) {
										System.out.print("Hey0");
										if(thisuser.get().equals(userOnline[i])) {
											ChatHistory.setText(ChatHistory.getText() + '\n' + username + " has logged out.");
											userOnline[i] = userOnline[numOnline - 1];
											userOnline[numOnline - 1] = "";
											numOnline--;
										}
									}
							      }
								//Wait for client to tell to which user(s) it wants to connect
								//And notice client if any change in online users occurs
								//This is when the client click "Send" button
								//Receive the name of the user client want to connect to
								new ListenThread() {
									public void run() {
										while (true) {
											String connectUser;
											String connectAddress;
											try {
												DataInputStream disConnect = new DataInputStream(socket.getInputStream());
												connectUser = disConnect.readUTF();
												if (connectUser.equals("Demand Update!")) {
													DataOutputStream dosUpdate = new DataOutputStream(socket.getOutputStream());
													dosUpdate.writeUTF("Regular Update!");
													for (int i = 0; i < numOnline; i++) {
														dosUpdate.writeUTF(userOnline[i]);
														}
													//Send a message announce that all online clients are sent
													dosUpdate.writeUTF("Update Complete!");
													continue;
												}
												connectAddress = disConnect.readUTF();
												//Send the recent online users list to client
												DataOutputStream dosUpdate = new DataOutputStream(socket.getOutputStream());
												dosUpdate.writeUTF("Online users update!");
												for (int i = 0; i < numOnline; i++) {
													dosUpdate.writeUTF(userOnline[i]);
													}
												//Send a message announce that all online clients are sent
												dosUpdate.writeUTF("Online client list is sent!");
												//Search for userOnline
												for (int i = 0; i < numUsers; i++) {
													//Found it, append it to the list of userToConnect
													if(connectUser.equals(userOnline[i])) {
														//Tell client that the user is available
														dosUpdate.writeUTF("User found!");
														dosUpdate.writeUTF(connectUser);
														//Message passing to the thread that communicate with user named connectUser
														userToConnect[numOfCommand][0] = connectUser;
														userToConnect[numOfCommand][1] = connectAddress;
														userToConnect[numOfCommand][2] = username;
														numOfCommand++;
														break;
													}
													//Didn't find it, tell client to try again
													if(i == numUsers - 1) {
														dosUpdate.writeUTF("User is offline, please try again!");
													}
												}
											} catch (Exception e) {
												for (int i = 0; i < numOnline; i++) {
													System.out.print("Hey1");
													if(thisuser.get().equals(userOnline[i])) {
														ChatHistory.setText(ChatHistory.getText() + '\n' + username + " has logged out.");
														userOnline[i] = userOnline[numOnline - 1];
														userOnline[numOnline - 1] = "";
														numOnline--;
														break;
													}
												}
												break;
											}
										}
									}
								}.start();
								
									//Check if there are any client ask for connection
								try {
									while(true) {
										Thread.sleep(1000);
										for (int i = 0; i < numOfCommand; i++) {
											if (username.equals(userToConnect[i][0])) {
												//Send message to the client telling which address and which port to connect to
												DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
												dos.writeUTF("Someone wants to connect!");
												dos.writeUTF(userToConnect[i][1]); //First send the address
												dos.writeUTF(userToConnect[i][2]); //Then send the connectUser which then can be encode into a port number
												ChatHistory.setText(ChatHistory.getText() + '\n' + 
														username + " has connected with " + userToConnect[i][2]);
												userToConnect[i][0] = userToConnect[numOfCommand - 1][0];
												userToConnect[i][1] = userToConnect[numOfCommand - 1][1];
												userToConnect[i][2] = userToConnect[numOfCommand - 1][2];
												userToConnect[numOfCommand - 1][0] = userToConnect[numOfCommand - 1][1]= userToConnect[numOfCommand - 1][2] = "";
												numOfCommand--;
												break;
											}
										}
									}
								} catch (Exception e) {
									for (int i = 0; i < numOnline; i++) {
										System.out.print("Hey2");
										if(thisuser.get().equals(userOnline[i])) {
											ChatHistory.setText(ChatHistory.getText() + '\n' + username + " has logged out.");
											userOnline[i] = userOnline[numOnline - 1];
											userOnline[numOnline - 1] = "";
											numOnline--;
											break;
										}
									}
								}
								
								
							}
							
							if (type.equals("signup")) {
								//Have to write the account information back to clientdata.csv
								for (int i = 0; i < numUsers; i++) {
									if(username.equals(users[i][0])) {
										ChatHistory.setText(ChatHistory.getText() + '\n' + username + " has signed up unsuccessfully.");
										//Tell client that this username is chosen
										try {
											DataOutputStream dos = new DataOutputStream(socket.getOutputStream());									
											dos.writeUTF("This username has been chosen. Please try again!");
											break;
										} catch (IOException e) {
											break;
										}
									}
									if (i == numUsers - 1) { //username is legal
										//Print out sign up successfully
										ChatHistory.setText(ChatHistory.getText() + '\n' + username + " has signed up successfully.");
										//Write the new user information into clientdata.csv
										try {
											FileWriter txtWriter = new FileWriter("C:\\Users\\Thanh Cong Nguyen\\Desktop\\clientdata.txt",true);
											txtWriter.write(username);
											txtWriter.write(",");
											txtWriter.write(password);
											txtWriter.write("\n");
											txtWriter.flush();
											txtWriter.close();
										} catch (IOException e1) {
											break;
										}
										//Add this information to users
										users[numUsers][0] = username;
										users[numUsers++][1] = password;									
										//Announce that the sign up has been successful
										try {
											DataOutputStream dos = new DataOutputStream(socket.getOutputStream());									
												dos.writeUTF("Sign up successful!");
											} catch (IOException e) {
												break;
											}
										break;
									}
								}
							}
						}
					}
				}.start();
			}
			catch (Exception e){
				System.out.print("Hey3");
				break;
		    }
		}
	}
	public static void main(String[] args) throws UnknownHostException,
	IOException {
		new MulticonnectServer();
	}
}

class ServerThread extends Thread{
	Socket socket;
	String type = null;
	String username = null;
	String password = null;
	public ServerThread(Socket serverSocket) {
		this.socket = serverSocket;
	}
}

class ListenThread extends Thread{
}
