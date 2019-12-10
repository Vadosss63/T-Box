package com.gmail.parusovvadim.t_box_media_player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

/**
 * Helper APIs for constructing MediaStyle notifications
 */
class MediaStyleHelper
{
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     *
     * @param context      Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    static NotificationCompat.Builder from(Context context, MediaSessionCompat mediaSession, String idChanel)
    {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, idChanel);
        builder.setContentTitle(description.getTitle()).setContentText(description.getSubtitle()).setSubText(description.getDescription()).setLargeIcon(description.getIconBitmap()).setContentIntent(controller.getSessionActivity()).setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder;
    }
}


class NotificationSoundControl
{
    private static final String CHANEL_ID = "AUDIo";
    private static final String CHANEL_NAME = "AUDIo";
    private static final int NOTIFICATION_ID = 1;

    private enum requestCode
    {MainActivity, Next, Previous, Play, Pause, Exit}

    private final MPlayer m_service;

    private PendingIntent m_pendNext;
    private PendingIntent m_pendPrevious;
    private PendingIntent m_pendPlay;
    private PendingIntent m_pendPause;
    private PendingIntent m_pendExit;

    private final MediaSessionCompat m_mediaSessionCompat;

    NotificationSoundControl(MPlayer service, MediaSessionCompat mediaSessionCompat)
    {

        this.m_mediaSessionCompat = mediaSessionCompat;
        this.m_service = service;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {

            NotificationManager notificationManager = (NotificationManager) m_service.getSystemService(Context.NOTIFICATION_SERVICE);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(CHANEL_ID, CHANEL_NAME, importance);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{0L});
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
        createPendingIntent();
    }

    private void createPendingIntent()
    {
        Intent intentNext = MPlayer.newIntentNext(m_service);
        m_pendNext = PendingIntent.getService(m_service, requestCode.Next.ordinal(), intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPrevious = MPlayer.newIntentPrevious(m_service);
        m_pendPrevious = PendingIntent.getService(m_service, requestCode.Previous.ordinal(), intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPlay = MPlayer.newIntentPlay(m_service);
        m_pendPlay = PendingIntent.getService(m_service, requestCode.Play.ordinal(), intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPause = MPlayer.newIntentPause(m_service);
        m_pendPause = PendingIntent.getService(m_service, requestCode.Pause.ordinal(), intentPause, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentExit = MainActivity.newIntentExit();
        m_pendExit = PendingIntent.getBroadcast(m_service, requestCode.Exit.ordinal(), intentExit, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    void refreshNotificationAndForegroundStatus(int playbackState)
    {
        switch(playbackState)
        {
            case PlaybackStateCompat.STATE_PLAYING:
            {
                m_service.startForeground(NOTIFICATION_ID, getNotification(playbackState));
                break;
            }
            case PlaybackStateCompat.STATE_PAUSED:
            {
                // На паузе мы перестаем быть foreground, однако оставляем уведомление,
                // чтобы пользователь мог play нажать
                NotificationManagerCompat.from(m_service).notify(NOTIFICATION_ID, getNotification(playbackState));
                m_service.stopForeground(false);
                break;
            }
            default:
            {
                // Все, можно прятать уведомление
                m_service.stopForeground(true);
                break;
            }
        }
    }

    private Notification getNotification(int playbackState)
    {

        NotificationCompat.Builder builder = MediaStyleHelper.from(m_service.getApplicationContext(), m_mediaSessionCompat, CHANEL_ID);

        // Добавляем кнопки
//  TODO не работает на OREO
//
//        // на предыдущий трек
//        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "previous",
//                MediaButtonReceiver.buildMediaButtonPendingIntent(m_service.getApplicationContext(), PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
//
//        // play/pause
//        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
//            builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "pause",
//                    MediaButtonReceiver.buildMediaButtonPendingIntent(m_service, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
//        else
//            builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "play",
//                    MediaButtonReceiver.buildMediaButtonPendingIntent(m_service, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
//
//        // на следующий трек
//        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "next",
//                MediaButtonReceiver.buildMediaButtonPendingIntent(m_service, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));

        // на предыдущий трек
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "previous", m_pendPrevious));

        // play/pause
        if(playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "pause", m_pendPause));
        else
            builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "play", m_pendPlay));

        // на следующий трек
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, "next", m_pendNext));

        // выход
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "exit", m_pendExit));


        builder.setStyle(new MediaStyle()
                // В компактном варианте показывать Action с данным порядковым номером.
                // В нашем случае это play/pause.
                .setShowActionsInCompactView(1)
                // Отображать крестик в углу уведомления для его закрытия.
                // Это связано с тем, что для API < 21 из-за ошибки во фреймворке
                // пользователь не мог смахнуть уведомление foreground-сервиса
                // даже после вызова stopForeground(false).
                // Так что это костыль.
                // На API >= 21 крестик не отображается, там просто смахиваем уведомление.
                .setShowCancelButton(true)
                // Указываем, что делать при нажатии на крестик или смахивании
                .setCancelButtonIntent(m_pendExit)
                // Передаем токен. Это важно для Android Wear. Если токен не передать,
                // кнопка на Android Wear будет отображаться, но не будет ничего делать
                .setMediaSession(m_mediaSessionCompat.getSessionToken()));

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setColor(ContextCompat.getColor(m_service, R.color.colorPrimaryDark));

        // Не отображать время создания уведомления. В нашем случае это не имеет смысла
        builder.setShowWhen(false);

        // Это важно. Без этой строчки уведомления не отображаются на Android Wear
        // и криво отображаются на самом телефоне.
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        // Не надо каждый раз вываливать уведомление на пользователя
        builder.setOnlyAlertOnce(true);

        return builder.build();
    }

}