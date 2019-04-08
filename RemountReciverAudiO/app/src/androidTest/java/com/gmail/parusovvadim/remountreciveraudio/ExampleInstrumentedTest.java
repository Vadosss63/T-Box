package com.gmail.parusovvadim.remountreciveraudio;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayDeque;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith (AndroidJUnit4.class)
public class ExampleInstrumentedTest
{

    class SenderThread extends Thread
    {

        private boolean m_isStartThread = true;

        class ErrorSender
        {
            Byte m_answer = -1;
        }

        final ErrorSender m_errorSender = new ErrorSender();

        private void SetAnswer(byte answer)
        {
            synchronized(m_errorSender)
            {
                m_errorSender.m_answer = answer;
                m_errorSender.notify();
            }
        }

        // блокирует поток на 5 секунд
        private byte GetAnswer() throws InterruptedException
        {
            byte answer;
            synchronized(m_errorSender)
            {
                if(m_errorSender.m_answer == -1) m_errorSender.wait(5000);
                answer = m_errorSender.m_answer;
                m_errorSender.m_answer = (byte) -1; // Выполняем сброс ответа
            }
            return answer;
        }

        @Override
        public void run()
        {
            while(m_isStartThread)
            {
                try
                {
                    Execute();
                    System.out.println("Execute");
                } catch(InterruptedException e)
                {
                    m_isStartThread = false;
                }
            }
        }

        private void Execute() throws InterruptedException
        {
            byte answer = GetAnswer();
        }
    }

    @Test
    public void SenderThreadTest()
    {
        SenderThread senderThread = new SenderThread();
        senderThread.start();

        Thread adder = new Thread(()->{

            while(!Thread.currentThread().isInterrupted()) try
            {
                senderThread.SetAnswer((byte) 0);
                Thread.sleep(500);
            } catch(InterruptedException e)
            {
                System.out.println("Interrupted()");
            }
        });

        adder.start();


        try
        {
            Thread.sleep(20000);
        } catch(InterruptedException e)
        {
            System.out.println("Interrupted Main Thread()");
        }

        adder.interrupt();
        senderThread.isInterrupted();
        try
        {
            adder.join();
            senderThread.join();
        } catch(InterruptedException e)
        {
            System.out.println("Interrupted Main()");

        }

    }
}
