package com.git.clownvin.sscf.connection;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public abstract class PrivateConnection extends AbstractConnection {
	
	private PublicKey publicKey;
	private KeyAgreement keyAgreement;
	private byte[] secret;
	protected volatile boolean exchangeComplete = false;
	
	protected static final String ALGORITHM = "AES";

	public PrivateConnection(Socket socket) throws IOException {
		super(socket);
		setup();
	}
	
	protected void setup() {
		exchangeComplete = false;
		setupKeyExchange();
	}
	
	public final PublicKey getPublicKey() {
		return publicKey;
	}
	
	public final boolean exchangeComplete() {
		return exchangeComplete;
	}
	
	private final void setupKeyExchange() {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
			kpg.initialize(256);
			KeyPair kp = kpg.generateKeyPair();
			publicKey = kp.getPublic();
			keyAgreement = KeyAgreement.getInstance("ECDH");
			keyAgreement.init(kp.getPrivate());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public final boolean finishKeyExchange(PublicKey publicKey) {
		try {
			keyAgreement.doPhase(publicKey, true);
			secret = keyAgreement.generateSecret();
			exchangeComplete = true;
			synchronized (keyAgreement) {
				keyAgreement.notifyAll();
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public final byte[] encrypt(final byte[] data) throws KeyExchangeIncompleteException {
		var key = generateKey();
		try {
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] d = cipher.doFinal(data);
			return d;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public final byte[] decrypt(final byte[] data, final int length) throws KeyExchangeIncompleteException {
		var key = generateKey();
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(data, 0, length);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private final Key generateKey() throws KeyExchangeIncompleteException {
		int tries = 0;
		while (!exchangeComplete) {
			try {
				synchronized (keyAgreement) {
					keyAgreement.wait(1000);
				}
			} catch (InterruptedException e) {
			}
			if (tries++ == 3)
				throw new KeyExchangeIncompleteException("Failed to finish key exchange");
		}
		return new SecretKeySpec(secret, ALGORITHM);
	}

}
