package com.gmail.parusovvadim.t_box;

public class Track implements NodeDirectory
{
    private String m_name;
    private int m_number = -1;
    private int m_parentNumber = -1;
    private String m_path;

    Track(String name)
    {
        m_name = name;
    }

    void setPath(String path)
    {
        m_path = path;
    }

    void setNumber(int number)
    {
        m_number = number;
    }

    void setParentNumber(int parentNumber)
    {
        m_parentNumber = parentNumber;
    }

    @Override
    public void setName(String name)
    {
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
}
