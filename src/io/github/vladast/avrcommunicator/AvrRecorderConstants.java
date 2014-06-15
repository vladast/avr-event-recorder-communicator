package io.github.vladast.avrcommunicator;

/**
 * 
 * @author vladimir.stankovic
 * @version 0.0.1
 * 
 * Defines constants used by AVR device.
 */
public final class AvrRecorderConstants {
	
	@Deprecated
	public static final int	AVR_REC_VID		= 0x0000;
	@Deprecated
	public static final int AVR_REC_PID		= 0x0000;
	
    public static final byte SWID_UNKNOWN	= 0x00;
    public static final byte SWID_WITH    	= 0x01;
    public static final byte SWID_THROW   	= 0x02;
    public static final byte SWID_WITHOUT 	= 0x03;
    public static final byte OV_BIT       	= 0x20;
    
    public static final byte REQ_GET_HEADER = 0x00;

    public static final byte REQ_GET_DATA1  = 0x10;
    public static final byte REQ_GET_DATA2  = 0x11;
    public static final byte REQ_GET_DATA3  = 0x12;
    public static final byte REQ_GET_DATA4  = 0x13;
    public static final byte REQ_GET_DATA5  = 0x14;

    public static final byte REQ_SET_DATA1  = 0x20;
    public static final byte REQ_SET_DATA2  = 0x21;
    public static final byte REQ_SET_DATA3  = 0x22;
    public static final byte REQ_SET_DATA4  = 0x23;
    
    public static final byte STATE_INIT		= (byte) 0xB2; // Initialize device
    public static final byte STATE_RECORD	= (byte) 0xC3; // Record events (touch-switch states)
    public static final byte STATE_UPLOAD	= (byte) 0xD4; // Upload records to USB host
    public static final byte STATE_DELETE	= (byte) 0xE5; // Erase external EEPROM
    public static final byte STATE_RESES	= (byte) 0xF6; // Re-initiate session counter 

    public static final int		DEFAULT_PREF_EVENT_NUMBER			= 3;
    public static final boolean	DEFAULT_PREF_MONITOR_DEVICE			= true;
    public static final int		DEFAULT_PREF_MONITORING_INTERVAL	= 1;
    public static final boolean	DEFAULT_PREF_PREVENT_SLEEP			= false;
    public static final String	DEFAULT_PREF_TOUCHABLE_NAME_PREFIX	= "SW";
}
