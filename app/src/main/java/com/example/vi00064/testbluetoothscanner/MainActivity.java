package com.example.vi00064.testbluetoothscanner;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static java.lang.Math.pow;

public class MainActivity extends Activity {

    private ScanCallback scanCallback;
    ArrayList<HashMap<String, String>> beconsValues = new ArrayList<HashMap<String, String>>();
    private BluetoothLeScanner scanner;
    public static final int REQUEST_CODE_ENABLE_BLE = 1001;
    private static final List<ScanFilter> SCAN_FILTERS = buildScanFilters();
    private static List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        return scanFilters;
    }
    private static final ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder().
                    setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();
    Button btn_dataa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                int flag = 1;
                ScanRecord scanRecord = result.getScanRecord();
                List<ADStructure> structures = ADPayloadParser.getInstance().parse(scanRecord.getBytes());
                // For each AD structure contained in the payload.
                for (ADStructure structure : structures) {
                    if (structure instanceof IBeacon)
                    {
                        // iBeacon
                        IBeacon iBeacon = (IBeacon)structure;
                        HashMap<String,String> hashMap = new HashMap<>();
                        if(result.getDevice().toString().equals("F9:F4:1A:D8:4C:27")){
                            hashMap.put("name", "EST");
                            hashMap.put("power", String.valueOf(iBeacon.getPower()));
                        }else {
                            hashMap.put("name", scanRecord.getDeviceName());
                            hashMap.put("power", String.valueOf(iBeacon.getPower()));
                        }
                        hashMap.put("uuid", String.valueOf(result.getDevice()));
                        String rssi = String.valueOf(result.getRssi());

                        Double distance = distance(Double.parseDouble(rssi), Integer.parseInt(hashMap.get("power")));
                        hashMap.put("distance", String.valueOf(distance));

                        if (beconsValues.size() > 0) {
                            for (int i = 0; i < beconsValues.size(); i++) {
                                if (beconsValues.get(i).get("uuid").equalsIgnoreCase(hashMap.get("uuid"))) {

                                    flag = 2;
                                    beconsValues.get(i).remove("distance");
                                    Double newdistance = distance(Double.parseDouble(rssi), Integer.parseInt(hashMap.get("power")));
                                    beconsValues.get(i).put("distance", String.valueOf(newdistance));
                                }
                            }
                            if (flag == 1) {
                                beconsValues.add(hashMap);
                            }
                        }
                        else {
                            beconsValues.add(hashMap);
                        }
                    }
                }
                if (scanRecord == null) {
                    return;
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
            }
        };
        createScanner();

        btn_dataa = (Button)findViewById(R.id.btn_dataa);

        btn_dataa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanner.startScan(SCAN_FILTERS, SCAN_SETTINGS, scanCallback);
            }
        });
    }

    private void createScanner() {
        BluetoothManager btManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLE);
        }
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Can't enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        scanner = btAdapter.getBluetoothLeScanner();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE_BLE) {
            if (resultCode == Activity.RESULT_OK) {
                createScanner();
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public double distance (double rssi, int txPower) {
        double distance = 0;
        float i = (float) 0.75;

        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.0
        }
        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            distance =  pow(ratio,10);
        }
        else {
            distance =  (0.89976) * pow(ratio,7.7095) + 0.111;
        }
        if (distance < 0.1) {
            Log.e("Distance : " ,"low");
        }
        return distance;
    }
}
