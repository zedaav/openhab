/**
 * Copyright (c) 2010-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.rfxcom.internal;

<<<<<<< Upstream, based on v1.8.3
import java.io.IOException;
=======
>>>>>>> 8fc494d Add support for RFXCom TCP connector
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

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

/**
 * This class establishes the connection to the RFXCOM controller.
 *
 * @author Pauli Anttila, Evert van Es
 * @since 1.2.0
 */
public class RFXComConnection implements ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(RFXComConnection.class);

<<<<<<< Upstream, based on v1.8.3
    private static String serialPort = null;
    private static byte[] setMode = null;
=======
	private static String host = null;
	private static String serialPort = null;
	private static byte[] setMode = null;
>>>>>>> 8fc494d Add support for RFXCom TCP connector

<<<<<<< Upstream, based on v1.8.3
    static RFXComSerialConnector connector = new RFXComSerialConnector();
    private final MessageLister eventLister = new MessageLister();
=======
	static RFXComConnectorInterface connector = null;
	private final MessageLister eventLister = new MessageLister();
	
	public void activate() {
		logger.debug("Activate");
	}
>>>>>>> 8fc494d Add support for RFXCom TCP connector

    public void activate() {
        logger.debug("Activate");
    }

<<<<<<< Upstream, based on v1.8.3
    public void deactivate() {
        logger.debug("Deactivate");
=======
	/**
	 * Returns the RFXCOM client for communicating to the controller. The link
	 * can be null, if it has not (yet) been established successfully.
	 * 
	 * @return instance to current RFXCOM client.
	 */
	public static synchronized RFXComConnectorInterface getCommunicator() {
		return connector;
	}
>>>>>>> 8fc494d Add support for RFXCom TCP connector

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
    public static synchronized RFXComSerialConnector getCommunicator() {
        return connector;
    }

<<<<<<< Upstream, based on v1.8.3
    @Override
    public void updated(Dictionary<String, ?> config) throws ConfigurationException {
=======
		if (connector != null) {
			logger.debug("Close previous connection");
			connector.removeEventListener(eventLister);
			connector.disconnect();
		}
		
		if (config != null) {
>>>>>>> 8fc494d Add support for RFXCom TCP connector

<<<<<<< Upstream, based on v1.8.3
        logger.debug("Configuration updated, config {}", config != null ? true : false);
=======
			serialPort = (String) config.get("serialPort");
			host = (String) config.get("host");
			String setModeStr = (String) config.get("setMode");
>>>>>>> 8fc494d Add support for RFXCom TCP connector

<<<<<<< Upstream, based on v1.8.3
        if (serialPort != null) {
            logger.debug("Close previous connection");
            connector.removeEventListener(eventLister);
            connector.disconnect();
        }
=======
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
>>>>>>> 8fc494d Add support for RFXCom TCP connector

        if (config != null) {

            serialPort = (String) config.get("serialPort");
            String setModeStr = (String) config.get("setMode");

            if (setModeStr != null && setModeStr.isEmpty() == false) {

<<<<<<< Upstream, based on v1.8.3
                try {
                    setMode = DatatypeConverter.parseHexBinary(setModeStr);
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException("setMode", e.getMessage());
                }
=======
	private boolean isValidParameter(String paramValue) {
		return paramValue != null && paramValue.isEmpty() == false;
	}
>>>>>>> 8fc494d Add support for RFXCom TCP connector

<<<<<<< Upstream, based on v1.8.3
                if (setMode.length != 14) {
                    throw new ConfigurationException("setMode",
                            "hexBinary value lenght should be 14 bytes (28 characters)");
                }
            }
=======
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
>>>>>>> 8fc494d Add support for RFXCom TCP connector

<<<<<<< Upstream, based on v1.8.3
            try {
                connect();
=======
		connector.addEventListener(eventLister);
		connector.connect(connectParam);
>>>>>>> 8fc494d Add support for RFXCom TCP connector

<<<<<<< Upstream, based on v1.8.3
            } catch (Exception e) {
                logger.error("Connection to RFXCOM controller failed.", e);
            }
        }
=======
		if (isValidParameter(serialPort)) {
			// Reset only if mode is serial
			logger.debug("Reset controller");
			connector.sendMessage(RFXComMessageFactory.CMD_RESET);
			
			// controller does not response immediately after reset,
			// so wait a while
			Thread.sleep(1000);
		}
>>>>>>> 8fc494d Add support for RFXCom TCP connector

    }

    private void connect() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException,
            IOException, InterruptedException, ConfigurationException {

        logger.info("Connecting to RFXCOM [serialPort='{}' ].", new Object[] { serialPort });

        connector.addEventListener(eventLister);
        connector.connect(serialPort);

        logger.debug("Reset controller");
        connector.sendMessage(RFXComMessageFactory.CMD_RESET);

        // controller does not response immediately after reset,
        // so wait a while
        Thread.sleep(1000);
        // Clear received buffers
        connector.clearReceiveBuffer();        

        if (setMode != null) {
            try {
                logger.debug("Set mode: {}", DatatypeConverter.printHexBinary(setMode));
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
                logger.debug("Unknown data received, data: {}", DatatypeConverter.printHexBinary(data));
            }
        }

    }
}
