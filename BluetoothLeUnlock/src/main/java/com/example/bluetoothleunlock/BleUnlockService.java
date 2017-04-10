package com.example.bluetoothleunlock;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import static com.example.bluetoothleunlock.UnlockProfile.buildManufacturerData;

/**
 * Created by qw8880000 on 2017-04-07.
 */

public class BleUnlockService extends IntentService implements DvpBluetooth.Listener{

    private final String TAG = BleUnlockService.class.getSimpleName();
    private boolean isRunning = true;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothAdapter mBluetoothAdapter;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public BleUnlockService() {
        super("BleUnlockService");
        Log.i(TAG, "start");
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "create");

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        if(!mBluetoothAdapter.isEnabled()) {
            DvpBluetooth.openBluetooth(this, this);
        } else {
            initBluetoothLeAdvertiser();
            startAdvertising();
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        stopAdvertising();

        super.onDestroy();

        Log.i(TAG, "destroy");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            while (isRunning){
                Thread.sleep(5000);
                Log.i(TAG, "running");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                .addServiceUuid(UnlockProfile.UNLOCK_SERVICE)
                .addManufacturerData(UnlockProfile.MANUFACTURER_ID, buildManufacturerData(mBluetoothAdapter.getAddress(), UnlockProfile.UNLOCK_MODE_AUTO))
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);

        mBluetoothLeAdvertiser = null;
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
            //Toast.makeText(BleUnlockService.this, "LE Advertise Failed:" + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private void initBluetoothLeAdvertiser() {
        /*
        * Check for Bluetooth LE Support.
        */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            return;
        }

        /*
         * Check for advertising support. Not all devices are enabled to advertise
         * Bluetooth LE data.
         */
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            //Toast.makeText(this, "No Advertising Support.", Toast.LENGTH_SHORT).show();
            return;
        }

        /*
         * we should invoke getBluetoothLeAdvertiser after bluetooth turned on.
         */
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }

    @Override
    public void onBluetoothTurnedOn() {
        initBluetoothLeAdvertiser();
        startAdvertising();
    }
}
