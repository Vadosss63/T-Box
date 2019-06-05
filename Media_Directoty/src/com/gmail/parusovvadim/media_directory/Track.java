package com.gmail.parusovvadim.media_directory;

public class Track implements NodeDirectory, TrackInfo {
    private String m_name;
    private int m_number = -1;
    private int m_parentNumber = -1;
    private String m_path;

    private String m_artist = "Empty";
    private String m_title = "Empty";
    private String m_album = "Empty";
    private int m_duration = 0;
    private byte[] m_image = null;

    public Track(String name) {
        m_name = name;
    }

    @Override
    public void setPath(String path) {
        m_path = path;
    }

    @Override
    public String getPath() {
        return m_path;
    }

    public void setNumber(int number) {
        m_number = number;
    }

    public void setParentNumber(int parentNumber) {
        m_parentNumber = parentNumber;
    }

    @Override
    public void setName(String name) {
        m_name = name;
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public String getPathDir() {
        return m_path;
    }

    @Override
    public int getNumber() {
        return m_number;
    }

    @Override
    public int getParentNumber() {
        return m_parentNumber;
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
        return m_duration;
    }

    @Override
    public byte[] getImage() {
        return m_image;
    }


    void setArtist(String artist) {
        m_artist = artist;
    }

    void setTitle(String title) {
        m_title = title;
    }

    void setAlbum(String album) {
        m_album = album;
    }

    void setDuration(int duration) {
        m_duration = duration;
    }

    void setImage(byte[] image) {
        m_image = image;
    }

}
