package com.gmail.parusovvadim.t_box_control;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;

import com.gmail.parusovvadim.encoder_uart.CMD_DATA;
import com.gmail.parusovvadim.encoder_uart.EncoderMainHeader;
import com.gmail.parusovvadim.encoder_uart.EncoderTrackInfo;
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
    final static String AUDIO_PLAYER = "com.gmail.parusovvadim.t_box_media_player";

    static final int CMD_MEDIA_KEY = 0x00;
    static final int CMD_SYNC = 0x01;
    static final int CMD_SYNCHRONIZATION = 0x20;

    private MediaSessionManager.OnActiveSessionsChangedListener m_onActiveSessionsChangedListener = this::ChangedActiveSessions;

    private String m_title = "";

    private MediaController m_activePlayer = null;
    private TimeThread m_timeThread = null;
    private boolean m_isStop = true;
    private boolean m_isAudioPlayer = false;
    private MediaController.Callback m_callback = new MediaController.Callback()
    {
        @Override
        public void onSessionDestroyed()
        {
            super.onSessionDestroyed();
            StopMediaSession();
            m_isStop = true;
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state)
        {
            if(state == null) return;
            if(state.getState() == PlaybackState.STATE_PLAYING)
            {
                if(m_isStop) CreateTimer();
            } else m_isStop = true;

        }

        @Override // колбек при изменении данных
        public void onMetadataChanged(@Nullable MediaMetadata metadata)
        {
            String title = GetTitle(metadata);

            Log.d("MetadataChanged", title);
            SendCurrentTrack(metadata);
            if(!m_title.equals(title))
            {
                m_title = title;
                SendAUX();
                SendInfoTrack(metadata);
            }
        }
    };

    // Действие при смене активной сессии
    private void ChangedActiveSessions(List<MediaController> list)
    {
        if(list == null) return;

        if(list.isEmpty()) return;

        for(MediaController player : list)
        {
            // Выполняем синхронизацию в случае подключенного плеера
            String playerName = player.getPackageName();
            m_isAudioPlayer = AUDIO_PLAYER.equals(playerName);
            if(m_isAudioPlayer)
            {
                m_activePlayer = player;
                NotificationRunnableService notification = new NotificationRunnableService(this);
                notification.showNotification(this, "T-BoX", "Плеер");
                break;
            }
        }

        if(!m_isAudioPlayer) // если в списке сессий нет t_BOX плеера, берем последний
            m_activePlayer = list.get(list.size() - 1);

        // Устанавливаем колбеки
        m_activePlayer.registerCallback(m_callback);

        SendState();
    }

    private void SendState()
    {
        PlaybackState state = m_activePlayer.getPlaybackState();
        if(state == null) return;

        m_callback.onPlaybackStateChanged(state);
        if(m_activePlayer.getMetadata() != null)
            m_callback.onMetadataChanged(m_activePlayer.getMetadata());

    }

    private void SendCurrentTrack(MediaMetadata metadata)
    {

        if(!m_isAudioPlayer) return;

        if(metadata == null) return;

        if(metadata.containsKey(MediaMetadata.METADATA_KEY_MEDIA_ID))
        {
            String id = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            String[] ids = id.split(";");
            if(ids.length == 2)
            {
                int folder = Integer.parseInt(ids[0]);
                int track = Integer.parseInt(ids[1]);

                Intent intent = new Intent(this, UARTService.class);

                intent.putExtra("CMD", CMD_DATA.SELECTED_TRACK);
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

        Log.d("ReceiverService", "onCreate: ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Parser(intent);
        NotificationRunnableService notification = new NotificationRunnableService(this);
        notification.showNotification(this, "Сервис включен", "Статус");
        return super.onStartCommand(intent, flags, startId);
    }

    public void StopMediaSession()
    {
        NotificationRunnableService notification = new NotificationRunnableService(this);
        notification.showNotification(this, "Нет активного плеера", "Статус");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d("ReceiverService", "onDestroy: ");
        StopALL();
    }

    private void StopALL()
    {
        m_onActiveSessionsChangedListener = null;
        m_activePlayer = null;
        try
        {
            if(m_timeThread != null) if(m_timeThread.isAlive()) m_timeThread.interrupt();
        } catch(RuntimeException e)
        {
            e.fillInStackTrace();
        }
        m_timeThread = null;
        m_callback = null;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void Parser(Intent intent)
    {
        if(intent == null) return;

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
            case CMD_DATA.AUX:
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
        if(m_timeThread != null) if(m_timeThread.isAlive()) m_timeThread.interrupt();

        m_isStop = false;
        m_timeThread = new TimeThread();
        m_timeThread.start();
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
        intentUART.putExtra("CMD", CMD_DATA.TIME);
        intentUART.putExtra("time", msec);
        startService(intentUART);
    }

    // Перевод в aux режим
    private void OnAUX(byte on)
    {
        Vector<Byte> data = new Vector<>();
        data.add(on);

        String title = GetTransliterate(m_title);
        if(title.length() > 29) title = title.substring(0, 28);

        for(byte byteName : title.getBytes())
            data.add(byteName);

        EncoderMainHeader mainHeader = new EncoderMainHeader(data);
        mainHeader.AddMainHeader((byte) CMD_DATA.AUX);
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

    private void SendInfoTrack(MediaMetadata mediaMetadata)
    {
        if(mediaMetadata == null) return;

        EncoderTrackInfo trackInfo = new EncoderTrackInfo();

        if(mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_TITLE))
            trackInfo.setTitle(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE));

        if(mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ARTIST))
            trackInfo.setArtist(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST));

        if(mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ALBUM))
            trackInfo.setAlbum(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM));

        if(mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_YEAR))
            trackInfo.setYear((int) mediaMetadata.getLong(MediaMetadata.METADATA_KEY_YEAR));


        EncoderMainHeader mainHeader = new EncoderMainHeader(trackInfo.build());
        mainHeader.AddMainHeader((byte) CMD_DATA.TRACK_INFO);
        SendData(mainHeader);

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

            MediaMetadata metadata = m_activePlayer.getMetadata();
            SendCurrentTrack(metadata);
        } else
        {
            m_title = "";
            SendState();
        }
    }

    // класс отправки времени
    private class TimeThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                while(!m_isStop)
                {
                    if(m_activePlayer == null) return;
                    GetTime();
                    Log.d("TimeThread", "Send time");

                    sleep(1000);
                }
            } catch(InterruptedException e)
            {
                Log.d("TimeThread", "run: Interrupted");
            }
        }
    }
}



