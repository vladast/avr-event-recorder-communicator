/**
 * 
 */
package io.github.vladast.avrcommunicator.db.dao;

/**
 * @author vladimir.stankovic
 *
 */
public class TouchableDAO extends EventRecorderDAO {

	public static final int TOUCHABLE_END_EVENT_ID = 1000;
	public static final String TOUCHABLE_END_EVENT_NAME = "END";
	
	/**
	 * @param onDatabaseRequestListener
	 */
	public TouchableDAO(OnDatabaseRequestListener onDatabaseRequestListener) {
		super(onDatabaseRequestListener);
		// TODO Auto-generated constructor stub
	}

	/*
	create table touchable (
		    id integer primary key autoincrement not null, 
		    name text not null
		);
	*/
	
	/** Represents name of the touchable element */
	private String mName;
	
	/**
	 * Getter for touchable's name.
	 * @return Name of the touchable element.
	 */
	public String getName() {
		return mName;
	}
	
	/**
	 * Setter for touchable's name.
	 * @param name Name of the touchable element;
	 */
	public void setName(String name) {
		mName = name;
	}
}
