/**
 * 
 */
package io.github.vladast.avrcommunicator.db.dao;

/**
 * @author vladimir.stankovic
 *
 */
public class EventDAO extends EventRecorderDAO {

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
	
	private int mIdSession;
	
	public int getIdSession() {
		return mIdSession;
	}
	
	public void setIdSession(int idSession) {
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
	
	private int mTimestamp;
	
	public int getTimestamp() {
		return mTimestamp;
	}
	
	public void setTimestamp(int timestamp) {
		mTimestamp = timestamp;
	}
}
