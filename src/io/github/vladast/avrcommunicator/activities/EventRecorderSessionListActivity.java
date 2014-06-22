package io.github.vladast.avrcommunicator.activities;

import java.sql.Date;

import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.R.id;
import io.github.vladast.avrcommunicator.R.layout;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

/**
 * An activity representing a list of Sessions. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link EventRecorderSessionDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link EventRecorderSessionListFragment} and the item details (if present) is a
 * {@link EventRecorderSessionDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link EventRecorderSessionListFragment.Callbacks} interface to listen for item
 * selections.
 */
public class EventRecorderSessionListActivity extends FragmentActivity implements
		EventRecorderSessionListFragment.Callbacks {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_session_list);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		if (findViewById(R.id.session_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			((EventRecorderSessionListFragment) getSupportFragmentManager()
					.findFragmentById(R.id.session_list))
					.setActivateOnItemClick(true);
		}

		// TODO: If exposing deep links into your app, handle intents here.
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Callback method from {@link EventRecorderSessionListFragment.Callbacks} indicating
	 * that the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(SessionDAO session) {
		Bundle bundleSession = new Bundle();
		bundleSession.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_ID, session.getId());
		bundleSession.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_DEVICE_ID, session.getIdDevice());
		bundleSession.putString(EventRecorderSessionDetailFragment.ARG_SESSION_NAME, session.getName());
		bundleSession.putString(EventRecorderSessionDetailFragment.ARG_SESSION_DESCRIPTION, session.getDescription());
		bundleSession.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_INDEX_DEVICE_SESSION, session.getIndexDeviceSession());
		bundleSession.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_NUM_EVENTS, session.getNumberOfEvents());
		bundleSession.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_NUM_EVENT_TYPES, session.getNumberOfEventTypes());
		bundleSession.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_TIMESTAMP_REC, session.getTimestampRecorded().getTime());
		
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			EventRecorderSessionDetailFragment fragment = new EventRecorderSessionDetailFragment();
			fragment.setArguments(bundleSession);
			getSupportFragmentManager().beginTransaction().replace(R.id.session_detail_container, fragment).commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, EventRecorderSessionDetailActivity.class);
			detailIntent.putExtra(EventRecorderSessionDetailFragment.ARG_SESSION_ID, bundleSession);
			startActivity(detailIntent);
		}
	}
}
