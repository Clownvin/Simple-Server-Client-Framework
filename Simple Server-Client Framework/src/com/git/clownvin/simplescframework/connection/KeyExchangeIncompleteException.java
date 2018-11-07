package com.git.clownvin.simplescframework.connection;

public class KeyExchangeIncompleteException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7902702070720352277L;
	
	public KeyExchangeIncompleteException(final String message) {
		super(message);
	}
	
	public KeyExchangeIncompleteException(final Exception exception) {
		super(exception);
	}

}
