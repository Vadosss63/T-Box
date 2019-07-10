package com.gmail.parusovvadim.t_box_control;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver
{
    //static final String m_deviceName = "JBL Clip 2";
    static final String m_deviceName = "T-BOX audio";
    static final String m_tag = "BluetoothReceiver";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(intent == null || context == null)
        {
            Log.d(m_tag, "intent == null || context == null");
            return;
        }

        if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction()))
        {
            if(!checkDeviceName(intent)) return;
            Log.d(m_tag, "CONNECTED = " + m_deviceName);
            startApps(context);
        }
    }

    static private void startApps(Context context)
    {
        Intent autoRun = getIntent(context);
        // everything here executes after system restart
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(autoRun);
        else context.startService(autoRun);
    }

    static private Intent getIntent(Context context)
    {
        return new Intent(context, ReceiverService.class);
    }

    // Проверка имени устройства
    static public boolean checkDeviceName(Intent intent)
    {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(device == null) return false;
        return m_deviceName.equals(device.getName());
    }
}


