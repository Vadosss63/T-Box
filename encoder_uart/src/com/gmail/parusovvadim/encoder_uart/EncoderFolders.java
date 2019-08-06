package com.gmail.parusovvadim.encoder_uart;

import java.util.Vector;

public class EncoderFolders {
    private Vector<Byte> m_dataByte = new Vector<>();

    public void addHeader() {
        m_dataByte.clear();
        // Заголовок из 2-х байт 0x0000
        m_dataByte.add((byte) 0x00);
        m_dataByte.add((byte) 0x00);
    }

    public void addName(String name) {
        // стартовый байт для названия папки
        m_dataByte.add((byte) 0x02);
        if (name.length() > 20) name = name.substring(0, 20);

        for (byte byteName : name.getBytes())
            m_dataByte.add(byteName);

        // конец названия папки
        m_dataByte.add((byte) 0x00);
    }

    public void addNumber(int number) {
        convertToByte(number);
    }

    public void addNumberTracks(int numberTracks) {
        convertToByte(numberTracks);
        m_dataByte.add((byte) 0x00);
        m_dataByte.add((byte) 0x00);
    }

    public void addParentNumber(int parentNumber) {
        convertToByte(parentNumber);
    }

    public void addEnd() {
        m_dataByte.add((byte) 0x01);
        m_dataByte.add((byte) 0x00);

        m_dataByte.add((byte) 0xFF);
        m_dataByte.add((byte) 0xFF);

        m_dataByte.add((byte) 0x00);
        m_dataByte.add((byte) 0x00);

        m_dataByte.add((byte) 0x00);
        m_dataByte.add((byte) 0x00);

        m_dataByte.add((byte) 0x00);
        m_dataByte.add((byte) 0x00);
    }

    public byte[] getDataByte() {
        byte[] data = new byte[m_dataByte.size()];
        for (int i = 0; i < m_dataByte.size(); i++) {
            data[i] = m_dataByte.get(i);
        }
        return data;
    }

    public Vector<Byte> getVectorByte() {
        return m_dataByte;
    }

    public int size() {
        return m_dataByte.size();
    }

    private void convertToByte(int val) {
        byte b0 = (byte) val;
        byte b1 = (byte) (val >> 8);
        m_dataByte.add(b1);
        m_dataByte.add(b0);
    }
}
