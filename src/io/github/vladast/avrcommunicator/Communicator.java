package io.github.vladast.avrcommunicator;

import java.util.ArrayList;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

/**
 * 
 * @author vladimir.stankovic
 * @version 0.0.1
 * 
 * Main module used to establish connection with AVR device.
 */
public class Communicator {

	public static final String TAG = Communicator.class.getSimpleName();
	
	/** Static member used for "device detected" message */
	private static final int MSG_DEVICE_DETECTED		= 0x0001;
	/** Static member used for "check device status" message */
	private static final int MSG_CHECK_DEVICE_STATUS	= 0x0002;
	/** Static member used for "re-initialize device" message */
	private static final int MSG_REINIT_DEVICE			= 0x0003;
	
	/** Instance of <code>UsbManager</code> class, being an Android's USB connection manager */
	private UsbManager mUsbManager;
	/** Instance of <code>UsbDeviceConnection</code> class, representing the actual connection between Android and AVR devices */
	private UsbDeviceConnection mUsbDeviceConnection;
	/** Instance of <code>EventRecorderListeners</code> for utilization of events received from device. */
	private EventRecorderListeners mEventRecorderListeners;
	/** Indicates whether recorded events were read from device or not */
	private boolean mRecordsRead;
	/** Represents data read from AVR event-recorder device */
	private AvrRecorderDevice mAvrRecorderDevice;
	/** Instance of <code>Handler</code> class used for asynchronous even processing */ 
	private final Handler mAvrRecorderMonitorHandler;
	
	/**
	 * <code>Communicator</code> class constructor
	 * @param usbManager <code>UsbManager</code> class instance received from upper layer when connection with AVR device is established.
	 */
	public Communicator(UsbManager usbManager) {
		mUsbManager = usbManager;
		mRecordsRead = false;
		mAvrRecorderDevice = null;
		mEventRecorderListeners = new EventRecorderListeners();
		mAvrRecorderMonitorHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
		            case MSG_CHECK_DEVICE_STATUS:
		            	mEventRecorderListeners.OnDebugMessage("Received MSG_CHECK_DEVICE_STATUS message");
		            	if(!mRecordsRead)
		            	{
		            		mEventRecorderListeners.OnDeviceSearching();
		            		mAvrRecorderMonitorHandler.sendEmptyMessageDelayed(MSG_CHECK_DEVICE_STATUS, 1000);
		            	}
		            	break;
	                case MSG_DEVICE_DETECTED:
	                	mEventRecorderListeners.OnDeviceConnected();
	                	readDeviceData((UsbDevice)msg.obj);
	                    break;
	                case MSG_REINIT_DEVICE:
	                	reinitDevice();
	                	mEventRecorderListeners.OnDeviceReInitiated();
	                	break;
	                default:
	                    super.handleMessage(msg);
	                    break;
	            }
	        }
	    };			
	}
	
	/** Getter for <code>mAvrRecorderDevice</code> field.*/
	public final AvrRecorderDevice getDevice() {
		return mAvrRecorderDevice;
	}
	
	/** 
	 * Method used to register event listener provided by upper layer.
	 * All communication between <code>Communicator</code> class instance and upper layer is done via instantiated handler.
	 * @param onAvrRecorderEventListener Object reference from upper layer's implementation of <code>OnAvrRecorderEventListener</code> interface.
	 */
	public void registerListener(OnAvrRecorderEventListener onAvrRecorderEventListener) {
		mEventRecorderListeners.registerEventRecorderListener(onAvrRecorderEventListener);
	}

	/** Sends "check device status" message to the handler, notifying it that AVR's status (connected/disconnected) should be checked. */
	public void startDeviceDetection() {
		mAvrRecorderMonitorHandler.sendEmptyMessage(MSG_CHECK_DEVICE_STATUS);
	}
	
	/** Removes "check device status" message from handler's queue, indicating that AVR's status should not be checked from now on. */
	public void stopDeviceDetection() {
		mAvrRecorderMonitorHandler.removeMessages(MSG_CHECK_DEVICE_STATUS);
	}
	
	/** Sends "re-initialize device" message to the handler, signaling that device re-initialization is being requested. */
	public void reInitiateDevice() {
		mAvrRecorderMonitorHandler.sendEmptyMessage(MSG_REINIT_DEVICE);
	}
	
	/**
	 * Used by main activity to notify communicator that device was attached.
	 * @param usbDevice Instance of <code>UsbDevice</code> class instance received from main activity, upon device detection.
	 */
	public void useThisUsbDevice(UsbDevice usbDevice)
	{
		Message msg = new Message();
		msg.what = MSG_DEVICE_DETECTED;
		msg.obj = usbDevice;
		mAvrRecorderMonitorHandler.removeMessages(MSG_CHECK_DEVICE_STATUS);
		mAvrRecorderMonitorHandler.sendMessage(msg);
	}
	
	/** 
	 * Checks whether AVR device has been attached or not.
	 * <b>NOTE:</b> Deprecated from v0.0.1 - attached USB device is being detected from main activity.
	 */
	@Deprecated
	protected void checkDeviceStatus() {
    	
    	new AsyncTask<Void, Void, UsbDevice>() {

			@Override
			protected UsbDevice doInBackground(Void... arg0) {
				
				for (final UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
					if(usbDevice.getVendorId() == AvrRecorderConstants.AVR_REC_VID &&
							usbDevice.getProductId() == AvrRecorderConstants.AVR_REC_PID)
					{
						return usbDevice;
					}
				}
				
				return null;
			}
    		
			protected void onPostExecute(UsbDevice usbDevice) {
				if(usbDevice != null) {
					Message msg = new Message();
					msg.what = MSG_DEVICE_DETECTED;
					msg.obj = usbDevice;
					mAvrRecorderMonitorHandler.removeMessages(MSG_CHECK_DEVICE_STATUS);
					mAvrRecorderMonitorHandler.sendMessage(msg);
				}
			}
			
    	}.execute((Void)null);
	}
	
	/** 
	 * Sends USB control message for re-initialization of the device.
	 * If completed successfully, AVR device will move to INIT state, awaiting for new events.
	 */
	protected void reinitDevice() {
		byte[] buffer = new byte[4];
        int iRxByteCount = 0;
        
        iRxByteCount = mUsbDeviceConnection.controlTransfer(
                UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_OUT, 
                AvrRecorderConstants.REQ_SET_DATA1, AvrRecorderConstants.STATE_INIT, 0, buffer, 0, 5000);
		if(iRxByteCount < 0)
        {
			mEventRecorderListeners.OnError(AvrRecorderErrors.ERR_REINIT);
        }
        else
        {
        	mEventRecorderListeners.OnDebugMessage("Re-initiation message successfuly sent to device.");
        	mAvrRecorderMonitorHandler.removeMessages(MSG_REINIT_DEVICE);
        }
	}
	
	/**
	 * This method reads status header&codes and records from the device, respectively
	 * @param usbDevice Instance of <code>UsbDevice</code> representing connected USB-enabled device
	 */
	protected void readDeviceData(UsbDevice usbDevice) {
		mEventRecorderListeners.OnReadingStarted();
		
		mAvrRecorderDevice = new AvrRecorderDevice();
		
		mUsbDeviceConnection = mUsbManager.openDevice(usbDevice);
		
		for(int i = 0; i < usbDevice.getInterfaceCount(); ++i) {
			if(mUsbDeviceConnection.claimInterface(usbDevice.getInterface(i), true)) {
				readDeviceInfo(); // Read status header and status codes
				readDeviceRecords(); // Read records from device	
			}
		}
		
		mRecordsRead = true;
		
		if(mAvrRecorderDevice.getEventReadings().size() > 0) {
			mEventRecorderListeners.OnRecordsRead(mAvrRecorderDevice.getEventReadings());
		}
	}
	
	/**
	 * Reads device's status, state, session, and number of records.
	 * With each value read from the device, <code>mAvrRecorderDevice</code> is being updated so that it reflect read data.
	 */
	protected void readDeviceInfo() {
        byte[] buffer = new byte[4];
        int iRxByteCount = 0;
        
        iRxByteCount = mUsbDeviceConnection.controlTransfer(
                UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                AvrRecorderConstants.REQ_GET_HEADER, 0, 0, buffer, 4, 5000);
        if(iRxByteCount < 1)
        {
        	mEventRecorderListeners.OnError(AvrRecorderErrors.ERR_HEADER);
        }
        else
        {
            mAvrRecorderDevice.setDeviceCode((short) (buffer[0] | (buffer[1] << 8)));
            mEventRecorderListeners.OnDebugMessage(String.format("Detected device with code 0x%04x", mAvrRecorderDevice.getDeviceCode()));
            
            switch(mAvrRecorderDevice.getDeviceCode())
            {
            case (short)0xA001:
                break;
            case (short)0xA002:
                // Read device's state
                iRxByteCount = mUsbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                        AvrRecorderConstants.REQ_GET_DATA1, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                	mEventRecorderListeners.OnError(AvrRecorderErrors.ERR_STATE);
                }
                else
                {
                    mAvrRecorderDevice.setState(buffer[0]);
                    mEventRecorderListeners.OnDebugMessage(String.format("Device's state is '%s'", mAvrRecorderDevice.getStateName()));
                }
                
                // Read device's session
                iRxByteCount = mUsbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                        AvrRecorderConstants.REQ_GET_DATA2, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                	mEventRecorderListeners.OnError(AvrRecorderErrors.ERR_SESSION);
                }
                else
                {
                    mAvrRecorderDevice.setSession(buffer[0]);
                    mEventRecorderListeners.OnDebugMessage(String.format("Device's session is: %s [0x%02x]", Byte.toString(mAvrRecorderDevice.getSession()), mAvrRecorderDevice.getSession()));
                }
                
                // Read device's error cache
                iRxByteCount = mUsbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                        AvrRecorderConstants.REQ_GET_DATA3, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                	mEventRecorderListeners.OnError(AvrRecorderErrors.ERR_ERROR);
                }
                else
                {
                    mAvrRecorderDevice.setError(buffer[0]);
                    mEventRecorderListeners.OnDebugMessage(String.format("Error-code stored in device: 0x%02x", mAvrRecorderDevice.getError()));
                }           
                
                // Read event count
                iRxByteCount = mUsbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                        AvrRecorderConstants.REQ_GET_DATA4, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                	mEventRecorderListeners.OnError(AvrRecorderErrors.ERR_COUNT);
                }
                else
                {
                    mAvrRecorderDevice.setEntryCount((short) (buffer[0] | (buffer[1] << 8)));
                    mEventRecorderListeners.OnDebugMessage(String.format("Number of events recorded by device is: %d", mAvrRecorderDevice.getEntryCount()));
                }
            }
        }
	}
	
	/**
	 * Reads event records from device.
	 * For each read event record, <code>mAvrRecorderDevice</code> field is being updated.
	 */
	protected void readDeviceRecords() {
        short eepromdata = 0;
        int iRxByteCount = 0;
        byte[] buffer = new byte[4];
        boolean fReadNext = false;

    	Reading reading = new Reading();
        
        for(short i = 0; i < mAvrRecorderDevice.getEntryCount(); ++i)
        {
            iRxByteCount = mUsbDeviceConnection.controlTransfer(
                    UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                    AvrRecorderConstants.REQ_GET_DATA5, 0, 0, buffer, 4, 5000);

            if(iRxByteCount < 1)
            {
            	mEventRecorderListeners.OnError(AvrRecorderErrors.ERR_RECORD, i);
            }
            else
            {

                if(fReadNext) // overflow detected in previous iteration
                {
                    fReadNext = false;
                    eepromdata |= (buffer[0] << 5);
                    reading.setEntry((short) (reading.getEntry() + 1));
                    reading.setTimestamp(reading.getTimestamp() + eepromdata);
                    eepromdata = 0;
                    mAvrRecorderDevice.addEventReading(new Reading(reading));
                    mEventRecorderListeners.OnDebugMessage(String.format("Record %d:\t%d, %s, %d", i, reading.getEntry(), reading.getCodeName(),  reading.getTimestamp()));
                }
                else
                {
                    eepromdata = (short) (buffer[0] & 0x1F); // read last 5 bits
                    fReadNext = (buffer[0] & AvrRecorderConstants.OV_BIT) == AvrRecorderConstants.OV_BIT; // check OV bit
                    if((buffer[0] >> 6) == AvrRecorderConstants.SWID_UNKNOWN)
                    {
                    	mEventRecorderListeners.OnError(AvrRecorderErrors.ERR_SWITCH);
                        fReadNext = false;
                        continue;
                    }

                    reading.setCode((byte) (buffer[0] >> 6 & 0x03));

                    if(!fReadNext)
                    {
                        reading.setEntry((short) (reading.getEntry() + 1));
                        reading.setTimestamp(reading.getTimestamp() + eepromdata);
                        eepromdata = 0;
                        mAvrRecorderDevice.addEventReading(new Reading(reading));
                        mEventRecorderListeners.OnDebugMessage(String.format("Record %d:\t%d, %s, %d", i, reading.getEntry(), reading.getCodeName(),  reading.getTimestamp()));
                    }
                }
            }
        }
        
        mEventRecorderListeners.OnDebugMessage(String.format("Read %d event records from device", mAvrRecorderDevice.getEventReadings().size()));
	}
	
	/**
	 * Proof of Concept method, used to communicate with USB-enabled device and retrieve all data.
	 * Basically, it does same actions as host application from <a>https://github.com/vladast/avr-based-event-recorder-with-usb-support</a>
	 * @param usbDeviceConnection Instance of <code>UsbDeviceConnection</code>, representing established USB connection.
	 * @return String representation of console output with all read data (status, error-code, event records).
	 */
    static public String communicate(UsbDeviceConnection usbDeviceConnection)
    {   
        String sReturn = "communicate()\n";
        byte[] buffer = new byte[4];
        byte iSession = 0;
        byte entryCount = 0;
        int iRxByteCount = usbDeviceConnection.controlTransfer(
                UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, AvrRecorderConstants.REQ_GET_HEADER, 0, 0, buffer, 4, 5000);
        if(iRxByteCount < 1)
        {
            return "ERROR: USB_TYPE_VENDOR failed!";
        }
        else
        {
            short deviceCode = (short) (buffer[0] | (buffer[1] << 8));
            switch(deviceCode)
            {
            case (short)0xA001:
                sReturn += "AVR001 detected.\n";
                break;
            case (short)0xA002:
                sReturn += "AVR002 detected.\n";
            
                // Read device's state
                iRxByteCount = usbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, AvrRecorderConstants.REQ_GET_DATA1, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                    sReturn += "Error occurred while communicating with AVR002.\n";
                }
                else
                {
                    sReturn += "Device's state is: ";
                    
                    switch(buffer[0])
                    {
                        case (byte)0xA1: // START   = 0xA1, // Device is being started
                            sReturn += "START";
                            break;
                        case (byte)0xB2: // INIT    = 0xB2, // Initialize device
                            sReturn += "INIT";
                            break;
                        case (byte)0xC3: // RECORD  = 0xC3, // Record events (touch-switch states)
                            sReturn += "RECORD";
                            break;
                        case (byte)0xD4: // UPLOAD  = 0xD4, // Upload records to USB host
                            sReturn += "UPLOAD";
                            break;
                        case (byte)0xE5: // DELETE  = 0xE5, // Erase external EEPROM
                            sReturn += "DELETE";
                            break;
                        case (byte)0xF6: // RESES   = 0xF6  // Reinit session counter
                            sReturn += "RESES";
                            break;
                        default:
                            sReturn += "UNDEFINED" + String.format(" [0x%04x]", buffer[0]);                        
                    }
                }
                
                sReturn += "\n";
                
                // Read device's session
                iRxByteCount = usbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, AvrRecorderConstants.REQ_GET_DATA2, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                    sReturn += "Error occurred while communicating with AVR002!\n";
                }
                else
                {
                    iSession = buffer[0];
                    sReturn += "Device's session is: " + Byte.toString(iSession) + " [" + String.format("0x%x", iSession) + "]";
                    sReturn += "\n";
                }
                
                // Read device's error cache
                iRxByteCount = usbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, AvrRecorderConstants.REQ_GET_DATA3, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                    sReturn += "Error occurred while communicating with AVR002!";
                    sReturn += "\n";
                }
                else
                {
                    iSession = buffer[0];
                    
                    if(buffer[0] == (byte)0)
                    {
                        sReturn += "No errors were encountered by device.";
                        sReturn += "\n";
                    }
                    else
                    {
                        sReturn += String.format("Device has encountered an error with error-code 0x%x", buffer[0]);
                        sReturn += "\n";
                    }
                }           
                
                // Read event count
                iRxByteCount = usbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, AvrRecorderConstants.REQ_GET_DATA4, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                    sReturn = "Error occurred while communicating with AVR002!";
                }
                else
                {
                    entryCount = (byte) (buffer[0] | (buffer[1] << 8));
                    sReturn += "Number of events recorded by device: " + entryCount;
                    sReturn += "\n";
                }                
                
                sReturn += "Following events has been read from device:\n\n";
                sReturn += "\tNo.\tCode\tTimestamp\n";
                
                short eepromdata = 0;
                boolean fReadNext = false;
                Reading reading = new Reading();
                ArrayList<Reading> readings = new ArrayList<Reading>();

                for(short i = 0; i < entryCount; ++i)
                {
                    iRxByteCount = usbDeviceConnection.controlTransfer(
                            UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, AvrRecorderConstants.REQ_GET_DATA5, 0, 0, buffer, 4, 5000);

                    if(iRxByteCount < 1)
                    {
                        sReturn += "Error occurred while communicating with AVR002!";
                        sReturn += "\n";
                    }
                    else
                    {

                        if(fReadNext) // overflow detected in previous iteration
                        {
                            fReadNext = false;
                            eepromdata |= (buffer[0] << 5);
                            reading.setEntry((short) (reading.getEntry() + 1));
                            reading.setTimestamp(eepromdata);
                            eepromdata = 0;
                            readings.add(new Reading(reading));
                            sReturn += String.format("\t%d\t\t%s\t%d", reading.getEntry(), reading.getCodeName(),  reading.getTimestamp());
                            sReturn += "\n";
                        }
                        else
                        {
                            eepromdata = (short) (buffer[0] & 0x1F); // read last 5 bits
                            fReadNext = (buffer[0] & AvrRecorderConstants.OV_BIT) == AvrRecorderConstants.OV_BIT; // check OV bit
                            if((buffer[0] >> 6) == AvrRecorderConstants.SWID_UNKNOWN)
                            {
                                sReturn = "Received invalid event code!";
                                fReadNext = false;
                                continue;
                            }

                            reading.setCode((byte) (buffer[0] >> 6 & 0x03));

                            if(!fReadNext)
                            {
                                reading.setEntry((short) (reading.getEntry() + 1));
                                reading.setTimestamp(eepromdata);
                                eepromdata = 0;
                                readings.add(reading);
                                sReturn += String.format("\t%d\t\t%s\t%d", reading.getEntry(), reading.getCodeName(),  reading.getTimestamp());
                                sReturn += "\n";
                            }
                        }
                    }
                }
                
                sReturn += "\n";
                sReturn += String.format("Read %d events", readings.size());
                sReturn += "\n";
            
                // INIT    = 0xB2, // Initialize device
                // RECORD  = 0xC3, // Record events (touch-switch states)
                // UPLOAD  = 0xD4, // Upload records to USB host
                // DELETE  = 0xE5, // Erase external EEPROM
                // RESES   = 0xF6  // Reinit session counter

                // When completed, set device's state to INIT to start new session
                iRxByteCount = usbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_OUT, AvrRecorderConstants.REQ_SET_DATA1, 0xB2, 0, buffer, 0, 5000);
                
                if(iRxByteCount < 0)
                {
                    sReturn += "Failed to set INIT state onto device!\n";
                }
                
                break;
            default:
                sReturn += String.format("Received value is: 0x%04x", deviceCode);
                sReturn += "\n";
            }
        }
        return sReturn;
        
    }
}
