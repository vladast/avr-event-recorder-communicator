/**
 * 
 */
package io.github.vladast.avrcommunicator;

import io.github.vladast.avrcommunicator.activities.EventRecorderSessionDetailActivity;
import io.github.vladast.avrcommunicator.activities.EventRecorderSessionDetailFragment;
import io.github.vladast.avrcommunicator.activities.EventRecorderSettingsActivity;
import io.github.vladast.avrcommunicator.activities.HomeScreenActivity;
import io.github.vladast.avrcommunicator.db.EventRecorderDatabaseHandler;
import io.github.vladast.avrcommunicator.db.dao.DeviceDAO;
import io.github.vladast.avrcommunicator.db.dao.EventDAO;
import io.github.vladast.avrcommunicator.db.dao.EventRecorderDAO;
import io.github.vladast.avrcommunicator.db.dao.SessionDAO;
import io.github.vladast.avrcommunicator.db.dao.TouchableDAO;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Xml;

/**
 * @author vladimir.stankovic
 * Singleton class representing customized <code>Application</code> object.
 */
public class EventRecorderApplication extends Application implements OnSharedPreferenceChangeListener, OnAvrRecorderEventListener {
	private final String TAG = EventRecorderApplication.class.getSimpleName();
	
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

	
	/**
	 * Single instance of <code>EventRecorderApplication</code> object.
	 */
	private static EventRecorderApplication eventRecorderApplication;
	
	private EventRecorderDatabaseHandler mEventRecorderDatabaseHandler;
	
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
	
	/** Intent used to request permissions for detected USB device. */
	private PendingIntent mUsbPermissionIntent;
	
	/** Broadcast receiver used to grant permissions for USB device. */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        if (ACTION_USB_PERMISSION.equals(action)) {
	            synchronized (this) {
	                UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	                    if(usbDevice != null){
	                    	Log.d(TAG, "permission granted for device " + usbDevice);
	                    	mCommunicator.useThisUsbDevice(usbDevice);
	                   }
	                } 
	                else {
	                    Log.d(TAG, "permission denied for device " + usbDevice);
	                    mCommunicator.startDeviceDetection();
	                }
	            }
	        }
	    }
	};	
	
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
		
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		
		mEventRecorderDatabaseHandler = new EventRecorderDatabaseHandler(this);
		mEventRecorderDatabaseHandler.getWritableDatabase().close(); // Just to ensure that database is created --> check if necessary!
		
		mUsbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);
		
		initCommunicator();		
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
		mCommunicator = new Communicator((UsbManager)getSystemService(Context.USB_SERVICE), mUsbPermissionIntent);
		mCommunicator.registerListener(this);
		
		
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

	/**
	 * Getter for <code>EventRecorderDatabaseHandler</code> instance.
	 * @return Database handler.
	 */
	public EventRecorderDatabaseHandler getDatabaseHandler() {
		return mEventRecorderDatabaseHandler;
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

	@Override
	public void OnDeviceFound() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnDeviceConnected() {

	}

	@Override
	public void OnDeviceSearching() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnDeviceReInitiated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnRecordsRead(ArrayList<Reading> eventReadings) {
		
		/**
		 * NOTE:
		 * 		Instead of notifying user on OnDeviceDetected event, we'll wait for all record to be read from device and then notify user about the device.
		 */
		
		/** 
		 * Read data from device 
		 * 	1. Check if detected device info is already in database --> based upon PID/VID pair
		 * 		a) If in database, get device name from database
		 * 		b) If not in database, send USB control message, asking for device description that hold device name
		 * 		   Save new data into database.
		 * 		c) In the case that device code (0xa002) is read from device, marking it as compatible, in the case that device name is not on the device,
		 * 		   set it to default value, "Compatible device". TODO Default device name should be configurable from Settings
		 * 	2. Prepare session data:
		 * 		mName: Upload date
		 * 		mDescription: "Read from device at HH:mm:ss"
		 * 		mNumberOfEvents: Number of events from device
		 * 		mIndexDeviceSession: Session index stored on device
		 * 		mNumberOfEventTypes: Number of event types used by particular device --> run through recordings, and determine how many different event types are detected
		 * 		mTimestampRecorded = mTimestampUploaded: Current datetime.
		 *  3. Prepare event data (for each one recorded):
		 *  	mIdSession: session ID of above created session
		 *  	mIdTouchable: index of touchable element --> TODO map between presets and touchables
		 * 		mIndexDeviceEvent: event index stored on device, which actually denotes index of device's memory slot holding the event timestamp
		 * 		mTimestamp: event timestamp (number of seconds after the reading started)
		 */
		
		mCommunicator.stopDeviceDetection();
		
		/** Check if detected device is in the database already.*/
		// TODO Create appropriate method within database handler
		DeviceDAO device = null;
		ArrayList<EventRecorderDAO> devices = mEventRecorderDatabaseHandler.getDatabaseObjects(DeviceDAO.class);
		for(EventRecorderDAO deviceDao : devices) {
			if(((DeviceDAO)deviceDao).getProductId() == mCommunicator.getDevice().getProductId() && 
					((DeviceDAO)deviceDao).getProductId() == mCommunicator.getDevice().getProductId()) {
				device = (DeviceDAO) deviceDao;
				break;
			}
		}
		
		/** If not already in the database, store device info */
		if(device == null) {
			device = new DeviceDAO(null);
			device.setCode(mCommunicator.getDevice().getDeviceCode());
			device.setDescription(mCommunicator.getDevice().getDeviceName().equals("") ? 
					String.format("%s [%d/%d]", getResources().getString(R.string.notification_device_detected_default_name), 
							mCommunicator.getDevice().getVendorId(), mCommunicator.getDevice().getProductId()) :
					mCommunicator.getDevice().getDeviceName());
			device.setProductId(mCommunicator.getDevice().getProductId());
			device.setVendorId(mCommunicator.getDevice().getVendorId());
			device.setType(DeviceDAO.DEVICE_TYPE_AVR);
			mEventRecorderDatabaseHandler.OnAdd(device);
			device.setId(mEventRecorderDatabaseHandler.getLastDatabaseObject(DeviceDAO.class).getId());
		}
		
		/** Prepare session data */
		SessionDAO session = new SessionDAO(null);
		Calendar calendar = Calendar.getInstance();
		ArrayList<Byte> eventTypes = new ArrayList<Byte>();
		session.setName((String) DateFormat.format("MM-dd-yyyy", calendar));
		session.setDescription(String.format("%s %s", 
				getResources().getString(R.string.notification_device_detected_session_description), 
				DateFormat.format("HH:mm:ss", calendar)));
		session.setNumberOfEvents(mCommunicator.getDevice().getEventReadings().size());
		// Determine number of different event types (aka number of touchables)
		for(Reading reading : mCommunicator.getDevice().getEventReadings()) {
			if(!eventTypes.contains(reading.getCode()))
				eventTypes.add(reading.getCode());
		}
		session.setNumberOfEventTypes(eventTypes.size());
		session.setIndexDeviceSession(mCommunicator.getDevice().getSession());
		session.setTimestampRecorded(calendar.getTime());
		session.setTimestampUploaded(calendar.getTime());
		session.setIdDevice(device.getId());
		mEventRecorderDatabaseHandler.OnAdd(session);
		session.setId(mEventRecorderDatabaseHandler.getLastDatabaseObject(SessionDAO.class).getId());
		
		/** Prepare recordings data */
		ArrayList<EventRecorderDAO> touchables = mEventRecorderDatabaseHandler.getDatabaseObjects(TouchableDAO.class);
		Collections.sort(eventTypes);
		for(Reading reading : mCommunicator.getDevice().getEventReadings()) {
			EventDAO event = new EventDAO(null);
			event.setIdSession(session.getId());
			event.setIdTouchable(touchables.get(eventTypes.indexOf(reading.getCode())).getId());
			event.setIndexDeviceEvent(reading.getEntry());
			event.setTimestamp(reading.getTimestamp());
			mEventRecorderDatabaseHandler.OnAdd(event);
		}
		
		/** Prepare indent extras */
		// TODO Following section should be extracted as helper method, since is being used on several places
		Bundle bundleSession = new Bundle();
		bundleSession.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_ID, session.getId());
		bundleSession.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_DEVICE_ID, session.getIdDevice());
		bundleSession.putString(EventRecorderSessionDetailFragment.ARG_SESSION_NAME, session.getName());
		bundleSession.putString(EventRecorderSessionDetailFragment.ARG_SESSION_DESCRIPTION, session.getDescription());
		bundleSession.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_INDEX_DEVICE_SESSION, session.getIndexDeviceSession());
		bundleSession.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_NUM_EVENTS, session.getNumberOfEvents());
		bundleSession.putInt(EventRecorderSessionDetailFragment.ARG_SESSION_NUM_EVENT_TYPES, session.getNumberOfEventTypes());
		bundleSession.putLong(EventRecorderSessionDetailFragment.ARG_SESSION_TIMESTAMP_REC, session.getTimestampRecorded().getTime());		
		
		Intent detailIntent = new Intent(this, EventRecorderSessionDetailActivity.class);
		detailIntent.putExtra(EventRecorderSessionDetailFragment.ARG_SESSION_OBJ, bundleSession);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, detailIntent, 0);
		
		/** Prepare notification data */
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
		notificationBuilder.setContentTitle(device.getDescription());
		notificationBuilder.setContentText(
				String.format("%d %s", session.getNumberOfEvents(), getResources().getString(R.string.notification_device_detected_content_text_fragment))); 
		notificationBuilder.setTicker(getResources().getString(R.string.notification_device_detected_ticker));
		notificationBuilder.setSmallIcon(R.drawable.ic_launcher); // TODO Use appropriate icon instead		
		notificationBuilder.setContentIntent(pendingIntent);
		
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, notificationBuilder.build());
		
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors, int data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnReadingStarted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnDebugMessage(String message) {
		Log.d(TAG, message);
	}
}
