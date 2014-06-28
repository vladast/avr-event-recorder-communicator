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
import io.github.vladast.avrcommunicator.db.dao.EventDAO;
import io.github.vladast.avrcommunicator.db.dao.EventRecorderDAO;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;
import io.github.vladast.avrcommunicator.db.dao.TouchableDAO;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.DateFormat;
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
	private static final int TOUCHABLES_LAYOUT_OUT_MARGIN	= 10;
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
	/** List of database objects designating available touchable elements */
	private ArrayList<EventRecorderDAO> mTouchables;
	/** Map of counts for each touchable element */
	private SparseIntArray mSparseIntArrayTouchCounts;
	/** Color of touchable element */
	// TODO Create a map between touchables and colors from settings; that can be also pulled out from db.
	private int mColorTouchable;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_session);
		
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
		
		mTextViewTimer = (TextView)findViewById(R.id.textViewTimer);
		mTextViewTimer.setText(MEASURE_MILLISECONDS ? "00:00:00.000" : "00:00:00");
		mTimerStarted = false;
		
		((ImageButton)findViewById(R.id.imageButtonRecordToggle)).setOnClickListener(this);
		
		long currentSessionCount = ((EventRecorderApplication)this.getApplicationContext()).getDatabaseHandler().getDatabaseObjectCount(SessionDAO.class) + 1;
		((TextView)findViewById(R.id.textViewSessionCount)).setText(String.valueOf(currentSessionCount));
		
		mTouchables = ((EventRecorderApplication)this.getApplicationContext()).getDatabaseHandler().getDatabaseObjects(TouchableDAO.class);
		
		/** Initialize map of counts */
		mSparseIntArrayTouchCounts = new SparseIntArray(mTouchables.size());
		for (EventRecorderDAO touchable : mTouchables) {
			mSparseIntArrayTouchCounts.put((int) touchable.getId(), 0);
		}
		
		mColorTouchable = 0xffdaeaba; // TODO Read this value from Settings
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume...");
		mHandlerLayout.postDelayed(mRunnableLayoutThread, LAYOUT_CHECK_INTERVAL);
	}
	
	/**
	 * Displays touchable elements dynamically.
	 */
	public void displayTouchables() {
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
		
		int numberOfTouchables = PreferenceManager.getDefaultSharedPreferences(this).getInt(EventRecorderSettingsActivity.KEY_PREF_EVENT_NUMBER, 3); 
		Log.d(TAG, String.format("Creating %d touchable elements...", numberOfTouchables));
		
		boolean isPortraitOrientation = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		
		TableLayout.LayoutParams tableLayoutParams = new TableLayout.LayoutParams(
				TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.MATCH_PARENT);

		TableRow.LayoutParams tableRowLayoutParams = null;
		
		RelativeLayout.LayoutParams relativeLayoutCellLayoutParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		
		TableLayout tableLayoutTouchables = (TableLayout)findViewById(R.id.tableLayoutTouchables);
		ArrayList<TableRow> tableRows = new ArrayList<TableRow>();
		TableRow tableRow = null;
		TextView textViewTouchCounter = null;
		TextView textViewTouchableName = null;
		RelativeLayout relativeLayoutCell = null;
		
		int touchableWidth = 0;
		int touchableHeight = 0;
		int numberOfRows = 0;
		int numberOfColumns = 0;
		
		// TODO Use value from database.
		numberOfTouchables = 4;
		switch (numberOfTouchables) {
		case 1:
		{
			/**
			 * Number of rows:		1
			 * Number of columns:	1
			 * No inbound margins
			 */
			
			touchableWidth = tableLayoutTouchables.getWidth() - 2 * TOUCHABLES_LAYOUT_OUT_MARGIN;
			touchableHeight = tableLayoutTouchables.getHeight() - 2 * TOUCHABLES_LAYOUT_OUT_MARGIN;
			
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
			textViewTouchCounter.setText("0");
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
				touchableWidth = tableLayoutTouchables.getWidth() - 2 * TOUCHABLES_LAYOUT_OUT_MARGIN;
				touchableHeight = (tableLayoutTouchables.getHeight() - 3 * TOUCHABLES_LAYOUT_OUT_MARGIN) / 2;
				
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
					textViewTouchCounter.setText("0");
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
				touchableWidth = (tableLayoutTouchables.getWidth() - 3 * TOUCHABLES_LAYOUT_OUT_MARGIN) / 2;
				touchableHeight = tableLayoutTouchables.getHeight() - 2 * TOUCHABLES_LAYOUT_OUT_MARGIN;
				
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
					textViewTouchCounter.setText("0");
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
				touchableWidth = tableLayoutTouchables.getWidth() - 2 * TOUCHABLES_LAYOUT_OUT_MARGIN;
				touchableHeight = (tableLayoutTouchables.getHeight() - 4 * TOUCHABLES_LAYOUT_OUT_MARGIN) / 3;
				
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
					textViewTouchCounter.setText("0");
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
				touchableWidth = (tableLayoutTouchables.getWidth() - 4 * TOUCHABLES_LAYOUT_OUT_MARGIN) / 3;
				touchableHeight = tableLayoutTouchables.getHeight() - 2 * TOUCHABLES_LAYOUT_OUT_MARGIN;
				
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
					textViewTouchCounter.setText("0");
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
			touchableWidth = (tableLayoutTouchables.getWidth() - 3 * TOUCHABLES_LAYOUT_OUT_MARGIN) / 2;
			touchableHeight = (tableLayoutTouchables.getHeight() - 3 * TOUCHABLES_LAYOUT_OUT_MARGIN) / 2;
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
					textViewTouchCounter.setText("0");
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
			touchableWidth = tableLayoutTouchables.getWidth() / 2;
			touchableHeight = tableLayoutTouchables.getHeight() / 3;
			break;
		case 6:
			touchableWidth = tableLayoutTouchables.getWidth() / 2;
			touchableHeight = tableLayoutTouchables.getHeight() / 3;
			break;
		}
	}

	@Override
	public void onClick(View clickableView) {
		if(clickableView.getId() == R.id.imageButtonRecordToggle){
			if(mTimerStarted) {
				mHandlerTimer.removeCallbacks(mRunnableTimerThread);
				//mHandlerTimer.removeMessages(MSG_TIMER_TICK);
				mTimerStarted = false;
				// TODO Change button image to "save" & open dialog box (dialog fragment) with save/edit options
			} else {
				mStartTime = SystemClock.elapsedRealtime();
				// When start button is clicked, fire timer event with 1ms delay, no matter of MEASURE_MILLISECONDS value
				mHandlerTimer.postDelayed(mRunnableTimerThread, 1);
				//mHandlerTimer.sendEmptyMessageDelayed(MSG_TIMER_TICK, 1);
				mTimerStarted = true;
				// TODO Change button image to "recording in progress" (toggling image each second)
			}
		} else {
			Message keyDownMessage = new Message();
			keyDownMessage.what = MSG_TOUCH_KEY_DOWN_COLOR;
			keyDownMessage.obj = Integer.valueOf(clickableView.getId());
        	mHandlerTouch.sendMessage(keyDownMessage);
			
			mSparseIntArrayTouchCounts.put(clickableView.getId(), mSparseIntArrayTouchCounts.get(clickableView.getId()) + 1);
			((TextView)(findViewById(clickableView.getId()).findViewById(0))).setText(String.valueOf(mSparseIntArrayTouchCounts.get(clickableView.getId())));
		}
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
}
