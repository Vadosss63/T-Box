package com.gmail.parusovvadim.remountreciveraudio;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AutoStartReceiver extends BroadcastReceiver {
    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            Intent autorun = new Intent(context, ReceiverService.class);
            // everything here executes after system restart
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(autorun);
            else
                context.startService(autorun);

        }
    }
}


