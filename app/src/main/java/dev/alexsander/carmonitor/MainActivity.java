package dev.alexsander.carmonitor;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private static final String OBD_LABEL = "OBDII";
    private static final String TAG = "MainActivity";
    private static final UUID UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int TIME_OUT = 200;

    protected BluetoothAdapter bluetoothAdapter;
    private ImageButton bluetoothButton;
    private BluetoothSocket socket;
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
                Log.d(TAG, "Bluetooth device: " + device.getName());
                connectToOBD2(device);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                dialog.dismiss();
            }
        }
    };

    private void connectToOBD2(BluetoothDevice device) {
        Log.d(TAG, "Connected to a device");
        Log.d(TAG, "Bluetooth device: " + device.getName());

        if (device.getName().equals(OBD_LABEL)) {
            // Connect to this device
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(UUID);
                socket.connect();
                configOBD2(socket);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void configOBD2(final BluetoothSocket socket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                    new TimeoutCommand(TIME_OUT).run(socket.getInputStream(), socket.getOutputStream());
                    new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
                    new AmbientAirTemperatureCommand().run(socket.getInputStream(), socket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String getRPM(BluetoothSocket socket) throws IOException, InterruptedException {
        RPMCommand rpmCommand = new RPMCommand();
        while (!Thread.currentThread().isInterrupted()) {
            rpmCommand.run(socket.getInputStream(), socket.getOutputStream());
            return rpmCommand.getFormattedResult();
        }
        return "";
    }
}
