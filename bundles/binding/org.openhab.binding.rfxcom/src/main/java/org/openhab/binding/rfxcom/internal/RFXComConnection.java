/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.rfxcom.internal;

import java.util.Dictionary;
import java.util.EventObject;

import javax.xml.bind.DatatypeConverter;

import org.openhab.binding.rfxcom.internal.connector.RFXComConnectorInterface;
import org.openhab.binding.rfxcom.internal.connector.RFXComEventListener;
import org.openhab.binding.rfxcom.internal.connector.RFXComSerialConnector;
import org.openhab.binding.rfxcom.internal.connector.RFXComTcpConnector;
import org.openhab.binding.rfxcom.internal.messages.RFXComMessageFactory;
import org.openhab.binding.rfxcom.internal.messages.RFXComMessageInterface;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class establishes the connection to the RFXCOM controller.
 * 
 * @author Pauli Anttila, Evert van Es
 * @since 1.2.0
 */
public class RFXComConnection implements ManagedService {

	private static final Logger logger = LoggerFactory
			.getLogger(RFXComConnection.class);

	private static String host = null;
	private static String serialPort = null;
	private static byte[] setMode = null;

	static RFXComConnectorInterface connector = null;
	private final MessageLister eventLister = new MessageLister();
	
	public void activate() {
		logger.debug("Activate");
	}

	public void deactivate() {
		logger.debug("Deactivate");
		
		if (connector != null) {
			connector.removeEventListener(eventLister);
			connector.disconnect();
		}
	}

	/**
	 * Returns the RFXCOM client for communicating to the controller. The link
	 * can be null, if it has not (yet) been established successfully.
	 * 
	 * @return instance to current RFXCOM client.
	 */
	public static synchronized RFXComConnectorInterface getCommunicator() {
		return connector;
	}

	@Override
	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {

		logger.debug("Configuration updated, config {}", config != null ? true
				: false);

		if (connector != null) {
			logger.debug("Close previous connection");
			connector.removeEventListener(eventLister);
			connector.disconnect();
		}
		
		if (config != null) {

			serialPort = (String) config.get("serialPort");
			host = (String) config.get("host");
			String setModeStr = (String) config.get("setMode");

			if (isValidParameter(setModeStr)) {
				
				try {
					setMode = DatatypeConverter.parseHexBinary(setModeStr);
				} catch (IllegalArgumentException e) {
					throw new ConfigurationException("setMode", e.getMessage());
				}
				
				if (setMode.length != 14) {
					throw new ConfigurationException("setMode", "hexBinary value lenght should be 14 bytes (28 characters)");
				}
			}

			try {
				connect();

			} catch (Exception e) {
				logger.error("Connection to RFXCOM controller failed.", e);
			}
		}

	}

	private boolean isValidParameter(String paramValue) {
		return paramValue != null && paramValue.isEmpty() == false;
	}

	private void connect() throws Exception {
		
		// Check mode (serial or TCP)
		String connectParam = null;
		if (isValidParameter(serialPort)) {
			connector = new RFXComSerialConnector();
			connectParam = serialPort;
			logger.info("Connecting to RFXCOM [serialPort='{}' ].", new Object[] { serialPort });
		} else if (isValidParameter(host)) {
			connector = new RFXComTcpConnector();
			connectParam = host;
			logger.info("Connecting to RFXCOM [host='{}' ].", new Object[] { host });
		}

		connector.addEventListener(eventLister);
		connector.connect(connectParam);

		if (isValidParameter(serialPort)) {
			// Reset only if mode is serial
			logger.debug("Reset controller");
			connector.sendMessage(RFXComMessageFactory.CMD_RESET);
			
			// controller does not response immediately after reset,
			// so wait a while
			Thread.sleep(1000);
		}

		if (setMode != null) {
			try {
				logger.debug("Set mode: {}",
						DatatypeConverter.printHexBinary(setMode));
			} catch (IllegalArgumentException e) {
				throw new ConfigurationException("setMode", e.getMessage());
			}
			
			connector.sendMessage(setMode);
		} else {
			connector.sendMessage(RFXComMessageFactory.CMD_STATUS);
		}
	}
	
	private static class MessageLister implements RFXComEventListener {

		@Override
		public void packetReceived(EventObject event, byte[] data) {

			try {
				RFXComMessageInterface obj = RFXComMessageFactory.getMessageInterface(data);
				logger.debug("Data received:\n{}", obj.toString());
				
			} catch (RFXComException e) {
				logger.debug("Unknown data received, data: {}",
						DatatypeConverter.printHexBinary(data));
			}
		}
		
	}
}
