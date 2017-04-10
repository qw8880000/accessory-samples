package com.example.bluetoothleunlock;

import android.os.ParcelUuid;
import android.util.Log;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by qw8880000 on 2017-04-07.
 */

public class UnlockProfile {

    private static final String TAG = UnlockProfile.class.getSimpleName();
    /* Full Bluetooth UUID that defines the Unlock Service */
    public static final ParcelUuid UNLOCK_SERVICE = ParcelUuid.fromString("00001809-0000-1000-8000-00805f9b34fb");

    public static final int MANUFACTURER_ID = 127;

    public static final byte MANUFACTURER_DATA_TYPE_MAC = 0x01;
    public static final byte MANUFACTURER_DATA_TYPE_UNLOCK_MODE = 0x02;

    public static final byte UNLOCK_MODE_AUTO = 0x01;       // 靠近开锁
    public static final byte UNLOCK_MODE_MANUAL = 0x02;     // 手动开锁

    public static final byte DATA_STRUCTURE_LENGTH_MAC = 0x07;
    public static final byte DATA_STRUCTURE_LENGTH_UNLOCK_MODE = 0x02;

    /**
     * 数据结构为 {length + data type + data} + {length + data type + data} + ...
     * 参考：http://www.race604.com/ble-advertising/
     * @param mac
     * @param unlockMode
     * @return
     */
    public static byte[] buildManufacturerData(String mac, byte unlockMode) {

        ByteBuffer buffer = ByteBuffer.allocate(256);
        byte[] address = new byte[6];

        buffer.clear();

        if(mac != null) {
            String[] addressParts = mac.split(":");
            for (int i = 0; i < 6; i++) {
                Integer hex = Integer.parseInt(addressParts[i], 16);
                address[i] = hex.byteValue();
            }

            buffer.put((byte)(address.length + 1));
            buffer.put(MANUFACTURER_DATA_TYPE_MAC);
            buffer.put(address);
        }

        buffer.put(DATA_STRUCTURE_LENGTH_UNLOCK_MODE);
        buffer.put(MANUFACTURER_DATA_TYPE_UNLOCK_MODE);
        buffer.put(unlockMode);

        buffer.flip();
        byte[] dataPacket = new byte[buffer.remaining()];
        buffer.get(dataPacket);

        Log.i(TAG, "buildManufacturerData:" + Arrays.toString(dataPacket));

        return dataPacket;
    }

}
