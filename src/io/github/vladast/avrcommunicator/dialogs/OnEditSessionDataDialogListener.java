/**
 * 
 */
package io.github.vladast.avrcommunicator.dialogs;

import io.github.vladast.avrcommunicator.db.dao.SessionDAO;

/**
 * @author vladimir.stankovic
 * Interface used to notify details fragment about changes performed via edit session dialog
 */
public interface OnEditSessionDataDialogListener {
	/**
	 * Fired when session data gets changed via edit dialog
	 * @param sessionData Modified session data.
	 */
	public void OnSessionDataChanged(SessionDAO sessionData);
}
