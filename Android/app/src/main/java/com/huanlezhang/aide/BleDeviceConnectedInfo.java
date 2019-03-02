package com.huanlezhang.aide;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;


public class BleDeviceConnectedInfo {

    private BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mCharacteristic;

    public BleDeviceConnectedInfo(BluetoothDevice bluetoothDevice) {
        mDevice = bluetoothDevice;
        mGatt = null;
        mCharacteristic = null;
    }

    public BluetoothDevice getBleDevice() {
        return mDevice;
    }

    public void setBleGatt(BluetoothGatt gatt) {
        mGatt = gatt;
    }

    public BluetoothGatt getBleGatt() {
        return mGatt;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
    }

    public BluetoothGattCharacteristic getBleCharacteristic(){
        return mCharacteristic;
    }
}
