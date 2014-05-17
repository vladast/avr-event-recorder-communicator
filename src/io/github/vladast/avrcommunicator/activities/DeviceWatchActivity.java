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
import android.widget.TextView;
import android.widget.Toast;

public class DeviceWatchActivity extends Activity implements OnAvrRecorderEventListener {

	private final String TAG = DeviceWatchActivity.class.getSimpleName();
	
	private TextView mTextViewStatus;
	private TextView mTextViewData;
	private boolean mRecordsRead;
	
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

	@Override
	public void OnDeviceFound() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnDeviceConnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnDeviceSearching() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnDeviceReInitiated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnRecordsRead(ArrayList<Reading> eventReadings) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "onError1", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors, int data) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "onError2", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnReadingStarted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnDebugMessage(String message) {
		// TODO Auto-generated method stub
		
	}    
}
