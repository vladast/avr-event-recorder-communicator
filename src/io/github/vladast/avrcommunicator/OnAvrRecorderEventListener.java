package io.github.vladast.avrcommunicator;

import java.util.ArrayList;

public interface OnAvrRecorderEventListener {
	public void OnDeviceFound();
	public void OnDeviceConnected();
	public void OnDeviceSearching();
	public void OnDeviceReInitiated();
	public void OnRecordsRead(ArrayList<Reading> eventReadings);
	public void OnError(AvrRecorderErrors avrRecorderErrors);
	public void OnError(AvrRecorderErrors avrRecorderErrors, int data);
	public void OnReadingStarted();
	public void OnDebugMessage(String message);
}
