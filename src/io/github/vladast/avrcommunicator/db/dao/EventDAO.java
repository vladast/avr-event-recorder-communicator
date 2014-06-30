/**
 * 
 */
package io.github.vladast.avrcommunicator.db.dao;

/**
 * @author vladimir.stankovic
 *
 */
public class EventDAO extends EventRecorderDAO {

	public static final String DB_COUNTABLE_COLUMN = "timestamp";
	
	/**
	 * @param onDatabaseRequestListener
	 */
	public EventDAO(OnDatabaseRequestListener onDatabaseRequestListener) {
		super(onDatabaseRequestListener);
		// TODO Auto-generated constructor stub
	}

	/*
	create table event (
		    id integer primary key autoincrement not null, 
		    idSession integer not null, 
		    idTouchable integer not null, 
		    indexDeviceEvent integer, 
		    timestamp integer,
		    foreign key (idSession) references session(id), 
		    foreign key (idTouchable) references touchable(id)
		);
	*/
	
	private long mIdSession;
	
	public long getIdSession() {
		return mIdSession;
	}
	
	public void setIdSession(long idSession) {
		mIdSession = idSession;
	}
	
	private int mIdTouchable;
	
	public int getIdTouchable() {
		return mIdTouchable;
	}
	
	public void setIdTouchable(int idTouchable) {
		mIdTouchable = idTouchable;
	}
	
	private int mIndexDeviceEvent;
	
	public int getIndexDeviceEvent() {
		return mIndexDeviceEvent;
	}
	
	public void setIndexDeviceEvent(int indexDeviceEvent) {
		mIndexDeviceEvent = indexDeviceEvent;
	}
	
	private long mTimestamp;
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	public void setTimestamp(long timeSample) {
		mTimestamp = timeSample;
	}
}
