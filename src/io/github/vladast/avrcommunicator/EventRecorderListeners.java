/**
 * 
 */
package io.github.vladast.avrcommunicator;

import java.util.ArrayList;

/**
 * @author vladimir.stankovic
 *
 */
public class EventRecorderListeners implements OnAvrRecorderEventListener {

	/**
	 * List of listeners that are being attached to <code>Communicator</code> instance.s
	 */
	private ArrayList<OnAvrRecorderEventListener> mOnAvrRecorderEventListeners;

	/**
	 * <code>EventRecorderListeners</code> constructor.
	 * Instantiates list of event recorder listeners.
	 */
	public EventRecorderListeners() {
		mOnAvrRecorderEventListeners = new ArrayList<OnAvrRecorderEventListener>();
	}
	
	/**
	 * Adds new event listener to the list of event recorder listeners.
	 * @param onAvrRecorderEventListener Listener that is to be added to the list.
	 */
	public void registerEventRecorderListener(OnAvrRecorderEventListener onAvrRecorderEventListener) {
		mOnAvrRecorderEventListeners.add(onAvrRecorderEventListener);
	}
	
	/**
	 * Reads number of attached listeners.
	 * @return Size of the list of attached listeners.
	 */
	public int size() {
		return mOnAvrRecorderEventListeners.size();
	}
	
	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.OnAvrRecorderEventListener#OnDeviceFound()
	 */
	@Override
	public void OnDeviceFound() {
		for (OnAvrRecorderEventListener onAvrRecorderEventListener : mOnAvrRecorderEventListeners) {
			onAvrRecorderEventListener.OnDeviceFound();
		}
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.OnAvrRecorderEventListener#OnDeviceConnected()
	 */
	@Override
	public void OnDeviceConnected() {
		for (OnAvrRecorderEventListener onAvrRecorderEventListener : mOnAvrRecorderEventListeners) {
			onAvrRecorderEventListener.OnDeviceConnected();
		}
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.OnAvrRecorderEventListener#OnDeviceSearching()
	 */
	@Override
	public void OnDeviceSearching() {
		for (OnAvrRecorderEventListener onAvrRecorderEventListener : mOnAvrRecorderEventListeners) {
			onAvrRecorderEventListener.OnDeviceSearching();
		}
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.OnAvrRecorderEventListener#OnDeviceReInitiated()
	 */
	@Override
	public void OnDeviceReInitiated() {
		for (OnAvrRecorderEventListener onAvrRecorderEventListener : mOnAvrRecorderEventListeners) {
			onAvrRecorderEventListener.OnDeviceReInitiated();
		}
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.OnAvrRecorderEventListener#OnRecordsRead(java.util.ArrayList)
	 */
	@Override
	public void OnRecordsRead(ArrayList<Reading> eventReadings) {
		for (OnAvrRecorderEventListener onAvrRecorderEventListener : mOnAvrRecorderEventListeners) {
			onAvrRecorderEventListener.OnRecordsRead(eventReadings);
		}
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.OnAvrRecorderEventListener#OnError(io.github.vladast.avrcommunicator.AvrRecorderErrors)
	 */
	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors) {
		for (OnAvrRecorderEventListener onAvrRecorderEventListener : mOnAvrRecorderEventListeners) {
			onAvrRecorderEventListener.OnError(avrRecorderErrors);
		}
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.OnAvrRecorderEventListener#OnError(io.github.vladast.avrcommunicator.AvrRecorderErrors, int)
	 */
	@Override
	public void OnError(AvrRecorderErrors avrRecorderErrors, int data) {
		for (OnAvrRecorderEventListener onAvrRecorderEventListener : mOnAvrRecorderEventListeners) {
			onAvrRecorderEventListener.OnError(avrRecorderErrors, data);
		}
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.OnAvrRecorderEventListener#OnReadingStarted()
	 */
	@Override
	public void OnReadingStarted() {
		for (OnAvrRecorderEventListener onAvrRecorderEventListener : mOnAvrRecorderEventListeners) {
			onAvrRecorderEventListener.OnReadingStarted();
		}
	}

	/* (non-Javadoc)
	 * @see io.github.vladast.avrcommunicator.OnAvrRecorderEventListener#OnDebugMessage(java.lang.String)
	 */
	@Override
	public void OnDebugMessage(String message) {
		for (OnAvrRecorderEventListener onAvrRecorderEventListener : mOnAvrRecorderEventListeners) {
			onAvrRecorderEventListener.OnDebugMessage(message);
		}
	}

}
