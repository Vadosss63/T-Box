package com.gmail.parusovvadim.encoder_uart;

import java.util.Vector;

public class Encoder_UART {

    static final private int MAX_SIZE_DATA = 2816;

    public static void main(String[] args) {

        Vector<Byte> data = new Vector<>();
        data.setSize(0);

        Vector<Vector<Byte>> list = GetListData(data);
        for (Vector<Byte> vec : list) {
            System.out.println(vec.size());

        }

        EncoderByteMainHeader lh;

    }

    private static Vector<Vector<Byte>> GetListData(Vector<Byte> data) {
        Vector<Vector<Byte>> list = new Vector<>();
        int startIndex = 0;
        int stopIndex;

        do {
            stopIndex = startIndex + MAX_SIZE_DATA;
            if (stopIndex > data.size())
                stopIndex = data.size();

            list.add(new Vector<>(data.subList(startIndex, stopIndex)));
            startIndex += MAX_SIZE_DATA;

        } while (stopIndex < data.size());
        return list;
    }
}
