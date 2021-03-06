package com.example.bluetoothleunlock;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import static com.example.bluetoothleunlock.UnlockProfile.buildManufacturerData;

public class BleUnlockActivity extends Activity implements View.OnClickListener, DvpBluetooth.Listener {

    private static final String TAG = BleUnlockActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_unlock);

        Button button = (Button) findViewById(R.id.open);
        button.setOnClickListener(this);

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        if(!mBluetoothAdapter.isEnabled()) {
            DvpBluetooth.openBluetooth(this, this);
        } else {
            initBluetoothLeAdvertiser();
        }

        stopService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startService();
    }

    private void stopService() {
        Intent startIntent = new Intent(this, BleUnlockService.class);
        this.stopService(startIntent);
    }

    private void startService() {
        Intent startIntent = new Intent(this, BleUnlockService.class);
        this.startService(startIntent);
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
                .addManufacturerData(UnlockProfile.MANUFACTURER_ID, buildManufacturerData(mBluetoothAdapter.getAddress(), UnlockProfile.UNLOCK_MODE_MANUAL))
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
            Toast.makeText(BleUnlockActivity.this, "LE Advertise Failed:" + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private void initBluetoothLeAdvertiser() {
        /*
        * Check for Bluetooth LE Support.
        */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
        }

        /*
         * Check for advertising support. Not all devices are enabled to advertise
         * Bluetooth LE data.
         */
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "No Advertising Support.", Toast.LENGTH_SHORT).show();
            finish();
        }

        /*
         * we should invoke getBluetoothLeAdvertiser after bluetooth turned on.
         */
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }

    private CountDownTimer timer = new CountDownTimer(2000, 1000) {

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            stopAdvertising();
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id) {
            case R.id.open:
                startAdvertising();
                timer.start();
                break;
            default:break;
        }
    }

    @Override
    public void onBluetoothTurnedOn() {
        initBluetoothLeAdvertiser();
    }
}
