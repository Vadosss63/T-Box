package com.gmail.parusovvadim.t_box_control;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
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
import java.util.concurrent.TimeUnit;

public class ReceiverService extends Service
{
    final static String AUDIO_PLAYER = "com.gmail.parusovvadim.t_box_media_player";
    static final int CMD_MEDIA_KEY = 0x00;
    static final int CMD_SYNC = 0x01;

    private MediaSessionManager.OnActiveSessionsChangedListener m_onActiveSessionsChangedListener = this::changedActiveSessions;

    private String m_title = "";

    private MediaController m_activePlayer = null;
    private TimeThread m_timeThread = null;
    private volatile boolean m_isStop = true;
    private volatile boolean m_isAudioPlayer = false;
    private MediaController.Callback m_callback = new MediaController.Callback()
    {
        @Override
        public void onSessionDestroyed()
        {
            super.onSessionDestroyed();
            stopMediaSession();
            m_isStop = true;
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state)
        {
            if(state == null) return;
            if(state.getState() == PlaybackState.STATE_PLAYING)
            {
                if(m_isStop) createTimer();
            } else m_isStop = true;

        }

        @Override // колбек при изменении данных
        public void onMetadataChanged(@Nullable MediaMetadata metadata)
        {
            String title = getTitle(metadata);

            Log.d("MetadataChanged", title);
            sendCurrentTrack(metadata);
            if(!m_title.equals(title))
            {
                m_title = title;
                sendAUX();
                sendInfoTrack(metadata);
            }
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();
        sync();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mReceiver, filter);
        Log.d("ReceiverService", "onCreate: ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        parser(intent);
        NotificationRunnableService notification = new NotificationRunnableService(this);
        notification.showNotification(this, "Сервис включен", "Статус");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d("ReceiverService", "onDestroy: ");
        stopALL();
    }

    private void stopALL()
    {
        m_isStop = true;
        if(m_timeThread != null) if(m_timeThread.isAlive()) m_timeThread.interrupt();

        m_activePlayer = null;
        m_onActiveSessionsChangedListener = null;
        m_timeThread = null;
        m_callback = null;
        unregisterReceiver(mReceiver);
    }

    public void stopMediaSession()
    {
        NotificationRunnableService notification = new NotificationRunnableService(this);
        notification.showNotification(this, "Нет активного плеера", "Статус");
    }

    // Действие при смене активной сессии
    private void changedActiveSessions(List<MediaController> list)
    {
        if(list == null || list.isEmpty()) return;

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

        if(m_activePlayer == null) return;

       if(m_callback != null) m_activePlayer.registerCallback(m_callback);
        else
        {
            Log.d("BluetoothReceiver", "m_callback == null");
        }

        sendState();
    }

    private void sendState()
    {
        if(m_activePlayer == null || m_callback == null) return;

        PlaybackState state = m_activePlayer.getPlaybackState();
        if(state == null) return;

        m_callback.onPlaybackStateChanged(state);
        if(m_activePlayer.getMetadata() != null)
            m_callback.onMetadataChanged(m_activePlayer.getMetadata());

    }

    private void sendCurrentTrack(MediaMetadata metadata)
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
                StartService.start(this, intent);
            }
        }
    }

    private void sendAUX()
    {
        if(m_isAudioPlayer) onAUX((byte) 0x00);
        else onAUX((byte) 0x01);
    }

    private void parser(Intent intent)
    {
        if(intent == null) return;

        int cmd = intent.getIntExtra("CMD", -1);
        switch(cmd)
        {
            case CMD_MEDIA_KEY:
            {
                int key = intent.getIntExtra("keycodeMedia", -1);
                int action = intent.getIntExtra("action", -1);

                if(action == -1) sendKey(key);
                else sendKeyRewind(key, action);

                break;
            }

            case CMD_DATA.SELECTED_TRACK:
            {
                int folder = intent.getIntExtra("folder", -1);
                int track = intent.getIntExtra("track", -1);
                if(m_activePlayer != null)
                {
                    String mediaId = folder + ";" + track;
                    m_activePlayer.getTransportControls().playFromMediaId(mediaId, new Bundle());
                }
                break;
            }

            case 12:
            {
                int isShuffle = intent.getIntExtra("isShuffle", 0);
                if(m_activePlayer != null)
                {
                    Bundle shuffleBundle = new Bundle();
                    shuffleBundle.putInt("isShuffle", isShuffle);
                    m_activePlayer.getTransportControls().sendCustomAction("shuffleMode", shuffleBundle);
                }
                break;
            }
            case CMD_SYNC:
            {
                sync();
                break;
            }
            case CMD_DATA.AUX:
            {
                syncUART();
                break;
            }
            default:
                break;
        }
    }

    private void sendKey(int key)
    {
        if(m_activePlayer == null) return;
        sendKeyRewind(key, KeyEvent.ACTION_DOWN);
        sendKeyRewind(key, KeyEvent.ACTION_UP);
    }

    private void sendKeyRewind(int key, int action)
    {
        if(m_activePlayer == null) return;
        m_activePlayer.dispatchMediaButtonEvent(new KeyEvent(action, key));
    }

    // Таймер отправки времени в com порт
    private void createTimer()
    {
        if(m_timeThread != null) if(m_timeThread.isAlive()) m_timeThread.interrupt();

        m_isStop = false;
        m_timeThread = new TimeThread();
        m_timeThread.start();
    }

    private void sendData(EncoderMainHeader headerData)
    {
        Intent intent = new Intent(this, UARTService.class);
        intent.putExtra("CMD", UARTService.CMD_SEND_DATA);
        intent.putExtra("Data", headerData.getDataByte());
        StartService.start(this, intent);
    }

    void sync()
    {

        MediaSessionManager mediaSessionManager = (MediaSessionManager) getApplicationContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
        if(mediaSessionManager == null) return;

        try
        {
            // Проверяем запущенные сессии
            List<MediaController> mediaControllerList = mediaSessionManager.getActiveSessions(new ComponentName(getApplicationContext(), NotificationReceiverService.class));
            changedActiveSessions(mediaControllerList);
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

    private void sendTime(int msec)
    {
        Log.d("TimeThread", "Send time = " + TimeUnit.SECONDS.convert(msec, TimeUnit.MILLISECONDS));

        Intent intentUART = new Intent(this, UARTService.class);
        intentUART.putExtra("CMD", CMD_DATA.TIME);
        intentUART.putExtra("time", msec);
        StartService.start(this, intentUART);
    }

    // Перевод в aux режим
    private void onAUX(byte on)
    {
        Vector<Byte> data = new Vector<>();
        data.add(on);

        String title = getTransliterate(m_title);
        if(title.length() > 29) title = title.substring(0, 28);

        for(byte byteName : title.getBytes())
            data.add(byteName);

        EncoderMainHeader mainHeader = new EncoderMainHeader(data);
        mainHeader.addMainHeader((byte) CMD_DATA.AUX);
        sendData(mainHeader);
    }

    private String getTitle(MediaMetadata mediaMetadata)
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

    private void sendInfoTrack(MediaMetadata mediaMetadata)
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
        mainHeader.addMainHeader((byte) CMD_DATA.TRACK_INFO);
        sendData(mainHeader);

    }

    private void getTime()
    {
        if(m_activePlayer == null) return;

        PlaybackState state = m_activePlayer.getPlaybackState();

        if(state == null) return;

        long time = state.getPosition();
        sendTime((int) time);
    }

    @NonNull
    private static String getTransliterate(String msg)
    {
        return TranslitAUDI.translate(msg);
    }

    private void syncUART()
    {
        if(m_isAudioPlayer)
        {
            m_activePlayer.getTransportControls().sendCustomAction("synchronization", new Bundle());
            MediaMetadata metadata = m_activePlayer.getMetadata();
            sendCurrentTrack(metadata);
        } else
        {
            m_title = "";
            sendState();
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
                    getTime();
                    sleep(1000);
                }
            } catch(InterruptedException e)
            {
                Log.d("TimeThread", "run: Interrupted");
            }
        }
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                // Bluetooth is disconnected, do handling here
                if(state == BluetoothAdapter.STATE_OFF)
                {
                    Log.d("BluetoothReceiver", "BroadcastReceiver stop");

                    Intent intentUART = new Intent(context, UARTService.class);
                    context.stopService(intentUART);
                    stopSelf();
                }
                return;
            }

            if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction()))
            {
                if(!BluetoothReceiver.checkDeviceName(intent)) return;
                Log.d("BluetoothReceiver", "DISCONNECTED");
                Intent intentUART = new Intent(context, UARTService.class);
                context.stopService(intentUART);
                stopSelf();
            }

        }

    };
}



