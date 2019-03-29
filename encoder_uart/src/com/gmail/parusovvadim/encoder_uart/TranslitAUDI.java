package com.gmail.parusovvadim.encoder_uart;

public class TranslitAUDI {

    // Массив кириллицы
    private static final String[] m_abcLat = {"a", "b", "v", "g", "d", "e", "zh", "z", "i", "y", "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "f", "h", "ts", "ch", "sh", "sch", "", "i", "", "e", "ju", "ja"};
    // Особый случай
    private static final char[] m_eChar = {'Ё', 'ё', 'E', 'e'};
    private static final char[] m_iChar = {'і', 'i'};
    // Сдвиг массива на "A" в char (JAVA)
    private static final int m_shiftArrayChar = 1040;
    // Количество букв в кириллице без учета "ё"
    private static final int m_sizeCyr = 32;

    public static String translate(String message) {

        if (message == null) return "";

        StringBuilder builder = new StringBuilder();
        // Проходим по всему сообщению
        for (int i = 0; i < message.length(); i++) {
            char currentChar = message.charAt(i);
            // Проверяем диапазон алфавита с учетом больших и маленьких букв
            if (currentChar >= m_shiftArrayChar && currentChar <= (m_shiftArrayChar + 2 * m_sizeCyr)) {

                if (currentChar < m_shiftArrayChar + m_sizeCyr) {
                    //Диапазон больших букв
                    builder.append(m_abcLat[currentChar - m_shiftArrayChar].toUpperCase());
                } else {
                    //Диапазон маленьких букв
                    builder.append(m_abcLat[currentChar - (m_shiftArrayChar + m_sizeCyr)]);
                }

            } else {

                if (currentChar < 127) {
                    // ascii
                    builder.append(currentChar);
                } else {
                    // Проверяем особый случай
                    if (currentChar == m_eChar[0]) {
                        builder.append(m_eChar[2]);
                    } else if (currentChar == m_eChar[1]) {
                        builder.append(m_eChar[3]);
                    } else if (currentChar == m_iChar[0]) {
                        builder.append(m_iChar[1]);
                    }
                }
            }
        }

        return builder.toString();
    }
}