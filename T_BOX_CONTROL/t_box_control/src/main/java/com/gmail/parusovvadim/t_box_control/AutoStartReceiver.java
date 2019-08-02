package com.gmail.parusovvadim.t_box_control;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AutoStartReceiver extends BroadcastReceiver
{
    @TargetApi (Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
        {
            Intent autoRun = new Intent(context, ReceiverService.class);
            StartService.start(context, autoRun);
        }
    }
}


