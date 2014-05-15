package io.github.vladast.avrcommunicator;

import java.util.ArrayList;

public class AvrRecorderDevice {	
	private byte _DeviceCode;
	private byte _Session;
	private byte _EntryCount;
	private ArrayList<Reading> _EventReadings;
	
	AvrRecorderDevice(){
		_DeviceCode = 0x00;
		_Session = 0x00;
		_EntryCount = 0x00;
		_EventReadings = new ArrayList<Reading>();
	}

	public byte getDeviceCode() {
		return _DeviceCode;
	}

	public void setDeviceCode(byte deviceCode) {
		_DeviceCode = deviceCode;
	}

	public byte getSession() {
		return _Session;
	}

	public void setSession(byte session) {
		_Session = session;
	}

	public byte getEntryCount() {
		return _EntryCount;
	}

	public void setEntryCount(byte entryCount) {
		_EntryCount = entryCount;
	}

	public ArrayList<Reading> getEventReadings() {
		return _EventReadings;
	}

	public void setEventReadings(ArrayList<Reading> eventReadings) {
		_EventReadings = eventReadings;
	}
	
	public void addEventReading(Reading eventReading) {
		_EventReadings.add(eventReading);
	}
}
