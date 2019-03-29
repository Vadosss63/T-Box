package com.gmail.parusovvadim.encoder_uart;

import java.util.Vector;

public class EncoderTrack
{
    private Vector<Byte> m_dataByte;

    public EncoderTrack(Vector<Byte> dataByte)
    {
        m_dataByte = dataByte;
    }

    public EncoderTrack(int numberFolder)
    {
        m_dataByte = new Vector<>();
        convertToByte(numberFolder);
    }

    public void SetTrackNumber(int trackNumber)
    {
        convertToByte(trackNumber);
    }

    public int GetFolder()
    {
        return convertToInt(0);
    }

    public int GetTrackNumber()
    {
        return convertToInt(2);
    }

    public Vector<Byte> GetVectorByte()
    {
        return m_dataByte;
    }


    private void convertToByte(int val)
    {
        byte b0 = (byte) val;
        byte b1 = (byte) (val >> 8);
        m_dataByte.add(b1);
        m_dataByte.add(b0);
    }

    private int convertToInt(int startByte)
    {
        return (m_dataByte.get(startByte) << 8) & 0xff00 | (m_dataByte.get(startByte + 1)) & 0x00ff;
    }

}
