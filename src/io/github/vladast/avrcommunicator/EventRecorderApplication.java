/**
 * 
 */
package io.github.vladast.avrcommunicator;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * @author vladimir.stankovic
 * Singleton class representing customized <code>Application</code> object.
 */
public class EventRecorderApplication extends Application {
	private final String TAG = EventRecorderApplication.class.getSimpleName();
	
	/**
	 * Single instance of <code>EventRecorderApplication</code> object.
	 */
	private static EventRecorderApplication eventRecorderApplication;
	
	/**
	 * Instance of <code>Communicator</code> class that will be focal point for communication with USB device
	 */
	private Communicator mCommunicator;
	
	/**
	 * Obtains single instance of <code>EventRecorderApplication</code> 
	 * @return Instance of <code>EventRecorderApplication</code>
	 */
	public EventRecorderApplication getInstance() {
		Log.d(TAG, "Method getInstance called.");
		return eventRecorderApplication;
	}
	
	/**
	 * Called when application is created.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Method onCreate called.");
		eventRecorderApplication = this;
		mCommunicator = new Communicator((UsbManager)getSystemService(Context.USB_SERVICE));
	}

	/**
	 * Called from Android activity when needed to capture events coming out from attached USB device.
	 * @param onAvrRecorderEventListener Implemented interface from attached object/activity.
	 */
	public void registerCommunicatorListener(OnAvrRecorderEventListener onAvrRecorderEventListener) {
		mCommunicator.registerListener(onAvrRecorderEventListener);
	}
}
