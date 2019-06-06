package com.gmail.parusovvadim.media_directory;

/// TODO сделать патерн состояния для исключения проверки на инициализацию
public class Track extends IReaderTrackInfo  implements NodeDirectory, TrackInfo {

    private String m_name;
    private int m_number = -1;
    private int m_parentNumber = -1;
    private String m_path = null;

    private String m_artist = "Empty";
    private String m_title = "Empty";
    private String m_album = "Empty";
    private int m_duration = 0;
    private byte[] m_image = null;

    private boolean m_isInit = false;

    public Track(String name) {
        m_name = name;
    }

    @Override
    public boolean isInit() {
        return m_isInit;
    }


    @Override
    public void setPath(String path) {
        m_path = path;
    }

    @Override
    public String getPath() {
        return m_path;
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
        initInfo();
        return m_artist;
    }

    @Override
    public String getTitle() {
        initInfo();
        return m_title;
    }

    @Override
    public String getAlbum() {
        initInfo();
        return m_album;
    }

    @Override
    public int getDuration() {
        initInfo();
        return m_duration;
    }

    @Override
    public byte[] getImage() {
        initInfo();
        return m_image;
    }

    void setNumber(int number) {
        m_number = number;
    }

    void setParentNumber(int parentNumber) {
        m_parentNumber = parentNumber;
    }

    private void initInfo() {
        if (!m_isInit)
            loudInfo();
    }
    // Обращение к метаданным
    synchronized private void loudInfo() {

        if (m_readerTrackInfo == null)
            return;

        m_readerTrackInfo.setPath(getPath());
        m_album = m_readerTrackInfo.getAlbum();
        m_artist = m_readerTrackInfo.getArtist();
        m_title = m_readerTrackInfo.getTitle();
        m_duration = m_readerTrackInfo.getDuration();
        m_image = m_readerTrackInfo.getImage();
        m_isInit = true;
    }
}


