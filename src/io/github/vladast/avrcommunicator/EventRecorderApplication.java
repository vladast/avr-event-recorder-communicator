/**
 * 
 */
package io.github.vladast.avrcommunicator;

import android.app.Application;
import android.util.Log;

/**
 * @author vladimir.stankovic
 * Singleton class representing customized <code>Application</code> object.
 */
public class EventRecorderApplication extends Application {
	private final String TAG = EventRecorderApplication.class.getSimpleName();
	
	private static EventRecorderApplication singleton;
	
	/**
	 * Obtains single instance of <code>EventRecorderApplication</code> 
	 * @return Instance of <code>EventRecorderApplication</code>
	 */
	public EventRecorderApplication getInstance() {
		Log.d(TAG, "Method getInstance called.");
		return singleton;
	}
	
	/**
	 * Called when application is created.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Method onCreate called.");
		singleton = this;
	}

}
