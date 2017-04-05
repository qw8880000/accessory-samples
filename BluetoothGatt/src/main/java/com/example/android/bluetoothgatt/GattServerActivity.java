package com.example.android.bluetoothgatt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Dave Smith
 * Date: 11/13/14
 * GattServerActivity
 */
public class GattServerActivity extends Activity implements DvpBluetooth.Listener, View.OnClickListener {
    private static final String TAG = "GattServerActivity";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;

    private ArrayList<BluetoothDevice> mConnectedDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<String> mDeviceList = new ArrayList<String>();
    private ArrayAdapter<String> mDevicesListAdapter;

    private DvpBluetooth dvpBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        mDevicesListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mDeviceList);
        ListView list = (ListView) findViewById(R.id.list);
        list.setAdapter(mDevicesListAdapter);

        Button button = (Button) findViewById(R.id.notify);
        button.setOnClickListener(this);

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        dvpBluetooth = new DvpBluetooth(this, this);
        dvpBluetooth.init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopAdvertising();
        deinitServer();

        dvpBluetooth.deinit();
    }

    /*
     * Create the GATT server instance, attaching all services and
     * characteristics that should be exposed
     */
    private void initServer() {

        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);

        BluetoothGattService service = new BluetoothGattService(DeviceProfile.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic temperatureCharacteristic =
                new BluetoothGattCharacteristic(DeviceProfile.CHARACTERISTIC_TEMPERATURE_UUID,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic numberCharacteristic =
                new BluetoothGattCharacteristic(DeviceProfile.CHARACTERISTIC_NUMBER_UUID,
                //Read+write permissions
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(temperatureCharacteristic);
        service.addCharacteristic(numberCharacteristic);

        mGattServer.addService(service);
    }

    private void deinitServer() {

        if (mGattServer == null) return;
        mGattServer.close();
    }

    /*
     * Callback handles all incoming requests from GATT clients.
     * From connections to read/write requests.
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.i(TAG, "onConnectionStateChange "
                    +DeviceProfile.getStatusDescription(status)+" "
                    +DeviceProfile.getStateDescription(newState));

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                postDeviceChange(device, true);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                postDeviceChange(device, false);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device,
                                                int requestId,
                                                int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.i(TAG, "onCharacteristicReadRequest " + characteristic.getUuid().toString());

            if (DeviceProfile.CHARACTERISTIC_TEMPERATURE_UUID.equals(characteristic.getUuid())) {
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        getTemperature());
            }

            if (DeviceProfile.CHARACTERISTIC_NUMBER_UUID.equals(characteristic.getUuid())) {
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        getNumber());
            }

            /*
             * Unless the characteristic supports WRITE_NO_RESPONSE,
             * always send a response back for any request.
             */
            mGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 final byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.i(TAG, "onCharacteristicWriteRequest "+characteristic.getUuid().toString());

            if (DeviceProfile.CHARACTERISTIC_NUMBER_UUID.equals(characteristic.getUuid())) {

                if (responseNeeded) {
                    mGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            value);
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setNumber(value);
                    }
                });
            }
        }
    };

    /*
     * Initialize the advertiser
     */
    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(DeviceProfile.SERVICE_UUID))
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    /*
     * Terminate the advertiser
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /*
     * Callback handles events from the framework describing
     * if we were successful in starting the advertisement requests.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Advertisement Started.");
            postStatusMessage("GATT Server Ready");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "Advertisement Failed: "+errorCode);
            postStatusMessage("GATT Server Error "+errorCode);
        }
    };

    private Handler mHandler = new Handler();
    private void postStatusMessage(final String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setTitle(message);
            }
        });
    }

    private void postDeviceChange(final BluetoothDevice device, final boolean toAdd) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String line;
                if(device.getName() != null){
                    line = device.getName() + "  " + device.getAddress();
                } else {
                    line = "Unknown Device" + "  " + device.getAddress();
                }
                //This will add the item to our list and update the adapter at the same time.
                if (toAdd) {
                    mDevicesListAdapter.add(line);
                    mConnectedDevices.add(device);
                } else {
                    mDevicesListAdapter.remove(line);
                    mConnectedDevices.remove(device);
                }

                mDevicesListAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onBluetoothTurnedOn() {
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /*
         * Check for advertising support. Not all devices are enabled to advertise
         * Bluetooth LE data.
         */
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "No Advertising Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        
        initServer();
        startAdvertising();
    }

    private byte[] getTemperature() {
        TextView textView  = (TextView) findViewById(R.id.temperature);
        int temperature = Integer.valueOf(textView.getText().toString());

        Log.d(TAG, "temperature is:" + temperature);

        return DeviceProfile.bytesFromInt(temperature);
    }

    private byte[] getNumber() {
        TextView textView  = (TextView) findViewById(R.id.number);
        int number = Integer.valueOf(textView.getText().toString());

        Log.d(TAG, "Number is:" + number);

        return DeviceProfile.bytesFromInt(number);
    }

    private void setNumber(byte[] value) {
        TextView textView  = (TextView) findViewById(R.id.number);
        int number = DeviceProfile.unsignedIntFromBytes(value);
        textView.setText(String.valueOf(number));
    }

    private void setNotify() {
        for (BluetoothDevice device : mConnectedDevices) {
            BluetoothGattCharacteristic readCharacteristic = mGattServer.getService(DeviceProfile.SERVICE_UUID)
                    .getCharacteristic(DeviceProfile.CHARACTERISTIC_TEMPERATURE_UUID);
            readCharacteristic.setValue(DeviceProfile.bytesFromInt(2048));
            mGattServer.notifyCharacteristicChanged(device, readCharacteristic, false);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.notify:
                setNotify();
                break;
        }
    }
}
