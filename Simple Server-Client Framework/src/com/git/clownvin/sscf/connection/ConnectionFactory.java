package com.git.clownvin.sscf.connection;

import java.io.IOException;
import java.net.Socket;

public interface ConnectionFactory {
	public AbstractConnection createConnection(Socket socket) throws IOException;
}
