package com.huanlezhang.aide;

import java.util.UUID;

public interface BLE_UUID {

    UUID LED_SERVICE_UUID = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb");
    UUID LED_CHARACTERISTIC_UUID = UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb");

    byte[] LED_GREEN = {0x56, 0x00, 0x10, 0x00, 0x00, (byte)0xf0, (byte) 0xaa};
    byte[] LED_BLUE = {0x56, 0x00, 0x00, 0x10, 0x00, (byte)0xf0, (byte) 0xaa};
}
