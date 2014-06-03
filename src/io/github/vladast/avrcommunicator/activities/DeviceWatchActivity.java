package io.github.vladast.avrcommunicator.activities;


import java.util.ArrayList;

import io.github.vladast.avrcommunicator.AvrRecorderErrors;
import io.github.vladast.avrcommunicator.Communicator;
import io.github.vladast.avrcommunicator.OnAvrRecorderEventListener;
import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.Reading;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceWatchActivity extends Activity implements OnAvrRecorderEventListener {

	private final String TAG = DeviceWatchActivity.class.getSimpleName();
	
	private TextView mTextViewStatus;
	private TextView mTextViewData;
	private TableLayout mTableLayoutResults;
	private RelativeLayout mRelativeLayoutContainer;
	
	private Communicator mCommunicator;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_watch);
		mCommunicator = new Communicator((UsbManager)getSystemService(Context.USB_SERVICE));
		mCommunicator.registerListener(this);
		
		mTextViewStatus = (TextView)findViewById(R.id.textViewDeviceStatus);
		mTextViewData = (TextView)findViewById(R.id.textViewData);
		mTableLayoutResults = (TableLayout)findViewById(R.id.tableLayoutResults);
		mRelativeLayoutContainer = (RelativeLayout)findViewById(R.id.container);
	}
	
    @Override
    protected void onResume() {
        super.onResume();
        //mTextViewData.setText("Waiting for connection...");
        mCommunicator.startDeviceDetection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCommunicator.stopDeviceDetection();
    }

	private String parseAvrRecorderErrorCode(AvrRecorderErrors avrRecorderErrors) {
		String errorMessage;
		switch (avrRecorderErrors) {
		case ERR_HEADER:
			errorMessage = "Encountered error while reading header data from device.";
			break;
		case ERR_STATE:
			errorMessage = "Encountered error while reading state data from device.";
			break;
		case ERR_SESSION:
			errorMessage = "Encountered error while reading session data from device.";
			break;
		case ERR_ERROR:
			errorMessage = "Encountered error while reading error data from device.";
			break;
		case ERR_COUNT:
			errorMessage = "Encountered error while reading count data from device.";
			break;
		case ERR_RECORD:
			errorMessage = "Encountered error while reading record data from device.";
			break;
		case ERR_SWITCH:
			errorMessage = "Invalid switch code detected.";
			break;
		case ERR_REINIT:
			errorMessage = "Encountered error while re-initiating device.";
			break;
		default:
			errorMessage = "Unknown error code received.";
			break;
		}
		return errorMessage;
	}

	@Override
	public void OnDeviceFound() {
		mTextViewStatus.setText("Device has been found.");
		//Toast.makeText(this, "OnDeviceFound...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnDeviceConnected() {
		mTextViewStatus.setText("Device has been connected.");
		//Toast.makeText(this, "OnDeviceConnected...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnDeviceSearching() {
		mTextViewStatus.setText("Searching for device...");
		//Toast.makeText(this, "OnDeviceSearching...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnDeviceReInitiated() {
		mTextViewStatus.setText("Device has been re-initiated.");
		//Toast.makeText(this, "OnDeviceReInitiated...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnRecordsRead(ArrayList<Reading> eventReadings) {
		//Toast.makeText(this, "OnRecordsRead: " + eventReadings.size(), Toast.LENGTH_LONG).show();
		/*mTextViewData.setText(String.format("Reading completed (session #%d: %d records)", 
				mCommunicator.getDevice().getSession(), eventReadings.size()));*/
		mTextViewStatus.setText(getResources().getText(R.string.device_reading_completed));
		mTextViewData.setText(String.format("\t%s:\t%d\n\t%s:\t%d",
				getResources().getText(R.string.device_session),
				mCommunicator.getDevice().getSession(),
				getResources().getText(R.string.device_records),
				eventReadings.size()));
		for(int i = 0; i < eventReadings.size(); ++i) {
			//mTextViewData.append(String.format("%d\t%s\t%d\n", eventReadings.get(i).getEntry(), eventReadings.get(i).getCodeName(), eventReadings.get(i).getTimestamp()));
			addRecordToTable(eventReadings.get(i), i);
		}
		addOptions();
	}

	private void addOptions() {
		
		int buttonWidth = mRelativeLayoutContainer.getWidth() / 3;
		
		RelativeLayout.LayoutParams relativeLayoutParamsSave = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		RelativeLayout.LayoutParams relativeLayoutParamsShare = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		RelativeLayout.LayoutParams relativeLayoutParamsReInit = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		/*
		Button buttonSave = new Button(this);
		buttonSave.setText("Save");
		buttonSave.setId(0x8001);
		buttonSave.setWidth(buttonWidth);
		buttonSave.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Log.d(TAG, "Saving...");
				String filename = String.format("output_%d.csv", mCommunicator.getDevice().getSession());
				String string = "Hello world!";
				
		        String path = arg0.getContext().getFilesDir() + "/Results/";
		        File file = new File(path);
		        if(!file.isDirectory()) {
		        	file.mkdirs();
		        }
		        path += filename;
		        
				FileOutputStream outputStream;

				try {
				  outputStream = openFileOutput(file.getAbsolutePath(), Context.MODE_APPEND);
				  outputStream.write(string.getBytes());
				  outputStream.close();
				} catch (Exception e) {
				  e.printStackTrace();
				}
			}
		});
		*/
		
		Button buttonShare = new Button(this);
		buttonShare.setText(getResources().getText(R.string.records_options_share));
		buttonShare.setId(0x8002);
		buttonShare.setWidth(buttonWidth);
		buttonShare.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Log.d(TAG, "Sharing...");
				Intent shareIntent = new Intent();
				shareIntent.setAction(Intent.ACTION_SEND);
				shareIntent.putExtra(Intent.EXTRA_TEXT, getRecordsInCsvFormat());
				shareIntent.setType("text/text");
				startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.records_options_share_with)));
			}
		});
		
		Button buttonReInit = new Button(this);
		buttonReInit.setText(getResources().getText(R.string.records_options_re_init));
		buttonReInit.setId(0x8003);
		buttonReInit.setWidth(buttonWidth);
		//buttonReInit.setMinWidth(100);
		buttonReInit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Log.d(TAG, "Re-initiating...");
				mCommunicator.reInitiateDevice();
			}
		});
		
		relativeLayoutParamsSave.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//relativeLayoutParamsSave.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		relativeLayoutParamsShare.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//relativeLayoutParamsShare.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		relativeLayoutParamsReInit.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		relativeLayoutParamsReInit.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		
		//relativeLayoutParamsSave.addRule(RelativeLayout.BELOW, R.id.scrollViewData);
		//relativeLayoutParamsSave.addRule(RelativeLayout.LEFT_OF, buttonShare.getId());
		//mRelativeLayoutContainer.addView(buttonSave, relativeLayoutParamsSave);
		
		//relativeLayoutParamsShare.addRule(RelativeLayout.RIGHT_OF, buttonSave.getId());
		mRelativeLayoutContainer.addView(buttonShare, relativeLayoutParamsShare);
		
		relativeLayoutParamsReInit.addRule(RelativeLayout.RIGHT_OF, buttonShare.getId());
		mRelativeLayoutContainer.addView(buttonReInit, relativeLayoutParamsReInit);
		
		mRelativeLayoutContainer.invalidate();
	}


	private String getRecordsInCsvFormat() {
		//String output = "Index,Entry,SwitchId,Timestamp\n";
		String output = String.format("%s,%s,%s,%s\n",
				getResources().getText(R.string.records_table_column_index),
				getResources().getText(R.string.records_table_column_entry),
				getResources().getText(R.string.records_table_column_switch_id),
				getResources().getText(R.string.records_table_column_timestamp));
		ArrayList<Reading> readings = mCommunicator.getDevice().getEventReadings();
		for(int i = 0; i < readings.size(); ++i) {
			output += String.format("%d,%d,%s,%d\n", 
					i,
					readings.get(i).getEntry(),
					readings.get(i).getCodeName(),
					readings.get(i).getTimestamp()
					);
		}
		return output;
	}
	
	private void addRecordToTable(Reading reading, int i) {
		
		/* Create one row per result */
		TableLayout.LayoutParams tableLayoutParams = new TableLayout.LayoutParams(
				TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.WRAP_CONTENT);
		TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT);
		tableRowParams.width = mTableLayoutResults.getWidth() / 4;
		
		TableRow tableRow;
		TextView textViewIndex, textViewEvent, textViewSwitch, textViewTimestamp;
		
		tableRow = new TableRow(this);
		tableRow.setGravity(LinearLayout.HORIZONTAL);
		textViewIndex = new TextView(this);
		textViewEvent = new TextView(this);
		textViewSwitch = new TextView(this);
		textViewTimestamp = new TextView(this);
		
		textViewIndex.setText(String.valueOf(i));
		textViewEvent.setText(Short.toString(reading.getEntry()));
		textViewSwitch.setText(reading.getCodeName());
		textViewTimestamp.setText(String.valueOf(reading.getTimestamp()));
		
		tableRow.addView(textViewIndex, tableRowParams);
		tableRow.addView(textViewEvent, tableRowParams);
		tableRow.addView(textViewSwitch, tableRowParams);
		tableRow.addView(textViewTimestamp, tableRowParams);
		
		mTableLayoutResults.addView(tableRow, tableLayoutParams);
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors) {
		//Toast.makeText(this, "onError1", Toast.LENGTH_LONG).show();
		Log.e(Communicator.TAG, parseAvrRecorderErrorCode(avrRecorderErrors));
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors, int data) {
		Toast.makeText(this, "onError2", Toast.LENGTH_LONG).show();
		Log.e(Communicator.TAG, parseAvrRecorderErrorCode(avrRecorderErrors) + String.format("[data: %d]", data));
	}

	@Override
	public void OnReadingStarted() {
		//Toast.makeText(this, "OnReadingStarted", Toast.LENGTH_LONG).show();
	}

	@Override
	public void OnDebugMessage(String message) {
		//Toast.makeText(this, "OnDebugMessage: " + message, Toast.LENGTH_LONG).show();
		Log.d(Communicator.TAG, message);
	}    
}
