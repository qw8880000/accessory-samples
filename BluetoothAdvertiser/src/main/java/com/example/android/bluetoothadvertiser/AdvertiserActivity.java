package com.example.android.bluetoothadvertiser;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Dave Smith
 * Date: 11/12/14
 * AdvertiserActivity
 */
public class AdvertiserActivity extends Activity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private static final String TAG = "AdvertiseActivity";
    private static final int DEFAULT_VALUE = 20;

    /* Full Bluetooth UUID that defines the Health Thermometer Service */
    public static final ParcelUuid THERM_SERVICE = ParcelUuid.fromString("00001809-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    /* UI to control advertise value */
    private TextView mCurrentValue;
    private SeekBar mSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advertiser);

        mCurrentValue = (TextView) findViewById(R.id.current);
        mSlider = (SeekBar) findViewById(R.id.slider);

        mSlider.setMax(100);
        mSlider.setOnSeekBarChangeListener(this);
        mSlider.setProgress(DEFAULT_VALUE);

        Button button = (Button) findViewById(R.id.update);
        button.setOnClickListener(this);

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
       // mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        openBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeBluetooth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAdvertising();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAdvertising();
    }

    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(THERM_SERVICE)
                .addServiceData(THERM_SERVICE, buildTempPacket())
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    private void restartAdvertising() {
        stopAdvertising();
        startAdvertising();
    }

    private byte[] buildTempPacket() {
        int value;
        try {
            value = Integer.parseInt(mCurrentValue.getText().toString());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new byte[] {(byte)value, 0x00};
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    /** Callbacks to update UI when slider changes */

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mCurrentValue.setText(String.valueOf(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id) {
            case R.id.update:
                restartAdvertising();
                break;
            default:break;
        }
    }

    private void openBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            //mBluetoothAdapter.enable();
        }

        // Register for broadcasts when Bluetooth turned on.
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }
    private void closeBluetooth() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }

        unregisterReceiver(mReceiver);
    }

    // Create a BroadcastReceiver for Bluetooth turned on.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                //Log.d(TAG, "state="+state+" prestate="+prevState);
                if(prevState == BluetoothAdapter.STATE_TURNING_ON && state == BluetoothAdapter.STATE_ON) {

                    /*
                     * Check for Bluetooth LE Support.
                     */
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                        Toast.makeText(context, "No LE Support.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    /*
                     * Check for advertising support. Not all devices are enabled to advertise
                     * Bluetooth LE data.
                     */
                    if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        Toast.makeText(context, "No Advertising Support.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                    startAdvertising();
                }
            }
        }
    };
}
