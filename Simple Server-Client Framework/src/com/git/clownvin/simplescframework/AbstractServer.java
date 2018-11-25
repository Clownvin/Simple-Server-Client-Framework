package com.git.clownvin.simplescframework;

import com.git.clownvin.simplescframework.connection.ConnectionAcceptor;

public abstract class AbstractServer implements Runnable {
	protected final int[] ports;
	private volatile boolean stop, stopped = true;
	protected final String name;
	protected Thread serverThread;

	public AbstractServer(final int... ports) {
		this("Server", ports);
	}
	
	public AbstractServer(final String name, final int... ports) {
		this.name = name;
		this.ports = ports;
	}

	public void start() {
		if (!stopped) {
			throw new RuntimeException("Server is already running!");
		}
		stopped = stop = false;
		serverThread = new Thread(this);
		serverThread.setName(this + ":ServerThread");
		serverThread.start();
	}
	
	public void stop() {
		if (stop)
			return;
		stop = true;
		try {
			serverThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e); // We're already trying to stop. But it'd be weird if we're still getting interrupted...
		}
	}

	@Override
	public String toString() {
		return name;
	}

	public abstract void duringLoop() throws InterruptedException;
	
	public abstract void atStart();
	
	public abstract void atStop();
	
	public boolean isRunning() {
		return !stop;
	}

	@Override
	public void run() {
		for (var port : ports) {
			ConnectionAcceptor.start(port);
		}
		atStart();
		while (isRunning()) {
			try {
				duringLoop();
			} catch (InterruptedException e) {
				stop();
			}
		}
		ConnectionAcceptor.stopAll();
		atStop();
		stopped = true;
	}
}
