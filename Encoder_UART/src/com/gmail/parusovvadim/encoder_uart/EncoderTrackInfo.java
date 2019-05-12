package com.gmail.parusovvadim.encoder_uart;

import java.util.Vector;

public class EncoderTrackInfo
{
    private Vector<Byte> m_title = new Vector<>();
    private Vector<Byte> m_artist = new Vector<>();
    private Vector<Byte> m_album = new Vector<>();
    private Vector<Byte> m_year = new Vector<>();

    public void setTitle(String title)
    {
        m_title = TextToByte(title);
    }

    public void setArtist(String artist)
    {
        m_artist = TextToByte(artist);
    }

    public void setAlbum(String album)
    {
        m_album = TextToByte(album);
    }

    public void setYear(int year)
    {
        convertToByte(trackNumber);
    }

    public Vector<Byte> build()
    {
        Vector<Byte> rerult = new Vector<Byte>();
        rerult.add(getStart());
        rerult.add(m_title);
        rerult.add(getEnd());

        rerult.add(getStart());
        rerult.add(m_artist);
        rerult.add(getEnd());

        rerult.add(getStart());
        rerult.add(m_album);
        rerult.add(getEnd());

        rerult.add(getStart());
        rerult.add(m_year);
        rerult.add(getEnd());

        rerult.add(getStart());
        rerult.add(getEnd());

        return rerult;
    }

    private void convertToByte(int val)
    {
        byte b0 = (byte) val;
        byte b1 = (byte) (val >> 8);
        m_year.add(b1);
        m_year.add(b0);
    }

    private int convertToInt(int startByte)
    {
        return (m_dataByte.get(startByte) << 8) & 0xff00 | (m_dataByte.get(startByte + 1)) & 0x00ff;
    }

    private Byte getStart()
    {
        return 0x02;
    }

    private Byte getEnd()
    {
        return 0x00;
    }

    private Vector<Byte> textToByte(String text)
    {
        return new Vector<Byte>();
    }

}

class RussianCode
{


}


