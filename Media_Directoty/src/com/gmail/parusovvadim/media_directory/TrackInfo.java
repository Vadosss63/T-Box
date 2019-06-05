package com.gmail.parusovvadim.media_directory;

public interface TrackInfo {

    void setPath(String path);

    String getPath();

    String getArtist();

    String getTitle();

    String getAlbum();

    int getDuration();

    byte[] getImage();
}
