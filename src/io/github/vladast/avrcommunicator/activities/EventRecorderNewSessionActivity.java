package io.github.vladast.avrcommunicator.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.github.vladast.avrcommunicator.AvrRecorderConstants;
import io.github.vladast.avrcommunicator.EventRecorderApplication;
import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.R.layout;
import io.github.vladast.avrcommunicator.db.EventRecorderDatabaseHandler;
import io.github.vladast.avrcommunicator.db.dao.DeviceDAO;
import io.github.vladast.avrcommunicator.db.dao.EventDAO;
import io.github.vladast.avrcommunicator.db.dao.EventRecorderDAO;
import io.github.vladast.avrcommunicator.db.dao.OnDatabaseRequestListener;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;
import io.github.vladast.avrcommunicator.db.dao.TouchableDAO;
import io.github.vladast.avrcommunicator.dialogs.DiscardNewSessionAlertDialog;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.TableLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.os.Build;
import android.preference.PreferenceManager;

public class EventRecorderNewSessionActivity extends Activity implements OnClickListener {

	private static final String TAG = EventRecorderNewSessionActivity.class.getSimpleName();
	
	// TODO Move this property to Settings so that user can decide on smallest measured time interval
	/** In v1.0, only seconds are being measured */
	private static final boolean MEASURE_MILLISECONDS = false;
	/** Static member defining layout check interval in milliseconds */
	private static final int LAYOUT_CHECK_INTERVAL 	= 100;
	
	/** Static member defining outbound margin for touchable elements */
	private static final int TOUCHABLES_LAYOUT_OUT_MARGIN	= 4;
	/** Static member defining inbound margin for touchable elements */
	private static final int TOUCHABLES_LAYOUT_IN_MARGIN	= 10;
	/** Static member defining ratio of touchable's height and count text height */
	private static final int TOUCHABLES_COUNT_RATIO			= 20;
	/** Static member defining ratio of touchable's height and name text height */
	private static final int TOUCHABLES_NAME_RATIO			= 10;
	
	/** Static member used for "change to key down color" message */
	private static final int MSG_TOUCH_KEY_DOWN_COLOR		= 0x0001;
	/** Static member used for "change to key up color" message */
	private static final int MSG_TOUCH_KEY_UP_COLOR			= 0x0002;	
	
	private static final String STATE_TIME_START			= "timeStart";
	private static final String STATE_TIME_CURRENT			= "timeCurrent";
	private static final String STATE_TIME_PREVIOUS			= "timePrevious";
	private static final String STATE_TIMER_STARTED			= "timerStarted";
	private static final String STATE_COUNTERS				= "touchCounters";
	private static final String STATE_EVENTS				= "events";
	private static final String STATE_SESSION_ID			= "idSession";
	
	/** A flag that indicates whether timer is started on not. */
	private boolean mTimerStarted;
	/** <code>TextView</code> element linked to time display. */
	private TextView mTextViewTimer;
	/** Timer handler used to start/stop timer thread. */
	private Handler mHandlerTimer;
	/** Layout handler used to start/stop layout thread. */
	private Handler mHandlerLayout;
	/** Layout handler used to change background color of touched elements */
	private Handler mHandlerTouch;
	/** Start time in milliseconds. */
	private long mStartTime;
	/** Current time in milliseconds. */
	private long mCurrentTime;
	/** Previous time in milliseconds. Used to calculate time difference between two events. */
	private long mPreviousTime;
	/** List of database objects designating available touchable elements */
	private ArrayList<EventRecorderDAO> mTouchables;
	/** Map of counts for each touchable element */
	private SparseIntArray mSparseIntArrayTouchCounts;
	/** Color of touchable element */
	// TODO Create a map between touchables and colors from settings; that can be also pulled out from db.
	private int mColorTouchable;
	/** Color of disabled touchable element */
	private int mColorTouchableDisabled;
	/** List of recorded events. */
	private ArrayList<EventDAO> mEvents;
	/** Current session's name. Created when recording is completed, from completion date&time */
	private SessionDAO mCurrentSession;
	/** Timestamp of the recording, when recording got completed. */
	private Date mTimestampRecording;
	/** Dialog displayed before session gets discarded. By clicking on Cancel, user is able to cancel the action. */
	private DiscardNewSessionAlertDialog mPreDiscardDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_session);
		
		mColorTouchable = 0xffdaeaba; // TODO Read this value from Settings
		mColorTouchableDisabled = 0xff00da00; // TODO Read this value from Settings	
		
		mTextViewTimer = (TextView)findViewById(R.id.textViewTimer);
		mTextViewTimer.setText(MEASURE_MILLISECONDS ? "00:00:00.000" : "00:00:00");

		((ImageButton)findViewById(R.id.imageButtonRecordToggle)).setOnClickListener(this);		

		long currentSessionCount = ((EventRecorderApplication)this.getApplicationContext()).getDatabaseHandler().getDatabaseObjectCount(SessionDAO.class);		

		mTouchables = ((EventRecorderApplication)this.getApplicationContext()).getDatabaseHandler().getDatabaseObjects(TouchableDAO.class);
		mEvents = new ArrayList<EventDAO>();
		mCurrentSession = new SessionDAO(null);		
		mSparseIntArrayTouchCounts = new SparseIntArray(mTouchables.size());
		
		if(savedInstanceState == null || !savedInstanceState.containsKey(STATE_TIMER_STARTED)) {
			
			mTimerStarted = false;

			((TextView)findViewById(R.id.textViewSessionCount)).setText(String.valueOf(currentSessionCount + 1));
			
			/** Initialize map of counts */
			for (EventRecorderDAO touchable : mTouchables) {
				mSparseIntArrayTouchCounts.put((int) touchable.getId(), 0);
			}
			
			mCurrentTime = mStartTime = mPreviousTime = 0;			
		} else {
			/** Restore data from saved instance state */
			mTimerStarted = savedInstanceState.getBoolean(STATE_TIMER_STARTED);
			if(mTimerStarted) {
				((TextView)findViewById(R.id.textViewSessionCount)).setText(String.valueOf(currentSessionCount + 1));
				
				mStartTime = savedInstanceState.getLong(STATE_TIME_START);
				mCurrentTime = savedInstanceState.getLong(STATE_TIME_CURRENT);
				mPreviousTime = savedInstanceState.getLong(STATE_TIME_PREVIOUS);
				mEvents = (ArrayList<EventDAO>) savedInstanceState.getSerializable(STATE_EVENTS);
			} else {
				((TextView)findViewById(R.id.textViewSessionCount)).setText(String.valueOf(currentSessionCount));
				mCurrentSession = (SessionDAO) ((EventRecorderApplication)this.getApplicationContext()).getDatabaseHandler().getDatabaseObjectById(SessionDAO.class, savedInstanceState.getLong(STATE_SESSION_ID));
				
				mTimestampRecording = mCurrentSession.getTimestampRecorded();
				
				showDialogSave();
				
			}
			ArrayList<Integer> touchCountsInterlaced = savedInstanceState.getIntegerArrayList(STATE_COUNTERS);
			for(int i = 0; i < touchCountsInterlaced.size(); i += 2) {
				mSparseIntArrayTouchCounts.put(touchCountsInterlaced.get(i), touchCountsInterlaced.get(i + 1));
			}
		}
		
		mPreDiscardDialog = new DiscardNewSessionAlertDialog(this);
		mPreDiscardDialog.setTitle(R.string.new_session_pre_discard_title);
		mPreDiscardDialog.setMessage(R.string.new_session_pre_discard_message);
		mPreDiscardDialog.setPositiveButton(R.string.new_session_pre_discard_button_text_discard, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				removeSessionAndEvents();
				switch (mPreDiscardDialog.getCaller()) {
				case DiscardNewSessionAlertDialog.DIALOG_DISCARD:
					Intent intentHome = new Intent(mPreDiscardDialog.getContext(), EventRecorderHomeActivity.class);
					startActivity(intentHome);
					break;
				case DiscardNewSessionAlertDialog.DIALOG_DISCARD_BACK:
					((Activity)mPreDiscardDialog.getContext()).onBackPressed();
					break;
				case DiscardNewSessionAlertDialog.DIALOG_DISCARD_NAVIGATE_UP:
					((Activity)mPreDiscardDialog.getContext()).onNavigateUp();
					break;
				default:
					break;
				}
			}
		});
		mPreDiscardDialog.setNegativeButton(R.string.new_session_pre_discard_button_text_cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.d(TAG, "Session discard canceled");
				mPreDiscardDialog.SetCaller(DiscardNewSessionAlertDialog.DIALOG_DISCARD);
			}
		});
		
		mHandlerTimer = new Handler();
		mHandlerLayout = new Handler();
		mHandlerTouch = new Handler() {
			@Override
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
		            case MSG_TOUCH_KEY_DOWN_COLOR:
	                	// TODO Use inverted color value for the background color
	                	// TODO Add additional Settings entry - user can choose whether inverted color should be displayed, or pre-defined ones.
		            	findViewById(((Integer)msg.obj).intValue()).setBackgroundColor(0xba00ba);
		            	Message keyUpMessage = new Message();
		            	keyUpMessage.what = MSG_TOUCH_KEY_UP_COLOR;
		            	keyUpMessage.obj = msg.obj;
		            	mHandlerTouch.sendMessageDelayed(keyUpMessage, 100);
		            	break;
	                case MSG_TOUCH_KEY_UP_COLOR:
		            	findViewById(((Integer)msg.obj).intValue()).setBackgroundColor(mColorTouchable);
	                    break;
	                default:
	                    super.handleMessage(msg);
	                    break;
	            }
	        }
		};		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume...");
		mHandlerLayout.postDelayed(mRunnableLayoutThread, LAYOUT_CHECK_INTERVAL);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		/**
		 * The only useful cases for saving instance state are:
		 * 	1. Recording has stopped, and the Save dialog is being opened
		 * 	2. Recording is in progress
		 */
		if(mEvents.size() > 1 || mTimerStarted) {
			savedInstanceState.putBoolean(STATE_TIMER_STARTED, mTimerStarted);
			if(mTimerStarted) {
				savedInstanceState.putLong(STATE_TIME_START, mStartTime);
				savedInstanceState.putLong(STATE_TIME_CURRENT, mCurrentTime);
				savedInstanceState.putLong(STATE_TIME_PREVIOUS, mPreviousTime);
				savedInstanceState.putSerializable(STATE_EVENTS, mEvents);	
			} else {
				savedInstanceState.putLong(STATE_SESSION_ID, mCurrentSession.getId());
			}
			
			ArrayList<Integer> touchCountsInterlaced = new ArrayList<Integer>();
			for(int i = 0; i < mSparseIntArrayTouchCounts.size(); i++) {
				int value, key;
				value = mSparseIntArrayTouchCounts.valueAt(i);
				key = mSparseIntArrayTouchCounts.keyAt(i);
				touchCountsInterlaced.add(key);
				touchCountsInterlaced.add(value);
			}
			
			savedInstanceState.putIntegerArrayList(STATE_COUNTERS, touchCountsInterlaced);
		}
		
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onBackPressed() {
		Log.d(TAG, "Back pressed!");
		if(mPreDiscardDialog.getCaller() != DiscardNewSessionAlertDialog.DIALOG_DISCARD_BACK && mEvents.size() > 1)
			mPreDiscardDialog.show(DiscardNewSessionAlertDialog.DIALOG_DISCARD_BACK);
		else
			super.onBackPressed();
	}
	
	@Override
	public boolean onNavigateUp() {
		Log.d(TAG, "Navigate up pressed!");
		if(mPreDiscardDialog.getCaller() != DiscardNewSessionAlertDialog.DIALOG_DISCARD_NAVIGATE_UP && mEvents.size() > 1)
			mPreDiscardDialog.show(DiscardNewSessionAlertDialog.DIALOG_DISCARD_NAVIGATE_UP);
		else
			super.onNavigateUp();
		return true;
	}
	
	/**
	 * Displays touchable elements dynamically.
	 */
	private void displayTouchables() {
		/**
		 * Create table dynamically
		 * 
		 * v1.0:
		 * 	1. Read number of touchable elements from Settings
		 * 	2. Create table based upon read number
		 * 
		 * TODO v2.0:
		 * 	1. Read number of default touchable elements from Settings
		 * 	2. Display presets (pre-defined touchable names that can be edited by user from Settings)
		 * 	3. Display number option - for pre-defined pattern names, user only selects number of touchables that are going to be displayed
		 */
		
		int numberOfTouchables = Integer.parseInt(
				PreferenceManager.getDefaultSharedPreferences(this).getString(EventRecorderSettingsActivity.KEY_PREF_EVENT_NUMBER, "3").split(" ")[0]); 
		Log.d(TAG, String.format("Creating %d touchable elements...", numberOfTouchables));
		
		boolean isPortraitOrientation = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

		TableRow.LayoutParams tableRowLayoutParams = null;
		
		RelativeLayout.LayoutParams relativeLayoutCellLayoutParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		
		TableLayout tableLayoutTouchables = (TableLayout)findViewById(R.id.tableLayoutTouchables);
		TableRow tableRow = null;
		TextView textViewTouchCounter = null;
		TextView textViewTouchableName = null;
		RelativeLayout relativeLayoutCell = null;
		
		int touchableWidth = 0;
		int touchableHeight = 0;
		int numberOfRows = 0;
		int numberOfColumns = 0;
		
		switch (numberOfTouchables) {
		case 1:
		{
			/**
			 * Number of rows:		1
			 * Number of columns:	1
			 * No inbound margins
			 */
			numberOfRows = numberOfColumns = 1;
			touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
			touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
			
			tableRow = new TableRow(this);
			
			tableRowLayoutParams = new TableRow.LayoutParams(
					TableRow.LayoutParams.WRAP_CONTENT,
					TableRow.LayoutParams.WRAP_CONTENT);
			tableRowLayoutParams.width = touchableWidth;
			tableRowLayoutParams.height = touchableHeight;
			tableRowLayoutParams.setMargins(TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
			
			textViewTouchCounter = new TextView(this);
			textViewTouchableName = new TextView(this);
			
			textViewTouchCounter.setId(0);
			textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(0).getId())));
			textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
			textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
			textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
			
			textViewTouchableName.setText(((TouchableDAO)mTouchables.get(0)).getName());
			textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
			textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
			textViewTouchableName.setGravity(Gravity.CENTER);
			
			relativeLayoutCell = new RelativeLayout(this);
			relativeLayoutCell.setId((int) mTouchables.get(0).getId()); // There's only one touchable --> index 0 is hardcoded
			relativeLayoutCell.setGravity(Gravity.CENTER);
			relativeLayoutCell.setBackgroundColor(mColorTouchable);
			relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
			relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
			
			relativeLayoutCell.setOnClickListener(this);

			tableRow.addView(relativeLayoutCell, tableRowLayoutParams);
			tableLayoutTouchables.addView(tableRow);
			break;
		}
		case 2:
		{
			if(isPortraitOrientation) {
				/**
				 * Portrait:
				 * 	Number of rows:		2
				 * 	Number of columns:	1
				 */	
				numberOfRows = 2;
				numberOfColumns = 1;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					
					tableRowLayoutParams = new TableRow.LayoutParams(
							TableRow.LayoutParams.WRAP_CONTENT,
							TableRow.LayoutParams.WRAP_CONTENT);
					tableRowLayoutParams.width = touchableWidth;
					tableRowLayoutParams.height = touchableHeight;
					if(i == 0)
						// For first row, bottom margin is twice as smaller than outbound margin
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
					else
						// For second row, top margin is twice as smaller than outbount margin
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
					
					textViewTouchCounter = new TextView(this);
					textViewTouchableName = new TextView(this);
					
					textViewTouchCounter.setId(0);
					textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(i).getId())));
					textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
					textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
					textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
					
					textViewTouchableName.setText(((TouchableDAO)mTouchables.get(i)).getName());
					textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
					textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
					textViewTouchableName.setGravity(Gravity.CENTER);
					
					relativeLayoutCell = new RelativeLayout(this);
					relativeLayoutCell.setId((int) mTouchables.get(i).getId());
					relativeLayoutCell.setGravity(Gravity.CENTER);
					relativeLayoutCell.setBackgroundColor(mColorTouchable);
					relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
					relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
					
					relativeLayoutCell.setOnClickListener(this);

					tableRow.addView(relativeLayoutCell, tableRowLayoutParams);
					tableLayoutTouchables.addView(tableRow);
				}
				
			} else {
				/**
				 * Landscape:
				 * 	Number of rows:		1
				 * 	Number of columns:	2
				 */
				numberOfRows = 1;
				numberOfColumns = 2;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				
				tableRow = new TableRow(this);
				
				for(int i = 0; i < numberOfColumns; ++i) {
					tableRowLayoutParams = new TableRow.LayoutParams(
							TableRow.LayoutParams.WRAP_CONTENT,
							TableRow.LayoutParams.WRAP_CONTENT);
					tableRowLayoutParams.width = touchableWidth;
					tableRowLayoutParams.height = touchableHeight;
					
					if(i == 0)
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
					else
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
					
					textViewTouchCounter = new TextView(this);
					textViewTouchableName = new TextView(this);
					
					textViewTouchCounter.setId(0);
					textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(i).getId())));
					textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
					textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
					textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
					
					textViewTouchableName.setText(((TouchableDAO)mTouchables.get(i)).getName());
					textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
					textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
					textViewTouchableName.setGravity(Gravity.CENTER);
					
					relativeLayoutCell = new RelativeLayout(this);
					relativeLayoutCell.setId((int) mTouchables.get(i).getId());
					relativeLayoutCell.setGravity(Gravity.CENTER);
					relativeLayoutCell.setBackgroundColor(mColorTouchable);
					relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
					relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
					relativeLayoutCell.setOnClickListener(this);

					tableRow.addView(relativeLayoutCell, tableRowLayoutParams);
				}
				tableLayoutTouchables.addView(tableRow);				
			}
			break;
		}
		case 3:
		{
			if(isPortraitOrientation) {
				/**
				 * Portrait:
				 * 	Number of rows:		3
				 * 	Number of columns:	1
				 */
				numberOfRows = 3;
				numberOfColumns = 1;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					
					tableRowLayoutParams = new TableRow.LayoutParams(
							TableRow.LayoutParams.WRAP_CONTENT,
							TableRow.LayoutParams.WRAP_CONTENT);
					tableRowLayoutParams.width = touchableWidth;
					tableRowLayoutParams.height = touchableHeight;
					if(i == 0)
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
					else if (i == 1)
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
					else
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
					
					textViewTouchCounter = new TextView(this);
					textViewTouchableName = new TextView(this);
					
					textViewTouchCounter.setId(0);
					textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(i).getId())));
					textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
					textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
					textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
					
					textViewTouchableName.setText(((TouchableDAO)mTouchables.get(i)).getName());
					textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
					textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
					textViewTouchableName.setGravity(Gravity.CENTER);
					
					relativeLayoutCell = new RelativeLayout(this);
					relativeLayoutCell.setId((int) mTouchables.get(i).getId());
					relativeLayoutCell.setGravity(Gravity.CENTER);
					relativeLayoutCell.setBackgroundColor(mColorTouchable);
					relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
					relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
					
					relativeLayoutCell.setOnClickListener(this);

					tableRow.addView(relativeLayoutCell, tableRowLayoutParams);
					tableLayoutTouchables.addView(tableRow);
				}
			} else {
				/**
				 * Landscape:
				 * 	Number of rows:		1
				 * 	Number of columns:	3
				 */
				numberOfRows = 1;
				numberOfColumns = 3;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				
				tableRow = new TableRow(this);
				
				for(int i = 0; i < numberOfColumns; ++i) {
					tableRowLayoutParams = new TableRow.LayoutParams(
							TableRow.LayoutParams.WRAP_CONTENT,
							TableRow.LayoutParams.WRAP_CONTENT);
					tableRowLayoutParams.width = touchableWidth;
					tableRowLayoutParams.height = touchableHeight;
					
					if (i == 0)
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
					else if (i == 1)
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
					else
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
					
					textViewTouchCounter = new TextView(this);
					textViewTouchableName = new TextView(this);
					
					textViewTouchCounter.setId(0);
					textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(i).getId())));
					textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
					textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
					textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
					
					textViewTouchableName.setText(((TouchableDAO)mTouchables.get(i)).getName());
					textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
					textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
					textViewTouchableName.setGravity(Gravity.CENTER);
					
					relativeLayoutCell = new RelativeLayout(this);
					relativeLayoutCell.setId((int) mTouchables.get(i).getId());
					relativeLayoutCell.setGravity(Gravity.CENTER);
					relativeLayoutCell.setBackgroundColor(mColorTouchable);
					relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
					relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
					relativeLayoutCell.setOnClickListener(this);

					tableRow.addView(relativeLayoutCell, tableRowLayoutParams);
				}
				tableLayoutTouchables.addView(tableRow);	
			}
			break;
		}
		case 4:
		{
			/**
			 * Portrait & Landscape:
			 * 	Number of rows:		2
			 * 	Number of columns:	2
			 */
			numberOfRows = 2;
			numberOfColumns = 2;
			touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
			touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
			int indexTouchable = 0;
			for(int i = 0; i < numberOfRows; ++i) {
				tableRow = new TableRow(this);
				for(int j = 0; j < numberOfColumns; ++j) {
					tableRowLayoutParams = new TableRow.LayoutParams(
							TableRow.LayoutParams.WRAP_CONTENT,
							TableRow.LayoutParams.WRAP_CONTENT);
					tableRowLayoutParams.width = touchableWidth;
					tableRowLayoutParams.height = touchableHeight;
					
					indexTouchable = 2 * i + j;
					
					switch (indexTouchable) {
					case 0:
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
						break;
					case 1:
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
						break;
					case 2:
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
						break;
					case 3:
						tableRowLayoutParams.setMargins(
								TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
						break;
					}
						
					
					textViewTouchCounter = new TextView(this);
					textViewTouchableName = new TextView(this);
					
					textViewTouchCounter.setId(0);
					textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(indexTouchable).getId())));
					textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
					textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
					textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
					
					textViewTouchableName.setText(((TouchableDAO)mTouchables.get(indexTouchable)).getName());
					textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
					textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
					textViewTouchableName.setGravity(Gravity.CENTER);
					
					relativeLayoutCell = new RelativeLayout(this);
					relativeLayoutCell.setId((int) mTouchables.get(indexTouchable).getId());
					relativeLayoutCell.setGravity(Gravity.CENTER);
					relativeLayoutCell.setBackgroundColor(mColorTouchable);
					relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
					relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
					
					relativeLayoutCell.setOnClickListener(this);

					tableRow.addView(relativeLayoutCell, tableRowLayoutParams);	
				}
				tableLayoutTouchables.addView(tableRow);
			}
			break;
		}
		case 5:
		{
			if(isPortraitOrientation) {
				/**
				 * Portrait:
				 * 	Number of rows:		3
				 * 	Number of columns:	2
				 */
				numberOfRows = 3;
				numberOfColumns = 2;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				int indexTouchable = 0;
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					for(int j = 0; j < numberOfColumns; ++j) {
						tableRowLayoutParams = new TableRow.LayoutParams(
								TableRow.LayoutParams.WRAP_CONTENT,
								TableRow.LayoutParams.WRAP_CONTENT);
						tableRowLayoutParams.width = touchableWidth;
						tableRowLayoutParams.height = touchableHeight;
						
						indexTouchable = numberOfColumns * i + j;
						
						switch (indexTouchable) {
						case 0:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 1:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 2:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 3:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 4:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						}
							
						
						textViewTouchCounter = new TextView(this);
						textViewTouchableName = new TextView(this);
						
						textViewTouchCounter.setId(0);
						textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(indexTouchable).getId())));
						textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
						textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
						textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
						
						textViewTouchableName.setText(((TouchableDAO)mTouchables.get(indexTouchable)).getName());
						textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
						textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
						textViewTouchableName.setGravity(Gravity.CENTER);
						
						relativeLayoutCell = new RelativeLayout(this);
						relativeLayoutCell.setId((int) mTouchables.get(indexTouchable).getId());
						relativeLayoutCell.setGravity(Gravity.CENTER);
						relativeLayoutCell.setBackgroundColor(mColorTouchable);
						relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
						relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
						
						relativeLayoutCell.setOnClickListener(this);

						tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
						tableRow.addView(relativeLayoutCell, tableRowLayoutParams);	
						if(indexTouchable == 4)
							break;
					}
					tableLayoutTouchables.addView(tableRow);
				}
			} else {
				/**
				 * Landscape:
				 * 	Number of rows:		2
				 * 	Number of columns:	3
				 */
				numberOfRows = 2;
				numberOfColumns = 3;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				int indexTouchable = 0;
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					for(int j = 0; j < numberOfColumns; ++j) {
						tableRowLayoutParams = new TableRow.LayoutParams(
								TableRow.LayoutParams.WRAP_CONTENT,
								TableRow.LayoutParams.WRAP_CONTENT);
						tableRowLayoutParams.width = touchableWidth;
						tableRowLayoutParams.height = touchableHeight;
						
						indexTouchable = numberOfColumns * i + j;
						
						switch (indexTouchable) {
						case 0:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 1:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 2:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 3:
						case 4:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						}
							
						
						textViewTouchCounter = new TextView(this);
						textViewTouchableName = new TextView(this);
						
						textViewTouchCounter.setId(0);
						textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(indexTouchable).getId())));
						textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
						textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
						textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
						
						textViewTouchableName.setText(((TouchableDAO)mTouchables.get(indexTouchable)).getName());
						textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
						textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
						textViewTouchableName.setGravity(Gravity.CENTER);
						
						relativeLayoutCell = new RelativeLayout(this);
						relativeLayoutCell.setId((int) mTouchables.get(indexTouchable).getId());
						relativeLayoutCell.setGravity(Gravity.CENTER);
						relativeLayoutCell.setBackgroundColor(mColorTouchable);
						relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
						relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
						
						relativeLayoutCell.setOnClickListener(this);

						tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
						tableRow.addView(relativeLayoutCell, tableRowLayoutParams);
						if(indexTouchable == 4)
							break;
					}
					tableLayoutTouchables.addView(tableRow);
				}				
			}
			break;
		}
		case 6:
		{
			if(isPortraitOrientation) {
				/**
				 * Portrait:
				 * 	Number of rows:		3
				 * 	Number of columns:	2
				 */
				numberOfRows = 3;
				numberOfColumns = 2;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				int indexTouchable = 0;
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					for(int j = 0; j < numberOfColumns; ++j) {
						tableRowLayoutParams = new TableRow.LayoutParams(
								TableRow.LayoutParams.WRAP_CONTENT,
								TableRow.LayoutParams.WRAP_CONTENT);
						tableRowLayoutParams.width = touchableWidth;
						tableRowLayoutParams.height = touchableHeight;
						
						indexTouchable = numberOfColumns * i + j;
						
						switch (indexTouchable) {
						case 0:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 1:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 2:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 3:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 4:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						case 5:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						}
							
						
						textViewTouchCounter = new TextView(this);
						textViewTouchableName = new TextView(this);
						
						textViewTouchCounter.setId(0);
						textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(indexTouchable).getId())));
						textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
						textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
						textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
						
						textViewTouchableName.setText(((TouchableDAO)mTouchables.get(indexTouchable)).getName());
						textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
						textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
						textViewTouchableName.setGravity(Gravity.CENTER);
						
						relativeLayoutCell = new RelativeLayout(this);
						relativeLayoutCell.setId((int) mTouchables.get(indexTouchable).getId());
						relativeLayoutCell.setGravity(Gravity.CENTER);
						relativeLayoutCell.setBackgroundColor(mColorTouchable);
						relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
						relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
						
						relativeLayoutCell.setOnClickListener(this);

						tableRow.addView(relativeLayoutCell, tableRowLayoutParams);	
					}
					tableLayoutTouchables.addView(tableRow);
				}
			} else {
				/**
				 * Landscape:
				 * 	Number of rows:		2
				 * 	Number of columns:	3
				 */
				numberOfRows = 2;
				numberOfColumns = 3;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				int indexTouchable = 0;
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					for(int j = 0; j < numberOfColumns; ++j) {
						tableRowLayoutParams = new TableRow.LayoutParams(
								TableRow.LayoutParams.WRAP_CONTENT,
								TableRow.LayoutParams.WRAP_CONTENT);
						tableRowLayoutParams.width = touchableWidth;
						tableRowLayoutParams.height = touchableHeight;
						
						indexTouchable = numberOfColumns * i + j;
						
						switch (indexTouchable) {
						case 0:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 1:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 2:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 3:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						case 4:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						case 5:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						}
							
						
						textViewTouchCounter = new TextView(this);
						textViewTouchableName = new TextView(this);
						
						textViewTouchCounter.setId(0);
						textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(indexTouchable).getId())));
						textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
						textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
						textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
						
						textViewTouchableName.setText(((TouchableDAO)mTouchables.get(indexTouchable)).getName());
						textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
						textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
						textViewTouchableName.setGravity(Gravity.CENTER);
						
						relativeLayoutCell = new RelativeLayout(this);
						relativeLayoutCell.setId((int) mTouchables.get(indexTouchable).getId());
						relativeLayoutCell.setGravity(Gravity.CENTER);
						relativeLayoutCell.setBackgroundColor(mColorTouchable);
						relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
						relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
						
						relativeLayoutCell.setOnClickListener(this);

						tableRow.addView(relativeLayoutCell, tableRowLayoutParams);	
					}
					tableLayoutTouchables.addView(tableRow);
				}				
			}
			break;
		}
		case 7:
		{
			if(isPortraitOrientation == false) {
				/**
				 * Portrait:
				 * 	Number of rows:		4
				 * 	Number of columns:	2
				 */
				numberOfRows = 4;
				numberOfColumns = 2;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				int indexTouchable = 0;
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					for(int j = 0; j < numberOfColumns; ++j) {
						tableRowLayoutParams = new TableRow.LayoutParams(
								TableRow.LayoutParams.WRAP_CONTENT,
								TableRow.LayoutParams.WRAP_CONTENT);
						tableRowLayoutParams.width = touchableWidth;
						tableRowLayoutParams.height = touchableHeight;
						
						indexTouchable = numberOfColumns * i + j;
						
						switch (indexTouchable) {
						case 0:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 1:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 2:
						case 4:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 3:
						case 5:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 6:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						case 7:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						}
							
						
						textViewTouchCounter = new TextView(this);
						textViewTouchableName = new TextView(this);
						
						textViewTouchCounter.setId(0);
						textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(indexTouchable).getId())));
						textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
						textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
						textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
						
						textViewTouchableName.setText(((TouchableDAO)mTouchables.get(indexTouchable)).getName());
						textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
						textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
						textViewTouchableName.setGravity(Gravity.CENTER);
						
						relativeLayoutCell = new RelativeLayout(this);
						relativeLayoutCell.setId((int) mTouchables.get(indexTouchable).getId());
						relativeLayoutCell.setGravity(Gravity.CENTER);
						relativeLayoutCell.setBackgroundColor(mColorTouchable);
						relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
						relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
						
						relativeLayoutCell.setOnClickListener(this);

						tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
						tableRow.addView(relativeLayoutCell, tableRowLayoutParams);
						if(indexTouchable == 6)
							break;	
					}
					tableLayoutTouchables.addView(tableRow);
				}
			} else {
				/**
				 * Landscape:
				 * 	Number of rows:		2
				 * 	Number of columns:	4
				 */
				numberOfRows = 2;
				numberOfColumns = 4;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				int indexTouchable = 0;
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					for(int j = 0; j < numberOfColumns; ++j) {
						tableRowLayoutParams = new TableRow.LayoutParams(
								TableRow.LayoutParams.WRAP_CONTENT,
								TableRow.LayoutParams.WRAP_CONTENT);
						tableRowLayoutParams.width = touchableWidth;
						tableRowLayoutParams.height = touchableHeight;
						
						indexTouchable = numberOfColumns * i + j;
						
						switch (indexTouchable) {
						case 0:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 1:
						case 2:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 3:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 4:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						case 5:
						case 6:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						case 7:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						}
							
						
						textViewTouchCounter = new TextView(this);
						textViewTouchableName = new TextView(this);
						
						textViewTouchCounter.setId(0);
						textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(indexTouchable).getId())));
						textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
						textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
						textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
						
						textViewTouchableName.setText(((TouchableDAO)mTouchables.get(indexTouchable)).getName());
						textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
						textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
						textViewTouchableName.setGravity(Gravity.CENTER);
						
						relativeLayoutCell = new RelativeLayout(this);
						relativeLayoutCell.setId((int) mTouchables.get(indexTouchable).getId());
						relativeLayoutCell.setGravity(Gravity.CENTER);
						relativeLayoutCell.setBackgroundColor(mColorTouchable);
						relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
						relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
						
						relativeLayoutCell.setOnClickListener(this);

						tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
						tableRow.addView(relativeLayoutCell, tableRowLayoutParams);
						if(indexTouchable == 6)
							break;		
					}
					tableLayoutTouchables.addView(tableRow);
				}				
			}
			break;
		}
		case 8:
		{
			if(isPortraitOrientation) {
				/**
				 * Portrait:
				 * 	Number of rows:		4
				 * 	Number of columns:	2
				 */
				numberOfRows = 4;
				numberOfColumns = 2;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				int indexTouchable = 0;
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					for(int j = 0; j < numberOfColumns; ++j) {
						tableRowLayoutParams = new TableRow.LayoutParams(
								TableRow.LayoutParams.WRAP_CONTENT,
								TableRow.LayoutParams.WRAP_CONTENT);
						tableRowLayoutParams.width = touchableWidth;
						tableRowLayoutParams.height = touchableHeight;
						
						indexTouchable = numberOfColumns * i + j;
						
						switch (indexTouchable) {
						case 0:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 1:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 2:
						case 4:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 3:
						case 5:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 6:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						case 7:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						}
							
						
						textViewTouchCounter = new TextView(this);
						textViewTouchableName = new TextView(this);
						
						textViewTouchCounter.setId(0);
						textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(indexTouchable).getId())));
						textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
						textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
						textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
						
						textViewTouchableName.setText(((TouchableDAO)mTouchables.get(indexTouchable)).getName());
						textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
						textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
						textViewTouchableName.setGravity(Gravity.CENTER);
						
						relativeLayoutCell = new RelativeLayout(this);
						relativeLayoutCell.setId((int) mTouchables.get(indexTouchable).getId());
						relativeLayoutCell.setGravity(Gravity.CENTER);
						relativeLayoutCell.setBackgroundColor(mColorTouchable);
						relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
						relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
						
						relativeLayoutCell.setOnClickListener(this);

						tableRow.addView(relativeLayoutCell, tableRowLayoutParams);	
					}
					tableLayoutTouchables.addView(tableRow);
				}
			} else {
				/**
				 * Landscape:
				 * 	Number of rows:		2
				 * 	Number of columns:	4
				 */
				numberOfRows = 2;
				numberOfColumns = 4;
				touchableWidth = (tableLayoutTouchables.getWidth() - (numberOfColumns + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfColumns;
				touchableHeight = (tableLayoutTouchables.getHeight() - (numberOfRows + 1) * TOUCHABLES_LAYOUT_OUT_MARGIN) / numberOfRows;
				int indexTouchable = 0;
				for(int i = 0; i < numberOfRows; ++i) {
					tableRow = new TableRow(this);
					for(int j = 0; j < numberOfColumns; ++j) {
						tableRowLayoutParams = new TableRow.LayoutParams(
								TableRow.LayoutParams.WRAP_CONTENT,
								TableRow.LayoutParams.WRAP_CONTENT);
						tableRowLayoutParams.width = touchableWidth;
						tableRowLayoutParams.height = touchableHeight;
						
						indexTouchable = numberOfColumns * i + j;
						
						switch (indexTouchable) {
						case 0:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 1:
						case 2:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 3:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2);
							break;
						case 4:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						case 5:
						case 6:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						case 7:
							tableRowLayoutParams.setMargins(
									TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN / 2, TOUCHABLES_LAYOUT_OUT_MARGIN, TOUCHABLES_LAYOUT_OUT_MARGIN);
							break;
						}
							
						
						textViewTouchCounter = new TextView(this);
						textViewTouchableName = new TextView(this);
						
						textViewTouchCounter.setId(0);
						textViewTouchCounter.setText(String.valueOf(mSparseIntArrayTouchCounts.get((int) mTouchables.get(indexTouchable).getId())));
						textViewTouchCounter.setTextSize(touchableHeight / TOUCHABLES_COUNT_RATIO);
						textViewTouchCounter.setTypeface(textViewTouchCounter.getTypeface(), Typeface.BOLD);
						textViewTouchCounter.setGravity(Gravity.TOP | Gravity.LEFT);
						
						textViewTouchableName.setText(((TouchableDAO)mTouchables.get(indexTouchable)).getName());
						textViewTouchableName.setTextSize(touchableHeight / TOUCHABLES_NAME_RATIO);
						textViewTouchableName.setTypeface(textViewTouchableName.getTypeface(), Typeface.BOLD);
						textViewTouchableName.setGravity(Gravity.CENTER);
						
						relativeLayoutCell = new RelativeLayout(this);
						relativeLayoutCell.setId((int) mTouchables.get(indexTouchable).getId());
						relativeLayoutCell.setGravity(Gravity.CENTER);
						relativeLayoutCell.setBackgroundColor(mColorTouchable);
						relativeLayoutCell.addView(textViewTouchCounter, relativeLayoutCellLayoutParams);
						relativeLayoutCell.addView(textViewTouchableName, relativeLayoutCellLayoutParams);
						
						relativeLayoutCell.setOnClickListener(this);

						tableRow.addView(relativeLayoutCell, tableRowLayoutParams);	
					}
					tableLayoutTouchables.addView(tableRow);
				}				
			}
			break;
		}
		default:
			break;
		}
	}

	@Override
	public void onClick(View clickableView) {
		if(clickableView.getId() == R.id.imageButtonRecordToggle){
			if(mTimerStarted || mEvents.size() > 1) {
				mHandlerTimer.removeCallbacks(mRunnableTimerThread);
				//mHandlerTimer.removeMessages(MSG_TIMER_TICK);
				mTimerStarted = false;
				// TODO Change button image to "save" & open dialog box (dialog fragment) with save/edit options
				changeColorOnTouchables(mColorTouchableDisabled);
				mTimestampRecording = new Date();
				mCurrentSession.setName(new SimpleDateFormat("yyyy-MM-dd").format(mTimestampRecording));
				mCurrentSession.setDescription(new SimpleDateFormat("HH:mm:ss").format(mTimestampRecording));
				addEvent(TouchableDAO.TOUCHABLE_END_EVENT_ID, mCurrentTime - mPreviousTime);
				saveSessionAndEvents();
				showDialogSave();
			} else {
				if(mEvents.size() < 1) {
					if(mStartTime == 0) {
						mStartTime = SystemClock.elapsedRealtime();
						mCurrentTime = mPreviousTime = 0;						
					}

					// When start button is clicked, fire timer event with 1ms delay, no matter of MEASURE_MILLISECONDS value
					mHandlerTimer.postDelayed(mRunnableTimerThread, 1);
					//mHandlerTimer.sendEmptyMessageDelayed(MSG_TIMER_TICK, 1);
					mTimerStarted = true;
					changeColorOnTouchables(mColorTouchable);
					// TODO Change button image to "recording in progress" (toggling image each second)					
				}
			}
		} else {
			if(mTimerStarted) {
				Message keyDownMessage = new Message();
				keyDownMessage.what = MSG_TOUCH_KEY_DOWN_COLOR;
				keyDownMessage.obj = Integer.valueOf(clickableView.getId());
	        	mHandlerTouch.sendMessage(keyDownMessage);
				
				mSparseIntArrayTouchCounts.put(clickableView.getId(), mSparseIntArrayTouchCounts.get(clickableView.getId()) + 1);
				((TextView)(findViewById(clickableView.getId()).findViewById(0))).setText(String.valueOf(mSparseIntArrayTouchCounts.get(clickableView.getId())));
				addEvent(clickableView.getId(), mCurrentTime - mPreviousTime);
				mPreviousTime = mCurrentTime;
			} else {
				// TODO Buzz a user that buttons are disabled
			}
		}
	}
	
	private void saveSessionAndEvents() {
		for(EventRecorderDAO record : ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getDatabaseObjects(DeviceDAO.class)) {
			if(((DeviceDAO)record).getType() == DeviceDAO.DEVICE_TYPE_ANDROID) {
				mCurrentSession.setIdDevice(((DeviceDAO)record).getId());
				break;
			}
		}
		mCurrentSession.setIndexDeviceSession(0);
		mCurrentSession.setNumberOfEvents(mEvents.size());
		int numberOfTouchables = Integer.parseInt(
				PreferenceManager.getDefaultSharedPreferences(this).getString(EventRecorderSettingsActivity.KEY_PREF_EVENT_NUMBER, "3").split(" ")[0]);
		mCurrentSession.setNumberOfEventTypes(numberOfTouchables);
		mCurrentSession.setTimestampRecorded(mTimestampRecording);
		mCurrentSession.setTimestampUploaded(mTimestampRecording);
		
		// TODO Implement bulk add operation 
		((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().OnAdd(mCurrentSession);
		mCurrentSession.setId(((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getLastDatabaseObject(SessionDAO.class).getId());
		
		for(EventDAO event : mEvents) {
			event.setIdSession((int) mCurrentSession.getId());
			((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().OnAdd(event);
		}	
	}
	
	private void removeSessionAndEvents() {
		
		// TODO Implement bulk delete operation
		for(EventDAO event : mEvents) {
			event.setIdSession((int) mCurrentSession.getId());
			((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().OnDelete(event);
		}
		
		((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().OnDelete(mCurrentSession);
	}

	/**
	 * Displays Save/View dialog. 
	 * <b>NOTE:</b> Called from onClick method when recording stops, or on timeout.
	 */
	private void showDialogSave() {
		final Dialog dialogSave = new Dialog(this);
		dialogSave.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialogSave.setContentView(R.layout.dialog_save_new_session);
		dialogSave.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialogSave.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		
		dialogSave.findViewById(R.id.imageButtonSave).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d(TAG, "Save clicked!");
				Intent intentHome = new Intent(v.getContext(), EventRecorderHomeActivity.class);
				startActivity(intentHome);
				dialogSave.dismiss();
			}
		});

		dialogSave.findViewById(R.id.imageButtonDiscard).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d(TAG, "Discard clicked!");
				dialogSave.dismiss();
				mPreDiscardDialog.show(DiscardNewSessionAlertDialog.DIALOG_DISCARD);
			}
		});
		
		dialogSave.findViewById(R.id.imageButtonView).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d(TAG, "View clicked!");

				Bundle bundleSessionData = new Bundle();
				bundleSessionData.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_ID, mCurrentSession.getId());
				bundleSessionData.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_DEVICE_ID, mCurrentSession.getIdDevice());
				bundleSessionData.putString(EventRecorderSessionDetailFragment.ARG_SESSION_NAME, mCurrentSession.getName());
				bundleSessionData.putString(EventRecorderSessionDetailFragment.ARG_SESSION_DESCRIPTION, mCurrentSession.getDescription());
				bundleSessionData.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_INDEX_DEVICE_SESSION, mCurrentSession.getIndexDeviceSession());
				bundleSessionData.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_NUM_EVENTS, mCurrentSession.getNumberOfEvents());
				bundleSessionData.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_NUM_EVENT_TYPES, mCurrentSession.getNumberOfEventTypes());
				bundleSessionData.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_TIMESTAMP_REC, mCurrentSession.getTimestampRecorded().getTime());
				
				Intent intentViewSession = new Intent(v.getContext(), EventRecorderSessionDetailActivity.class);				
				intentViewSession.putExtra(EventRecorderSessionDetailFragment.ARG_SESSION_OBJ, bundleSessionData);
				startActivity(intentViewSession);				
				
				dialogSave.dismiss();
			}
		});
		
		dialogSave.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.d(TAG, "Dialog canceled");
			}
		});
		
		dialogSave.show();
	}

	/**
	 * Timer thread.
	 */
	private Runnable mRunnableTimerThread = new Runnable() {
	   public void run() {
		   synchronized (this) {
			   mCurrentTime = SystemClock.elapsedRealtime() - mStartTime;
			   mTextViewTimer.setText(getFormattedTime(mCurrentTime));
			   mHandlerTimer.postDelayed(mRunnableTimerThread, MEASURE_MILLISECONDS ? 1 : 1000);  
		   };
	   }
	};
	
	/**
	 * Layout thread.
	 */
	private Runnable mRunnableLayoutThread = new Runnable() {
	   public void run() {
		   Log.d(TAG, "Table layout widht = " + findViewById(R.id.tableLayoutTouchables).getWidth());
		   if(findViewById(R.id.tableLayoutTouchables).getWidth() == 0) {
			   mHandlerLayout.postDelayed(this, 100);
		   }
		   else {
			   mHandlerLayout.removeCallbacks(mRunnableLayoutThread);
			   displayTouchables();
			   if(!(mTimerStarted || mCurrentSession.getId() != EventRecorderDatabaseHandler.TABLE_INVALID_ID))
				   changeColorOnTouchables(mColorTouchableDisabled);
			   else if (mTimerStarted) {
				   mHandlerTimer.postDelayed(mRunnableTimerThread, 1);			
			   }
			   
		   }
	   }
	};
	
	/**
	 * Helper method used to format time into hh:mm:ss.SSS format.
	 * @param time Time in milLiseconds to be formatted.
	 * @return Formatted time.
	 */
	private String getFormattedTime(long time) {
	   long msecs = time % 1000;
	   long secs = (time / 1000) % 60;
	   long mins = (time / (1000 * 60)) % 60;
	   long hrs = time / (1000 * 60 * 60);	   
	   if(MEASURE_MILLISECONDS)
		   return String.format("%02d:%02d:%02d.%03d", hrs, mins, secs, msecs);
	   else
		   return String.format("%02d:%02d:%02d", hrs, mins, secs);
	}
	
	/**
	 * Changes color onto all touchable elements.
	 * @param color Color that is to be set on touchable elements.
	 */
	private void changeColorOnTouchables(int color) {
		/**
		 * Run through all views and find all of RelativeLayout types.
		 * For RelativeLayout elements, change color based upon given value.
		 */
		TableLayout tableLayoutTouchables = (TableLayout)findViewById(R.id.tableLayoutTouchables);
		for (EventRecorderDAO touchable : mTouchables) {
			View touchableView = tableLayoutTouchables.findViewById((int) touchable.getId());
			if(touchableView != null)
				touchableView.setBackgroundColor(color);
		}
	}
	
	/**
	 * Adds detected event to the list of events.
	 * @param idTouchable Identifier of touchable element that produced event.
	 * @param timeSample Current timer reading.
	 */
	private void addEvent(long idTouchable, long timeSample) {
		Log.d(TAG, String.format("Event (idTouchable/timestamp) = %d/%d", idTouchable, timeSample / 1000));
		EventDAO event = new EventDAO(null);
		event.setIdTouchable((int) idTouchable);
		event.setTimestamp(timeSample / 1000);
		mEvents.add(event);
	}
}
