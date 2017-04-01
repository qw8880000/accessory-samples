package com.example.bluetoothscanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class DvpBluetoothManager extends BroadcastReceiver {

	private final String TAG = DvpBluetoothManager.class.getSimpleName();
	private Context mContext;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private Listener mListener;
	
	public DvpBluetoothManager(Context context, Listener listener) {

		mContext = context;
		mListener = listener;
		mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();

	}



	public void init() {
		if (!mBluetoothAdapter.isEnabled()) {
			/**
			 * This is an asynchronous call: it will return immediately, and
			 * clients should listen for {@link #ACTION_STATE_CHANGED}
			 * to be notified of if the bluetooth is turned on.
			 */
			mBluetoothAdapter.enable();
		}

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);					// Register for broadcasts when a device is discovered.
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);	// For devices discovery start event
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);	// For devices discovery stop event
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);		// For Bluetooth has been turned on or off.
		this.mContext.registerReceiver(this, filter);
	}

	public void deinit() {
		if (mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.disable();
		}

		// Don't forget to unregister the ACTION_FOUND receiver.
		this.mContext.unregisterReceiver(this);
	}

	/**
	 *  For BLE and classic bluetooth
	 */
	public void startScan() {
		if(!mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.startDiscovery();
		}
	}

	/**
	 *  For BLE and classic bluetooth
	 */
	public void stopScan() {
		if(mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (BluetoothDevice.ACTION_FOUND.equals(action)) {
			// Discovery has found a device. Get the BluetoothDevice
			// object and its info from the Intent.
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			int rssi = intent.getShortExtra( BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE );
			mListener.onDeviceFound(device, rssi);
		}
		else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
			mListener.onDiscoveryStarted();
		}
		else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
			mListener.onDiscoveryFinished();
		}
		else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
			int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
			//Log.d(TAG, "state="+state+" prestate="+prevState);

			if(prevState == BluetoothAdapter.STATE_TURNING_ON && state == BluetoothAdapter.STATE_ON) {
				mListener.onBluetoothTurnedOn();
			}

		}
	}

	/**
	 *  Just for BLE
	 * @param callback
	 */
	public void startLeScan(LeScanCallback callback) {
		mBluetoothAdapter.startLeScan(callback);
	}

	/**
	 *  Just for BLE
	 * @param callback
	 */
	public void stopLeScan(LeScanCallback callback) {
		mBluetoothAdapter.stopLeScan(callback);
	}

	public interface Listener {

		void onDeviceFound(BluetoothDevice device, int rssi);

		void onDiscoveryStarted();

		void onDiscoveryFinished();

		void onBluetoothTurnedOn();
	}
}