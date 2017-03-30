package com.example.bluetoothscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class DvpBluetoothManager {
	
	private Context context;
	private DvpBluetoothBroadcastReceiver mReceiver;
	private BluetoothManager bluetoothManager;
	private BluetoothAdapter bluetoothAdapter;
	
	public DvpBluetoothManager(Context context, DvpBluetoothListener listener) {
		this.context = context;
		this.mReceiver = new DvpBluetoothBroadcastReceiver(listener);
		
		bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
		
		this.init();
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

	/**
	 *  For BLE and classic bluetooth
	 */
	public void startScan() {
	    // Register for broadcasts when a device is discovered.
	    IntentFilter filter = new IntentFilter();
	    filter.addAction(BluetoothDevice.ACTION_FOUND);
	    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
	    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	    this.context.registerReceiver(mReceiver, filter);
	    
	    bluetoothAdapter.startDiscovery();
	}

	/**
	 *  For BLE and classic bluetooth
	 */
	public void restartScan() {
		//bluetoothAdapter.cancelDiscovery();
		bluetoothAdapter.startDiscovery();
	}

	/**
	 *  For BLE and classic bluetooth
	 */
	public void stopScan() {
		
		bluetoothAdapter.cancelDiscovery();
		
	    // Don't forget to unregister the ACTION_FOUND receiver.
	    this.context.unregisterReceiver(mReceiver);
	}
	
	/**
	 * hardware initial
	 */
	private void init() {

		openBluetooth();
	}

	private void openBluetooth() {

		if (!bluetoothAdapter.isEnabled()) {
			bluetoothAdapter.enable();
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
    	}
    }

}