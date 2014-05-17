package io.github.vladast.avrcommunicator.activities;


import java.util.ArrayList;

import io.github.vladast.avrcommunicator.AvrRecorderErrors;
import io.github.vladast.avrcommunicator.Communicator;
import io.github.vladast.avrcommunicator.OnAvrRecorderEventListener;
import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.Reading;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceWatchActivity extends Activity implements OnAvrRecorderEventListener {

	private final String TAG = DeviceWatchActivity.class.getSimpleName();
	
	private TextView mTextViewStatus;
	private TextView mTextViewData;
	
	private Communicator mCommunicator;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_watch);
		mCommunicator = new Communicator((UsbManager)getSystemService(Context.USB_SERVICE));
		
		mTextViewStatus = (TextView)findViewById(R.id.textViewDeviceStatus);
		mTextViewData = (TextView)findViewById(R.id.textViewData);
		
		mCommunicator.registerListener(this);
	}
	
    @Override
    protected void onResume() {
        super.onResume();
        mCommunicator.startDeviceDetection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCommunicator.stopDeviceDetection();
    }

	private String parseAvrRecorderErrorCode(AvrRecorderErrors avrRecorderErrors) {
		switch (avrRecorderErrors) {
		case ERR_HEADER:
			Log.e(Communicator.TAG, "Encountered error while reading header data from device.");
			break;
		case ERR_STATE:
			Log.e(Communicator.TAG, "Encountered error while reading state data from device.");
			break;
		case ERR_SESSION:
			Log.e(Communicator.TAG, "Encountered error while reading session data from device.");
			break;
		case ERR_ERROR:
			Log.e(Communicator.TAG, "Encountered error while reading error data from device.");
			break;
		case ERR_COUNT:
			Log.e(Communicator.TAG, "Encountered error while reading count data from device.");
			break;
		case ERR_RECORD:
			Log.e(Communicator.TAG, "Encountered error while reading record data from device.");
			break;
		case ERR_SWITCH:
			Log.e(Communicator.TAG, "Invalid switch code detected.");
			break;
		default:
			Log.e(Communicator.TAG, "Unknown error code received.");
			break;
		}
		return null;
	}

	@Override
	public void OnDeviceFound() {
		mTextViewStatus.setText("Device has been found.");
		Toast.makeText(this, "OnDeviceFound...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnDeviceConnected() {
		mTextViewStatus.setText("Device has been connected.");
		Toast.makeText(this, "OnDeviceConnected...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnDeviceSearching() {
		mTextViewStatus.setText("Searching for device...");
		Toast.makeText(this, "OnDeviceSearching...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnDeviceReInitiated() {
		mTextViewStatus.setText("Device has been re-initiated.");
		Toast.makeText(this, "OnDeviceReInitiated...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnRecordsRead(ArrayList<Reading> eventReadings) {
		Toast.makeText(this, "OnRecordsRead: " + eventReadings.size(), Toast.LENGTH_LONG).show();
		mTextViewData.setText("");
		for(int i = 0; i < eventReadings.size(); ++i) {
			mTextViewData.append(String.format("%d\t%s\t%d\n", eventReadings.get(i).getEntry(), eventReadings.get(i).getCodeName(), eventReadings.get(i).getTimestamp()));
		}
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors) {
		Toast.makeText(this, "onError1", Toast.LENGTH_LONG).show();
		Log.e(Communicator.TAG, parseAvrRecorderErrorCode(avrRecorderErrors));
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors, int data) {
		Toast.makeText(this, "onError2", Toast.LENGTH_LONG).show();
		Log.e(Communicator.TAG, parseAvrRecorderErrorCode(avrRecorderErrors) + String.format("[data: %d]", data));
	}

	@Override
	public void OnReadingStarted() {
		// TODO Auto-generated method stub
		Toast.makeText(this, "OnReadingStarted", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnDebugMessage(String message) {
		//Toast.makeText(this, "OnDebugMessage: " + message, Toast.LENGTH_LONG).show();
		Log.d(Communicator.TAG, message);
	}    
}
