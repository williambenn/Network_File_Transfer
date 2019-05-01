import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class UDPTestSend{
	public static void main(String [] args) {
		RSendUDP sender = new RSendUDP();
		sender.setMode(1);
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
