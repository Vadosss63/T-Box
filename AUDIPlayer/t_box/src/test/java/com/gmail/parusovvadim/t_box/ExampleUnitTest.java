package com.gmail.parusovvadim.t_box;


import com.ibm.icu.text.Transliterator;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest
{
    @Test
    public void addition_isCorrect()
    {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void RunMy()
    {
        final String CYRILLIC_TO_LATIN = "Russian-Latin/BGN";
        String msg = "Привет Vin";
        Transliterator toLatinTrans = Transliterator.getInstance(CYRILLIC_TO_LATIN);
        String result = toLatinTrans.transliterate(msg);
        result = result + "__";
        assertEquals(4, 2 + 2);
    }
}