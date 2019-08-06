package com.gmail.parusovvadim.encoder_uart;

import java.util.Vector;

public class EncoderMainHeader
{
    private Vector<Byte> m_dataByte;
    private Vector<Byte> m_vectorHeader = new Vector<>();

    public EncoderMainHeader(Vector<Byte> dataByte)
    {
        this.m_dataByte = dataByte;
    }

    public void addMainHeader(byte command)
    {
        addHeader();
        addCommand(command);
        addSize();
        m_dataByte.addAll(0, m_vectorHeader);
        //  добавляем в конец CRC
        byte crc = checkSum();
        m_dataByte.add(crc);
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

    private void addHeader()
    {
        m_vectorHeader.clear();
        m_vectorHeader.add((byte) 0xAB);
        m_vectorHeader.add((byte) 0xBA);
    }

    private void addCommand(byte command)
    {
        m_vectorHeader.add(command);
    }

    private void addSize()
    {
        int size = m_dataByte.size();
        convertToByte(size);
    }

    private void convertToByte(int val)
    {
        byte b0 = (byte) val;
        byte b1 = (byte) (val >> 8);
        m_vectorHeader.add(b1);
        m_vectorHeader.add(b0);
    }

    private byte checkSum()
    {
        byte sum = 0;
        for (Byte aByte : m_dataByte) sum += aByte;
        return sum;
    }

}
