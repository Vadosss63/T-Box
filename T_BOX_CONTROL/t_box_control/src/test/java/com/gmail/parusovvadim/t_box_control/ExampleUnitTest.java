package com.gmail.parusovvadim.t_box_control;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

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
        System.out.println(TimeUnit.SECONDS.convert(2000, TimeUnit.MILLISECONDS));
        assertEquals(4, 2 + 2);
    }

}