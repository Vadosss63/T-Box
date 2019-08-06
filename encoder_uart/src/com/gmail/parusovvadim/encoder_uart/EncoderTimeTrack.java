package com.gmail.parusovvadim.encoder_uart;

import java.util.Vector;

public class EncoderTimeTrack
{
    private Vector<Byte> m_dataByte = new Vector<>();

    public void addHeader()
    {
        m_dataByte.clear();
    }

    public void addCurrentTimePosition(int msec)
    {
        convertIntToByte(msec);
    }

    public byte[] getDataByte()
    {
        byte[] data = new byte[m_dataByte.size()];
        for(int i = 0; i < m_dataByte.size(); i++)
        {
            data[i] = m_dataByte.get(i);
        }
        return data;
    }

    public Vector<Byte> getVectorByte()
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

    private void convertIntToByte(int val)
    {
        byte b0 = (byte) val;
        byte b1 = (byte) (val >> 8);
        byte b2 = (byte) (val >> 16);
        byte b3 = (byte) (val >> 24);

        m_dataByte.add(b3);
        m_dataByte.add(b2);
        m_dataByte.add(b1);
        m_dataByte.add(b0);
    }
}
