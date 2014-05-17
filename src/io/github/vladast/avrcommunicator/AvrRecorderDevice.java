package io.github.vladast.avrcommunicator;

import java.util.ArrayList;

public class AvrRecorderDevice {	
	private short _deviceCode;
	private byte _state;
	private byte _session;
	private byte _error;
	private short _entryCount;
	private ArrayList<Reading> _eventReadings;
	
	AvrRecorderDevice(){
		_deviceCode = 0x00;
		_state = 0x00;
		_session = 0x00;
		_entryCount = 0x00;
		_eventReadings = new ArrayList<Reading>();
	}

	public short getDeviceCode() {
		return _deviceCode;
	}

	public void setDeviceCode(short deviceCode) {
		_deviceCode = deviceCode;
	}

	public byte getSession() {
		return _session;
	}

	public void setSession(byte session) {
		_session = session;
	}

	public short getEntryCount() {
		return _entryCount;
	}

	public void setEntryCount(short entryCount) {
		_entryCount = entryCount;
	}

	public ArrayList<Reading> getEventReadings() {
		return _eventReadings;
	}

	public void setEventReadings(ArrayList<Reading> eventReadings) {
		_eventReadings = eventReadings;
	}
	
	public void addEventReading(Reading eventReading) {
		_eventReadings.add(eventReading);
	}

	/**
	 * @return the _State
	 */
	public byte getState() {
		return _state;
	}

	/**
	 * @param state the _State to set
	 */
	public void setState(byte state) {
		_state = state;
	}

	public String getStateName() {
		String stateName;
		
        switch(_state)
        {
            case (byte)0xA1: // START   = 0xA1, // Device is being started
            	stateName = "START";
                break;
            case (byte)0xB2: // INIT    = 0xB2, // Initialize device
            	stateName = "INIT";
                break;
            case (byte)0xC3: // RECORD  = 0xC3, // Record events (touch-switch states)
            	stateName = "RECORD";
                break;
            case (byte)0xD4: // UPLOAD  = 0xD4, // Upload records to USB host
            	stateName = "UPLOAD";
                break;
            case (byte)0xE5: // DELETE  = 0xE5, // Erase external EEPROM
            	stateName = "DELETE";
                break;
            case (byte)0xF6: // RESES   = 0xF6  // Reinit session counter
            	stateName = "RESES";
                break;
            default:
            	stateName = "UNDEFINED" + String.format(" [0x%04x]", _state);                        
        }
		
		return stateName;
	}

	/**
	 * @return the _error
	 */
	public byte getError() {
		return _error;
	}

	/**
	 * @param _error the _error to set
	 */
	public void setError(byte error) {
		_error = error;
	}
}
