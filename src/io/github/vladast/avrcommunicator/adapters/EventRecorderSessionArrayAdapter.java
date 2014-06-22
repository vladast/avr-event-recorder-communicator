package io.github.vladast.avrcommunicator.adapters;

import java.util.ArrayList;
import java.util.List;

import io.github.vladast.avrcommunicator.R;
import io.github.vladast.avrcommunicator.db.dao.EventRecorderDAO;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class EventRecorderSessionArrayAdapter extends BaseAdapter {

	private final Context mContext;
	private final List<EventRecorderDAO> mSessions;
	
	/**
	 * Session's array adapter constructor
	 * @param context Android context.
	 * @param sessions List of session DAOs, each representing one list row.
	 */
	public EventRecorderSessionArrayAdapter(Context context, List<EventRecorderDAO> sessions) {
		super();
		mContext = context;
		mSessions = sessions;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewSessionListRow = inflater.inflate(R.layout.session_list_row, parent, false);
        TextView textViewSessionName = (TextView) viewSessionListRow.findViewById(R.id.textViewSessionListRowName);
        TextView textViewSessionDescription = (TextView) viewSessionListRow.findViewById(R.id.textViewSessionListRowDescription);
        
        textViewSessionName.setText(((SessionDAO)(mSessions.get(position))).getName());
        textViewSessionDescription.setText(((SessionDAO)(mSessions.get(position))).getDescription());

        return viewSessionListRow;
	}

	@Override
	public int getCount() {
		return mSessions.size();
	}

	@Override
	public Object getItem(int position) {
		return mSessions.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mSessions.get(position).getId();
	}
}
