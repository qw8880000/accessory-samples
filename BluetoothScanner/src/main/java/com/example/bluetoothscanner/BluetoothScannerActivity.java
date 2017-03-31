package com.example.bluetoothscanner;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BluetoothScannerActivity extends Activity implements DvpBluetoothListener {

    private static final String TAG = BluetoothScannerActivity.class.getSimpleName();
    private List<BluetoothDevice> btDeviceList = new ArrayList<>();
    private DvpBluetoothManager dvpBluetoothManager;
    private DeviceListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dvpBluetoothManager = new DvpBluetoothManager(this, this);
        dvpBluetoothManager.init();

        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dvpBluetoothManager.deinit();
    }

    @Override
    public void onResume() {
        super.onResume();
        dvpBluetoothManager.startScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        dvpBluetoothManager.stopScan();
    }

    private void initViews() {
        adapter = new DeviceListAdapter(this, R.layout.scanner_list_item, btDeviceList);

        ListView listView = (ListView) findViewById(R.id.scanner_list);
        listView.setAdapter(adapter);
    }

    @Override
    public void onDeviceFound(final BluetoothDevice device, int rssi) {

        Log.d(TAG, device.getName() + ": " + device.getAddress() + ":" + rssi);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (!btDeviceList.contains(device)){
                    btDeviceList.add(device);
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onDiscoveryStarted() {

    }

    @Override
    public void onDiscoveryFinished() {

    }

    @Override
    public void onBluetoothTurnedOn() {
        dvpBluetoothManager.startScan();
    }

    private class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

        private int resourceId;

        private class ViewHolder {
            TextView deviceNameWidget;
            TextView deviceAddressWidget;
        }

        private DeviceListAdapter(Context context, int resource, List<BluetoothDevice> objects) {
            super(context, resource, objects);
            resourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view;
            ViewHolder viewHolder;

            if(convertView == null){
                view = LayoutInflater.from(getContext()).inflate(resourceId, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceNameWidget = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceAddressWidget = (TextView) view.findViewById(R.id.device_address);

                view.setTag(viewHolder);
            }else{
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice btDevice = getItem(position); // 获取当前项的实例
            if(btDevice != null) {
                String deviceName = btDevice.getName();
                if(deviceName == null){
                    deviceName = "Unknown device";
                }
                viewHolder.deviceNameWidget.setText(deviceName);
                viewHolder.deviceAddressWidget.setText(btDevice.getAddress());
            }

            return view;
        }
    }
}
