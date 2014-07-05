/**
 * 
 */
package io.github.vladast.avrcommunicator.dialogs;

import android.app.AlertDialog;
import android.content.Context;

/**
 * @author vladimir.stankovic
 * Customized <code>AlertDialog.Builder</code> which designates between methods that invoked the dialog.
 */
public class DiscardNewSessionAlertDialog extends AlertDialog.Builder {


	/** Static member used to signal that "navigate up" event was received */
	public static final int DIALOG_DISCARD_NAVIGATE_UP		= 0x0001;
	/** Static member used to signal that "back pressed" event was received */
	public static final int DIALOG_DISCARD_BACK				= 0x0002;	
	/** Static member used to signal that "discard" event was received */
	public static final int DIALOG_DISCARD					= 0x0003;
	/** Caller identifier - specific to Event Recorder's New Session activity. */
	private int mCaller;
	/** Context from which the dialog got created. */
	private Context mContext;
	
	/**
	 * @param context
	 */
	public DiscardNewSessionAlertDialog(Context context) {
		super(context);
		mCaller = 0;
		mContext = context;
	}

	/**
	 * @param context
	 * @param theme
	 */
	public DiscardNewSessionAlertDialog(Context context, int theme) {
		super(context, theme);	
		mCaller = 0;
	}
	
	/**
	 * Shows dialog.
	 * @param caller Identifier of the caller.
	 */
	public void show(int caller) {
		mCaller = caller;
		show();
	}

	/**
	 * Getter for the caller identifier.
	 * @return Caller identifier value.
	 */
	public int getCaller() {
		return mCaller;
	}
	
	/**
	 * Setter for the caller identifier.
	 * <b>NOTE:</b> Used to change behavior of the display.
	 * @param caller Caller identifier.
	 */
	public void SetCaller(int caller) {
		mCaller = caller;
	}
	
	/** 
	 * Getter for dialog's context.
	 * @return Context of the dialog.
	 */
	public Context getContext() {
		return mContext;
	}
}
