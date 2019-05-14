package com.gmail.parusovvadim.encoder_uart;

import java.util.Vector;

public class EncoderTrackInfo {
    private Vector<Byte> m_title = new Vector<>();
    private Vector<Byte> m_artist = new Vector<>();
    private Vector<Byte> m_album = new Vector<>();
    private Vector<Byte> m_year = new Vector<>();

    public void setTitle(String title) {

        if (title.length() > 29) title = title.substring(0, 28);

        m_title = textToByte(title);
    }

    public void setArtist(String artist) {
        if (artist.length() > 29) artist = artist.substring(0, 28);

        m_artist = textToByte(artist);
    }

    public void setAlbum(String album) {
        if (album.length() > 29) album = album.substring(0, 28);

        m_album = textToByte(album);
    }

    public void setYear(int year) {
        m_year = textToByte(Integer.toString(year));
    }

    public Vector<Byte> build() {
        Vector<Byte> result = new Vector<Byte>();
        result.add(getStart());
        if (!m_title.isEmpty())
            result.addAll(m_title);
        result.add(getEnd());

        result.add(getStart());
        if (!m_artist.isEmpty())
            result.addAll(m_artist);
        result.add(getEnd());

        result.add(getStart());
        if (!m_album.isEmpty())
            result.addAll(m_album);
        result.add(getEnd());

        result.add(getStart());
        if (!m_year.isEmpty())
            result.addAll(m_year);
        result.add(getEnd());

        result.add(getStart());
        result.add(getEnd());

        return result;
    }


    private Byte getStart() {
        return 0x02;
    }

    private Byte getEnd() {
        return 0x00;
    }

    private Vector<Byte> textToByte(String text) {
        return RussianCode.convert(text);
    }
}

class RussianCode {

    // Особый случай
    private static final char[] m_eChar = {'Ё', 'ё', 0xC385, 0xC3A5};
    private static final char[] m_iChar = {'і', 'i'};

    private static final char m_startRussianChar = 0xC380;
    // Сдвиг массива на "A" в char (JAVA)
    private static final char m_shiftArrayChar = 0x410;
    // Количество букв в кириллице без учета "ё"
    private static final char m_sizeCyr = 32;

    static Vector<Byte> convert(String message) {

        if (message == null) return new Vector<Byte>();

        Vector<Byte> builder = new Vector<Byte>();
        // Проходим по всему сообщению
        for (int i = 0; i < message.length(); i++) {
            char currentChar = message.charAt(i);
            // Проверяем диапазон алфавита с учетом больших и маленьких букв
            if (currentChar >= m_shiftArrayChar && currentChar <= (m_shiftArrayChar + 2 * m_sizeCyr)) {
                // выполняем преобразование

                char val = (char) (m_startRussianChar + (currentChar - m_shiftArrayChar));
                builder.add((byte) ((val & 0xFF00) >> 8));
                builder.add((byte) val);

            } else {
                if (currentChar < 127) {
                    // ascii
                    builder.add((byte) currentChar);
                } else {
                    // Проверяем особый случай
                    if (currentChar == m_eChar[0]) {
                        builder.add((byte) ((m_eChar[2] & 0xFF00) >> 8));
                        builder.add((byte) m_eChar[2]);
                    } else if (currentChar == m_eChar[1]) {
                        builder.add((byte) ((m_eChar[3] & 0xFF00) >> 8));
                        builder.add((byte) m_eChar[3]);
                    } else if (currentChar == m_iChar[0]) {
                        builder.add((byte) m_iChar[1]);
                    }
                }
            }
        }
        return builder;
    }

}


