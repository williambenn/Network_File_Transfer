import edu.utulsa.unet.RReceiveUDPI;

import java.io.*;
import java.net.DatagramPacket;
import edu.utulsa.unet.UDPSocket; //import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;

public class RReceiveUDP implements RReceiveUDPI{
	
	//Mode of Operation Variables
	private static int STOPANDWAIT = 0;
	private static int SLIDINGWINDOW = 1;
	private int modeOfOperation = STOPANDWAIT;

	//Mode Parameters
	private long windowSize = 256;

	//File Name Variables
	private String fileName;

	//Network Variables
	public int localPort; //Local Port
	public String hostName; //Address to be sent to
	public int portNumber = 12987; //Port to be sent to

	public RReceiveUDP() {
	}

	public boolean setMode(int mode) {
		modeOfOperation = mode;

		if(modeOfOperation == STOPANDWAIT 
				|| modeOfOperation == SLIDINGWINDOW)
			return true;
		else
			return false;
	}

	public int getMode() {
		return modeOfOperation;
	}

	public boolean setModeParameter(long n) {

		if(modeOfOperation == SLIDINGWINDOW) {
			windowSize = n;
			return true;
		}else
			return false;
	}

	public long getModeParameter() {
		return windowSize;
	}

	public void setFilename(String fname) {
		fileName = fname;
	}

	public String getFilename() {
		return fileName;
	}

	public boolean setLocalPort(int port) {
		portNumber = port;
		return true;
	}

	public int getLocalPort() {
		return localPort;
	}

	public boolean receiveFile() {
		if(modeOfOperation == STOPANDWAIT){
			stopAndWait();
		}else if(modeOfOperation == SLIDINGWINDOW) {
			slidingWindow();
		}else
			return false;
		return true;
	}

	public void stopAndWait() {
		try {			
			UDPSocket socket = new UDPSocket(localPort);
			int MTU = socket.getSendBufferSize();
			int msgSent;

			byte [] buffer = new byte[MTU];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			socket.receive(packet);

			System.out.println("Receiving file from " + packet.getAddress() + ":" + localPort +
					" to " + InetAddress.getLocalHost() + ":" + portNumber + " with " + buffer.length);
			System.out.println("Using Stop-and-Wait...");

			byte [] ack = new byte[16];

			ack[0] = buffer[0];
			ack[1] = buffer[1];

			msgSent = (buffer[0] & 0xFF * 255) + (buffer[1] & 0xFF * 255);

			System.out.println("Message " + msgSent + " sent with " + buffer.length + " of actual data...");

			DatagramPacket acknow = new DatagramPacket(ack, ack.length,
					packet.getAddress(), packet.getPort());

			socket.send(acknow);

			byte [] firstPacket = Arrays.copyOfRange(buffer, 2, buffer.length);
			int numPackets, packetsLeft;

			if(buffer[0] == (byte) 00) {
				numPackets = buffer[1] & 0xFF * 255;
			}else {
				numPackets = (buffer[0] & 0xFF * 255) + (buffer[1] & 0xFF * 255);
			}

			packetsLeft = numPackets - 1;

			byte[][] receivedPackets = new byte [numPackets][];

			while(packetsLeft != 0) {
				socket.receive(packet);

				receivedPackets[numPackets] = Arrays.copyOfRange(buffer, 2, buffer.length);

				ack[0] = buffer[0];
				ack[1] = buffer[0];

				msgSent = (buffer[0] & 0xFF * 255) + (buffer[1] & 0xFF * 255);

				System.out.println("Message " + msgSent + " sent with " + buffer.length + " of actual data...");

				acknow = new DatagramPacket(ack, ack.length,
						packet.getAddress(), packet.getPort()); 

				socket.send(acknow);
			}

			int offset = receivedPackets[0].length;

			byte[] receivedFile = new byte[numPackets * offset];

			if(numPackets == 1) {
				for(int k = 0; k < firstPacket.length; k++) {
					receivedFile[k] = firstPacket[k];
				}
			}
			else {
				for(int i = numPackets - 1; i > 0; i--) {
					for(int j = 0; j < offset; i++) {
						receivedFile[i-1* offset] = receivedPackets[i][j];
					}
				}

				for(int k = 0; k < firstPacket.length; k++) {
					receivedFile[numPackets - 1 * offset] = firstPacket[k];
				}
			}

			writeToFile(receivedFile);

			System.out.println("Successfully received " + fileName + " (" + buffer.length + " bytes) in ");
			socket.close();
		}catch(Exception e) {e.printStackTrace(); }
	}

	public void writeToFile(byte [] byteFile) {
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(fileName);
			fos.write(byteFile);
		}catch(IOException e){
			e.printStackTrace();
		}finally {
			try {
				fos.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}

	public void slidingWindow() {

	}
	
	public static void main(String [] args) {
		RReceiveUDP receiver = new RReceiveUDP();
		receiver.setMode(0);
		receiver.setModeParameter(512);
		receiver.setFilename("less_important.txt");
		receiver.setLocalPort(32456);
		receiver.receiveFile();
	}
}
