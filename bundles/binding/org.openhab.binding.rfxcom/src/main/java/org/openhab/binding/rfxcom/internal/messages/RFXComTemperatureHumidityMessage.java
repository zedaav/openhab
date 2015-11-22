/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.rfxcom.internal.messages;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.openhab.binding.rfxcom.RFXComValueSelector;
import org.openhab.binding.rfxcom.internal.RFXComException;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RFXCOM data class for temperature and humidity message.
 * 
 * @author Pauli Anttila
 * @since 1.2.0
 */
public class RFXComTemperatureHumidityMessage extends RFXComBaseMessage {

	public enum SubType {
		UNDEF(0),
		THGN122_123_132_THGR122_228_238_268(1),
		THGN800_THGR810(2),
		RTGR328(3),
		THGR328(4),
		WTGR800(5),
		THGR918_THGRN228_THGN50(6),
		TFA_TS34C__CRESTA(7),
		WT260_WT260H_WT440H_WT450_WT450H(8),
		VIKING_02035_02038(9),
		RUBICSON(10),
		THGR228N(26),

		UNKNOWN(255);

		private final int subType;

		SubType(int subType) {
			this.subType = subType;
		}

		SubType(byte subType) {
			this.subType = subType;
		}

		public byte toByte() {
			return (byte) subType;
		}
	}

	public enum HumidityStatus {
		NORMAL(0),
		COMFORT(1),
		DRY(2),
		WET(3),
		
		UNKNOWN(255);

		private final int humidityStatus;

		HumidityStatus(int humidityStatus) {
			this.humidityStatus = humidityStatus;
		}

		HumidityStatus(byte humidityStatus) {
			this.humidityStatus = humidityStatus;
		}

		public byte toByte() {
			return (byte) humidityStatus;
		}
	}

	private final static List<RFXComValueSelector> supportedValueSelectors = Arrays
			.asList(RFXComValueSelector.RAW_DATA,
					RFXComValueSelector.SIGNAL_LEVEL,
					RFXComValueSelector.BATTERY_LEVEL,
					RFXComValueSelector.TEMPERATURE,
					RFXComValueSelector.HUMIDITY,
					RFXComValueSelector.HUMIDITY_STATUS);

	public SubType subType = SubType.THGN122_123_132_THGR122_228_238_268;
	public int sensorId = 0;
	public double temperature = 0;
	public byte humidity = 0;
	public HumidityStatus humidityStatus = HumidityStatus.NORMAL;
	public byte signalLevel = 0;
	public byte batteryLevel = 0;

	public RFXComTemperatureHumidityMessage() {
		packetType = PacketType.TEMPERATURE_HUMIDITY;
	}

	public RFXComTemperatureHumidityMessage(byte[] data) {
		encodeMessage(data);
	}

	@Override
	public String toString() {
		String str = "";

		str += super.toString();
		str += "\n - Sub type = " + subType;
		str += "\n - Id = " + sensorId;
		str += "\n - Temperature = " + temperature;
		str += "\n - Humidity = " + humidity;
		str += "\n - Humidity status = " + humidityStatus;
		str += "\n - Signal level = " + signalLevel;
		str += "\n - Battery level = " + batteryLevel;

		return str;
	}

	@Override
	public void encodeMessage(byte[] data) {

		/*
		 * 501A2D404594140007366C   TH1[17668] THGN122N,THGR122NX,THGR228N,THGR268 CH 3 addr:45 temp:14,9°C | 58,82°F hum:70 Normal  battery empty bits=80
		 * 50: length in bits
		 * 1A2D: probe type
		 * 4045: probe address / to be similar to ZiBase ID, probe ID would be 0x1A2D4504   (Channel on last quartet; 1->1, 2->2, 4->3; e.g. here channel is 3)
		 * 9414: Temp on 0xX0YZ --> YZ.X (here 14.9°C) / on 0x0X00, bit 4 is for low battery (here, we're in low battery state)
		 * 0007: Humidity on 0xX00Y --> YX (here 70%)
		 * 36: checksum of the nibbles of the 8 first bytes (excluding length), - 10  (here: 1+A+2+D+4+0+4+5+9+4+1+4+0+0+0+7 - A)
		 * 
		 * Decoding inspired from https://github.com/beanz/device-rfxcom-perl/blob/build/master/lib/Device/RFXCOM/Decoder/Oregon.pm
		 */
		
		super.encodeMessage(data);

		subType = SubType.UNKNOWN;
		for (SubType candidate : SubType.values()) {
			if (candidate.toByte() == super.subType) {
				subType = candidate;
			}
		}

		// Specific processing for THGR228N
		if (subType == SubType.THGR228N) {
			// 4 bytes identifier (as it is identified on ZiBase)
			sensorId = (((int)data[2]) << 24) | (((int)data[3]) << 16) | (((int)data[5]) << 8) | ((((int)data[4]) & 0x0F) << 4) | ((((int)data[4]) & 0xF0) >> 4);
			
			// Temperature on 12 bits
			temperature = ((data[7] & 0xF0) >> 4)*10 + (data[7] & 0x0F) + ((data[6] & 0xF0) >> 4)*0.1;
			
			// Humidity split on 2 bytes
			humidity = (byte) ((data[9] & 0x0F) * 10 + ((data[8] & 0xF0) >> 4));
		} else {
			sensorId = (data[4] & 0xFF) << 8 | (data[5] & 0xFF);
			temperature = (short) ((data[6] & 0x7F) << 8 | (data[7] & 0xFF)) * 0.1;
			if ((data[6] & 0x80) != 0)
				temperature = -temperature;
			humidity = data[8];
		}
		
		try {
			humidityStatus = HumidityStatus.values()[data[9]];
		} catch (Exception e) {
			humidityStatus = HumidityStatus.UNKNOWN;
		}
		
		signalLevel = (byte) ((data[10] & 0xF0) >> 4);
		batteryLevel = (byte) (data[10] & 0x0F);
	}

	@Override
	public byte[] decodeMessage() {
		byte[] data = new byte[11];

		data[0] = 0x0A;
		data[1] = RFXComBaseMessage.PacketType.TEMPERATURE_HUMIDITY.toByte();
		data[2] = subType.toByte();
		data[3] = seqNbr;
		data[4] = (byte) ((sensorId & 0xFF00) >> 8);
		data[5] = (byte) (sensorId & 0x00FF);

		short temp = (short) Math.abs(temperature * 10);
		data[6] = (byte) ((temp >> 8) & 0xFF);
		data[7] = (byte) (temp & 0xFF);
		if (temperature < 0)
			data[6] |= 0x80;

		data[8] = humidity;
		data[9] = humidityStatus.toByte();
		data[10] = (byte) (((signalLevel & 0x0F) << 4) | (batteryLevel & 0x0F));

		return data;
	}
	
	@Override
	public String generateDeviceId() {
		 return String.valueOf(sensorId);
	}

	@Override
	public State convertToState(RFXComValueSelector valueSelector)
			throws RFXComException {
		
		org.openhab.core.types.State state = UnDefType.UNDEF;

		if (valueSelector.getItemClass() == NumberItem.class) {

			if (valueSelector == RFXComValueSelector.SIGNAL_LEVEL) {

				state = new DecimalType(signalLevel);

			} else if (valueSelector == RFXComValueSelector.BATTERY_LEVEL) {

				state = new DecimalType(batteryLevel);

			} else if (valueSelector == RFXComValueSelector.TEMPERATURE) {

				state = new DecimalType(temperature);

			} else if (valueSelector == RFXComValueSelector.HUMIDITY) {

				state = new DecimalType(humidity);

			} else {
				throw new RFXComException("Can't convert "
						+ valueSelector + " to NumberItem");
			}

		} else if (valueSelector.getItemClass() == StringItem.class) {

			if (valueSelector == RFXComValueSelector.RAW_DATA) {

				state = new StringType(
						DatatypeConverter.printHexBinary(rawMessage));

			} else if (valueSelector == RFXComValueSelector.HUMIDITY_STATUS) {

				state = new StringType(humidityStatus.toString());

			} else {
				throw new RFXComException("Can't convert " + valueSelector + " to StringItem");
			}
		} else {

			throw new RFXComException("Can't convert " + valueSelector
					+ " to " + valueSelector.getItemClass());

		}

		return state;
	}

	@Override
	public void convertFromState(RFXComValueSelector valueSelector, String id,
			Object subType, Type type, byte seqNumber) throws RFXComException {
		
		throw new RFXComException("Not supported");
	}

	@Override
	public Object convertSubType(String subType) throws RFXComException {
		
		for (SubType s : SubType.values()) {
			if (s.toString().equals(subType)) {
				return s;
			}
		}
		
		throw new RFXComException("Unknown sub type " + subType);
	}
	
	@Override
	public List<RFXComValueSelector> getSupportedValueSelectors() throws RFXComException {
		return supportedValueSelectors;
	}

}
