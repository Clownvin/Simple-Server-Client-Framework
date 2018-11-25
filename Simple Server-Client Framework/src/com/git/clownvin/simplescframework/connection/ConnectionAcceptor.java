package com.git.clownvin.simplescframework.connection;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 
 * @author Clownvin
 *
 *         Connection acceptor. Waits for connections, then encapsulates the
 *         sockets into Connection containers.
 */
public final class ConnectionAcceptor implements Runnable {
	
	private static BiConsumer<AbstractConnection, Integer> onConnectionAccept = (c, p) -> { System.out.println("Accepted " + c + " on port " + p);};
	
	private static final List<ConnectionAcceptor> acceptors = new ArrayList<>();
	
	private static Class<? extends AbstractConnection> connectionClass = AbstractConnection.class;
	
	public static void setConnectionClass(final Class<? extends AbstractConnection> connectionClass) {
		ConnectionAcceptor.connectionClass = connectionClass;
	}
	
	public static void setOnConnectConsumer(BiConsumer<AbstractConnection, Integer> onConnectionAccept) {
		ConnectionAcceptor.onConnectionAccept = onConnectionAccept;
	}

	/**
	 * Creates a new thread that runs the singleton object, and starts it.
	 * 
	 * @param port
	 *            port with which to listen for incoming connections on.
	 */
	public static void start(final int port) {
		if (connectionClass.equals(AbstractConnection.class))
			throw new RuntimeException("You must specify a class which extends AbstractConnection for ConnectionAcceptor to build Connection objects from!");
		synchronized (acceptors) {
			for (var acceptor : acceptors) {
				if (acceptor.port == port)
					throw new RuntimeException("Port "+port+" is already being listened on!");
			}
		}
		var acceptor = new ConnectionAcceptor(port);
		var thread = new Thread(acceptor); // New thread.
		thread.setName("ConnectionAcceptor(port " + port + ")"); // Set name so it can be identified easily.
		thread.start(); // Start thread.
		synchronized (acceptors) {
			acceptors.add(acceptor);
		}
	}

	public static void stop(final int port) {
		synchronized (acceptors) {
			var _acceptor = new ConnectionAcceptor(-1);
			for (var acceptor : acceptors) {
				if (acceptor.port != port)
					continue;
				acceptor.stop();
				_acceptor = acceptor;
				break;
			}
			acceptors.remove(_acceptor);
		}
	}
	
	public static void stopAll() {
		synchronized (acceptors) {
			for (var acceptor : acceptors) {
				acceptor.stop();
			}
			acceptors.clear();
		}
	}

	private final int port;
	
	private boolean stop = false;
	private volatile boolean awaitingConnection = false;

	private ConnectionAcceptor(final int port) {
		// Can only be instantiated internally.
		this.port = port;
	}
	
	private void stop() {
		stop = true;
		if (!awaitingConnection)
			return;
		try {
			connectionClass.getConstructor(Socket.class).newInstance(new Socket("localhost", port)); //Force a connection to exit the blocking "accept" call.
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
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
					var connection = connectionClass.getConstructor(Socket.class).newInstance(acceptorSocket.accept());
					awaitingConnection = false;
					if (stop) {
						connection.kill();
						break;
					}
					onConnectionAccept.accept(connection, port);
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Failed to accept new connection...");
					//TODO you were here
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
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
