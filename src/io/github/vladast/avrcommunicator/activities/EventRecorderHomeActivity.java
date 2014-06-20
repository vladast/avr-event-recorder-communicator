package io.github.vladast.avrcommunicator.activities;

import io.github.vladast.avrcommunicator.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ImageView;
import io.github.vladast.avrcommunicator.EventRecorderApplication;
import io.github.vladast.avrcommunicator.db.dao.EventDAO;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;

/**
 * Event Recorder's Home activity
 */
public class EventRecorderHomeActivity extends Activity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Removing title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);		
		
		setContentView(R.layout.activity_home);
		
		updateHomeScreenData();
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
		boolean devicePresent = false;
		
		/**
		 * Read data from database
		 */
		numberOfRecordedSessions = ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getDatabaseObjectCount(SessionDAO.class);
		durationOfRecordedEvents = ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getDatabaseObjectValueCount(EventDAO.class, EventDAO.DB_COUNTABLE_COLUMN);
		SessionDAO lastSession = (SessionDAO) ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getLastDatabaseObject(SessionDAO.class);
		lastSessionName = lastSession.getName();
		lastSessionDescription = lastSession.getDescription();
		lastSessionCount = ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getDatabaseObjectCountByForeignId(EventDAO.class, lastSession);
		lastSessionDuration = ((EventRecorderApplication)getApplicationContext()).getDatabaseHandler().getDatabaseObjectValueCountByForeignId(EventDAO.class, EventDAO.DB_COUNTABLE_COLUMN, lastSession);

		/**
		 * Check if compatible USB device is attached.
		 */
		
		/**
		 * Update Home screen with read data
		 */
		setNumberOfRecordedSessions(numberOfRecordedSessions);
		setCummulativeDurationOfRecordings(durationOfRecordedEvents);
		setLastSessionProps(lastSessionName, lastSessionDescription, lastSessionCount, lastSessionDuration);
		setDeviceStatus(devicePresent);
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
}
