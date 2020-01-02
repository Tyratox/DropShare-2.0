package ch.tyratox.network.dropshare;

import java.awt.Font;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

import com.cjsavage.java.net.discovery.ServiceAnnouncer;
import com.cjsavage.java.net.discovery.ServiceFinder;
import com.cjsavage.java.net.discovery.ServiceInfo;
import com.cjsavage.java.net.discovery.ServiceFinder.Listener;

import ch.tyratox.design.flatUI.java.FColors;
import ch.tyratox.network.deviceDiscovery.DeviceDiscovery;

public class DropShare {
	
	public DropShareGUI gui;
	
	public static final String serviceId = "b2238f941b4be98b3f77d8568c04fb63"; //ch.tyratox.network.dropshare => b2238f941b4be98b3f77d8568c04fb63
	public String serverName = "DropShareServer_";
	public static final int detectPort = 3218;
	public static final int filePort = 5569;
	public static final boolean isSecure = true;
	
	public ServiceFinder finder;
	public ServiceAnnouncer announcer;
	
	public static final Random random = new Random();
	
	private ServerSocket listener;
	
	public ArrayList<String> ipList = new ArrayList<String>();
	
	private boolean running = true;
	
	private String ip;
	
	private String tempFileName;
	private byte[] tempFileData;
	private boolean isFileComplete = false;
	
	public static void main(String[] args){
		new DropShare();
	}
	
	public DropShare(){
		serverName = serverName+random.nextInt(999999);
		
		Socket s;
		try {
			s = new Socket("google.com", 80);
			ip = s.getLocalAddress().getHostAddress();
			s.close();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		createTCPServerSocket();
		
		FColors colors = new FColors();
		colors.frameBackground = FColors.blue;
		colors.listBackground = FColors.blue;
		Font font = Fonts.lato(getClass(), 14f);
		
		gui = new DropShareGUI(colors, font, "Close", this, false);
		
		try {
			System.out.println("Init Announcer with SID " + serviceId);
			announcer = DeviceDiscovery.showServiceInNetwork(serviceId, serverName, detectPort, isSecure);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Init Finder with SID " + serviceId);
		scanNetwork();
		
		gui.setVisible(true);
	}
	
	private void createTCPServerSocket() {
		//Add Shutdown hook
				Runtime.getRuntime().addShutdownHook(new Thread() {
				    public void run() {
				        try {
				        	running = false;
							listener.close();
							finder.stopListening();
							announcer.stopListening();
						} catch (IOException e) {
							e.printStackTrace();
						}
				    }
				});
		try {
			listener = new ServerSocket(filePort);
			new Thread(){
				public void run(){
					while(running){
			            try {
							Socket socket = listener.accept();
							if(socket != null){
								readBytes(socket);
								socket.close();
								if(isFileComplete){
									System.out.println("Saving File.....");
									FileOutputStream fos = new FileOutputStream(new File(tempFileName));
									fos.write(tempFileData);
									fos.close();
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
			         }
				}
			}.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void scanNetwork(){
		finder = DeviceDiscovery.searchServiceInNetwork(serviceId, mListener);
	}
	
	private Listener mListener = new Listener() {
	    @Override
	    public void serverFound(ServiceInfo si, int requestId, ServiceFinder finder) {
	    	if(si.getServiceHost().equalsIgnoreCase(ip) != true){
		        System.out.println("Found service provider named " + si.getServerName() + " at " + si.getServiceHost() + ":" + si.getServicePort());
		        ipList.add(si.getServiceHost());
		        gui.list.addElement(si.getServerName() + " - " + si.getServiceHost());
	    	}else{
	    		System.out.println("Me: " + ip);
	    	}
	    }

	    @Override
	    public void listenStateChanged(ServiceFinder finder, boolean listening) {
	    	
	    }
	};
	
	public void sendBytes(Socket socket, byte[] myByteArray, String fileName, boolean finished) throws IOException {
	    sendBytes(socket, myByteArray, 0, myByteArray.length, fileName, finished);
	}

	public void sendBytes(Socket socket, byte[] myByteArray, int start, int len, String fileName, boolean finished) throws IOException {
	    if (len < 0)
	        throw new IllegalArgumentException("Negative length not allowed");
	    if (start < 0 || start >= myByteArray.length)
	        throw new IndexOutOfBoundsException("Out of bounds: " + start);
	    
	    OutputStream out = socket.getOutputStream(); 
	    DataOutputStream dos = new DataOutputStream(out);
	    
	    dos.writeUTF(fileName);
	    dos.writeInt(len);
	    if (len > 0) {
	        dos.write(myByteArray, start, len);
	    }
	    if(finished){
	    	dos.writeUTF("done");
	    }
	}
	
	public byte[] readBytes(Socket socket) throws IOException {
		
	    InputStream in = socket.getInputStream();
	    DataInputStream dis = new DataInputStream(in);
	    
	    String fName = dis.readUTF();
	    byte[] data;
	    if(tempFileName != null && tempFileName.equals(fName)){
	    	int len = dis.readInt();
		    data = new byte[len];
		    if (len > 0) {
		        dis.readFully(data);
		    }
		    byte[] newData = new byte[tempFileData.length + len];
		    System.arraycopy(tempFileData, 0, newData, 0, tempFileData.length);
		    System.arraycopy(data, 0, newData, tempFileData.length, data.length);
		    tempFileData = newData;
	    }else{
	    	System.out.println("New file...");
	    	tempFileName = fName;
	    	int len = dis.readInt();
		    data = new byte[len];
		    if (len > 0) {
		        dis.readFully(data);
		    }
		    tempFileData = data;
	    }
	    String done;
	    try{
	    if(dis.available() >= 0 && (done=dis.readUTF()) != null){
	    	if(done.equalsIgnoreCase("done")){
	    		System.out.println("Done! Got file completely!");
	    		isFileComplete = true;
	    	}else{
	    		isFileComplete = false;
	    	}
	    }else{
	    	isFileComplete = false;
	    }
	    }catch(Exception e){
	    	isFileComplete = false;
	    }
	    return data;
	}

}
