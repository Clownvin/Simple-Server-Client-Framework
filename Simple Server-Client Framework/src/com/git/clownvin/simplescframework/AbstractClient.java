package com.git.clownvin.simplescframework;

import com.git.clownvin.simplescframework.connection.AbstractConnection;

/**
 * 
 * @author Clownvin
 *
 */
public abstract class AbstractClient<ConnectionT extends AbstractConnection> implements Runnable {
	protected final String name;
	protected volatile boolean stop, stopped = true;
	protected Thread clientThread;
	protected ConnectionT connection;
	
	public AbstractClient(ConnectionT connection) {
		this("Client", connection);
	}
	
	public AbstractClient(final String name, final ConnectionT connection) {
		this.name = name;
		this.connection = connection;
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
		clientThread.setName(name+":ClientThread");
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
		return name;
	}
	
	@Override
	public void run() {
		atStart();
		while (!stop) {
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
