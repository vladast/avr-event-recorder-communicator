/**
 * 
 */
package io.github.vladast.avrcommunicator.db.dao;

import java.util.Date;

/**
 * @author vladimir.stankovic
 *
 */
public class SessionDAO extends EventRecorderDAO {

	/** Name of the root element representing session */
	public static final String XML_ROOT							= "session";
	/** Element name for session id */
	public static final String XML_FIELD_ID						= "id";
	/** Element name for device id */
	public static final String XML_FIELD_ID_DEVICE				= "idDevice";
	/** Element name for session index read from device */
	public static final String XML_FIELD_INDEX_DEV_SESSION		= "indexDeviceSession";
	/** Element name for session name */
	public static final String XML_FIELD_NAME					= "name";
	/** Element name for session description */
	public static final String XML_FIELD_DESCRIPTION			= "description";
	/** Element name for number of events */
	public static final String XML_FIELD_NUMBER_OF_EVENTS		= "numberOfEvents";
	/** Element name for number of different event types */
	public static final String XML_FIELD_NUMBER_OF_EVENT_TYPES	= "numberOfEventTypes";
	/** Element name for time of recording */
	public static final String XML_FIELD_TIMESTAMP_RECORDED		= "timestampRecorded";
	/** Element name for time of the upload */
	public static final String XML_FIELD_TIMESTAMP_UPLOADED		= "timestampUploaded";
	
	/**
	 * @param onDatabaseRequestListener
	 */
	public SessionDAO(OnDatabaseRequestListener onDatabaseRequestListener) {
		super(onDatabaseRequestListener);
		mTimestampUploaded = new Date(0L);
		mTimestampRecorded = new Date(0L);
	}

	/*
	create table session (
		    id integer primary key autoincrement not null, 
		    idDevice integer not null, 
		    name text not null, 
		    description text, 
		    numOfEvents integer not null, 
		    numOfEventTypes integer not null, 
		    indexDeviceSession integer, 
		    timestampUploaded datetime not null, 
		    timestampRecorded datetime not null,
		    foreign key (idDevice) references device(id)
		);
	*/
	
	private long mIdDevice;
	
	public long getIdDevice() {
		return mIdDevice;
	}
	
	public void setIdDevice(long idDevice) {
		mIdDevice = idDevice;
	}
	
	private String mName;
	
	public String getName() {
		return mName;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	private String mDescription;
	
	public String getDescription() {
		return mDescription;
	}
	
	public void setDescription(String description) {
		mDescription = description;
	}
	
	private int mNumberOfEvents;
	
	public int getNumberOfEvents() {
		return mNumberOfEvents;
	}
	
	public void setNumberOfEvents(int numberOfEvents) {
		mNumberOfEvents = numberOfEvents;
	}
	
	private int mNumberOfEventTypes;
	
	public int getNumberOfEventTypes() {
		return mNumberOfEventTypes;
	}
	
	public void setNumberOfEventTypes(int numberOfEventTypes) {
		mNumberOfEventTypes = numberOfEventTypes;
	}
	
	private int mIndexDeviceSession;
	
	public int getIndexDeviceSession() {
		return mIndexDeviceSession;
	}
	
	public void setIndexDeviceSession(int indexDeviceSession) {
		mIndexDeviceSession = indexDeviceSession;
	}
	
	private Date mTimestampUploaded;
	
	public Date getTimestampUploaded() {
		return mTimestampUploaded;
	}
	
	public void setTimestampUploaded(Date timestampUploaded) {
		mTimestampUploaded = timestampUploaded;
	}
	
	private Date mTimestampRecorded;
	
	public Date getTimestampRecorded() {
		return mTimestampRecorded;
	}
	
	public void setTimestampRecorded(java.util.Date mTimestampRecording) {
		mTimestampRecorded = mTimestampRecording;
	}
}
