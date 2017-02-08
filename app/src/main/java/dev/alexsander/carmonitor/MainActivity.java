package dev.alexsander.carmonitor;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String OBD_LABEL = "OBD2";

    protected BluetoothAdapter bluetoothAdapter;
    private ImageButton bluetoothButton;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothButton = (ImageButton) findViewById(R.id.btn_connect_bluetooth) ;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não disponível nesse dispositivo.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectBluetooth();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth foi ligado.", Toast.LENGTH_SHORT).show();
            startDiscovery();
        }
    }

    private void connectBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth está ligado.", Toast.LENGTH_SHORT).show();
            startDiscovery();
        } else {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 0);
        }
    }

    private void startDiscovery() {
        List<BluetoothDevice> bondedDevices = new ArrayList<BluetoothDevice>(bluetoothAdapter.getBondedDevices());

        for (BluetoothDevice device: bondedDevices) {
           connectToOBD2(device);
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        this.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        bluetoothAdapter.startDiscovery();
        dialog = ProgressDialog.show(this, "Car Monitor", "Buscando OBD2...", false, true);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                connectToOBD2(device);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                dialog.dismiss();
            }
        }
    };

    private void connectToOBD2(BluetoothDevice device) {
        if (device.getName().equals(OBD_LABEL)) {
            // Connect to this device

        }
    }
}
