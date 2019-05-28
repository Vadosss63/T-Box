package com.gmail.parusovvadim.t_box_media_player;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.BIND_AUTO_CREATE;

public class ControllerPlayerFragment extends Fragment {

    private View m_view = null;
    // время воспроизведения
    private TextView m_playTime = null;
    private TextView m_currentTitle = null;
    private TextView m_deractionTrack = null;
    private SeekBar m_seekPosTrack = null;
    private ImageView m_imageTitle = null;

    // Колбек для перемотки
    Runnable m_runSeek;
    // остановка перемотки
    boolean m_isStopSeek = false;
    boolean m_isSeek = false;

    MPlayer.MPlayerBinder m_playerServiceBinder;
    MediaControllerCompat m_mediaController;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (m_view == null) {
            m_view = inflater.inflate(R.layout.fragment_controller, null);
            CreateButtons();
        }
        return m_view;
    }

    private void SetTime(int msec) {
        String timeString = getStringTime(msec);
        m_playTime.setText(timeString);
        m_seekPosTrack.setProgress(msec);
    }

    private String getStringTime(int msec) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("m:ss");
        return sdf.format(new Date(msec));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void CreateButtons() {
        m_playTime = m_view.findViewById(R.id.PlayTime);
        m_seekPosTrack = m_view.findViewById(R.id.seekBar);
        m_currentTitle = m_view.findViewById(R.id.titleTrack);
        m_deractionTrack = m_view.findViewById(R.id.totalTime);
        m_imageTitle = m_view.findViewById(R.id.imageTitle);

        m_seekPosTrack.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (seekBar.getProgress() != 0 && m_mediaController != null) {
                            m_mediaController.getTransportControls().pause();
                            m_mediaController.getTransportControls().seekTo(seekBar.getProgress());
                            m_mediaController.getTransportControls().play();
                        }
                    }
                }
        );

        final Button play_pause = m_view.findViewById(R.id.play_pause_Button);
        final Button previousButton = m_view.findViewById(R.id.previousButton);
        final Button nextButton = m_view.findViewById(R.id.nextButton);

        getActivity().bindService(new Intent(getActivity(), MPlayer.class), new ServiceConnection() {

            private Handler m_handler;
            private Runnable m_runnable;
            private boolean m_isStop = true;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                m_playerServiceBinder = (MPlayer.MPlayerBinder) service;
                try {
                    m_mediaController = new MediaControllerCompat(getActivity(), m_playerServiceBinder.getMediaSessionToken());
                    m_mediaController.registerCallback(new MediaControllerCompat.Callback() {
                        @Override
                        public void onPlaybackStateChanged(PlaybackStateCompat state) {
                            if (state == null) return;
                            boolean playing = state.getState() == PlaybackStateCompat.STATE_PLAYING;
                            if (playing) {
                                play_pause.setSelected(true);
                                m_playTime.setFocusable(false);
                                m_playTime.setCursorVisible(false);

                                if (m_isStop) { // если плеер остановлен включаем таймер
                                    m_isStop = false;
                                    CreateTime();
                                }
                            } else {
                                play_pause.setSelected(false);
                                m_isStop = true;

                                m_playTime.setFocusable(true);
                                m_playTime.setCursorVisible(true);
                            }
                        }

                        @Override
                        public void onMetadataChanged(MediaMetadataCompat metadata) {

                            String id = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                            String[] ids = id.split(";");
                            if (ids.length == 2) {
                                int folder = Integer.parseInt(ids[0]);
                                int track = Integer.parseInt(ids[1]);
                                MainActivity ma = (MainActivity) getActivity();
                                if (ma == null) return;
                                ma.selectCurrentTrack(folder, track);
                                m_currentTitle.setText(GetTitle(metadata));
                                int maxTime = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                                m_deractionTrack.setText(getStringTime(maxTime));
                                m_seekPosTrack.setMax(maxTime);
                                Bitmap image = metadata.getDescription().getIconBitmap();

                                if (image != null)
                                    m_imageTitle.setImageBitmap(image);

                            }

                        }
                    });
                } catch (RemoteException e) {
                    m_mediaController = null;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                m_playerServiceBinder = null;
                m_mediaController = null;
            }

            // установка времени
            private void CreateTime() {
                m_runnable = () -> {

                    if (m_isStop) return;

                    PlaybackStateCompat state = m_mediaController.getPlaybackState();
                    if (state != null) SetTime((int) state.getPosition());

                    m_handler.postDelayed(m_runnable, 1000);
                };

                m_handler = new Handler();
                m_handler.post(m_runnable);
            }


        }, BIND_AUTO_CREATE);

        play_pause.setOnClickListener(v -> {
            if (m_mediaController != null) {
                PlaybackStateCompat state = m_mediaController.getPlaybackState();
                if (state != null) {
                    if (m_mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                        m_mediaController.getTransportControls().pause();
                        return;
                    }
                }

                m_mediaController.getTransportControls().play();

            }
        });

        previousButton.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                m_isStopSeek = false;
                m_isSeek = false;
                Rewind();
                return true;
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                m_isStopSeek = true;
                if (!m_isSeek) {
                    if (m_mediaController != null)
                        m_mediaController.getTransportControls().skipToPrevious();
                }
                return true;
            }
            return false;
        });

        nextButton.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                m_isStopSeek = false;
                m_isSeek = false;
                FastForward();
                return true;
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                m_isStopSeek = true;
                if (!m_isSeek) {
                    if (m_mediaController != null)
                        m_mediaController.getTransportControls().skipToNext();
                }
                return true;
            }

            return false;
        });

    }

    private String GetTitle(MediaMetadataCompat mediaMetadata) {
        String title = "Новая композиция";
        if (mediaMetadata != null) {

            if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_TITLE)) {
                if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ARTIST))
                    title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) + " - " + mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                else
                    title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);

            }
        }
        return title;
    }

    // Перемотка вперед
    void FastForward() {

        Handler handler = new Handler();
        m_runSeek = () -> {

            if (m_isStopSeek) // останавливаем таймер
                return;

            m_isSeek = true; // Переводим кноку в режим перемотки

            if (m_mediaController != null) {
                m_mediaController.getTransportControls().fastForward();
                handler.postDelayed(m_runSeek, 500); // Каждые 0.5 сек перематываем на 5 сек.
            }
        };

        handler.postDelayed(m_runSeek, 1500); // ждём 1.5 сек до перевода кнопки в режим перемотки иначе просто переключам песню
    }

    // Перемотка назад
    void Rewind() {

        Handler handler = new Handler();
        m_runSeek = () -> {

            if (m_isStopSeek) // останавливаем таймер
                return;

            m_isSeek = true; // Переводим кноку в режим перемотки

            if (m_mediaController != null) {
                m_mediaController.getTransportControls().rewind();
                handler.postDelayed(m_runSeek, 500); // Каждые 0.5 сек перематываем на 5 сек.
            }
        };

        handler.postDelayed(m_runSeek, 1500); // ждём 1.5 сек до перевода кнопки в режим перемотки иначе просто переключам песню
    }

}
