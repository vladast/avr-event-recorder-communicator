package io.github.vladast.avrcommunicator.activities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import io.github.vladast.avrcommunicator.AvrRecorderConstants;
import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.R.layout;
import io.github.vladast.avrcommunicator.db.dao.EventDAO;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.TableLayout.LayoutParams;
import android.widget.ImageButton;
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
	/** Static member used for "timer tick" message */
	private static final int MSG_TIMER_TICK			= 0x0001;
	/** Static member used for "timer stop" message */
	private static final int MSG_TIMER_STOP			= 0x0002;
	/** Static member used for "check layout" message */
	private static final int MSG_CHECK_LAYOUT		= 0x0003;
	/** Static member used for "display touchables" message */
	private static final int MSG_DISPLAY_TOUCHABLES	= 0x0004;	
	
	/** A flag that indicates whether timer is started on not. */
	private boolean mTimerStarted;
	/** <code>TextView</code> element linked to time display. */
	private TextView mTextViewTimer;
	/** Timer handler used to start/stop timer thread. */
	private Handler mHandlerTimer;
	/** Layout handler used to start/stop layout thread. */
	private Handler mHandlerLayout;
	/** Start time in milliseconds. */
	private long mStartTime;
	/** Current time in milliseconds. */
	private long mCurrentTime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_session);
		
		mHandlerTimer = new Handler();
		mHandlerLayout = new Handler();
		
		mTextViewTimer = (TextView)findViewById(R.id.textViewTimer);
		mTextViewTimer.setText(MEASURE_MILLISECONDS ? "00:00:00.000" : "00:00:00");
		mTimerStarted = false;
		
		((ImageButton)findViewById(R.id.imageButtonRecordToggle)).setOnClickListener(this);
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
		
		TableLayout.LayoutParams tableLayoutParams = new TableLayout.LayoutParams(
				TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.MATCH_PARENT);

		TableRow.LayoutParams tableRowLayoutParams = new TableRow.LayoutParams(
				TableRow.LayoutParams.MATCH_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT);
		
		RelativeLayout.LayoutParams relativeLayoutCellLayoutParams;/* = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);*/
		
		// tableLayoutTouchables
		TableLayout tableLayoutTouchables = (TableLayout)findViewById(R.id.tableLayoutTouchables);
		int touchableWidth = 0;
		int touchableHeight = 0;
		
		// TODO dynamically calculate width/height based on the number of touchables instead of harcoding it.
		numberOfTouchables = 1;
		switch (numberOfTouchables) {
		case 1:
			touchableWidth = tableLayoutTouchables.getWidth();
			touchableHeight = tableLayoutTouchables.getHeight();
			
			TableRow tableRow = new TableRow(this);
			
			Button buttonCell = new Button(this);
			buttonCell.setWidth(touchableWidth - 10);
			buttonCell.setHeight(touchableHeight - 10);
			
			
			RelativeLayout relativeLayoutCell = new RelativeLayout(this);
			TextView textViewTouchCounter = new TextView(this);
			TextView textViewTouchableName = new TextView(this);
			
			textViewTouchCounter.setText("19");
			textViewTouchCounter.setGravity(Gravity.CENTER);
			
			textViewTouchableName.setText("Switch 01");
			textViewTouchableName.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
			
			/*
			relativeLayoutCellLayoutParams.width = touchableWidth;
			relativeLayoutCellLayoutParams.height = touchableHeight;
			
			relativeLayoutCell.addView(textViewTouchCounter);
			relativeLayoutCell.addView(textViewTouchableName);
			
			tableRow.addView(relativeLayoutCell, relativeLayoutCellLayoutParams);
			
			tableLayoutTouchables.addView(tableRow, tableRowLayoutParams);
			*/
			
			//relativeLayoutCellLayoutParams.width = touchableWidth;
			//relativeLayoutCellLayoutParams.height = touchableHeight;
			relativeLayoutCell.setBackgroundColor(0xffdaeaba);
			relativeLayoutCellLayoutParams = new RelativeLayout.LayoutParams(70, 90);
			relativeLayoutCellLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			relativeLayoutCell.setLayoutParams(relativeLayoutCellLayoutParams);
			relativeLayoutCell.addView(textViewTouchCounter);
			

			
			//relativeLayoutCell.addView(textViewTouchableName);
			
			//tableRow.addView(relativeLayoutCell/*, relativeLayoutCellLayoutParams*/);
			tableRow.addView(buttonCell);
			tableRow.setBackgroundColor(0xff00bada);
			
			tableLayoutTouchables.addView(tableRow, tableRowLayoutParams);
			
			break;
		case 2:
			touchableWidth = tableLayoutTouchables.getWidth();
			touchableHeight = tableLayoutTouchables.getHeight() / 2;
			break;
		case 3:
			touchableWidth = tableLayoutTouchables.getWidth();
			touchableHeight = tableLayoutTouchables.getHeight() / 3;
			break;
		case 4:
			touchableWidth = tableLayoutTouchables.getWidth() / 2;
			touchableHeight = tableLayoutTouchables.getHeight() / 2;
			break;
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
			// TODO Other clickable elements were clicked
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
