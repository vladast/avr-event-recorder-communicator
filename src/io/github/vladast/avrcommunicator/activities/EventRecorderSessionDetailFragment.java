package io.github.vladast.avrcommunicator.activities;

import java.sql.Date;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import io.github.vladast.avrcommunicator.EventRecorderApplication;
import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.db.dao.DeviceDAO;
import io.github.vladast.avrcommunicator.db.dao.EventDAO;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;

/**
 * A fragment representing a single Session detail screen. This fragment is
 * either contained in a {@link EventRecorderSessionListActivity} in two-pane mode (on
 * tablets) or a {@link EventRecorderSessionDetailActivity} on handsets.
 */
public class EventRecorderSessionDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the session ID.
	 * It's also used as fragment identifier.
	 */
	public static final String ARG_SESSION_ID = "session_id";
	/**
	 * The fragment argument representing ID of the device used to record particular session.
	 */
	public static final String ARG_SESSION_DEVICE_ID = "session_device_id";
	/**
	 * The fragment argument representing the session's name.
	 */
	public static final String ARG_SESSION_NAME = "session_name";
	/**
	 * The fragment argument representing the session's description.
	 */
	public static final String ARG_SESSION_DESCRIPTION = "session_description";
	/**
	 * The fragment argument representing the number of events recorded during particular session.
	 */
	public static final String ARG_SESSION_NUM_EVENTS = "session_num_of_events";
	/**
	 * The fragment argument representing the number of different events recorded during particular session.
	 */
	public static final String ARG_SESSION_NUM_EVENT_TYPES = "session_num_of_event_types";
	/**
	 * The fragment argument representing the index of the session recorded with external device.
	 * <b>NOTE:</b> Only relevant for external sessions, recorded with compatible devices.
	 */
	public static final String ARG_SESSION_INDEX_DEVICE_SESSION = "session_index_device_session";
	/**
	 * The fragment argument representing the time of the recording.
	 */
	public static final String ARG_SESSION_TIMESTAMP_REC = "session_timestamp_recorded";

	/**
	 * The dummy content this fragment is presenting.
	 */
	private SessionDAO mItem;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EventRecorderSessionDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_SESSION_ID)) {
			mItem = new SessionDAO(null);
			mItem.setId(getArguments().getLong(ARG_SESSION_ID));
			mItem.setIdDevice(getArguments().getLong(ARG_SESSION_DEVICE_ID));
			mItem.setName(getArguments().getString(ARG_SESSION_NAME));
			mItem.setDescription(getArguments().getString(ARG_SESSION_DESCRIPTION));
			mItem.setIndexDeviceSession(getArguments().getInt(ARG_SESSION_INDEX_DEVICE_SESSION));
			mItem.setNumberOfEvents(getArguments().getInt(ARG_SESSION_NUM_EVENTS));
			mItem.setNumberOfEventTypes(getArguments().getInt(ARG_SESSION_NUM_EVENT_TYPES));
			mItem.setTimestampRecorded(new Date(getArguments().getLong(ARG_SESSION_TIMESTAMP_REC)));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_session_detail,
				container, false);

		// Show the dummy content as text in a TextView.
		if (mItem != null) {
			// Set name and description directly from session DAO
			((TextView) rootView.findViewById(R.id.textViewSessionName)).setText(mItem.getName());
			((TextView) rootView.findViewById(R.id.textViewSessionDescription)).setText(mItem.getDescription());
			
			// Display message based on the number of events
			String numberOfEvents = String.valueOf(mItem.getNumberOfEvents()) + " " + getResources().getString(R.string.home_recorded_events_only);
			((TextView) rootView.findViewById(R.id.textViewSessionEventCount)).setText(numberOfEvents);
			
			// Display message based on recorded events duration
			long duration = ((EventRecorderApplication)getActivity().getApplicationContext()).getDatabaseHandler().getDatabaseObjectValueCountByForeignId(EventDAO.class, EventDAO.DB_COUNTABLE_COLUMN, mItem);
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
			((TextView) rootView.findViewById(R.id.textViewSessionEventsDuration)).setText(eventsRecorded);
			
			// Display date
			((TextView) rootView.findViewById(R.id.textViewSessionRecordingDate)).setText(DateFormat.format("MM-DD-YYYY", mItem.getTimestampRecorded()));
			
			int deviceType = ((DeviceDAO)((EventRecorderApplication)getActivity().getApplicationContext()).getDatabaseHandler().getDatabaseObjectById(DeviceDAO.class, mItem.getIdDevice())).getType();
			if(deviceType == DeviceDAO.DEVICE_TYPE_AVR) {
				// TODO Display USB icon
				((ImageView)rootView.findViewById(R.id.imageViewDevice)).setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			} else {
				// TODO Display Android icon
				((ImageView)rootView.findViewById(R.id.imageViewDevice)).setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			}
			
			// Set click listeners for Edit&Save buttons
			rootView.findViewById(R.id.imageButtonSave).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Save mItem details to database and display Toast of the final report
				}
			});
			
			rootView.findViewById(R.id.imageButtonEdit).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Open Edit dialog fragment --> editing mItem object
				}
			});
		}

		return rootView;
	}
}
