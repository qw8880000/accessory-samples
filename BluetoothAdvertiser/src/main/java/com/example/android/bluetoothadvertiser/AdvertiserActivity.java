package com.example.android.bluetoothadvertiser;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Dave Smith
 * Date: 11/12/14
 * AdvertiserActivity
 */
public class AdvertiserActivity extends Activity implements View.OnClickListener, DvpBluetooth.Listener {

    private static final String TAG = AdvertiserActivity.class.getSimpleName();

    /* Full Bluetooth UUID that defines the Health Thermometer Service */
    public static final ParcelUuid THERM_SERVICE = ParcelUuid.fromString("00001809-0000-1000-8000-00805f9b34fb");
    private static final int MANUFACTURER_ID_FOR_MAC = 1;   //

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private DvpBluetooth mDvpBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advertiser);

        Button button = (Button) findViewById(R.id.update);
        button.setOnClickListener(this);

        mDvpBluetooth = new DvpBluetooth(this, this);
        mDvpBluetooth.init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDvpBluetooth.deinit();
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
                //.addServiceData(THERM_SERVICE, buildTempPacket())
                .addManufacturerData(MANUFACTURER_ID_FOR_MAC, buildTempPacket())
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

        /*
         * get mac address
         */
        String address = mBluetoothAdapter.getAddress();
        if(address != null) {
            byte[] addressBytes = new byte[6];
            String[] addressParts = address.split(":");
            for(int i=0; i<6; i++){
                Integer hex = Integer.parseInt(addressParts[i], 16);
                addressBytes[i] = hex.byteValue();
            }

            Log.i(TAG, "address is:" + new String(addressBytes));
            return addressBytes;
        } else {
            return new byte[] {0x01,0x02};
        }
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
            Toast.makeText(AdvertiserActivity.this, "LE Advertise Failed:" + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

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

    @Override
    public void onBluetoothTurnedOn() {

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

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
        
        startAdvertising();
    }
}
