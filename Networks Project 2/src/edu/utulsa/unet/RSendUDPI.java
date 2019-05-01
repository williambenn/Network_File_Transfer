package edu.utulsa.unet;

import java.net.InetSocketAddress;

public interface RSendUDPI {
	public boolean setMode(int mode);
	public int getMode();
	public boolean setModeParameter(long n);
	public long getModeParameter();
	public void setFilename(String fname);
	public String getFilename();
	public boolean setTimeout(long timeout);
	public long getTimeout();
	public boolean setLocalPort(int port);
	public int getLocalPort();
	public boolean setReceiver(InetSocketAddress receiver);
	public InetSocketAddress getReceiver();
	public boolean sendFile();
}