package com.gmail.parusovvadim.media_directory;

public class Folder implements NodeDirectory
{
    private String m_name;
    private int m_number;
    private int m_parentNumber;
    private int m_numberTracks;
    private boolean m_isFolderUp = false;
    private String m_path;

    public Folder(String name)
    {
        m_name = name;
    }

    public void setPath(String path) {
        m_path = path;
    }

    public void setNumber(int number) {
        m_number = number;
    }

    public void setParentNumber(int parentNumber) {
        m_parentNumber = parentNumber;
    }

    public void setNumberTracks(int numberTracks) {
        m_numberTracks = numberTracks;
    }

    public void setIsFolderUp(boolean isFolderUp) {
        m_isFolderUp = isFolderUp;
    }

    @Override
    public void setName(String name)
    {
        m_name = name;
    }

    @Override
    public String getName()
    {
        return m_name;
    }

    @Override
    public String getPathDir() {
        return m_path;
    }

    @Override
    public int getNumber()
    {
        return m_number;
    }

    @Override
    public int getParentNumber()
    {
        return m_parentNumber;
    }

    @Override
    public int getNumberTracks()
    {
        return m_numberTracks;
    }

    @Override
    public boolean isFolder()
    {
        return true;
    }

    @Override
    public boolean isFolderUp() {
        return m_isFolderUp;
    }
}
