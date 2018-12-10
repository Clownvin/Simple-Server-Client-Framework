package com.git.clownvin.simplescframework.connection;

import java.io.IOException;
import java.net.Socket;

public interface ConnectionFactory {
	public AbstractConnection createConnection(Socket socket) throws IOException;
}
