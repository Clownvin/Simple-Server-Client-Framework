package com.git.clownvin.simplescframework.connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.function.Consumer;

/**
 * 
 * @author Clownvin
 *
 *         Connection acceptor. Waits for connections, then encapsulates the
 *         sockets into Connection containers.
 */
public final class ConnectionAcceptor implements Runnable {
	
	private static final Hashtable<Integer, ConnectionAcceptor> acceptors = new Hashtable<>();

	/**
	 * Creates a new thread that runs the singleton object, and starts it.
	 * 
	 * @param port
	 *            port with which to listen for incoming connections on.
	 */
	public static void start(final int port, final ConnectionFactory factory) {
		synchronized (acceptors) {
			if (acceptors.containsKey(port))
				throw new RuntimeException("Port "+port+" is already being listened on!");
		}
		var acceptor = new ConnectionAcceptor(port, factory);
		var thread = new Thread(acceptor); // New thread.
		thread.setName("ConnectionAcceptor(port " + port + ")"); // Set name so it can be identified easily.
		thread.start(); // Start thread.
		synchronized (acceptors) {
			acceptors.put(port, acceptor);
		}
	}
	
	public static ConnectionAcceptor get(int port) {
		return acceptors.get(port);
	}

	public static void stop(final int port) {
		synchronized (acceptors) {
			var acceptor = acceptors.get(port);
			if (acceptor == null)
				return;
			acceptor.stop();
			acceptors.remove(port);
		}
	}
	
	public static void stopAll() {
		synchronized (acceptors) {
		  acceptors.keySet().forEach((key) -> acceptors.get(key).stop());
			acceptors.clear();
		}
	}

	private Consumer<AbstractConnection> onConnectionAccept = (c) -> { System.out.println("Default Consumer: Accepted " + c + " on port " + c.getSocket().getPort()); };
	
	private final int port;
	private final ConnectionFactory factory;
	
	private boolean stop = false;
	private volatile boolean awaitingConnection = false;

	private ConnectionAcceptor(final int port, final ConnectionFactory factory) {
		// Can only be instantiated internally.
		this.port = port;
		this.factory = factory;
	}
	
	public void setConnectionConsumer(Consumer<AbstractConnection> onConnectionAccept) {
		this.onConnectionAccept = onConnectionAccept;
	}
	
	private void stop() {
		stop = true;
		if (!awaitingConnection)
			return;
		try (Socket socket = new Socket("localhost", port)){
			//Force a connection to exit the blocking "accept" call.
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Run method.
	 */
	@Override
	public void run() {
		try (var acceptorSocket = new ServerSocket(port)) {
			System.out.println("Acceptor running on port: " + port);
			while (!stop && !acceptorSocket.isClosed()) { // While open and server is running..
				try {
					awaitingConnection = true;
					var connection = factory.createConnection(acceptorSocket.accept());
					awaitingConnection = false;
					if (stop) {
						connection.kill();
						break;
					}
					onConnectionAccept.accept(connection);
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Failed to accept new connection...");
				}
			}
			System.out.println("Stopping acceptor on port: " + port);
			try {
				if (acceptorSocket != null && !acceptorSocket.isClosed()) {
					acceptorSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			System.err.println("Failed to bind socket on port: " + port + ". Check if it is already bound to another program.");
			e.printStackTrace();
			System.exit(3);
		}
	}
}
