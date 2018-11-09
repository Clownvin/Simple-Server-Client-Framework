package com.git.clownvin.simplescframework.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public abstract class AbstractConnection {
	
	private final Runnable readerRunnable = new Runnable() {

		@Override
		public void run() {
			while (!kill) {
				try {
					if(!readInput())
						break;
					if(Thread.interrupted())
						break;
				} catch (Exception e) {
					e.printStackTrace(); // Don't want connection to stop because of a runaway exception.
				}
			}
		}

	};

	private final Runnable writerRunnable = new Runnable() {

		@Override
		public void run() {
			while (!kill) { //TODO TODO TODO TODO Implement outputLock.wait() here? 
				try {
					if(!writeOutput())
						break;
					if(Thread.interrupted())
						break;
				} catch (Exception e) {
					e.printStackTrace(); // Don't want connection to stop because of a runaway exception.
				}
			}
		}
	};
	
	private Thread packetReaderThread = null;
	private Thread packetWriterThread = null;

	protected Socket socket;
	protected OutputStream outputStream;
	protected InputStream inputStream;
	protected final Object outputLock = new Object();
	protected volatile boolean kill = true;

	public AbstractConnection(final Socket socket) throws IOException {
		setSocket(socket);
	}
	
	private void setSocket(Socket socket) throws IOException {
		this.socket = socket;
		outputStream = socket.getOutputStream();
		inputStream = socket.getInputStream();
	}
	
	private void setupThreads() {
		packetReaderThread = new Thread(readerRunnable);
		packetWriterThread = new Thread(writerRunnable);
		packetReaderThread.setName("PacketReader(" + this + ")");
		packetWriterThread.setName("PacketWriter(" + this + ")");
	}
	
	public void start() {
		if (!kill)
			throw new IllegalStateException("Connection is already running.");
		setupThreads();
		setup();
		kill = false;
		packetReaderThread.start();
		packetWriterThread.start();
	}

	public final InputStream getInputStream() {
		return inputStream;
	}

	public final OutputStream getOutputStream() {
		return outputStream;
	}

	public final void kill() throws IOException {
		if (kill)
			return;
		onKill();
		kill = true;
		inputStream.close();
		outputStream.close();
		synchronized (outputLock) {
			outputLock.notifyAll();
		}
		packetReaderThread.interrupt();
		packetWriterThread.interrupt();
		socket.close();
	}
	
	public abstract void onKill();
	
	/** A project for a rainy day. This shit don't want to work.
	public final void reconnect() throws IOException {
		if (!kill)
			kill();
		while (!socket.isClosed())
			continue;
		System.out.println("Connecting to "+socket.getInetAddress().getHostAddress()+", "+socket.getLocalPort());
		setSocket(new Socket(socket.getInetAddress().getHostAddress(), socket.getPort()));
		start();
	}
	*/
	
	protected abstract void setup();

	@Override
	public String toString() {
		return "Connection(" + socket.getInetAddress().getHostAddress() + ":lp" + socket.getLocalPort() + ":p"+ socket.getPort()+")";
	}

	public final Socket getSocket() {
		return socket;
	}
	
	public abstract boolean readInput();
	
	public abstract boolean writeOutput();
}
