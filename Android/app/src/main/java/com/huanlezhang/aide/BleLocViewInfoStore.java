package com.huanlezhang.aide;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class BleLocViewInfoStore {

    final static String TAG = "DTC BleLocViewInfoStore";

    private String mViewText; // text of the label view
    private Integer mViewId;  // id of the label view

    private String mDeviceAddr; // AIDE result

    // <device addr, rssi array>
    private HashMap<String, ArrayList<Integer> > mMeasureData = new HashMap<>();

    private long mMeasureStartTime = 0, mMeasureEndTime = 0;

    private boolean mIsNewStart = false;

    public BleLocViewInfoStore(String buttonText, Integer viewId) {
        mViewText = buttonText;
        mViewId = viewId;
    }

    public String getViewText() {
        return mViewText;
    }

    public Integer getViewId() {
        return mViewId;
    }

    public void addData(String deviceAddr, int rssi) {

        if (mIsNewStart) {
            mMeasureData.clear();
            mMeasureStartTime = System.currentTimeMillis();
            mIsNewStart = false;
        }

        if (!mMeasureData.containsKey(deviceAddr)) {
            mMeasureData.put(deviceAddr, new ArrayList<Integer>());
        }

        mMeasureEndTime = System.currentTimeMillis();
        mMeasureData.get(deviceAddr).add(rssi);
    }

    public void clearData() {
        mMeasureData.clear();
        mMeasureStartTime = mMeasureEndTime = System.currentTimeMillis();
        setNewStart();
    }

    public ArrayList<Double> calculateMean() {
        ArrayList<Double> ret = new ArrayList<>();

        SortedSet<String> keySet = new TreeSet<>(mMeasureData.keySet());

        for (String deviceAddr: keySet) {

            double meanValue = 0.0;

            ArrayList<Integer> keyValue = mMeasureData.get(deviceAddr);

            assert keyValue != null;

            double dataLength = keyValue.size();
            for (Integer dataPoint: keyValue) {
                meanValue += dataPoint / dataLength;
            }

            ret.add(meanValue);
        }

        return ret;
    }

    public int getMeasureTimeLength() {
        long duration = (mMeasureEndTime - mMeasureStartTime) / 1000;
        return (int) duration;
    }

    public void setNewStart() {
        mIsNewStart = true;
    }

    public void setDeviceAddr(String deviceAddr) {
        mDeviceAddr = deviceAddr;
    }

    public String getDeviceAddr() {
        return mDeviceAddr;
    }

    public int getDeviceNumber() {
        return mMeasureData.size();
    }

    public ArrayList<String> getSortedDeviceAddrArray(){

        ArrayList<String> ret = new ArrayList<>();

        SortedSet<String> keySet = new TreeSet<>(mMeasureData.keySet());
        for (String deviceAddr: keySet) {
            ret.add(deviceAddr);
        }

        return ret;
    }
}
