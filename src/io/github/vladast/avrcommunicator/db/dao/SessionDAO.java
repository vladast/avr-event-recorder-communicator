/**
 * 
 */
package io.github.vladast.avrcommunicator.db.dao;

import java.sql.Date;

/**
 * @author vladimir.stankovic
 *
 */
public class SessionDAO extends EventRecorderDAO {

	/**
	 * @param onDatabaseRequestListener
	 */
	public SessionDAO(OnDatabaseRequestListener onDatabaseRequestListener) {
		super(onDatabaseRequestListener);
		// TODO Auto-generated constructor stub
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
	
	private int mIdDevice;
	
	public int getIdDevice() {
		return mIdDevice;
	}
	
	public void setIdDevice(int idDevice) {
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
	
	public void setTimestampRecorded(Date timestampRecorded) {
		mTimestampRecorded = timestampRecorded;
	}
}
