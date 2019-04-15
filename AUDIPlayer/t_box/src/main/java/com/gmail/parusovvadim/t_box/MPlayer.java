package com.gmail.parusovvadim.t_box;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;


import com.gmail.parusovvadim.encoder_uart.EncoderByteMainHeader;
import com.gmail.parusovvadim.encoder_uart.EncoderFolders;
import com.gmail.parusovvadim.encoder_uart.EncoderMainHeader;
import com.gmail.parusovvadim.encoder_uart.TranslitAUDI;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;

class UARTService {
    static final int CMD_SEND_DATA = 0xAA;
    static final int CMD_CHANGE_DISC = 0x04;
}

public class MPlayer extends MediaBrowserServiceCompat implements OnCompletionListener, MediaPlayer.OnErrorListener {

    static final public int CMD_SELECT_TRACK = 0x05;
    static final public int CMD_PLAY = 0x06;
    static final public int CMD_PAUSE = 0x07;
    static final public int CMD_PREVIOUS = 0x08;
    static final public int CMD_NEXT = 0x09;
    static final public int CMD_CHANGE_ROOT = 0x0A;
    static final public int CMD_PLAY_PAUSE = 0x0B;
    static final public int CMD_SYNCHRONIZATION = 0x20;


    // метаданных трека
    final private MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
    // состояния плеера и обрабатываемые действия
    final private PlaybackStateCompat.Builder m_playbackStateCompat = new PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_STOP |
                    PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                    PlaybackStateCompat.ACTION_SEEK_TO |
                    PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM |
                    PlaybackStateCompat.ACTION_REWIND |
                    PlaybackStateCompat.ACTION_FAST_FORWARD
            );
    // Медиасессия плеера
    private MediaSessionCompat m_mediaSessionCompat;
    // Шторка управления плеером
    private NotificationSoundControl m_notificationSoundControl = null;

    // Плеер для воспроизведения
    private MediaPlayer m_mediaPlayer;

    // Действие при смене источника звука
    private NoisyAudioStreamReceiver m_noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

    // аудио фокус
    private AudioManager m_audioManager;
    private AFListener m_afLiListener = new AFListener();
    // Колбэк для рассылки времени клиентам
    private Handler m_handler;
    private Runnable m_runnable;

    // Текущий выбранный трек
    private NodeDirectory m_currentTrack; // Установить на первую песню

    // дериктория для воспроизведения
    private MusicFiles m_musicFiles;

    // корневая папка для воспроизведения музыки
    private String m_rootPath = "/Music";
    // Настройки папки воспроизведения
    private SettingApp m_settingApp;
    private AudioFocusRequest m_audioFocusRequest;

    @Override
    public void onCreate() {
        super.onCreate();

        createMediaSession();

        m_settingApp = new SettingApp(this);

        createAudioManager();

        createPlayer();
        changeRoot();
        m_notificationSoundControl = new NotificationSoundControl(this, m_mediaSessionCompat);
        selectTrack(1, 0);
    }

    private void createAudioManager() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    // Собираемся воспроизводить звуковой контент
                    // (а не звук уведомления или звонок будильника)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    // ...и именно музыку (а не трек фильма или речь)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            m_audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(m_afLiListener)
                    // Если получить фокус не удалось, ничего не делаем
                    // Если true - нам выдадут фокус как только это будет возможно
                    // (например, закончится телефонный разговор)
                    .setAcceptsDelayedFocusGain(false)
                    // Вместо уменьшения громкости собираемся вставать на паузу
                    .setWillPauseWhenDucked(true)
                    .setAudioAttributes(audioAttributes)
                    .build();
        }
        m_audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (m_mediaPlayer != null)
            m_mediaPlayer.release();

        stopPlayback();
        m_mediaSessionCompat.release();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(m_mediaSessionCompat, intent);
        parserCMD(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        m_mediaSessionCallback.onSkipToNext();
    }

    private void createMediaSession() {
        m_mediaSessionCompat = new MediaSessionCompat(this, "com.gmail.parusovvadim.t_box");
        m_mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        m_mediaSessionCompat.setCallback(m_mediaSessionCallback);

        Context appContext = getApplicationContext();
        Intent activityIntent = new Intent(appContext, MainActivity.class);
        // Запуск активити по умолчанию;
        m_mediaSessionCompat.setSessionActivity(PendingIntent.getActivity(appContext, 0, activityIntent, 0));

        // при(setActive(false)), его пробудят бродкастом
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null, appContext, MediaButtonReceiver.class);
        m_mediaSessionCompat.setMediaButtonReceiver(PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0));
    }

    private void createPlayList() {

        if (m_currentTrack == null)
            return;

        List<QueueItem> queue = new ArrayList<>();
        Vector<NodeDirectory> listTrack = m_musicFiles.getTracks(m_currentTrack.getParentNumber());

        for (int i = 0; i < listTrack.size(); i++) {
            String path = listTrack.get(i).getPathDir();

            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
            String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            long durationMs = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();

            // Заполняем данные о треке
            if (art != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeByteArray(art, 0, art.length));

            } else {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART,
                        BitmapFactory.decodeResource(getResources(), R.drawable.song_image));
            }
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs);

            MediaMetadataCompat metadata = metadataBuilder.build();

            queue.add(new QueueItem(metadata.getDescription(), i));
        }

        m_mediaSessionCompat.setQueue(queue);
    }

    private void abandonAudioFocus() {

        if (m_afLiListener == null)
            return;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            m_audioManager.abandonAudioFocusRequest(m_audioFocusRequest);
        } else {
            m_audioManager.abandonAudioFocus(m_afLiListener);
        }
    }

    private void play() {
        startPlayback();
        m_mediaPlayer.start();
    }

    private void pause() {
        stopPlayback();
        m_mediaPlayer.pause();
    }

    private void stop() {
        stopPlayback();
        m_mediaPlayer.stop();
    }

    private void playNext() {
        if (m_currentTrack != null) {
            int indexTrack = m_currentTrack.getNumber() + 1;
            selectTrack(m_currentTrack.getParentNumber(), indexTrack);
        }
    }

    private void playPrevious() {
        if (m_currentTrack != null) {
            int indexTrack = m_currentTrack.getNumber() - 1;
            selectTrack(m_currentTrack.getParentNumber(), indexTrack);
        }
    }

    private void selectTrack(int folder, int track) {
        NodeDirectory trackNode = m_musicFiles.getTrack(folder, track);
        if (trackNode != null) {
            // запускаем трек
            m_currentTrack = trackNode;
            startPlayer();
        }
    }

    private void startPlayer() {
        // Устанавливаем дорожу
        setupPlayer(m_currentTrack.getPathDir());
    }

    private boolean isPlay() {
        return m_mediaPlayer.isPlaying();
    }

    // получение в времени в мсек
    private int getCurrentPosition() {
        return m_mediaPlayer.getCurrentPosition();
    }

    // получение в времени в мсек
    private void setCurrentPosition(long pos) {
        if (m_mediaPlayer != null)
            m_mediaPlayer.seekTo((int) pos);
    }

    // Установка громкости плеера
    private void setVolume(float leftVolume, float rightVolume) {
        m_mediaPlayer.setVolume(leftVolume, rightVolume);
    }

    // создает плеер
    private void createPlayer() {
        m_mediaPlayer = new MediaPlayer();
        // Устанавливаем наблюдателя по оканчанию дорожки
        m_mediaPlayer.setOnCompletionListener(this);
    }

    // Устанавливаем дорожку для запуска плеера
    private void setupPlayer(String audio) {
        try {

            m_mediaPlayer.reset();
            m_mediaPlayer.setDataSource(audio);
            m_mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            m_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            m_mediaPlayer.prepare();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parserCMD(Intent intent) {
        if (intent == null) return;

        int cmd = intent.getIntExtra("CMD", 0);
        switch (cmd) {
            case CMD_NEXT:
                m_mediaSessionCallback.onSkipToNext();
                break;
            case CMD_PREVIOUS:
                m_mediaSessionCallback.onSkipToPrevious();
                break;
            case CMD_PAUSE:
                m_mediaSessionCallback.onPause();
                break;
            case CMD_PLAY:
                m_mediaSessionCallback.onPlay();
                break;

            case CMD_PLAY_PAUSE:
                if (isPlay())
                    m_mediaSessionCallback.onPause();
                else
                    m_mediaSessionCallback.onPlay();

                break;
            case CMD_CHANGE_ROOT:
                changeRoot();
                break;

            case CMD_SYNCHRONIZATION:
                startUART();
                break;
            case CMD_SELECT_TRACK: {
                int folder = intent.getIntExtra("folder", -1);
                int track = intent.getIntExtra("track", 0) - 1;
                selectTrack(folder, track);
                m_mediaSessionCallback.onPlay();
                break;

            }
            default:
                break;
        }
    }

    private void changeRoot() {

        m_settingApp.loadSetting();
        if (m_rootPath.equals(m_settingApp.getAbsolutePath()) && !m_musicFiles.isEmpty())
            return;

        m_rootPath = m_settingApp.getAbsolutePath();
        m_musicFiles = new MusicFiles(m_rootPath);
        selectTrack(1, 0);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    // Аудио фокус
    class AFListener implements AudioManager.OnAudioFocusChangeListener {

        @Override
        public void onAudioFocusChange(int i) {

            switch (i) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    m_mediaSessionCallback.onPause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    m_mediaSessionCallback.onPause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    setVolume(0.5f, 0.5f);
                    break;

                case AudioManager.AUDIOFOCUS_GAIN:
                    if (!isPlay())
                        m_mediaSessionCallback.onPlay();
                    setVolume(1.0f, 1.0f);
                    break;
            }
        }
    }

    // Действия при смене источника звука
    private class NoisyAudioStreamReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                m_mediaSessionCallback.onPause();
            }
        }
    }

    private void startPlayback() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            m_audioManager.requestAudioFocus(m_audioFocusRequest);
        } else {
            m_audioManager.requestAudioFocus(m_afLiListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        try {
            registerReceiver(m_noisyAudioStreamReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void stopPlayback() {
        abandonAudioFocus();
        try {
            unregisterReceiver(m_noisyAudioStreamReceiver);
        } catch (IllegalArgumentException e) {
        }
    }

    // Колбэки для обработки медиасесси
    MediaSessionCompat.Callback m_mediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            if (m_currentTrack == null)
                return;
            setMetaData();
            play();
            startMediaSession();
        }

        @Override
        public void onPause() {
            if (m_currentTrack == null)
                return;
            // Останавливаем воспроизведение
            pause();
            // Сообщаем новое состояние
            m_mediaSessionCompat.setPlaybackState(m_playbackStateCompat.setState(PlaybackStateCompat.STATE_PAUSED,
                    getCurrentPosition(), 1).build());
            m_notificationSoundControl.refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PAUSED);

        }

        @Override
        public void onSkipToQueueItem(long id) {
            if (m_currentTrack == null)
                return;
            selectTrack(m_currentTrack.getParentNumber(), (int) id);
        }

        @Override
        public void onStop() {
            if (m_currentTrack == null)
                return;

            // Останавливаем воспроизведение
            stop();

            // Все, больше мы не "главный" плеер, уходим со сцены
            m_mediaSessionCompat.setActive(false);

            // Сообщаем новое состояние
            m_mediaSessionCompat.setPlaybackState(
                    m_playbackStateCompat.setState(PlaybackStateCompat.STATE_STOPPED,
                            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            m_notificationSoundControl.refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_STOPPED);

        }

        @Override
        public void onSeekTo(long pos) {
            if (m_currentTrack == null)
                return;
            setCurrentPosition(pos);
        }

        @Override
        public void onSkipToNext() {
            if (m_currentTrack == null)
                return;
            playNext();
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            if (m_currentTrack == null)
                return;
            playPrevious();
            onPlay();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {

            if (m_currentTrack == null)
                return;

            selectTrack(m_currentTrack.getParentNumber(), Integer.parseInt(mediaId));

        }

        @Override
        public void onFastForward() {
            if (m_currentTrack == null)
                return;
            int pos = getCurrentPosition() + 5000;
            setCurrentPosition(pos);
            m_mediaSessionCompat.setPlaybackState(
                    m_playbackStateCompat.setState(PlaybackStateCompat.STATE_PLAYING,
                            getCurrentPosition(), 1).build());
        }

        @Override
        public void onRewind() {
            if (m_currentTrack == null)
                return;
            int pos = getCurrentPosition() - 5000;
            if (pos < 0)
                pos = 0;

            setCurrentPosition(pos);
            m_mediaSessionCompat.setPlaybackState(
                    m_playbackStateCompat.setState(PlaybackStateCompat.STATE_PLAYING,
                            getCurrentPosition(), 1).build());
        }

        // Делаем медиа сессию активной
        void startMediaSession() {

            // Указываем, что наше приложение теперь активный плеер и кнопки
            // на окне блокировки должны управлять именно нами
            m_mediaSessionCompat.setActive(true);

            m_mediaSessionCompat.setPlaybackState(
                    m_playbackStateCompat.setState(PlaybackStateCompat.STATE_PLAYING,
                            getCurrentPosition(), 1).build());

            m_notificationSoundControl.refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING);
        }

        // Установка данных о треке
        private void setMetaData() {

            if (m_currentTrack == null)
                return;

            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(m_currentTrack.getPathDir());
            String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            long durationMs = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();

            // Заполняем данные о треке
            if (art != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeByteArray(art, 0, art.length));

            } else {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART,
                        BitmapFactory.decodeResource(getResources(), R.drawable.song_image));
            }
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(m_currentTrack.getParentNumber()) + ";" + String.valueOf(m_currentTrack.getNumber()))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs);

            MediaMetadataCompat metadata = metadataBuilder.build();
            m_mediaSessionCompat.setMetadata(metadata);
        }

    };

    // Для доступа извне к MediaSession требуется токен
    @Override
    public IBinder onBind(Intent intent) {

        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return super.onBind(intent);
        }
        return new MPlayerBinder();
    }

    public class MPlayerBinder extends Binder {
        public MediaSessionCompat.Token getMediaSessionToken() {
            return m_mediaSessionCompat.getSessionToken();
        }
    }

    /// Для андроид авто
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {

        // Здесь мы возвращаем rootId - в нашем случае "Root".
        // Значение RootId непринципиально, оно будет просто передано
        // в onLoadChildren как parentId.
        // Идея здесь в том, что мы можем проверить clientPackageName и
        // в зависимости от того, что это за приложение, вернуть ему
        // разные плейлисты.
        // Если с неким приложением мы не хотим работать вообще,
        // можно написать return null;
        return new BrowserRoot("Root", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        // Возвращаем плейлист. Элементы могут быть FLAG_PLAYABLE
        // или FLAG_BROWSABLE.
        // Элемент FLAG_PLAYABLE нас могут попросить проиграть,
        // а FLAG_BROWSABLE отобразится как папка и, если пользователь
        // в нее попробует войти, то вызовется onLoadChildren с parentId
        // данного browsable-элемента.
        // То есть мы можем построить виртуальную древовидную структуру,
        // а не просто список треков.
        if (m_currentTrack == null)
            return;

        int idP = m_currentTrack.getParentNumber();
        Vector<NodeDirectory> tracks = m_musicFiles.getTracks(idP);

        int countTracks = tracks.size();
        ArrayList<MediaBrowserCompat.MediaItem> data = new ArrayList<>(countTracks);

        MediaDescriptionCompat.Builder descriptionBuilder =
                new MediaDescriptionCompat.Builder();

        for (int i = 0; i < countTracks; i++) {
            NodeDirectory track = tracks.get(i);

            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(track.getPathDir());
            String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
//            long durationMs = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();

            Uri icon;
            // Заполняем данные о треке
            if (art != null) {

                // надо както сделать правильное отображение через ресурсы
                Drawable drawable = new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(art, 0, art.length));

                icon = new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(getResources()
                                .getResourcePackageName(R.drawable.song_image))
                        .appendPath(getResources()
                                .getResourceTypeName(R.drawable.song_image))
                        .appendPath(getResources()
                                .getResourceEntryName(R.drawable.song_image))
                        .build();

            } else {

                icon = new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(getResources()
                                .getResourcePackageName(R.drawable.song_image))
                        .appendPath(getResources()
                                .getResourceTypeName(R.drawable.song_image))
                        .appendPath(getResources()
                                .getResourceEntryName(R.drawable.song_image))
                        .build();
            }

            MediaDescriptionCompat description = descriptionBuilder
                    .setDescription(artist)
                    .setTitle(title)
                    .setSubtitle(artist)
                    // Картинки отдавать только как Uri
                    //.setIconBitmap(...)
                    .setIconUri(icon)
                    .setMediaId(Integer.toString(i))
                    .build();


            data.add(new MediaBrowserCompat.MediaItem(description, FLAG_PLAYABLE));
        }
        result.sendResult(data);
    }

    // Синхронизация с АУДИ
    private void startUART() {
        changeDisk();
        sendInfoFoldersToComPort();
        sendInfoTracksToComPort();
    }

    ///// Для синхронизации ауди
    private void changeDisk() {
        Intent intent = getIntentServiceUART();
        intent.putExtra(getString(R.string.CMD), UARTService.CMD_CHANGE_DISC);
        startService(intent);
    }

    private void sendInfoTracksToComPort() {
        Vector<NodeDirectory> folders = m_musicFiles.getFolders();
        EncoderByteMainHeader.EncoderListTracks encoderListTracks = new EncoderByteMainHeader.EncoderListTracks();

        for (NodeDirectory folder : folders) {
            encoderListTracks.AddHeader(folder.getNumber());
            Vector<NodeDirectory> tracks = m_musicFiles.getTracks(folder.getNumber());
            for (NodeDirectory track : tracks) {
                encoderListTracks.AddTrackNumber(track.getNumber() + 1);
                encoderListTracks.AddName(getTranslate(track.getName()));
            }

            encoderListTracks.AddEnd();

            // Добавляем заголовок
            EncoderMainHeader headerData = new EncoderMainHeader(encoderListTracks.GetVectorByte());
            headerData.AddMainHeader((byte) 0x03);

            Intent intent = getIntentServiceUART();
            intent.putExtra(getString(R.string.CMD), UARTService.CMD_SEND_DATA);
            intent.putExtra("Data", headerData.GetDataByte());
            startService(intent);
        }
    }

    private void sendInfoFoldersToComPort() {
        Vector<NodeDirectory> folders = m_musicFiles.getFolders();
        EncoderFolders encoderFolders = new EncoderFolders();
        encoderFolders.AddHeader();

        for (NodeDirectory folder : folders) {
            encoderFolders.AddName(getTranslate(folder.getName()));
            encoderFolders.AddNumber(folder.getNumber());
            encoderFolders.AddNumberTracks(folder.getNumberTracks());
            encoderFolders.AddParentNumber(folder.getParentNumber());
        }

        encoderFolders.AddEnd();
        // Добавляем заголовок
        EncoderMainHeader headerData = new EncoderMainHeader(encoderFolders.GetVectorByte());
        headerData.AddMainHeader((byte) 0x02);

        Intent intent = getIntentServiceUART();
        intent.putExtra(getString(R.string.CMD), UARTService.CMD_SEND_DATA);
        intent.putExtra(getString(R.string.CMD_data), headerData.GetDataByte());
        startService(intent);
    }

    // Получение Intent для отправки в UART
    private Intent getIntentServiceUART() {
        Intent intent = new Intent();
        intent.setClassName("com.gmail.parusovvadim.remountreciveraudio", "com.gmail.parusovvadim.remountreciveraudio.UARTService");
        return intent;
    }

    @NotNull
    private String getTranslate(String msg) {
        return TranslitAUDI.translate(msg);
    }

}