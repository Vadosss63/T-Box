package com.gmail.parusovvadim.t_box_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class NotificationRunnableService {

    private static final String CHANEL_ID = "T-BOX";
    private static final String CHANEL_NAME = "CONTROL";
    private static final int NOTIFICATION_ID = 1991;

    public NotificationRunnableService(Service service) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(CHANEL_ID, CHANEL_NAME, importance);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{0L});
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            if (notificationManager != null)
                notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void showNotification(Service service, String msg, String title) {
        service.startForeground(NOTIFICATION_ID, getNotification(service, msg, title));
    }

    private Notification getNotification(Service service, String msg, String title) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, CHANEL_ID);
        builder.setContentTitle(title)
                .setContentText(msg)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.t_box)
                .setColor(ContextCompat.getColor(service, R.color.colorNotif))
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);
        return builder.build();
    }

}