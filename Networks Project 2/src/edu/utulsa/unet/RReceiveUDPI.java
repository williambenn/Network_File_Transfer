package edu.utulsa.unet;

public interface RReceiveUDPI {
	public boolean setMode(int mode);
	public int getMode();
	public boolean setModeParameter(long n);
	public long getModeParameter();
	public void setFilename(String fname);
	public String getFilename();
	public boolean setLocalPort(int port);
	public int getLocalPort();
	public boolean receiveFile();
}