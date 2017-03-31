package com.example.bluetoothscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.util.Log;

public class DvpBluetoothManager {

	private final String TAG = DvpBluetoothManager.class.getSimpleName();
	private Context context;
	private DvpBluetoothBroadcastReceiver mReceiver;
	private BluetoothManager bluetoothManager;
	private BluetoothAdapter bluetoothAdapter;
	
	public DvpBluetoothManager(Context context, DvpBluetoothListener listener) {
		this.context = context;
		this.mReceiver = new DvpBluetoothBroadcastReceiver(listener);
		
		bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();

	}

	public void init() {
		if (!bluetoothAdapter.isEnabled()) {
			bluetoothAdapter.enable();
		}

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);					// Register for broadcasts when a device is discovered.
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);	// For devices discovery start event
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);	// For devices discovery stop event
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);		// For Bluetooth has been turned on or off.
		this.context.registerReceiver(mReceiver, filter);
	}

	public void deinit() {
		if (bluetoothAdapter.isEnabled()) {
			bluetoothAdapter.disable();
		}

		// Don't forget to unregister the ACTION_FOUND receiver.
		this.context.unregisterReceiver(mReceiver);
	}

	/**
	 *  For BLE and classic bluetooth
	 */
	public void startScan() {
		if(!bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.startDiscovery();
		}
	}

	/**
	 *  For BLE and classic bluetooth
	 */
	public void stopScan() {
		if(bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.cancelDiscovery();
		}
	}
    
    private class DvpBluetoothBroadcastReceiver extends BroadcastReceiver {

    	private DvpBluetoothListener listener;
    	
    	public DvpBluetoothBroadcastReceiver(DvpBluetoothListener listener){
    		this.listener = listener;
    	}
    	
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
    	        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    	        int rssi = intent.getShortExtra( BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE );
    	        this.listener.onDeviceFound(device, rssi);
    		}
    		else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
    			this.listener.onDiscoveryStarted();
    		}
    		else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
    			this.listener.onDiscoveryFinished();
    		}
    		else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
				int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
				//Log.d(TAG, "state="+state+" prestate="+prevState);

				if(prevState == BluetoothAdapter.STATE_TURNING_ON && state == BluetoothAdapter.STATE_ON) {
					this.listener.onBluetoothTurnedOn();
				}

			}
    	}
    }

	/**
	 *  Just for BLE
	 * @param callback
	 */
	public void startLeScan(LeScanCallback callback) {
		bluetoothAdapter.startLeScan(callback);
	}

	/**
	 *  Just for BLE
	 * @param callback
	 */
	public void stopLeScan(LeScanCallback callback) {
		bluetoothAdapter.stopLeScan(callback);
	}

}