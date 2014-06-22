package io.github.vladast.avrcommunicator.activities;

import java.sql.Date;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.R.id;
import io.github.vladast.avrcommunicator.R.layout;
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
		/*if (mItem != null) {
			((TextView) rootView.findViewById(R.id.session_detail))
					.setText(mItem.content);
		}*/

		return rootView;
	}
}
