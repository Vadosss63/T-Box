package com.gmail.parusovvadim.t_box_media_player;

import android.media.MediaMetadataRetriever;

import com.gmail.parusovvadim.media_directory.TrackInfo;

public class ReaderTrackInfo implements TrackInfo {

    private final MediaMetadataRetriever m_mediaMetadataRetriever = new MediaMetadataRetriever();
    private String m_path = null;
    private String m_artist = "Empty";
    private String m_title = "Empty";
    private String m_album = "Empty";
    private int m_durationMs = 0;
    byte[] m_image = null;

    @Override
    public boolean isInit() {
        return false;
    }

    @Override
    public void setPath(String path) {
        m_path = path;
        cleanInfo();

        try {
            m_mediaMetadataRetriever.setDataSource(path);
            m_artist = m_mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            m_title = m_mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            m_album = m_mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (m_album == null) m_album = m_artist;

            m_durationMs = (int) Long.parseLong(m_mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            m_image = m_mediaMetadataRetriever.getEmbeddedPicture();

        } catch (RuntimeException e) {
            e.fillInStackTrace();

        }
    }

    private void cleanInfo() {
        m_artist = "Empty";
        m_title = "Empty";
        m_album = "Empty";
        m_durationMs = 0;
        m_image = null;
    }

    @Override
    public String getPath() {
        return m_path;
    }

    @Override
    public String getArtist() {
        return m_artist;
    }

    @Override
    public String getTitle() {
        return m_title;
    }

    @Override
    public String getAlbum() {
        return m_album;
    }

    @Override
    public int getDuration() {
        return m_durationMs;
    }

    @Override
    public byte[] getImage() {
        return m_image;
    }


}
