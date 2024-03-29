package com.gmail.parusovvadim.media_directory;

public interface NodeDirectory {

    void setName(String name);

    String getName();

    String getPathDir();

    int getNumber();

    int getParentNumber();

    default int getNumberTracks() {
        return -1;
    }

    default boolean isFolder() {
        return false;
    }

    default boolean isFolderUp() {
        return false;
    }
}
