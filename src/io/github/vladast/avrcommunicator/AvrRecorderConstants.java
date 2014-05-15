package io.github.vladast.avrcommunicator;

public final class AvrRecorderConstants {
	
	public static final int	AVR_REC_VID		= 0x16c0;
	public static final int AVR_REC_PID		= 0x03e8;
	
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
    public static final byte STATE_RESES	= (byte) 0xF6; // Reinit session counter 

}
