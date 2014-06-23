package io.github.vladast.avrcommunicator.activities;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.R.layout;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.os.Build;

public class EventRecorderNewSessionActivity extends Activity implements OnClickListener {

	private static final String TAG = EventRecorderNewSessionActivity.class.getSimpleName();
	
	private boolean mChronometerStarted;
	
	private TextView mTextViewTimer;
	private Handler mHandlerTimer;
	private long mStartTime;
	private long mCurrentTime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_session);
		
		mHandlerTimer = new Handler();
		
		mTextViewTimer = (TextView)findViewById(R.id.textViewTimer);
		
		((ImageButton)findViewById(R.id.imageButtonRecordToggle)).setOnClickListener(this);
		
		mChronometerStarted = false;
	}

	@Override
	public void onClick(View clickableView) {
		if(clickableView.getId() == R.id.imageButtonRecordToggle){
			if(mChronometerStarted) {
				mHandlerTimer.removeCallbacks(mRunnableTimerThread);
				mChronometerStarted = false;
				// TODO Change button image to "save" & open dialog box (dialog fragment) with save/edit options
			} else {
				mStartTime = SystemClock.elapsedRealtime();
				mHandlerTimer.postDelayed(mRunnableTimerThread, 1);
				mChronometerStarted = true;
				// TODO Change button image to "recording in progress" (toggling image each second)
			}
		} else {
			
		}
	}
	
   private Runnable mRunnableTimerThread = new Runnable() {
	   public void run() {
		   mCurrentTime = SystemClock.elapsedRealtime() - mStartTime;
		   mTextViewTimer.setText(getFormattedTime(mCurrentTime));
		   mHandlerTimer.postDelayed(this, 1);
	   }
   };
   
   private String getFormattedTime(long time) {
	   long msecs = time % 1000;
	   long secs = (time / 1000) % 60;
	   long mins = (time / (1000 * 60)) % 60;
	   long hrs = time / (1000 * 60 * 60);	   
	   return String.format("%02d:%02d:%02d.%03d", hrs, mins, secs, msecs);
   }
}
