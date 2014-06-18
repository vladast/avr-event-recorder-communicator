/**
 * 
 */
package io.github.vladast.avrcommunicator.db.dao;

/**
 * @author vladimir.stankovic
 * Interface used by DAO object to communicate with <code>EventRecorderDatabaseHandler</code> instance.
 */
public interface OnDatabaseRequestListener {
	/**
	 * Fired when database object is to be added.
	 * @param obj Instance to be added.
	 * @return 
	 */
	public long OnAdd(Object obj);
	/**
	 * Fired when database object is to be updated.
	 * @param obj Instance to be updated.
	 */
	public void OnUpdate(Object obj);
	/**
	 * Fired when database object is to be deleted.
	 * @param obj Instance to be deleted.
	 */
	public void OnDelete(Object obj);
}
