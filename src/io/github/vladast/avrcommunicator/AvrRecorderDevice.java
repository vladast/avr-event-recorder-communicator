package io.github.vladast.avrcommunicator;

import java.util.ArrayList;

/**
 * Represents abstraction of data stored on the compatible device.
 * @author vladimir.stankovic
 */
public class AvrRecorderDevice {	
	/** Device code upon which a decision compatible/incompatible is set. */
	private short _deviceCode;
	/** Device name stored on the device. Empty string if no name is read from device. */
	private String _deviceName;
	/** Device state at the time of the data upload. For device debugging purposes. */
	private byte _state;
	/** Device session index stored on the device. */
	private byte _session;
	/** Error code set by device. For troubleshooting purposes. */
	private byte _error;
	/** Number of events stored on the device. */
	private short _entryCount;
	/** List of recorded events. */
	private ArrayList<Reading> _eventReadings;
	
	/** C-tor of <code>AvrRecorderDevice</code> class. */
	AvrRecorderDevice() {
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

	public String getDeviceName() {
		return _deviceName;
	}
	
	public void setDeviceName(String deviceName) {
		_deviceName = deviceName;
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
