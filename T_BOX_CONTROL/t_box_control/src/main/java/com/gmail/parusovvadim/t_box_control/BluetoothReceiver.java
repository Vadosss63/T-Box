package com.gmail.parusovvadim.t_box_control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver {
    static final String DEVICE_NAME = "T-BOX audio";
    static final String TAG = "BluetoothReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || context == null) {
            Log.d(TAG, "intent == null || context == null");
            return;
        }

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
            if (!checkDeviceName(intent)) return;
            Log.d(TAG, "CONNECTED = " + DEVICE_NAME);
            startApps(context);
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        }
    }

    static private void startApps(Context context) {
        Intent autoRun = getIntent(context);
        // everything here executes after system restart
        StartService.start(context, autoRun);
    }

    static private Intent getIntent(Context context) {
        return new Intent(context, ReceiverService.class);
    }

    // Проверка имени устройства
    static public boolean checkDeviceName(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) return false;
        return DEVICE_NAME.equals(device.getName());
    }
}


