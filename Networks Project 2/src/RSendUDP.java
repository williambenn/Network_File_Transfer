import edu.utulsa.unet.RSendUDPI;
import java.net.DatagramPacket;
import edu.utulsa.unet.UDPSocket; //import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.*;

public class RSendUDP implements RSendUDPI{

	//Mode of Operation Variables
	private static int STOPANDWAIT = 0;
	private static int SLIDINGWINDOW = 1;
	private int modeOfOperation = STOPANDWAIT;

	//Mode Parameters
	private long windowSize = 256;

	//File Name Variables
	private String fileName;

	//Timeout Variables
	private int timeoutMS = 1000; //timeout in milliseconds

	//Network Variables
	public int localPort; //Local Port
	public String hostName; //Address to be sent to
	public int portNumber = 12987; //Port to be sent to

	public RSendUDP() {
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

	public boolean setTimeout(long timeout) {
		if(timeout != 0) {
			timeoutMS = (int) timeout;		
			return true;
		}else
			return false;
	}

	public long getTimeout() {
		return timeoutMS;
	}

	public boolean setLocalPort(int port) {
		portNumber = port;
		return true;
	}

	public int getLocalPort() {
		return localPort;
	}

	public boolean setReceiver(InetSocketAddress receiver) {
		hostName = receiver.getHostString();
		portNumber = receiver.getPort();
		return true;
	}

	public InetSocketAddress getReceiver() {
		return (new InetSocketAddress(hostName, portNumber));
	}

	public boolean sendFile() {
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
			File file = new File(fileName);
			FileInputStream fStream = new FileInputStream(file);
			byte [] buffer = new byte[(int)file.length()];
			fStream.read(buffer);

			System.out.println("Sending " + fileName + " from " + InetAddress.getLocalHost() + ":" + localPort + 
					" to " + hostName + ":" + portNumber + " with " + buffer.length);
			System.out.println("Using Stop-and-Wait...");
			
			
			int ackReceived;
			int msgSent;

			if(MTU > (buffer.length + 2)) {
				byte [] sendBuffer = new byte[buffer.length + 2];
				sendBuffer[0] = (byte) 0;
				sendBuffer[1] = (byte) 0;
				for(int i = 2; i < sendBuffer.length; i++) {
					sendBuffer[i] = buffer[i];
				}

				DatagramPacket dp;
				byte [] ack = new byte[16];

				while(ack == null) {
					dp = new DatagramPacket(sendBuffer, buffer.length,
							InetAddress.getByName(hostName), portNumber);

					socket.send(dp);

					msgSent = (sendBuffer[0] & 0xFF * 255) + (sendBuffer[1] & 0xFF * 255);

					System.out.println("Message " + msgSent + " sent with " + buffer.length + " of actual data...");

					socket.setSoTimeout(timeoutMS);
					DatagramPacket packet = new DatagramPacket(ack, ack.length);


					try {
						socket.receive(packet);

						ackReceived = (ack[0] & 0xFF) + (ack[1] & 0xFF * 255);

						System.out.println("Message " + ackReceived + " Acknowledged...");

					}catch(SocketTimeoutException ste) {
						socket.send(dp);
						continue;
					}
				}
			}else {

				int sbs = (int) Math.ceil((double)buffer.length / (MTU - 2));

				int sml = (int) Math.ceil((double)buffer.length / sbs) + 2;

				byte [][] smallBuf = new byte[sbs][sml];

				int offset;		

				for(int i = 0; i < sbs; i++) {
					smallBuf[i][0] = (byte) (i/256);
					smallBuf[i][1] = (byte) (i%256);
					offset = sml * i;

					for(int j = 2; j < (sml); j++) {
						smallBuf[i][j] = buffer[j + offset];
					}
				}

				int packetsSent = sbs;
				DatagramPacket dp;
				while(packetsSent != 0) {
					dp = new DatagramPacket(smallBuf[packetsSent], smallBuf[packetsSent].length,
							InetAddress.getByName(hostName), portNumber);

					socket.send(dp);

					msgSent = (smallBuf[packetsSent][0] & 0xFF * 255) + (smallBuf[packetsSent][1] & 0xFF * 255);

					System.out.println("Message " + msgSent + " sent with " + buffer.length + " bytes of actual data...");

					socket.setSoTimeout(timeoutMS);

					byte [] ack = new byte[16];

					DatagramPacket packet = new DatagramPacket(ack, ack.length);


					try {
						socket.receive(packet);

						ackReceived = (ack[0] & 0xFF) + (ack[1] & 0xFF * 255);
						System.out.println("Message " + ackReceived + " Acknowledged...");

					}catch(SocketTimeoutException ste) {
						continue;
					}
					packetsSent--;

				}				
			}
			
			System.out.println("Successfully transferred " + fileName + " (" + buffer.length + " bytes) in ");
			socket.close();
			fStream.close();
		}catch(Exception e) {e.printStackTrace(); }
	}

	public void slidingWindow() {

	}
	
	public static void main (String [] args) {
		RSendUDP sender = new RSendUDP();
		sender.setMode(0);
		sender.setModeParameter(512);
		sender.setTimeout(10000);
		sender.setFilename("important.txt");
		sender.setLocalPort(23456);
		try {
			sender.setReceiver(new InetSocketAddress(InetAddress.getLocalHost(), 32456));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sender.sendFile();
	}
}
