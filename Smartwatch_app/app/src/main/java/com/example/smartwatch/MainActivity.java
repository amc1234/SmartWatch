package com.example.smartwatch;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView statusLabel;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    boolean isConnected = false;
    String callNum = " ";
    String textNum = " ";
    static int BLUETOOTH_PERMISSION_CODE = 100;
    int counter = 0;
    boolean useCounter = false;
    static int maxCount = 15;

    final SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
    SimpleDateFormat currentDate = new SimpleDateFormat("MMMM dd, yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button connButton = (Button)findViewById(R.id.connectButton);
        Button disButton = (Button)findViewById(R.id.disconnectButton);
        statusLabel = (TextView)findViewById(R.id.connStatus);

        connButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try
                {
                    findBT();
                    openBT();
                    sendData();
                }
                catch (IOException ex) { }
            }
        });

        disButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try
                {
                    closeBT();
                }
                catch (IOException ex) { }
            }
        });
    }
    public class ServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                Bundle bundle = intent.getExtras();
                Object[] pdus = (Object[]) bundle.get("pdus");
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    textNum = textNum + messages[i];
                    useCounter = true;
                }
            } else {
                telephony.listen(new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String incomingNumber) {
                        super.onCallStateChanged(state, incomingNumber);
                        callNum = incomingNumber;
                        useCounter = true;
                    }
                }, PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
    }
    // Function to check and request permission
    public void checkPermission(String permission, int requestCode)
    {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
        else {
            Log.d("MainActivity", "Permission already granted");
        }
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.d("MainActivity","No bluetooth adapter available");
        }

        checkPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_PERMISSION_CODE);

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        Log.d("MainActivity","Bluetooth Device Found");
    }

    void openBT() throws IOException
    {
        checkPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_PERMISSION_CODE);

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();;

        statusLabel.setText("Connected");
        isConnected = true;
        Log.d("MainActivity","Connected");
    }

    void closeBT() throws IOException
    {
        isConnected = false;
        mmOutputStream.close();
        mmSocket.close();
        statusLabel.setText("Not Connected");
        Log.d("MainActivity","Not Connected");
    }

    void sendData() throws IOException {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (!isConnected){
                    timer.cancel();
                }

                if (useCounter){
                    if (counter == maxCount){
                        useCounter = false;
                        counter = 0;
                        callNum = " ";
                        textNum = " ";
                    }
                    else{
                        counter++;
                    }
                }

                String msg = currentTime.format(new Date()) + "|" + currentDate.format(new Date()) + "|" + callNum + "|" + textNum + "\n";
                try {
                    mmOutputStream.write(msg.getBytes());
                } catch (IOException e) {}
                Log.d("MainActivity", msg);
            }
        };
        timer.schedule(task, 01, 1000);
    }
}