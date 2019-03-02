package com.huanlezhang.aide;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public class MyBle {

    private static final String TAG = "DTC MyBle";

    private Context mContext;

    private Handler mHandler = new Handler();
    private Timer mTimer;

    private RecyclerView mScanView;
    private RecyclerViewAdapter mScanViewAdapter;
    private RecyclerView.LayoutManager mScanViewLayoutManager;

    private ConstraintLayout mBleMainLayout;
    private Button mBleCloseBtn;

    // main panel
    private Button mBleBtn;
    private ToggleButton mCaptureToggleBtn;
    private ToggleButton mMeasureToggleBtn;
    private TextView mMeasureLengthTextView;
    private Button mConfirmButton;
    private ToggleButton mOnOffToggleBtn;

    // <Device Addr, position in the layout view>
    private HashMap<String, Integer> mDeviceToViewPositionMap = new HashMap<>();

    private HashMap<String, BleDeviceInfoStore> mScanResultMap = new HashMap<>();
    private ArrayList<BleDeviceInfoStore> mScanResultList = new ArrayList<>();

    // <location label, data measurement at that label>
    private HashMap<String, BleLocViewInfoStore> mLocDataMap;

    // <device addr, ble connection information>
    private HashMap<String, BleDeviceConnectedInfo> mDeviceConnectInfoMap = new HashMap<>();

    // aide algorithm finished
    private boolean mIsConfirmDone = false;
    // final result: <location label, device addr>
    private HashMap<String, String> mLocDeviceMap;

    private boolean mIsBleConfigured = false;
    private boolean mIsBleOK = false;
    private HashSet<String> mDeviceSelectedSet = new HashSet<>();
    private String mLocKey; // location label

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothScanner;
    private ScanCallback mBleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                BluetoothDevice bluetoothDevice = result.getDevice();
                String deviceName = bluetoothDevice.getName();

                if (deviceName == null) {
                    // devices without name, return immediately
                    return;
                }

                String deviceAddress = bluetoothDevice.getAddress();
                int rssi = result.getRssi();
                Long timestamp = System.currentTimeMillis();

                BleDeviceInfoStore bleDeviceInfoStore = new BleDeviceInfoStore(deviceName, deviceAddress, rssi, timestamp);

                int position = mScanViewAdapter.update(bleDeviceInfoStore);
                mDeviceToViewPositionMap.put(deviceAddress, position);

                // not a very good place to do it
                if (mIsBleConfigured) {
                    if (mScanViewAdapter.mCheckBoxArray.get(position, false)) {
                        mDeviceSelectedSet.add(deviceAddress);
                    } else {
                        mDeviceSelectedSet.remove(deviceAddress);
                    }
                }

                if (mIsBleOK) {
                    if (mDeviceSelectedSet.contains(deviceAddress)) {
                        mLocDataMap.get(mLocKey).addData(deviceAddress, rssi);

                        if (!mDeviceConnectInfoMap.containsKey(deviceAddress)) {
                            mDeviceConnectInfoMap.put(deviceAddress, new BleDeviceConnectedInfo(bluetoothDevice));
                        }
                    }
                }
            }
        }
    };

    private final BluetoothGattCallback mBleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, gatt.getDevice().getAddress() + " connected");

                mDeviceConnectInfoMap.get(gatt.getDevice().getAddress()).setBleGatt(gatt);
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, gatt.getDevice().getAddress() + " disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            try {
                mDeviceConnectInfoMap.get(gatt.getDevice().getAddress()).setCharacteristic(
                        gatt.getService(BLE_UUID.LED_SERVICE_UUID).getCharacteristic(BLE_UUID.LED_CHARACTERISTIC_UUID));
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    public MyBle(Context context, HashMap<String, BleLocViewInfoStore> locViewMap) {
        mContext = context;
        mLocDataMap = locViewMap;

        Activity activity = (Activity) mContext;

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            activity.finish();
        }
    }

    public void showBleLayout() {

        final Activity activity = (Activity) mContext;

        for (BleLocViewInfoStore bleLocViewInfoStore: mLocDataMap.values()) {
            Integer viewId = bleLocViewInfoStore.getViewId();
            Button button = activity.findViewById(viewId);
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);

            bleLocViewInfoStore.clearData();
        }

        // reinitialize all related variables
        mIsConfirmDone = false;
        mLocDeviceMap = null;
        mDeviceConnectInfoMap.clear();
        mDeviceSelectedSet.clear();

        mBleMainLayout = activity.findViewById(R.id.bleMainLayout);
        mBleMainLayout.bringToFront();
        mBleMainLayout.setVisibility(View.VISIBLE);
        (activity.findViewById(R.id.mainLayout)).invalidate();

        mBleBtn = activity.findViewById(R.id.bleBtn);
        mCaptureToggleBtn = activity.findViewById(R.id.captureBtn);

        mBleCloseBtn = activity.findViewById(R.id.ble_main_closeBtn);
        mBleCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mIsBleConfigured = false;
                stopScan();

                if (mDeviceSelectedSet.size() < mLocDataMap.size()) {
                    Toast.makeText(mContext, "# of BLE IDs < # of Light Bulbs\n Please Re-configure BLE",
                            Toast.LENGTH_LONG).show();

                    mMeasureToggleBtn.setEnabled(false);
                    mDeviceSelectedSet.clear();
                    mIsBleOK = false;
                } else {
                    mIsBleOK = true;
                }

                Log.d(TAG, "Selected Devices");
                for (String deviceAddr: mDeviceSelectedSet) {
                    Log.d(TAG, "----" + deviceAddr);
                }

                mBleBtn.setEnabled(true);
                mCaptureToggleBtn.setEnabled(true);
                mBleMainLayout.setVisibility(View.GONE);

                for (BleLocViewInfoStore bleLocViewInfoStore: mLocDataMap.values()) {
                    Integer viewId = bleLocViewInfoStore.getViewId();
                    Button button = activity.findViewById(viewId);
                    button.setVisibility(View.VISIBLE);
                    if (mIsBleOK) {
                        // enable button listener here
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // reset all colors to default
                                for (BleLocViewInfoStore bleLocViewInfoStore: mLocDataMap.values()) {
                                    Integer viewId = bleLocViewInfoStore.getViewId();
                                    (activity.findViewById(viewId)).setBackgroundColor(Color.WHITE);
                                }
                                Button button1 = (Button) v;
                                button1.setBackgroundColor(mContext.getColor(R.color.locViewMap));

                                mLocKey = button1.getText().toString();

                                mMeasureToggleBtn.setEnabled(true);
                                mMeasureLengthTextView.setText(Integer.toString(mLocDataMap.get(mLocKey).getMeasureTimeLength()));
                                mLocDataMap.get(mLocKey).setNewStart();

                                if (mIsConfirmDone) {
                                    mOnOffToggleBtn.setEnabled(true);
                                }
                            }
                        });
                    }
                }
            }
        });

        mMeasureToggleBtn = activity.findViewById(R.id.measureBtn);
        mMeasureToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ToggleButton toggleButton = (ToggleButton) v;

                mIsConfirmDone = false;

                if (toggleButton.isChecked()) {

                    startScan();

                    // disable button during measurement
                    for (BleLocViewInfoStore bleLocViewInfoStore: mLocDataMap.values()) {
                        Integer viewId = bleLocViewInfoStore.getViewId();
                        (activity.findViewById(viewId)).setEnabled(false);
                        bleLocViewInfoStore.setNewStart();
                    }

                    mBleBtn.setEnabled(false);
                    mConfirmButton.setEnabled(false);

                    // update textview timer
                    mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mMeasureLengthTextView.setText(Integer.toString(mLocDataMap.get(mLocKey).getMeasureTimeLength()));
                                }
                            });
                        }
                    }, 0, 1000);

                } else {
                    // stop measuring

                    if (mTimer != null) {
                        mTimer.cancel();
                        mTimer = null;
                    }

                    stopScan();

                    for (BleLocViewInfoStore bleLocViewInfoStore: mLocDataMap.values()) {
                        Integer viewId = bleLocViewInfoStore.getViewId();
                        (activity.findViewById(viewId)).setEnabled(true);
                    }

                    boolean enableConfirmBtn = true;
                    for (BleLocViewInfoStore bleLocViewInfoStore: mLocDataMap.values()) {
                        if (bleLocViewInfoStore.getMeasureTimeLength() < 1) {
                            enableConfirmBtn = false;
                            break;
                        }
                    }
                    if (enableConfirmBtn) {
                        mConfirmButton.setEnabled(true);
                    } else {
                        mConfirmButton.setEnabled(false);
                    }

                    mBleBtn.setEnabled(true);
                }
            }
        });
        mMeasureToggleBtn.setEnabled(false);

        mConfirmButton = activity.findViewById(R.id.resultBtn);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // AIDE algorithm runs here...
                Log.d(TAG, "AIDE runs...");

                mLocDeviceMap = AideAlgorithm.result(mLocDataMap);
                for (String location: mLocDataMap.keySet()) {
                    Log.d(TAG, "location: " + location + "has device: " + mLocDeviceMap.get(location));

                    mLocDataMap.get(location).setDeviceAddr(mLocDeviceMap.get(location));

                    mDeviceConnectInfoMap.get(mLocDeviceMap.get(location)).getBleDevice().connectGatt(mContext,
                            false, mBleGattCallback);
                }

                v.setEnabled(false);

                for (BleLocViewInfoStore bleLocViewInfoStore: mLocDataMap.values()) {
                    Integer viewId = bleLocViewInfoStore.getViewId();
                    (activity.findViewById(viewId)).setBackgroundColor(Color.WHITE);
                }
                mMeasureToggleBtn.setEnabled(false);

                mIsConfirmDone = true;
            }
        });
        mConfirmButton.setEnabled(false);

        mOnOffToggleBtn = activity.findViewById(R.id.onOffBtn);
        mOnOffToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToggleButton toggleButton = (ToggleButton) v;
                if (toggleButton.getText().toString().trim().toLowerCase().equals("green")) {
                    changeLedLight(Color.GREEN);
                } else {
                    changeLedLight(Color.BLUE);
                }
            }
        });
        mOnOffToggleBtn.setEnabled(false);

        mMeasureLengthTextView = activity.findViewById(R.id.measureTextview);
        mMeasureLengthTextView.setText("0");

        mScanView = activity.findViewById(R.id.ble_main_recyclerView);
        mScanViewLayoutManager = new GridLayoutManager(mContext, 2);
        mScanView.setLayoutManager(mScanViewLayoutManager);

        mScanViewAdapter = new RecyclerViewAdapter(mScanResultList);
        mScanView.setAdapter(mScanViewAdapter);

        mIsBleConfigured = true;
        mIsBleOK = false;
        startScan();
    }

    public void disconnectAllBle() {

        for (BleDeviceConnectedInfo bleDeviceConnectedInfo: mDeviceConnectInfoMap.values()) {
            BluetoothGatt gatt = bleDeviceConnectedInfo.getBleGatt();
            if (gatt != null) {

                BluetoothGattService bluetoothGattService = gatt.getService(BLE_UUID.LED_SERVICE_UUID);

                if (bluetoothGattService != null) {

                    BluetoothGattCharacteristic characteristic = bluetoothGattService
                            .getCharacteristic(BLE_UUID.LED_CHARACTERISTIC_UUID);

                    if (characteristic != null) {
                        characteristic.setValue(BLE_UUID.LED_BLUE);
                        gatt.writeCharacteristic(characteristic);
                    }
                }

                //
                gatt.disconnect();
            }
        }
    }

    private void startScan() {

        mScanResultMap.clear();
        mScanViewAdapter.clear();

        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothManager == null || !mBluetoothAdapter.isEnabled()) {
            ((Activity) mContext).finish();
        }

        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        builder.setReportDelay(0);

        mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothScanner.startScan(null, builder.build(), mBleScanCallback);
    }

    private void stopScan() {
        mBluetoothScanner.stopScan(mBleScanCallback);
    }

    private void changeLedLight(int color) {

        String deviceAddr = mLocDeviceMap.get(mLocKey);

        BluetoothGatt gatt = mDeviceConnectInfoMap.get(deviceAddr).getBleGatt();

        BluetoothGattCharacteristic characteristic = mDeviceConnectInfoMap.get(deviceAddr).getBleCharacteristic();

        if (gatt == null) {
            Toast.makeText(mContext, "Cannot recognize gatt of this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (characteristic == null) {
            Toast.makeText(mContext, "Cannot recognize characteristic of this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (color == Color.GREEN) {
            characteristic.setValue(BLE_UUID.LED_GREEN);
            gatt.writeCharacteristic(characteristic);
        } else if (color == Color.BLUE) {
            characteristic.setValue(BLE_UUID.LED_BLUE);
            gatt.writeCharacteristic(characteristic);
        }
    }
}
