/**
 * 
 */
package io.github.vladast.avrcommunicator.dialogs;

import io.github.vladast.avrcommunicator.db.dao.SessionDAO;

import java.util.Calendar;

import io.github.vladast.avrcommunicator.R;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

/**
 * @author vladimir.stankovic
 *
 */
public class EditSessionDataDialog extends Dialog {

	/** Denotes datetime when the recording of the session took place. */
	private Calendar mDateTimeRecorded;
	/** Context which created the dialog. */
	private Context mContext;
	/** Session that is being edited. */
	private SessionDAO mSession;
	/** Indicates whether session data were altered. */
	private boolean mDirty;
	/** Listener for "session data changed" events. */
	private OnEditSessionDataDialogListener onSessionDataChanged;
	
	/**
	 * @param context
	 * @hide
	 */
	private EditSessionDataDialog(Context context) {
		super(context);
	}

	public EditSessionDataDialog(Context context, SessionDAO session) {
		super(context);
		setContentView(R.layout.dialog_edit_session);
		mContext = context;
		mSession = session;
		
		setTitle(mContext.getResources().getString(R.string.edit_session_data_title));
		
		((EditText)findViewById(R.id.editTextSessionName)).setText(mSession.getName());
		((EditText)findViewById(R.id.editTextSessionDescription)).setText(mSession.getDescription());
		mDateTimeRecorded = Calendar.getInstance();
		mDateTimeRecorded.setTime(mSession.getTimestampRecorded());
		/**
		 * Initialize DatePicker
		 */
		((DatePicker)findViewById(R.id.datePickerRecordedOn)).init(
				mDateTimeRecorded.get(Calendar.YEAR), mDateTimeRecorded.get(Calendar.MONTH), mDateTimeRecorded.get(Calendar.DAY_OF_MONTH), 
				new OnDateChangedListener() {
			
			@Override
			public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				mDateTimeRecorded.set(year, monthOfYear, dayOfMonth);
			}
		});
		/**
		 * Initialize TimePicker
		 */
		((TimePicker)findViewById(R.id.timePickerRecordedOn)).setIs24HourView(true);
		((TimePicker)findViewById(R.id.timePickerRecordedOn)).setCurrentHour(mDateTimeRecorded.get(Calendar.HOUR_OF_DAY));
		((TimePicker)findViewById(R.id.timePickerRecordedOn)).setCurrentMinute(mDateTimeRecorded.get(Calendar.MINUTE));
		((TimePicker)findViewById(R.id.timePickerRecordedOn)).setOnTimeChangedListener(new OnTimeChangedListener() {
			
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				mDateTimeRecorded.set(mDateTimeRecorded.get(Calendar.YEAR), mDateTimeRecorded.get(Calendar.MONTH), mDateTimeRecorded.get(Calendar.DAY_OF_MONTH), 
						hourOfDay, minute);
			}
		});
		/**
		 * Initialize buttons.
		 */
		((Button)findViewById(R.id.buttonKeepChanges)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDirty = true;
				mSession.setName(((EditText)findViewById(R.id.editTextSessionName)).getText().toString());
				mSession.setDescription(((EditText)findViewById(R.id.editTextSessionDescription)).getText().toString());
				mSession.setTimestampRecorded(mDateTimeRecorded.getTime());
				onSessionDataChanged.OnSessionDataChanged(mSession);
				dismiss();
			}
		});
		((Button)findViewById(R.id.buttonDiscard)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDirty = false;
				dismiss();
			}
		});
	}
	
	/**
	 * @param context
	 * @param theme
	 * @hide
	 */
	private EditSessionDataDialog(Context context, int theme) {
		super(context, theme);
	}

	/**
	 * @param context
	 * @param cancelable
	 * @param cancelListener
	 * @hide
	 */
	private EditSessionDataDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	public void registerSessionDataListener(OnEditSessionDataDialogListener sessionDataListener) {
		onSessionDataChanged = sessionDataListener;
	}
	
	public boolean isDirty() {
		return mDirty;
	}
	
	public SessionDAO getSessionData() {
		return mSession;
	}
}
