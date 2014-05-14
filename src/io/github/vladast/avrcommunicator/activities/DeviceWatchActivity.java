package io.github.vladast.avrcommunicator.activities;


import io.github.vladast.avrcommunicator.AvrRecorderConstants;
import io.github.vladast.avrcommunicator.Communicator;
import io.github.vladast.avrcommunicator.R;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class DeviceWatchActivity extends Activity {

	private final String TAG = DeviceWatchActivity.class.getSimpleName();
	
	private static final int MSG_DEVICE_DETECTED		= 0x0001;
	private static final int MSG_CHECK_DEVICE_STATUS	= 0x0002;
	private static final int MSG_REINIT_DEVICE			= 0x0003;
	
	private UsbManager mUsbManager;
	private TextView mTextViewStatus;
	private TextView mTextViewData;
	private boolean mRecordsRead;
	
	private final Handler mAvrRecorderMonitorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
	            case MSG_CHECK_DEVICE_STATUS:
	            	if(!mRecordsRead)
	            	{
	            		checkDeviceStatus();
	            		mAvrRecorderMonitorHandler.sendEmptyMessageDelayed(MSG_CHECK_DEVICE_STATUS, 1000);
	            	}
	            	break;
                case MSG_DEVICE_DETECTED:
                	getEventRecords((UsbDevice)msg.obj);
                    break;
                case MSG_REINIT_DEVICE:
                	reinitDevice((UsbDevice)msg.obj);
                	break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_watch);
		
		mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
		
		mTextViewStatus = (TextView)findViewById(R.id.textViewDeviceStatus);
		mTextViewData = (TextView)findViewById(R.id.textViewData);
		
		mRecordsRead = false;
	}

    protected void reinitDevice(UsbDevice usbDevice) {
		mTextViewStatus.setText("Status: Getting event records...");
		
		UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(usbDevice);
		String sOutput = "No message received.";
		
		for(int i = 0; i < usbDevice.getInterfaceCount(); ++i) {
			if(usbDeviceConnection.claimInterface(usbDevice.getInterface(i), true)) {
				sOutput = Communicator.communicate(usbDeviceConnection);		
			}
		}
		
		mRecordsRead = true;
		
		mTextViewData.setText(sOutput);
	}

	protected void checkDeviceStatus() {
    	
    	mTextViewStatus.setText("Status: searching...");
        
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

	protected void getEventRecords(UsbDevice usbDevice) {
		mTextViewStatus.setText("Status: Getting event records...");
		
		UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(usbDevice);
		String sOutput = "No message received.";
		
		for(int i = 0; i < usbDevice.getInterfaceCount(); ++i) {
			if(usbDeviceConnection.claimInterface(usbDevice.getInterface(i), true)) {
				sOutput = Communicator.communicate(usbDeviceConnection);		
			}
		}
		
		mRecordsRead = true;
		
		mTextViewData.setText(sOutput);
	}
	
    @Override
    protected void onResume() {
        super.onResume();
        mAvrRecorderMonitorHandler.sendEmptyMessage(MSG_CHECK_DEVICE_STATUS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAvrRecorderMonitorHandler.removeMessages(MSG_CHECK_DEVICE_STATUS);
    }    
}
