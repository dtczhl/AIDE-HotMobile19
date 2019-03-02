package com.huanlezhang.aide;

public class BleDeviceInfoStore {

    public String name;
    public String address;
    public int rssi;
    public long timestamp;

    public BleDeviceInfoStore(String name, String address, int rssi, long timestamp){
        this.name = name;
        this.address = address;
        this.rssi = rssi;
        this.timestamp = timestamp;
    }
}
