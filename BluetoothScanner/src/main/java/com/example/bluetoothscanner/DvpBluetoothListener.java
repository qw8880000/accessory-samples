package com.example.bluetoothscanner;

import android.bluetooth.BluetoothDevice;

public interface DvpBluetoothListener {

	void onDeviceFound(BluetoothDevice device, int rssi);

	void onDiscoveryStarted();

	void onDiscoveryFinished();

	void onBluetoothTurnedOn();
}