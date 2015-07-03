package com.gxwtech.rileylinktest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MainActivity extends ActionBarActivity {
    public int REQUEST_ENABLE_BT = 42; // any int greater than zero.
    public BluetoothAdapter mBluetoothAdapter;
    ArrayList<String> msgList = new ArrayList<>();
    ArrayAdapter<String> adapter = null;
    List<MinimedPacket> outgoingQueue = new ArrayList<>();

    public boolean mScanning;
    public Handler mHandler;
    public BluetoothAdapter.LeScanCallback mLeScanCallback;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, msgList);
        ListView lv = (ListView) findViewById(R.id.listView_log);
        lv.setAdapter(adapter);
        log("RileyLinkTest ready");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            log("This device does not support Bluetooth");
            return;
        }

        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                log("Scan Found: " + device.toString() + ", rssi: " + rssi + " (and a scan record)");
            }
        };

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // Use this check to determine whether BLE is supported on the device. Then
            // you can selectively disable BLE-related features.
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                log("BLE not supported");
            } else {
                log("Bluetooth and BLE already enabled, good to go.");
                go();
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                log("Found Bluetooth device");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                log(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Bluetooth was permitted.
                log("Received permission to use Bluetooth");
                go();
            } else {
                log("Bluetooth permission failed: " + resultCode);
            }
        }
    }

    private static final int MaxLogSize = 500;
    public void log(String msg) {
        // keep 50 messages?  make configurable?
        if (msg == null) {
            msg = "(null message)";
        }
        if (msg.equals("")) {
            msg = "(empty message)";
        }
        adapter.insert(msg, 0);
        if (adapter.getCount() > MaxLogSize) {
            adapter.remove(adapter.getItem(adapter.getCount() - 1));
        }
    }

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    log("Scan stopped.");
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    public void go() {
        boolean REG_BT = false;
        if (REG_BT) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    log(device.getName() + "\n" + device.getAddress());
                }
            } else {
                log("No paired devices found - starting discovery");
                visible();
                mBluetoothAdapter.startDiscovery();
            }
        }
        log("Starting scan");
        scanLeDevice(true);
    }

    public void visible(){
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    public void onScanButtonClicked(View view) {
        log("Starting scan");
        scanLeDevice(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
    }

    public void stop() {
        mBluetoothAdapter.cancelDiscovery();
        log("Discovery stopped.");
    }

    public void sendPacket(MinimedPacket packet) {
        outgoingQueue.add(packet);
    }



}
