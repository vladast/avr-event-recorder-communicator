package io.github.vladast.avrcommunicator.activities;

import java.util.ArrayList;
import java.util.Calendar;

import io.github.vladast.avrcommunicator.AvrRecorderErrors;
import io.github.vladast.avrcommunicator.OnAvrRecorderEventListener;
import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.Reading;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import io.github.vladast.avrcommunicator.EventRecorderApplication;
import io.github.vladast.avrcommunicator.db.dao.EventDAO;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;

/**
 * Event Recorder's Home activity
 */
public class EventRecorderHomeActivity extends Activity implements OnAvrRecorderEventListener {

	private static final String TAG = EventRecorderHomeActivity.class.getSimpleName();

	protected static final String NotificationManager = null;
	
	private OnClickListener mOnViewSessionsListener;
	private OnClickListener mOnSessionDetailsListener;
	private OnClickListener mOnHelpListener;

	private Bundle mBundleSession;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_home);
		updateHomeScreenData();
		
		mOnViewSessionsListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Navigate to View Sessions activity
				Log.d(TAG, "Clicked on view sessions listener.");
				startActivity(new Intent(v.getContext(), EventRecorderSessionListActivity.class));
			}
		};
		
		mOnSessionDetailsListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Navigate to Session Details activity
				// 1. To last session's details
				// 2. To device session's details
				Log.d(TAG, "Clicked on session details listener.");
				
				Intent detailIntent = new Intent(v.getContext(), EventRecorderSessionDetailActivity.class);
				detailIntent.putExtra(EventRecorderSessionDetailFragment.ARG_SESSION_OBJ, mBundleSession);
				startActivity(detailIntent);
			}
		};
		
		mOnHelpListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Navigate to Help activity
				// 1. From menu --> to general Help
				// 2. From device status --> to device specific Help
				Log.d(TAG, "Clicked on help listener.");
			}
		};
		
		findViewById(R.id.linearLayoutHomeNumOfRecordedSessions).setOnClickListener(mOnViewSessionsListener);
		findViewById(R.id.linearLayoutHomeNumOfRecordedEvents).setOnClickListener(mOnViewSessionsListener);
		findViewById(R.id.linearLayoutLastSession).setOnClickListener(mOnSessionDetailsListener);
		findViewById(R.id.relativeLayoutDeviceStatus).setOnClickListener(mOnHelpListener);
		
		findViewById(R.id.imageButtonNewSession).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Navigate to New Session activity
				Log.d(TAG, "Clicked on image button.");
				startActivity(new Intent(v.getContext(), EventRecorderNewSessionActivity.class));
			}
		});
		
		((EventRecorderApplication)getApplicationContext()).registerCommunicatorListener(this);
		
		getActionBar().setHomeButtonEnabled(true);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		updateHomeScreenData();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.action_home, menu);
		return true;
	} 
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
	    switch (menuItem.getItemId()) {
	    case R.id.action_settings:
	    	startActivity(new Intent(this, EventRecorderSettingsActivity.class));
	    	break;
	    case R.id.action_help:
	    	break;
	    case R.id.action_new_session:
	    	startActivity(new Intent(this, EventRecorderNewSessionActivity.class));
	    default:
	    	break;
	    }

	    return true;
	}
	
	/**
	 * Should be called whenever displayed data invalidation is pending
	 */
	protected void updateHomeScreenData() {
		// TODO Read data from database
		long numberOfRecordedSessions = 0;
		long durationOfRecordedEvents = 0;
		String lastSessionName = "";
		String lastSessionDescription = "";
		long lastSessionCount = 0;
		long lastSessionDuration = 0;
		
		/**
		 * Read data from database
		 */
		numberOfRecordedSessions = ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getDatabaseObjectCount(SessionDAO.class);
		durationOfRecordedEvents = ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getDatabaseObjectValueCount(EventDAO.class, EventDAO.DB_COUNTABLE_COLUMN);
		SessionDAO lastSession = null;
		if(numberOfRecordedSessions > 0) {
			lastSession = (SessionDAO) ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getLastDatabaseObject(SessionDAO.class);
			lastSessionCount = ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getDatabaseObjectCountByForeignId(EventDAO.class, lastSession);
			lastSessionDuration = ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getDatabaseObjectValueCountByForeignId(EventDAO.class, EventDAO.DB_COUNTABLE_COLUMN, lastSession);
		} else {
			lastSession = new SessionDAO(null);
			lastSession.setName("No stored sessions");
			lastSession.setDescription("CLick on + to create new session");
			lastSession.setTimestampRecorded(Calendar.getInstance().getTime());
			lastSession.setTimestampUploaded(Calendar.getInstance().getTime());
			lastSessionCount = lastSessionDuration = 0;
		}
		lastSessionName = lastSession.getName();
		lastSessionDescription = lastSession.getDescription();
		
		/**
		 * Save session's data into bundle - used to navigate to session details activity.
		 */
		mBundleSession = new Bundle();
		mBundleSession.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_ID, lastSession.getId());
		mBundleSession.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_DEVICE_ID, lastSession.getIdDevice());
		mBundleSession.putString(EventRecorderSessionDetailFragment.ARG_SESSION_NAME, lastSession.getName());
		mBundleSession.putString(EventRecorderSessionDetailFragment.ARG_SESSION_DESCRIPTION, lastSession.getDescription());
		mBundleSession.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_INDEX_DEVICE_SESSION, lastSession.getIndexDeviceSession());
		mBundleSession.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_NUM_EVENTS, lastSession.getNumberOfEvents());
		mBundleSession.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_NUM_EVENT_TYPES, lastSession.getNumberOfEventTypes());
		mBundleSession.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_TIMESTAMP_REC, lastSession.getTimestampRecorded().getTime());		
		
		/**
		 * Check if compatible USB device is attached.
		 */
		((EventRecorderApplication)getApplicationContext()).getCommunicator().startDeviceDetection();
		
		/**
		 * Update Home screen with read data
		 */
		setNumberOfRecordedSessions(numberOfRecordedSessions);
		setCummulativeDurationOfRecordings(durationOfRecordedEvents);
		setLastSessionProps(lastSessionName, lastSessionDescription, lastSessionCount, lastSessionDuration);
	}
	
	/**
	 * Used to update number of recorded sessions Home screen content 
	 * @param numberOfRecordedSessions Number of recorded sessions to be displayed on Home screen
	 */
	protected void setNumberOfRecordedSessions(long numberOfRecordedSessions) {
		((TextView)findViewById(R.id.textViewNumOfRecordedSessions)).setText(String.valueOf(numberOfRecordedSessions));
        String sessionCountText;
		if(numberOfRecordedSessions == 1) {
			sessionCountText = getResources().getString(R.string.home_recorded_session);
        } else {
			sessionCountText = getResources().getString(R.string.home_recorded_sessions);
        }
    	((TextView)findViewById(R.id.textViewSessionsRecorded)).setText(sessionCountText);
	}
	
	/**
	 * Used to update duration of all recorded events on Home screen
	 * @param duration Duration [sec] of all recorded events that is to be displayed
	 */
	protected void setCummulativeDurationOfRecordings(long duration) {
		((TextView)findViewById(R.id.textViewDurationOfRecordedEvents)).setText(String.valueOf(duration));
		
		String eventsRecorded;
		
		if(duration < 60) {
			// seconds
			eventsRecorded = getResources().getString(R.string.home_recorded_seconds);
		} else if (duration >= 60 && duration < 120) {
			// one minute
			eventsRecorded = getResources().getString(R.string.home_recorded_minute);
		} else if (duration >= 120 && duration < 3600) {
			// minutes
			eventsRecorded = getResources().getString(R.string.home_recorded_minutes);
		} else if (duration >= 3600 && duration < 2 * 3600) {
			// one hour
			eventsRecorded = getResources().getString(R.string.home_recorded_hour);
		} else {
			// hours
			eventsRecorded = getResources().getString(R.string.home_recorded_hours);
		}
		eventsRecorded += " " + getResources().getString(R.string.home_recorded_events);
		
		((TextView)findViewById(R.id.textViewEventsRecorded)).setText(eventsRecorded);	
	}
	
	/**
	 * Used to update last session's properties
	 * @param name Last session's name
	 * @param description Last session's description
	 * @param events Number of events recorded during last session
	 * @param duration Duration of events recorded during last session
	 */
	protected void setLastSessionProps(String name, String description, long events, long duration) {
		((TextView)findViewById(R.id.textViewLastSessionName)).setText(name);
		((TextView)findViewById(R.id.textViewLastSessionDescription)).setText(description);
		((TextView)findViewById(R.id.textViewLastSessionNumberOfEvents)).setText(String.valueOf(events) + 
				" " + getResources().getString(R.string.home_recorded_events_only));
		
		String eventsRecorded = String.valueOf(duration) + " ";
		
		if(duration < 60) {
			// seconds
			eventsRecorded += getResources().getString(R.string.home_recorded_seconds);
		} else if (duration >= 60 && duration < 120) {
			// one minute
			eventsRecorded += getResources().getString(R.string.home_recorded_minute);
		} else if (duration >= 120 && duration < 3600) {
			// minutes
			eventsRecorded += getResources().getString(R.string.home_recorded_minutes);
		} else if (duration >= 3600 && duration < 2 * 3600) {
			// one hour
			eventsRecorded += getResources().getString(R.string.home_recorded_hour);
		} else {
			// hours
			eventsRecorded += getResources().getString(R.string.home_recorded_hours);
		}		
		
		eventsRecorded += " " + getResources().getString(R.string.home_recorded_events);
		
		((TextView)findViewById(R.id.textViewLastSessionDurationOfEvents)).setText(eventsRecorded);
	}
	
	/**
	 * Used to update status of connected devices
	 * @param detected Whether compatible device is connected or not
	 */
	protected void setDeviceStatus(boolean detected) {
		// TODO Create drawables for attached/detached device display
		if(detected) {
			((ImageView)findViewById(R.id.imageViewDeviceStatus)).setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			((TextView)findViewById(R.id.textViewDeviceStatus)).setText(getResources().getString(R.string.home_device_attached));
		} else {
			((ImageView)findViewById(R.id.imageViewDeviceStatus)).setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			((TextView)findViewById(R.id.textViewDeviceStatus)).setText(getResources().getString(R.string.home_device_detached));		
		}
	}

	@Override
	public void OnDeviceFound() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnDeviceConnected() {
		setDeviceStatus(true);
		findViewById(R.id.relativeLayoutDeviceStatus).setOnClickListener(mOnSessionDetailsListener);
	}

	@Override
	public void OnDeviceSearching() {
		setDeviceStatus(false);
		findViewById(R.id.relativeLayoutDeviceStatus).setOnClickListener(mOnHelpListener);
	}

	@Override
	public void OnDeviceReInitiated() {
		setDeviceStatus(false);
	}

	@Override
	public void OnRecordsRead(ArrayList<Reading> eventReadings) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors, int data) {
		// TODO Auto-generated method stub
		
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
