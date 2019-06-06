package com.gmail.parusovvadim.media_directory;

// интерфейс для получения метаданных
class IReaderTrackInfo {

    static TrackInfo m_readerTrackInfo = null;

    static void setReaderTrackInfo(TrackInfo reader) {
        m_readerTrackInfo = reader;
    }
}
