package com.gmail.parusovvadim.t_box_control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String tag = "MyBluetooth";
        Log.d(tag, "MyBluetooth " + intent.getAction());

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {

            Log.d(tag, "ACTION_ACL_CONNECTED");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {

                if (device.getName().equals("T-BOX data")) {
                    Log.d(tag, "equals(\"T-BOX data\")");

                }
            }

        }
    }

}
