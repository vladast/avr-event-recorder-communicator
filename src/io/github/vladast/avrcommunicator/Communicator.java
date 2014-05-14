package io.github.vladast.avrcommunicator;

import java.util.ArrayList;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;

public final class Communicator {
	
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
