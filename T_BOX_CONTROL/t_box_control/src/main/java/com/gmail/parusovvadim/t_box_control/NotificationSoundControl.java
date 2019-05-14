package com.gmail.parusovvadim.t_box_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

class NotificationRunnableService
{

    static private String CHANEL_ID = "AUDIo";
    static private String CHANEL_NAME = "Connection";
    static private int NOTIFICATION_ID = 1991;

    NotificationRunnableService(Service service)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(CHANEL_ID, CHANEL_NAME, importance);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{0L});
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    void showNotification(Service service, String msg)
    {
        service.startForeground(NOTIFICATION_ID, getNotification(service, msg));
    }

    private Notification getNotification(Service service, String msg)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, CHANEL_ID);
        builder.setContentTitle("ACR").setContentText(msg).
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setSmallIcon(R.mipmap.t_box).setColor(ContextCompat.getColor(service, R.color.colorPrimaryDark)).setShowWhen(false).setPriority(NotificationCompat.PRIORITY_HIGH).setOnlyAlertOnce(true);
        return builder.build();
    }

}