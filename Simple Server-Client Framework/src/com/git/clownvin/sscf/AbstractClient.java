package com.git.clownvin.sscf;

import com.git.clownvin.sscf.connection.AbstractConnection;

/**
 * 
 * @author Clownvin
 *
 */
public abstract class AbstractClient<ConnectionT extends AbstractConnection> implements Runnable {
   protected String destination = "localhost";
   protected int port = 6969;
	protected volatile boolean connected, stop, stopped = true;
	protected Thread clientThread;
	protected ConnectionT connection;
	
	public AbstractClient(String destination, int port) {
	   this.destination = destination;
	   this.port = port;
	}
	
	public AbstractClient(ConnectionT connection) {
	   this.connection = connection;
	   destination = connection.getSocket().getInetAddress().getHostAddress();
	   port = connection.getSocket().getPort();
	   connected = true;
	}
	
	public abstract void atStart();
	
	public abstract void duringLoop() throws InterruptedException;
	
	public abstract void atStop();
	
	public void start() {
		if (!stopped) {
			throw new RuntimeException("Client is already running!!!");
		}
		stopped = stop = false;
		clientThread = new Thread(this);
		clientThread.setName("ClientThread");
		clientThread.start();
	}
	
	public ConnectionT getConnection() {
		return connection;
	}
	
	public void stop() {
		stop = true;
	}
	
	@Override
	public String toString() {
		return null;
	}
	
	public abstract void reconnect();
	
	@Override
	public void run() {
		atStart();
		while (!stop) {
		   if (!connected) {
		      try {
		         reconnect();
		      } finally {
		         if (!connected || Thread.interrupted()) { // Either failed to reconnect or caught early.
		            stop();
		         }
		      }
		   }
			try {
				duringLoop();
			} catch (InterruptedException e) {
				stop();
			}
		}
		atStop();
		stopped = true;
	}
}
