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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.KeyEvent;

import com.gmail.parusovvadim.encoder_uart.CMD_DATA;
import com.gmail.parusovvadim.encoder_uart.EncoderMainHeader;
import com.gmail.parusovvadim.encoder_uart.EncoderTrackInfo;
import com.gmail.parusovvadim.encoder_uart.TranslitAUDI;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class ReceiverService extends Service {
    private static final String TAG = "ReceiverService";
    private static final String AUDIO_PLAYER = "com.gmail.parusovvadim.t_box_media_player";
    private static final String CMD = "CMD";
    private static final String FOLDER = "folder";
    private static final String TRACK = "track";
    private static final String SHUFFLE = "isShuffle";

    private static final String KEYCODE_MEDIA = "keycodeMedia";
    private static final String KEYCODE_ACTION = "action";

    private static final int CMD_MEDIA_KEY = 0x00;
    private static final int CMD_SYNC = 0x01;
    private static final int CMD_SHUFFLE = 12;

    private MediaSessionManager.OnActiveSessionsChangedListener mOnActiveSessionsChangedListener
            = this::changedActiveSessions;

    private String mTitle = "";

    private Boolean mIsTitleTranslate = true;
    private Boolean mIsTagTitleTranslate = false;

    private MediaController mActivePlayer = null;
    private TimeThread mTimeThread = null;
    private volatile boolean mIsStop = true;
    private volatile boolean mIsAudioPlayer = false;
    private MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            stopMediaSession();
            mIsStop = true;
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            if (state == null) return;
            if (state.getState() == PlaybackState.STATE_PLAYING) {
                if (mIsStop) createTimer();
            } else mIsStop = true;

        }

        @Override // колбек при изменении данных
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            String title = getTitle(metadata);

            Log.i("MetadataChanged", title);
            sendCurrentTrack(metadata);
            if (!mTitle.equals(title)) {
                mTitle = title;
                sendAUX();
                sendInfoTrack(metadata);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mIsTitleTranslate = SettingPreferences.getStoreIsTitleTranslate(this);
        mIsTagTitleTranslate = SettingPreferences.getStoreIsTagTitleTranslate(this);

        sync();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mReceiver, filter);
        Log.i(TAG, "onCreate: ");
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, ReceiverService.class);
    }

    public static Intent newSelectedTrackIntent(Context context, int folder, int track) {
        Intent intent = new Intent(context, ReceiverService.class);
        intent.putExtra(CMD, CMD_DATA.SELECTED_TRACK);
        intent.putExtra(FOLDER, folder);
        intent.putExtra(TRACK, track);
        return intent;
    }

    public static Intent newSetShuffleIntent(Context context, int isShuffle) {
        Intent intent = new Intent(context, ReceiverService.class);
        intent.putExtra(CMD, CMD_SHUFFLE);
        intent.putExtra(SHUFFLE, isShuffle);
        return intent;
    }

    public static Intent newSetAUXIntent(Context context) {
        Intent intent = new Intent(context, ReceiverService.class);
        intent.putExtra(CMD, CMD_DATA.AUX);
        return intent;
    }

    public static Intent newSyncIntent(Context context) {
        Intent intent = new Intent(context, ReceiverService.class);
        intent.putExtra(CMD, CMD_SYNC);
        return intent;
    }

    public static Intent newPressKeyIntent(Context context, int keycodeMedia, int action) {
        Intent intent = new Intent(context, ReceiverService.class);
        intent.putExtra(CMD, CMD_MEDIA_KEY);
        intent.putExtra(KEYCODE_MEDIA, keycodeMedia);
        intent.putExtra(KEYCODE_ACTION, action);
        return intent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        parser(intent);
        NotificationRunnableService notification = new NotificationRunnableService(this);
        notification.showNotification(this, "Сервис включен", "Статус");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
        stopALL();
    }

    private void stopALL() {
        mIsStop = true;
        if (mTimeThread != null) if (mTimeThread.isAlive()) mTimeThread.interrupt();

        mActivePlayer = null;
        mOnActiveSessionsChangedListener = null;
        mTimeThread = null;
        mCallback = null;
        unregisterReceiver(mReceiver);
    }

    public void stopMediaSession() {
        NotificationRunnableService notification = new NotificationRunnableService(this);
        notification.showNotification(this, "Нет активного плеера", "Статус");
    }

    // Действие при смене активной сессии
    private void changedActiveSessions(List<MediaController> list) {
        if (list == null || list.isEmpty()) return;

        for (MediaController player : list) {
            // Выполняем синхронизацию в случае подключенного плеера
            String playerName = player.getPackageName();
            mIsAudioPlayer = AUDIO_PLAYER.equals(playerName);
            if (mIsAudioPlayer) {
                mActivePlayer = player;
                NotificationRunnableService notification = new NotificationRunnableService(this);
                notification.showNotification(this, "T-BoX", "Плеер");
                break;
            }
        }

        if (!mIsAudioPlayer) // если в списке сессий нет t_BOX плеера, берем последний
            mActivePlayer = list.get(list.size() - 1);

        if (mActivePlayer == null) return;

        if (mCallback != null) mActivePlayer.registerCallback(mCallback);
        else {
            Log.i("BluetoothReceiver", "mCallback == null");
        }

        sendState();
    }

    private void sendState() {
        if (mActivePlayer == null || mCallback == null) return;

        PlaybackState state = mActivePlayer.getPlaybackState();
        if (state == null) return;

        mCallback.onPlaybackStateChanged(state);
        if (mActivePlayer.getMetadata() != null)
            mCallback.onMetadataChanged(mActivePlayer.getMetadata());

    }

    private void sendCurrentTrack(MediaMetadata metadata) {

        if (!mIsAudioPlayer) return;

        if (metadata == null) return;

        if (metadata.containsKey(MediaMetadata.METADATA_KEY_MEDIA_ID)) {
            String id = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            String[] ids = id.split(";");
            if (ids.length == 2) {
                int folder = Integer.parseInt(ids[0]);
                int track = Integer.parseInt(ids[1]);

                Intent intent = new Intent(this, UARTService.class);

                intent.putExtra(CMD, CMD_DATA.SELECTED_TRACK);
                intent.putExtra(FOLDER, folder);
                intent.putExtra(TRACK, track + 1);
                StartService.start(this, intent);
            }
        }
    }

    private void sendAUX() {
        if (mIsAudioPlayer) onAUX((byte) 0x00);
        else onAUX((byte) 0x01);
    }

    private void parser(Intent intent) {
        if (intent == null) return;

        int cmd = intent.getIntExtra(CMD, -1);
        switch (cmd) {
            case CMD_MEDIA_KEY: {
                int key = intent.getIntExtra(KEYCODE_MEDIA, -1);
                int action = intent.getIntExtra(KEYCODE_ACTION, -1);

                if (action == -1) sendKey(key);
                else sendKeyRewind(key, action);
                break;
            }

            case CMD_DATA.SELECTED_TRACK: {
                int folder = intent.getIntExtra(FOLDER, -1);
                int track = intent.getIntExtra(TRACK, -1);
                if (mActivePlayer != null) {
                    String mediaId = folder + ";" + track;
                    mActivePlayer.getTransportControls().playFromMediaId(mediaId, new Bundle());
                }
                break;
            }

            case CMD_SHUFFLE: {
                int isShuffle = intent.getIntExtra(SHUFFLE, 0);
                if (mActivePlayer != null) {
                    Bundle shuffleBundle = new Bundle();
                    shuffleBundle.putInt(SHUFFLE, isShuffle);
                    mActivePlayer.getTransportControls().sendCustomAction("shuffleMode", shuffleBundle);
                }
                break;
            }
            case CMD_SYNC: {
                sync();
                break;
            }
            case CMD_DATA.AUX: {
                syncUART();
                break;
            }
            default:
                break;
        }
    }

    private void sendKey(int key) {
        if (mActivePlayer == null) return;
        sendKeyRewind(key, KeyEvent.ACTION_DOWN);
        sendKeyRewind(key, KeyEvent.ACTION_UP);
    }

    private void sendKeyRewind(int key, int action) {
        if (mActivePlayer == null) return;
        mActivePlayer.dispatchMediaButtonEvent(new KeyEvent(action, key));
    }

    // Таймер отправки времени в com порт
    private void createTimer() {
        if (mTimeThread != null) if (mTimeThread.isAlive()) mTimeThread.interrupt();

        mIsStop = false;
        mTimeThread = new TimeThread();
        mTimeThread.start();
    }

    private void sendData(EncoderMainHeader headerData) {
        Intent intent = UARTService.newSendDataIntent(this, headerData.getDataByte());
        StartService.start(this, intent);
    }

    void sync() {
        MediaSessionManager mediaSessionManager = (MediaSessionManager) getApplicationContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (mediaSessionManager == null) return;

        try {
            // Проверяем запущенные сессии
            List<MediaController> mediaControllerList = mediaSessionManager.getActiveSessions(new ComponentName(getApplicationContext(), NotificationReceiverService.class));
            changedActiveSessions(mediaControllerList);
            // Устанавливаем прослушку на новые сессии
            mediaSessionManager.addOnActiveSessionsChangedListener(mOnActiveSessionsChangedListener, new ComponentName(getApplicationContext(), NotificationReceiverService.class));

        } catch (SecurityException e) {
            // Запрашиваем разрешение на соединение
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    private void sendTime(int msec) {
        Log.i("TimeThread", "Send time = " + TimeUnit.SECONDS.convert(msec, TimeUnit.MILLISECONDS));
        Intent intent = UARTService.newSendTimeIntent(this, msec);
        StartService.start(this, intent);
    }

    // Перевод в aux режим
    private void onAUX(byte on) {
        Vector<Byte> data = new Vector<>();
        data.add(on);
        String title = mIsTitleTranslate ? getTransliterate(mTitle) : mTitle;
        if (title.length() > 29) title = title.substring(0, 28);

        for (byte byteName : title.getBytes())
            data.add(byteName);

        EncoderMainHeader mainHeader = new EncoderMainHeader(data);
        mainHeader.addMainHeader((byte) CMD_DATA.AUX);
        sendData(mainHeader);
    }

    private String getTitle(MediaMetadata mediaMetadata) {
        String title = "No title";
        if (mediaMetadata != null) {

            if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_TITLE)) {
                if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ARTIST))
                    title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) + " - " + mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                else title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            }
        }
        return title;
    }

    private void sendInfoTrack(MediaMetadata mediaMetadata) {
        if (mediaMetadata == null) return;

        EncoderTrackInfo trackInfo = new EncoderTrackInfo();

        if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_TITLE))

            if (mIsTagTitleTranslate)
                trackInfo.setTitle(getTransliterate(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)));
            else
                trackInfo.setTitle(mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE));


        if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ARTIST))
            trackInfo.setArtist(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST));

        if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ALBUM))
            trackInfo.setAlbum(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM));

        if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_YEAR))
            trackInfo.setYear((int) mediaMetadata.getLong(MediaMetadata.METADATA_KEY_YEAR));


        EncoderMainHeader mainHeader = new EncoderMainHeader(trackInfo.build());
        mainHeader.addMainHeader((byte) CMD_DATA.TRACK_INFO);
        sendData(mainHeader);

    }

    private void getTime() {
        if (mActivePlayer == null) return;

        PlaybackState state = mActivePlayer.getPlaybackState();

        if (state == null) return;

        long time = state.getPosition();
        sendTime((int) time);
    }

    @NonNull
    private static String getTransliterate(String msg) {
        return TranslitAUDI.translate(msg);
    }

    private void syncUART() {
        if (mIsAudioPlayer) {
            mActivePlayer.getTransportControls().sendCustomAction("synchronization", new Bundle());
            MediaMetadata metadata = mActivePlayer.getMetadata();
            sendCurrentTrack(metadata);
        } else {
            mTitle = "";
            sendState();
        }
    }

    // класс отправки времени
    private class TimeThread extends Thread {
        @Override
        public void run() {
            try {
                while (!mIsStop) {
                    if (mActivePlayer == null) return;
                    getTime();
                    sleep(1000);
                }
            } catch (InterruptedException e) {
                Log.i("TimeThread", "run: Interrupted");
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                // Bluetooth is disconnected, do handling here
                if (state == BluetoothAdapter.STATE_OFF) {
                    Log.i("BluetoothReceiver", "BroadcastReceiver stop");

                    Intent intentUART = new Intent(context, UARTService.class);
                    context.stopService(intentUART);
                    stopSelf();
                }
                return;
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                if (!BluetoothReceiver.checkDeviceName(intent)) return;
                Log.i("BluetoothReceiver", "DISCONNECTED");
                Intent intentUART = new Intent(context, UARTService.class);
                context.stopService(intentUART);
                stopSelf();
            }
        }

    };
}



