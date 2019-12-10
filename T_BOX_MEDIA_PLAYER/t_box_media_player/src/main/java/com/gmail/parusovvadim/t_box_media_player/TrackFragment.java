package com.gmail.parusovvadim.t_box_media_player;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gmail.parusovvadim.media_directory.MusicFiles;
import com.gmail.parusovvadim.media_directory.Track;


public class TrackFragment extends Fragment {

    private static final String ARG_FOLDER = "com.gmail.parusovvadim.t_box_media_player.folder";
    private static final String ARG_TRACK = "com.gmail.parusovvadim.t_box_media_player.track";
    private Bitmap m_img = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int folder = (int) getArguments().getSerializable(ARG_FOLDER);
        int track = (int) getArguments().getSerializable(ARG_TRACK);
        Track trackFile = (Track) MusicFiles.getInstance().getTrack(folder, track);
        if (trackFile != null) {
            byte[] art = trackFile.getImage();
            // Загружаем картинку о треке
            if (art != null) {
                m_img = BitmapFactory.decodeByteArray(art, 0, art.length);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_track, container, false);
        ImageView imageForTitle = view.findViewById(R.id.imageTitle);
        if (m_img != null)
            imageForTitle.setImageBitmap(m_img);
        return view;
    }

    public static TrackFragment newInstance(int folder, int track) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_FOLDER, folder);
        args.putSerializable(ARG_TRACK, track);
        TrackFragment fragment = new TrackFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
