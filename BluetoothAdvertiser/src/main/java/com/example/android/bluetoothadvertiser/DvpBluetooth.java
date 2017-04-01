package com.example.android.bluetoothadvertiser;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import static android.content.Context.BLUETOOTH_SERVICE;


public class DvpBluetooth extends BroadcastReceiver {

    private final String TAG = DvpBluetooth.class.getSimpleName();
    private Context mContext;
    private Listener mListener;
    private BluetoothAdapter mBluetoothAdapter;

    public DvpBluetooth(Context context, Listener listener) {

        mContext = context;
        mListener = listener;

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        BluetoothManager manager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
    }

    public void init() {
        if (!mBluetoothAdapter.isEnabled()) {
            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivity(enableBtIntent);
            mBluetoothAdapter.enable();
        }

        // Register for broadcasts when Bluetooth turned on.
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(this, filter);
    }

    public void deinit() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }

        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
            //Log.d(TAG, "state="+state+" prestate="+prevState);
            if (prevState == BluetoothAdapter.STATE_TURNING_ON && state == BluetoothAdapter.STATE_ON) {
                mListener.onBluetoothTurnedOn();
            }
        }
    }

    public interface Listener {

        void onBluetoothTurnedOn();
    }
}
