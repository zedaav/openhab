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

/**
 * This interface defines interface to communicate RFXCOM controller.
 *
 * @author Pauli Anttila, Evert van Es
 * @since 1.2.0
 */
public interface RFXComConnectorInterface {

    /**
     * Procedure for connecting to RFXCOM controller.
     * 
     * @param device
     *            Controller connection parameters (e.g. serial port name or IP
     *            address).
     */
    public void connect(String device) throws Exception;

    /**
     * Procedure for disconnecting to RFXCOM controller.
     * 
     */
    public void disconnect();

    /**
     * Procedure for send raw data to RFXCOM controller.
     * 
     * @param data
     *            raw bytes.
     */
    public void sendMessage(byte[] data) throws IOException;

    /**
     * Procedure for register event listener.
     * 
     * @param listener
     *            Event listener instance to handle events.
     */
    public void addEventListener(RFXComEventListener listener);

<<<<<<< Upstream, based on v1.8.3
    /**
     * Procedure for remove event listener.
     * 
     * @param listener
     *            Event listener instance to remove.
     */
    public void removeEventListener(RFXComEventListener listener);
    
    /**
     * Ignore any data in the receive buffer
     */
    public void clearReceiveBuffer();
=======
	/**
	 * Procedure for remove event listener.
	 * 
	 * @param listener
	 *            Event listener instance to remove.
	 */
	public void removeEventListener(RFXComEventListener listener);

	/**
	 * Checks if connection to the RFXCOM controller is active
	 */
	public boolean isConnected();
>>>>>>> 8fc494d Add support for RFXCom TCP connector
}
