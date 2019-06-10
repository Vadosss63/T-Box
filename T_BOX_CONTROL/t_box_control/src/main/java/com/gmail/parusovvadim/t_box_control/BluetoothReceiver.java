package com.gmail.parusovvadim.t_box_control;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver
{

    //   static final String m_deviceName = "JBL Clip 2";
    static final String m_deviceName = "T-BOX audio";

    @Override
    public void onReceive(Context context, Intent intent)
    {

        String tag = "MyBluetooth";
        Log.d(tag, "MyBluetooth " + intent.getAction());

        if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction()))
        {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(device != null)
            {
                if(device.getName().equals(m_deviceName))
                {
                    Log.d(tag, m_deviceName);
                    Intent autoRun = new Intent(context, ReceiverService.class);
                    // everything here executes after system restart
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        context.startForegroundService(autoRun);
                    else context.startService(autoRun);
                }
            }
        }

        if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction()))
        {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(device != null)
            {
                if(device.getName().equals(m_deviceName))
                {
                    Log.d(tag, "DIS = " + m_deviceName);
                    Intent intentReceiver = new Intent(context, ReceiverService.class);
                    context.stopService(intentReceiver);
                    Intent intentUART = new Intent(context, UARTService.class);
                    context.stopService(intentUART);
                }
            }
        }

    }

}
