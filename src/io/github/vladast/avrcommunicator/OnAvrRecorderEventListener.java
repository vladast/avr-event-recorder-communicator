package io.github.vladast.avrcommunicator;

public interface OnAvrRecorderEventListener {
	public void OnDeviceFound();
	public void OnDeviceConnected();
	public void OnDeviceSearching();
	public void OnDeviceReInitiated();
	public void OnRecordsRead();
	public void OnError(AvrRecorderErrors avrRecorderErrors);
}
