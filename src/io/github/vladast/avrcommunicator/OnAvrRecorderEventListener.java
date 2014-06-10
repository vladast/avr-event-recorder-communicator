package io.github.vladast.avrcommunicator;

import java.util.ArrayList;

/**
 * Interface used to generate event notifications from attached USB device.
 * @author vladimir.stankovic
 *
 */
public interface OnAvrRecorderEventListener {
	/**
	 * Device has been detected.
	 */
	public void OnDeviceFound();
	/**
	 * Device is connected.
	 */
	public void OnDeviceConnected();
	/**
	 * Search for attached device(s) is in progress.
	 */
	public void OnDeviceSearching();
	/**
	 * Device has been re-initiated.
	 */
	public void OnDeviceReInitiated();
	/**
	 * Event records have been read from device.
	 * @param eventReadings Event records read from device.
	 */
	public void OnRecordsRead(ArrayList<Reading> eventReadings);
	/**
	 * Error handler.
	 * @param avrRecorderErrors Error descriptor.
	 */
	public void OnError(AvrRecorderErrors avrRecorderErrors);
	/**
	 * Error handler.
	 * @param avrRecorderErrors Error descriptor.
	 * @param data Additional data.
	 */
	public void OnError(AvrRecorderErrors avrRecorderErrors, int data);
	/**
	 * Event records reading has started.
	 */
	public void OnReadingStarted();
	/**
	 * Debug messages handler.
	 * @param message Debug message to be logged.
	 */
	public void OnDebugMessage(String message);
}
