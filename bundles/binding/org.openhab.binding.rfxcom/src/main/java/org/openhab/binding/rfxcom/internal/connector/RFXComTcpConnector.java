/**
 * Copyright (c) 2010-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.rfxcom.internal.connector;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RFXCOM connector for TCP/IP communication.
 * 
 * @author Pauli Anttila, Evert van Es
 * @since 1.2.0
 */
public class RFXComTcpConnector implements RFXComConnectorInterface {

	private static final Logger logger = LoggerFactory.getLogger(RFXComTcpConnector.class);

	private static final List<RFXComEventListener> _listeners = new CopyOnWriteArrayList<RFXComEventListener>();

	InputStream in = null;
	OutputStream out = null;
	Socket socket = null;
	Thread readerThread = null;

	public RFXComTcpConnector() {

	}

	@Override
	public void connect(String host) throws Exception {
		socket = new Socket(host, 10001);
		in = socket.getInputStream();
		out = socket.getOutputStream();

		out.flush();
		if (in.markSupported()) {
			in.reset();
		}

		// Run reader
		readerThread = new SocketReader(in);
		readerThread.start();
	}

	@Override
	public void disconnect() {
		logger.debug("Disconnecting");

		if (readerThread != null) {
			logger.debug("Interrupt socket listener");
			readerThread.interrupt();
		}

		if (out != null) {
			logger.debug("Close socket out stream");
			IOUtils.closeQuietly(out);
		}
		if (in != null) {
			logger.debug("Close socket in stream");
			IOUtils.closeQuietly(in);
		}

		if (socket != null) {
			logger.debug("Close socket");
			try {
				socket.close();
			} catch (IOException e) {
				logger.debug("Exception while closing socket", e);
			}
		}

		readerThread = null;
		socket = null;
		out = null;
		in = null;

		logger.debug("Closed");
	}

	@Override
	public void sendMessage(byte[] data) throws IOException {
		out.write(data);
		out.flush();
	}

	public void addEventListener(RFXComEventListener rfxComEventListener) {
		_listeners.add(rfxComEventListener);
	}

	public void removeEventListener(RFXComEventListener listener) {
		_listeners.remove(listener);
	}

<<<<<<< Upstream, based on v1.8.3
    @Override
    public void clearReceiveBuffer() {
        logger.error("clearReceiveBuffer not implemented");
    }

=======
	public boolean isConnected() {
		return out != null;
	}

	public class SocketReader extends Thread {
		boolean interrupted = false;
		InputStream in;

		public SocketReader(InputStream in) {
			this.in = in;
		}

		@Override
		public void interrupt() {
			interrupted = true;
			super.interrupt();
			try {
				in.close();
			} catch (IOException e) {
			} // quietly close
		}

		public void run() {
			final int dataBufferMaxLen = Byte.MAX_VALUE;

			byte[] dataBuffer = new byte[dataBufferMaxLen];

			int msgLen = 0;
			boolean start_found = false;

			logger.debug("Data listener started");

			try {

				byte[] tmpData = new byte[20];
				int len = -1;

				while ((len = in.read(tmpData)) > 0 && !interrupted) {

					byte[] logData = Arrays.copyOf(tmpData, len);
					logger.debug("Received data (len={}): {}", len, DatatypeConverter.printHexBinary(logData));

					// Just one byte received? --> start of the frame
					if (len == 1) {
						// We're OK to go with the next read. This is the frame length (in bits)
						start_found = true;
						dataBuffer[0] = tmpData[0];
						logger.debug("Start of frame detected");
					} else if (start_found) {
						// Validate length
						if ((dataBuffer[0] <= len*8) && (dataBuffer[0] > (len-1)*8)) {
							// Remember the length
							msgLen = len+1;
	
							// Copy the frame
							System.arraycopy(tmpData, 0, dataBuffer, 1, len);
	
							// whole message received, send an event
							byte[] msg = new byte[msgLen + 1];
	
							for (int j = 0; j < msgLen; j++)
								msg[j] = dataBuffer[j];
	
							RFXComMessageReceivedEvent event = new RFXComMessageReceivedEvent(this);
	
							try {
								Iterator<RFXComEventListener> iterator = _listeners.iterator();
								while (iterator.hasNext()) {
									iterator.next().packetReceived(event, msg);
								}
	
							} catch (Exception e) {
								logger.error("Event listener invoking error", e);
							}
						} else {
							logger.debug("Invalid frame length: {}: was expecting {}", len*8, dataBuffer[0]);
						}

						// find new start
						start_found = false;
					} else {
						// Garbage data, don't know what to do with it
						logger.debug("Unknown data");
					}
				}
			} catch (InterruptedIOException e) {
				Thread.currentThread().interrupt();
				logger.error("Interrupted via InterruptedIOException");
			} catch (IOException e) {
				logger.error("Reading from socket failed", e);
			}
		}
	}
>>>>>>> 8fc494d Add support for RFXCom TCP connector
}
