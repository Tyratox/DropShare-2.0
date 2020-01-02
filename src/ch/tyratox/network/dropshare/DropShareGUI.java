package ch.tyratox.network.dropshare;

import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.apache.commons.io.IOUtils;

import ch.tyratox.design.flatUI.java.FBorders;
import ch.tyratox.design.flatUI.java.FColors;
import ch.tyratox.design.flatUI.java.FFrame;
import ch.tyratox.design.flatUI.java.FList;

public class DropShareGUI extends FFrame implements DropTargetListener{
	
	@SuppressWarnings("unused")
	private DropTarget dt;
	
	private DropShare dropShare;
	
	ArrayList<String> file_list = new ArrayList<String>();
	
	public int listCounter = 0;
	public DefaultListModel<String> list = new DefaultListModel<String>();
	public JList<String> jlist = new JList<String>(list);

	/**
	 * 
	 */
	private static final long serialVersionUID = -6238268075868479511L;

	public DropShareGUI(FColors colors, Font font, String closeText, DropShare dropShare, boolean customCloseAction) {
		super(colors, font, closeText, customCloseAction);
		
		this.dropShare = dropShare;
		
		FBorders borders = new FBorders(colors);
		
		setBounds(100, 100, 300, 450);
		setLayout(null);
		
		dt = new DropTarget(getContentPane(), this);
		
		FList jlist = new FList(colors, borders, font, list);
		jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlist.setBounds(0, 25, 300, 425);
		
		JScrollPane jscrollpane = new JScrollPane(jlist);
		jscrollpane.setBounds(0, 25, 300, 425);
		jscrollpane.setVisible(true);
		getContentPane().add(jscrollpane);
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void drop(DropTargetDropEvent dtde) {
		
		Transferable tr = dtde.getTransferable();
        DataFlavor[] flavors = tr.getTransferDataFlavors();
        for (int i = 0; i < flavors.length; i++) {
          if (flavors[i].isFlavorJavaFileListType()) {
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
			List list = null;
			try {
				list = (List) tr.getTransferData(flavors[i]);
			} catch (UnsupportedFlavorException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
            for (int j = 0; j < list.size(); j++) {
             String file = list.get(j).toString();
             System.out.println(file);
             file_list.add(file);
            }
            sendFiles();
          }
        }

	}
	
	private void sendFiles(){
		for (int i = 0; i < dropShare.ipList.size(); i++) {
			
			for (int j = 0; j < file_list.size(); j++) {
				try {
					System.out.println("Sending " + file_list.get(j) + " to " + dropShare.ipList.get(i));
					InputStream is = new FileInputStream(new File(file_list.get(j)));
					byte[] fileData = IOUtils.toByteArray(is);
					int b = fileData.length - 1;
					System.out.println("Splitting Data....... : " + b + " b");
				    for(int x = 0;x < fileData.length;x++){
				    	byte[] temp;
				    	boolean finished = false;
				    	if(b > 1024){
				    		temp = Arrays.copyOfRange(fileData, x*1024, x*1024 + 1024);
				    		b -= 1024;
				    	}else{
				    		temp = Arrays.copyOfRange(fileData, x*1024, x*1024 + b);
				    		b -= b;
				    	}
				    	if(b <= 0){
				    		finished = true;
				    		x = fileData.length;
				    	}
			    		Socket socket = new Socket(dropShare.ipList.get(i), DropShare.filePort);
			    		dropShare.sendBytes(socket, temp, file_list.get(j).split(File.separatorChar+"")[file_list.get(j).split(File.separatorChar+"").length - 1], finished);
			    		socket.close();
				    }
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
