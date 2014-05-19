package io.github.vladast.avrcommunicator;

import java.util.ArrayList;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class Communicator {

	public static final String TAG = Communicator.class.getSimpleName();
	
	private static final int MSG_DEVICE_DETECTED		= 0x0001;
	private static final int MSG_CHECK_DEVICE_STATUS	= 0x0002;
	private static final int MSG_REINIT_DEVICE			= 0x0003;
	
	private UsbManager mUsbManager;
	private UsbDeviceConnection mUsbDeviceConnection;
	private OnAvrRecorderEventListener mAvrRecorderEventListener;
	
	private boolean mRecordsRead;
	
	private AvrRecorderDevice mAvrRecorderDevice;
	
	private final Handler mAvrRecorderMonitorHandler;
	
	public Communicator(UsbManager usbManager) {
		mUsbManager = usbManager;
		mRecordsRead = false;
		mAvrRecorderDevice = null;
		mAvrRecorderMonitorHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
		            case MSG_CHECK_DEVICE_STATUS:
		            	mAvrRecorderEventListener.OnDebugMessage("Received MSG_CHECK_DEVICE_STATUS message");
		            	if(!mRecordsRead)
		            	{
		            		checkDeviceStatus();
		            		mAvrRecorderMonitorHandler.sendEmptyMessageDelayed(MSG_CHECK_DEVICE_STATUS, 1000);
		            	}
		            	break;
	                case MSG_DEVICE_DETECTED:
	                	mAvrRecorderEventListener.OnDeviceConnected();
	                	readDeviceData((UsbDevice)msg.obj);
	                    break;
	                case MSG_REINIT_DEVICE:
	                	//reinitDevice((UsbDevice)msg.obj);
	                	break;
	                default:
	                    super.handleMessage(msg);
	                    break;
	            }
	        }
	    };			
	}
	
	public void registerListener(OnAvrRecorderEventListener onAvrRecorderEventListener) {
		mAvrRecorderEventListener = onAvrRecorderEventListener;
	}
	
	public void startDeviceDetection() {
		mAvrRecorderMonitorHandler.sendEmptyMessage(MSG_CHECK_DEVICE_STATUS);
	}
	
	public void stopDeviceDetection() {
		mAvrRecorderMonitorHandler.removeMessages(MSG_CHECK_DEVICE_STATUS);
	}
	
	protected void checkDeviceStatus() {
    	
    	mAvrRecorderEventListener.OnDeviceSearching();
        
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
	
	protected void readDeviceData(UsbDevice usbDevice) {
		mAvrRecorderEventListener.OnReadingStarted();
		
		mAvrRecorderDevice = new AvrRecorderDevice();
		
		mUsbDeviceConnection = mUsbManager.openDevice(usbDevice);
		
		for(int i = 0; i < usbDevice.getInterfaceCount(); ++i) {
			if(mUsbDeviceConnection.claimInterface(usbDevice.getInterface(i), true)) {
				readDeviceInfo(); // Read status header and status codes
				readDeviceRecords(); // Read records from device
				// At this moment, via UI activity, user will be asked for further actions:
				// 1. To re-initiate device or not
				// 2. To share collected data with other application (i.e. Mail client)
				
				// sOutput = Communicator.communicate(mUsbDeviceConnection);		
			}
		}
		
		mRecordsRead = true;
		
		if(mAvrRecorderDevice.getEventReadings().size() > 0) {
			mAvrRecorderEventListener.OnRecordsRead(mAvrRecorderDevice.getEventReadings());
		}
	}
	
	protected void readDeviceInfo() {
        byte[] buffer = new byte[4];
        int iRxByteCount = 0;
        
        iRxByteCount = mUsbDeviceConnection.controlTransfer(
                UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                AvrRecorderConstants.REQ_GET_HEADER, 0, 0, buffer, 4, 5000);
        if(iRxByteCount < 1)
        {
            mAvrRecorderEventListener.OnError(AvrRecorderErrors.ERR_HEADER);
        }
        else
        {
            mAvrRecorderDevice.setDeviceCode((short) (buffer[0] | (buffer[1] << 8)));
            mAvrRecorderEventListener.OnDebugMessage(String.format("Detected device with code 0x%04x", mAvrRecorderDevice.getDeviceCode()));
            
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
                	mAvrRecorderEventListener.OnError(AvrRecorderErrors.ERR_STATE);
                }
                else
                {
                    mAvrRecorderDevice.setState(buffer[0]);
                    mAvrRecorderEventListener.OnDebugMessage(String.format("Device's state is '%s'", mAvrRecorderDevice.getStateName()));
                }
                
                // Read device's session
                iRxByteCount = mUsbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                        AvrRecorderConstants.REQ_GET_DATA2, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                    mAvrRecorderEventListener.OnError(AvrRecorderErrors.ERR_SESSION);
                }
                else
                {
                    mAvrRecorderDevice.setSession(buffer[0]);
                    mAvrRecorderEventListener.OnDebugMessage(String.format("Device's session is: %s [0x%02x]", Byte.toString(mAvrRecorderDevice.getSession()), mAvrRecorderDevice.getSession()));
                }
                
                // Read device's error cache
                iRxByteCount = mUsbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                        AvrRecorderConstants.REQ_GET_DATA3, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                	mAvrRecorderEventListener.OnError(AvrRecorderErrors.ERR_ERROR);
                }
                else
                {
                    mAvrRecorderDevice.setError(buffer[0]);
                    mAvrRecorderEventListener.OnDebugMessage(String.format("Error-code stored in device: 0x%02x", mAvrRecorderDevice.getError()));
                }           
                
                // Read event count
                iRxByteCount = mUsbDeviceConnection.controlTransfer(
                        UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                        AvrRecorderConstants.REQ_GET_DATA4, 0, 0, buffer, 4, 5000);
                
                if(iRxByteCount < 0)
                {
                	mAvrRecorderEventListener.OnError(AvrRecorderErrors.ERR_COUNT);
                }
                else
                {
                    mAvrRecorderDevice.setEntryCount((short) (buffer[0] | (buffer[1] << 8)));
                    mAvrRecorderEventListener.OnDebugMessage(String.format("Number of events recorded by device is: %d", mAvrRecorderDevice.getEntryCount()));
                }
            }
        }
	}
	
	protected void readDeviceRecords() {
        short eepromdata = 0;
        int iRxByteCount = 0;
        byte[] buffer = new byte[4];
        boolean fReadNext = false;
        
        for(short i = 0; i < mAvrRecorderDevice.getEntryCount(); ++i)
        {
        	Reading reading = new Reading();
        	
            iRxByteCount = mUsbDeviceConnection.controlTransfer(
                    UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_ENDPOINT_XFER_CONTROL | UsbConstants.USB_DIR_IN, 
                    AvrRecorderConstants.REQ_GET_DATA5, 0, 0, buffer, 4, 5000);

            if(iRxByteCount < 1)
            {
                mAvrRecorderEventListener.OnError(AvrRecorderErrors.ERR_RECORD, i);
            }
            else
            {

                if(fReadNext) // overflow detected in previous iteration
                {
                    fReadNext = false;
                    eepromdata |= (buffer[0] << 5);
                    reading.setEntry((byte) (reading.getEntry() + 1));
                    reading.setTimestamp(eepromdata);
                    eepromdata = 0;
                    mAvrRecorderDevice.addEventReading(reading);
                    mAvrRecorderEventListener.OnDebugMessage(String.format("Record %d:\t%d, %s, %d", i, reading.getEntry(), reading.getCodeName(),  reading.getTimestamp()));
                }
                else
                {
                    eepromdata = (short) (buffer[0] & 0x1F); // read last 5 bits
                    fReadNext = (buffer[0] & AvrRecorderConstants.OV_BIT) == AvrRecorderConstants.OV_BIT; // check OV bit
                    if((buffer[0] >> 6) == AvrRecorderConstants.SWID_UNKNOWN)
                    {
                    	mAvrRecorderEventListener.OnError(AvrRecorderErrors.ERR_SWITCH);
                        fReadNext = false;
                        continue;
                    }

                    reading.setCode((byte) (buffer[0] >> 6 & 0x03));

                    if(!fReadNext)
                    {
                        reading.setEntry((byte) (reading.getEntry() + 1));
                        reading.setTimestamp(eepromdata);
                        eepromdata = 0;
                        mAvrRecorderDevice.addEventReading(reading);
                        mAvrRecorderEventListener.OnDebugMessage(String.format("Record %d:\t%d, %s, %d", i, reading.getEntry(), reading.getCodeName(),  reading.getTimestamp()));
                    }
                }
            }
        }
        
        mAvrRecorderEventListener.OnDebugMessage(String.format("Read %d event records from device", mAvrRecorderDevice.getEventReadings().size()));
	}
	
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
                            reading.setEntry((byte) (reading.getEntry() + 1));
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
                                reading.setEntry((byte) (reading.getEntry() + 1));
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
