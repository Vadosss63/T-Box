package com.gmail.parusovvadim.t_box_media_player;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.gmail.parusovvadim.media_directory.MusicFiles;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.BIND_AUTO_CREATE;

public class ControllerPlayerFragment extends Fragment {
    private ViewPager m_viewPager;
    PagerAdapter m_adapter;
    // дериктория для воспроизведения
    private MusicFiles m_musicFiles;

    private View m_view = null;
    // время воспроизведения
    private TextView m_playTime = null;
    private TextView m_currentTitle = null;
    private TextView m_currentArtist = null;

    private TextView m_durationTrack = null;
    private SeekBar m_seekPosTrack = null;

    private int m_currentFolder = 1;
    private int m_currentTrack = -1;

    boolean m_shuffle = false;
    private ImageView shuffleButton;


    private MPlayer.MPlayerBinder m_playerServiceBinder;
    private MediaControllerCompat m_mediaController;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (m_view == null) {
            m_view = inflater.inflate(R.layout.fragment_controller, null);
            createButtons();
        }
        return m_view;
    }


    private void setTime(int mSec) {
        String timeString = getStringTime(mSec);
        m_playTime.setText(timeString);
        m_seekPosTrack.setProgress(mSec);
    }

    private String getStringTime(int mSec) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("m:ss");
        return sdf.format(new Date(mSec));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createButtons() {
        m_viewPager = m_view.findViewById(R.id.image_view_pager);
        m_musicFiles = MusicFiles.getInstance();

        m_playTime = m_view.findViewById(R.id.PlayTime);
        m_seekPosTrack = m_view.findViewById(R.id.seekBar);
        m_currentTitle = m_view.findViewById(R.id.titleTrack);
        m_durationTrack = m_view.findViewById(R.id.totalTime);
        m_currentArtist = m_view.findViewById(R.id.artistTrack);

        FragmentManager fm = getActivity().getSupportFragmentManager();
        m_adapter = new FragmentStatePagerAdapter(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            @NotNull
            @Override
            public Fragment getItem(int position) {

                return TrackFragment.newInstance(m_currentFolder, position);
            }

            @Override
            public int getCount() {
                return m_musicFiles.getTracks(m_currentFolder).size();
            }

            @Override
            public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                super.setPrimaryItem(container, position, object);
                if (m_currentTrack != position) {
                    m_currentTrack = position;
                    selectTrack(m_currentFolder, m_currentTrack);
                }
            }
        };

        m_viewPager.setAdapter(m_adapter);
        m_viewPager.setCurrentItem(2);

        m_seekPosTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        });

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
                m_shuffle = m_playerServiceBinder.getShuffle();
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

                                if (track == m_currentTrack && folder == m_currentFolder) return;

                                getTitle(metadata);
                                int maxTime = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                                m_durationTrack.setText(getStringTime(maxTime));
                                m_seekPosTrack.setMax(maxTime);
                                m_currentFolder = folder;
                                m_currentTrack = track;
                                m_viewPager.setCurrentItem(m_currentTrack);
                                m_adapter.notifyDataSetChanged();

                            }

                        }

                        @Override
                        public void onShuffleModeChanged(int shuffleMode) {
                            if (PlaybackStateCompat.SHUFFLE_MODE_NONE == shuffleMode) {
                                shuffleButton.setImageResource(R.drawable.shuffle_off);
                                return;
                            }

                            if (PlaybackStateCompat.SHUFFLE_MODE_ALL == shuffleMode ||
                                    PlaybackStateCompat.SHUFFLE_MODE_GROUP == shuffleMode) {
                                shuffleButton.setImageResource(R.drawable.shuffle_on);
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
                    if (state != null) setTime((int) state.getPosition());

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

        previousButton.setOnClickListener(v -> {
            if (m_mediaController != null)
                m_mediaController.getTransportControls().skipToPrevious();

        });

        nextButton.setOnClickListener(v -> {
            if (m_mediaController != null) m_mediaController.getTransportControls().skipToNext();

        });

        shuffleButton = m_view.findViewById(R.id.shuffleButton);
        if (m_playerServiceBinder != null) {
            if (m_playerServiceBinder.getShuffle())
                shuffleButton.setImageResource(R.drawable.shuffle_on);
            else shuffleButton.setImageResource(R.drawable.shuffle_off);
        }

        shuffleButton.setOnClickListener(v -> {
            if (m_playerServiceBinder == null) return;

            boolean isShuffle = m_playerServiceBinder.getShuffle();
            /// TODO инкапсулировать Shuffle в плеер
            Bundle shuffleBundle = new Bundle();

            if (isShuffle)
                shuffleBundle.putInt("isShuffle", 0);
            else
                shuffleBundle.putInt("isShuffle", 1);

            m_mediaController.getTransportControls().sendCustomAction("shuffleMode", shuffleBundle);

        });

    }

    private void getTitle(MediaMetadataCompat mediaMetadata) {
        if (mediaMetadata != null) {
            if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_TITLE)) {
                String title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                if (title == null) title = "Empty";
                m_currentTitle.setText(title);
            }

            if (mediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ARTIST)) {
                String artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                if (artist == null) artist = "Empty";
                m_currentArtist.setText(artist);
            }
        }
    }

    public void selectTrack(int folder, int track) {
        String mediaId = folder + ";" + track;
        if (m_mediaController != null)
            m_mediaController.getTransportControls().playFromMediaId(mediaId, new Bundle());

    }
}
