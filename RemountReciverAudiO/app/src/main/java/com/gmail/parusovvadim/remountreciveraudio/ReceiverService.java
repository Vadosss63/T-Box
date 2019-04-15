package com.gmail.parusovvadim.remountreciveraudio;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;

import com.gmail.parusovvadim.encoder_uart.EncoderMainHeader;
import com.gmail.parusovvadim.encoder_uart.TranslitAUDI;

import java.util.List;
import java.util.Vector;

public class ReceiverService extends Service
{

    // Поддерживаемые плееры
//    final static String YA_MUSIC = "ru.yandex.music";
//    final static String GOOGLE_MUSIC = "com.google.android.music";
//    final static String VK_MUSIC = "com.vkontakte.music";
//    final static String ZYCEV_NET = "com.p74.player";
//    final static String MI_PLAYER = "media.music.mp3player.musicplayer";
    final static String AUDIO_PLAYER = "com.gmail.parusovvadim.t_box";

    static final int CMD_MEDIA_KEY = 0x00;
    static final int CMD_SYNC = 0x01;
    static final int CMD_SYNCHRONIZATION = 0x20;


    private MediaSessionManager.OnActiveSessionsChangedListener m_onActiveSessionsChangedListener = this::ChangedActiveSessions;

    // Действие при смене активной сессии
    private void ChangedActiveSessions(@Nullable List<MediaController> list)
    {
        if(list == null) return;

        if(list.isEmpty()) return;

        m_activePlayer = list.get(list.size() - 1);

        // Устанавливаем колбеки
        m_activePlayer.registerCallback(m_callback);

        // Выполняем синхронизацию в случае подключенного плеера
        String playerName = m_activePlayer.getPackageName();
        m_isAudioPlayer = playerName.equals(AUDIO_PLAYER);

        SendState();
    }

    private void SendState()
    {
        PlaybackState state = m_activePlayer.getPlaybackState();
        if(state != null)
        {
            m_callback.onPlaybackStateChanged(state);
            if(m_activePlayer.getMetadata() != null)
                m_callback.onMetadataChanged(m_activePlayer.getMetadata());
        }
    }

    private String m_title = "";

    private MediaController m_activePlayer = null;

    private Handler m_handler;
    private Runnable m_runnable;

    private boolean m_isStop = true;
    private boolean m_isAudioPlayer = false;
    private MediaController.Callback m_callback = new MediaController.Callback()
    {
        @Override
        public void onSessionDestroyed()
        {
            super.onSessionDestroyed();
            m_isStop = true;
        }

        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state)
        {
            if(state == null) return;
            boolean playing = state.getState() == PlaybackState.STATE_PLAYING;
            if(playing)
            {
                if(m_isStop)
                { // если плеер остановлен включаем таймер
                    m_isStop = false;
                    CreateTimer();
                }
            } else
            {
                m_isStop = true;
            }
        }

        @Override // колбек при изменении данных
        public void onMetadataChanged(@Nullable MediaMetadata metadata)
        {

            String title = GetTitle(metadata);
            if(!title.equals(m_title))
            {
                m_title = title;
                SendAUX();
            }
            SendCurrentTrack(metadata);
        }
    };

    private void SendCurrentTrack(@Nullable MediaMetadata metadata)
    {

        if(metadata == null) return;

        if(!m_isAudioPlayer) return;

        if(metadata.containsKey(MediaMetadata.METADATA_KEY_MEDIA_ID))
        {
            String id = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            String ids[] = id.split(";");
            if(ids.length == 2)
            {
                int folder = Integer.parseInt(ids[0]);
                int track = Integer.parseInt(ids[1]);

                Intent intent = new Intent(this, UARTService.class);

                intent.putExtra("CMD", UARTService.CMD_SELECT_TRACK);
                intent.putExtra("folder", folder);
                intent.putExtra("track", track + 1);
                startService(intent);
            }
        }
    }

    private void SendAUX()
    {
        if(m_isAudioPlayer) OnAUX((byte) 0x00);
        else OnAUX((byte) 0x01);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Sync();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Parser(intent);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            O.createNotification(this, "Работает система управления АУДИ");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void Parser(Intent intent)
    {

        int cmd = intent.getIntExtra("CMD", -1);
        switch(cmd)
        {
            case CMD_MEDIA_KEY:
            {
                int key = intent.getIntExtra("keycodeMedia", -1);
                int action = intent.getIntExtra("action", -1);

                if(action == -1) SendKey(key);
                else SendKeyRewind(key, action);

                break;
            }
            case CMD_SYNC:
            {
                Sync();
                break;
            }
            case UARTService.CMD_AUX:
            {
                SyncUART();
                break;
            }
            default:
                break;
        }
    }

    private void SendKey(int key)
    {
        if(m_activePlayer == null) return;
        SendKeyRewind(key, KeyEvent.ACTION_DOWN);
        SendKeyRewind(key, KeyEvent.ACTION_UP);
    }

    private void SendKeyRewind(int key, int action)
    {
        if(m_activePlayer == null) return;
        m_activePlayer.dispatchMediaButtonEvent(new KeyEvent(action, key));
    }

    // Таймер отправки времени в com порт
    private void CreateTimer()
    {
        m_handler = new Handler();
        m_runnable = ()->{
            if(m_activePlayer == null || m_isStop) return;
            GetTime();
            m_handler.postDelayed(m_runnable, 1000);
        };

        m_handler.postDelayed(m_runnable, 1000);
    }

    private void SendData(EncoderMainHeader headerData)
    {
        Intent intent = new Intent(this, UARTService.class);
        intent.putExtra("CMD", UARTService.CMD_SEND_DATA);
        intent.putExtra("Data", headerData.GetDataByte());
        startService(intent);
    }

    void Sync()
    {

        MediaSessionManager mediaSessionManager = (MediaSessionManager) getApplicationContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
        if(mediaSessionManager == null) return;

        try
        {
            // Проверяем запущенные сессии
            List<MediaController> mediaControllerList = mediaSessionManager.getActiveSessions(new ComponentName(getApplicationContext(), NotificationReceiverService.class));
            ChangedActiveSessions(mediaControllerList);
            // Устанавливаем прослушку на новые сессии
            mediaSessionManager.addOnActiveSessionsChangedListener(m_onActiveSessionsChangedListener, new ComponentName(getApplicationContext(), NotificationReceiverService.class));

        } catch(SecurityException e)
        {
            // Запрашиваем разрешение на соединение
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            {

                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    private void SendTime(int msec)
    {
        Intent intentUART = new Intent(this, UARTService.class);
        intentUART.putExtra("CMD", UARTService.CMD_SEND_TIME);
        intentUART.putExtra("time", msec);
        startService(intentUART);
    }

    // Перевод в aux режим
    private void OnAUX(byte on)
    {
        Vector<Byte> data = new Vector<>();
        data.add(on);

        String title = GetTransliterate(m_title);

        for(byte byteName : title.getBytes())
            data.add(byteName);

        EncoderMainHeader mainHeader = new EncoderMainHeader(data);
        mainHeader.AddMainHeader((byte) UARTService.CMD_AUX);
        SendData(mainHeader);
    }

    private String GetTitle(MediaMetadata mediaMetadata)
    {
        String title = "No title";
        if(mediaMetadata != null)
        {

            if(mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_TITLE))
            {
                if(mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ARTIST))
                    title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) + " - " + mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                else title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);

            }
        }
        return title;
    }

    private void GetTime()
    {

        if(m_activePlayer == null) return;

        PlaybackState state = m_activePlayer.getPlaybackState();

        if(state == null) return;

        long time = state.getPosition();
        SendTime((int) time);

    }

    @NonNull
    private static String GetTransliterate(String msg)
    {
        return TranslitAUDI.translate(msg);
    }

    private void SyncUART()
    {
        if(m_isAudioPlayer)
        {
            Intent intent = new Intent();
            intent.setClassName(AUDIO_PLAYER, AUDIO_PLAYER + ".MPlayer");
            intent.putExtra("CMD", CMD_SYNCHRONIZATION);
            startService(intent);
        } else
        {
            m_title = "";
            SendState();
        }
    }

    public static class O
    {
        static final String CHANNEL_ID = String.valueOf(getRandomNumber());
        static final int ONGOING_NOTIFICATION_ID = 1991;

        private static int getRandomNumber()
        {
            return 1177;
        }

        static void createNotification(Service context, String msg)
        {
            String channelId = createChannel(context);
            Notification notification = buildNotification(context, channelId, msg);
            context.startForeground(ONGOING_NOTIFICATION_ID, notification);
        }

        @TargetApi (Build.VERSION_CODES.O)
        private static Notification buildNotification(Service context, String channelId, String msg)
        {
            Intent intentNext = new Intent(context, MainActivity.class);
            PendingIntent piLaunchMainActivity = PendingIntent.getService(context, 1, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

            // Create a notification.
            return new Notification.Builder(context, channelId).setContentTitle("ARC").setContentText(msg).setSmallIcon(R.mipmap.ic_launcher).setContentIntent(piLaunchMainActivity).setStyle(new Notification.BigTextStyle()).build();
        }

        @TargetApi (Build.VERSION_CODES.O)
        @NonNull
        private static String createChannel(Service ctx)
        {
            // Create a channel.
            NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence channelName = "Управление";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, channelName, importance);

            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
            return CHANNEL_ID;
        }
    }
}


