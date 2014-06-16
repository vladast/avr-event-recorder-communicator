/**
 * 
 */
package io.github.vladast.avrcommunicator;

import io.github.vladast.avrcommunicator.activities.EventRecorderSettingsActivity;
import io.github.vladast.avrcommunicator.activities.HomeScreenActivity;
import io.github.vladast.avrcommunicator.db.EventRecorderDatabaseHandler;

import java.io.IOException;
import java.io.StringWriter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;

/**
 * @author vladimir.stankovic
 * Singleton class representing customized <code>Application</code> object.
 */
public class EventRecorderApplication extends Application implements OnSharedPreferenceChangeListener  {
	private final String TAG = EventRecorderApplication.class.getSimpleName();
	
	/**
	 * Single instance of <code>EventRecorderApplication</code> object.
	 */
	private static EventRecorderApplication eventRecorderApplication;
	
	private EventRecorderDatabaseHandler eventRecorderDatabaseHandler;
	
	/**
	 * Instance of <code>Communicator</code> class that will be focal point for communication with USB device
	 */
	private Communicator mCommunicator;
	
	/**
	 * Device monitoring flag defined via <code>EventRecorderSettingsActivity</code> instance.
	 */
	private boolean mMonitorDevices;
	
	/**
	 * Monitoring interval (in seconds) defined via <code>EventRecorderSettingsActivity</code> instance.
	 */
	private int mMonitoringInterval;
	
	/**
	 * Sleep prevention flag defined via <code>EventRecorderSettingsActivity</code> instance.
	 */
	private boolean mPreventSleep;
	
	/**
	 * Getter for device monitoring flag.
	 * @return Device monitoring flag read from <code>EventRecorderSettingsActivity</code> instance.
	 */
	public boolean getMonitorDevices() {
		return mMonitorDevices;
	}
	
	/**
	 * Setter for device monitoring flag.
	 * <b>NOTE:</b> Should be called whenever Preference change occurs for device monitoring flag.
	 * @param monitorDevices Device monitoring flag read from <code>EventRecorderSettingsActivity</code> instance..
	 * @hide
	 */
	public void setMonitorDevices(boolean monitorDevices) {
		mMonitorDevices = monitorDevices;
		if(mMonitorDevices == true) {
			mCommunicator.startDeviceDetection();
		} else {
			mCommunicator.stopDeviceDetection();
		}
	}
	
	/**
	 * Getter for monitoring interval.
	 * @return Monitoring interval read from <code>EventRecorderSettingsActivity</code> instance.
	 */
	public int getMonitoringInterval() {
		return mMonitoringInterval;
	}
	
	/** 
	 * Setter for monitoring interval.
	 * <b>NOTE:</b> Should be called whenever Preference change occurs for monitoring interval.
	 * @param monitoringInterval Monitoring interval (in seconds) read from <code>EventRecorderSettingsActivity</code> instance.
	 * @hide
	 */
	public void setMonitoringInterval(int monitoringInterval) {
		mMonitoringInterval = (monitoringInterval > 0) ? monitoringInterval : AvrRecorderConstants.DEFAULT_PREF_MONITORING_INTERVAL;
		Log.d(TAG, "Monitoring interval set to " + mMonitoringInterval + " seconds.");
		mCommunicator.setMonitoringInterval(mMonitoringInterval);
	}
	
	/**
	 * Getter for sleep prevention flag.
	 * @return Sleep prevention flag read from <code>EventRecorderSettingsActivity</code> instance.
	 */
	public boolean getPreventSleep() {
		return mPreventSleep;
	}
	
	/**
	 * Setter for sleep prevention flag.
	 * <b>NOTE:</b> Should be called whenever Preference change occurs for monitoring interval.
	 * @param preventSleep
	 */
	public void setPreventSleep(boolean preventSleep) {
		mPreventSleep = preventSleep;
	}
	
	
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
		
		initCommunicator();
		
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		
		eventRecorderDatabaseHandler = new EventRecorderDatabaseHandler(this);
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "System memory is low!");
		Log.d(TAG, "Stopping device monitoring...");
		mCommunicator.stopDeviceDetection();
	}
	
	private void initCommunicator() {
		Log.d(TAG, "Creating Communicator instance...");
		mCommunicator = new Communicator((UsbManager)getSystemService(Context.USB_SERVICE));
		
		
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(EventRecorderSettingsActivity.KEY_PREF_MONITOR_DEVICES, true) == true) {
			mCommunicator.startDeviceDetection();
		} else {
			mCommunicator.stopDeviceDetection();
		}

		// TODO: Should be thread-safe
		mCommunicator.setMonitoringInterval(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(EventRecorderSettingsActivity.KEY_PREF_MONITOR_INTERVAL, "1").split(" ")[0]));
	}
	
	/**
	 * Called from Android activity when needed to capture events coming out from attached USB device.
	 * <b>NOTE:</b> Same as calling <code>registerListener</code> method from <code>Communicator</code> instance.
	 * @param onAvrRecorderEventListener Implemented interface from attached object/activity.
	 */
	public void registerCommunicatorListener(OnAvrRecorderEventListener onAvrRecorderEventListener) {
		mCommunicator.registerListener(onAvrRecorderEventListener);
	}
	
	/**
	 * Gets <code>Communicator</code> instance.
	 * @return Instance of <code>Communicator</code> class.
	 */
	public Communicator getCommunicator() {
		return mCommunicator;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(EventRecorderSettingsActivity.KEY_PREF_MONITOR_DEVICES)) {
        	mMonitorDevices = sharedPreferences.getBoolean(EventRecorderSettingsActivity.KEY_PREF_MONITOR_DEVICES, true);
        	if(mMonitorDevices) {
        		mCommunicator.startDeviceDetection();
        	} else {
        		mCommunicator.stopDeviceDetection();
        	}
        } else if (key.equals(EventRecorderSettingsActivity.KEY_PREF_MONITOR_INTERVAL)) {
        	mMonitoringInterval = sharedPreferences.getInt(EventRecorderSettingsActivity.KEY_PREF_MONITOR_INTERVAL, 
        			AvrRecorderConstants.DEFAULT_PREF_MONITORING_INTERVAL);
        	mCommunicator.setMonitoringInterval(mMonitoringInterval);
        } else if (key.equals(EventRecorderSettingsActivity.KEY_PREF_PREVENT_SLEEP)) {
            mPreventSleep = sharedPreferences.getBoolean(EventRecorderSettingsActivity.KEY_PREF_PREVENT_SLEEP, true);
            // TODO: Implement sleep prevention
        }
	}
}
