/**
 * 
 */
package io.github.vladast.avrcommunicator.db.dao;

import io.github.vladast.avrcommunicator.db.EventRecorderDatabaseHandler;

/**
 * @author vladimir.stankovic
 *
 */
public class EventRecorderDAO implements OnDatabaseRequestListener {

	/** Database record identifier */
	protected int mId;
	
	/**
	 * Getter for database record identifier.
	 * @return Database record identifier.
	 */
	public int getId() {
		return mId;
	}
	
	/**
	 * Instance of <code>OnDatabaseRequestListener</code> object that will listen for database requests.
	 */
	private OnDatabaseRequestListener mOnDatabaseRequestListener;
	
	/**
	 * Constructor
	 * @param onDatabaseRequestListener Object that implements <code>OnDatabaseRequestListener</code>.
	 */
	EventRecorderDAO(OnDatabaseRequestListener onDatabaseRequestListener) {
		mOnDatabaseRequestListener = onDatabaseRequestListener;	
		mId = EventRecorderDatabaseHandler.TABLE_INVALID_ID; /* Invalid number signaling that instance is not synced with database. */
	}
	
	/**
	 * Setter for database record identifier.
	 * @param id Database record identifier.
	 */
	public void setId(int id) {
		mId = id;
	}
	
	/**
	 * Getter for database instance.
	 * @return Database handler instance.
	 */
	private EventRecorderDatabaseHandler getDatabaseInstance() {
		return (EventRecorderDatabaseHandler)mOnDatabaseRequestListener;
	}
	
	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.db.dao.OnDatabaseRequestListener#OnAdd(java.lang.Object)
	 */
	@Override
	public void OnAdd(Object obj) {
		mOnDatabaseRequestListener.OnAdd(obj);
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.db.dao.OnDatabaseRequestListener#OnUpdate(java.lang.Object)
	 */
	@Override
	public void OnUpdate(Object obj) {
		mOnDatabaseRequestListener.OnUpdate(obj);
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.db.dao.OnDatabaseRequestListener#OnDelete(java.lang.Object)
	 */
	@Override
	public void OnDelete(Object obj) {
		mOnDatabaseRequestListener.OnDelete(obj);
	}

	/**
	 * Saves database object to the database.
	 * Should be called whenever object needs to be added or updated.
	 */
	public void save() {
		if(mId == EventRecorderDatabaseHandler.TABLE_INVALID_ID) {
			OnAdd(this);
		} else {
			OnUpdate(this);
		}
	}
	
	/**
	 * Deletes database object from database.
	 */
	public void delete() {
		OnDelete(this);
	}
}
