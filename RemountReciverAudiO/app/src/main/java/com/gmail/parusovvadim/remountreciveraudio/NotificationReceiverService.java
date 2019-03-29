package com.gmail.parusovvadim.remountreciveraudio;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;

public class NotificationReceiverService extends NotificationListenerService
{
    @Override
    public IBinder onBind(Intent intent)
    {
        return super.onBind(intent);
    }

}