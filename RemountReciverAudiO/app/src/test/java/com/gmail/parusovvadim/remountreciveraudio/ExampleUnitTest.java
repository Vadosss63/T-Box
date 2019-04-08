package com.gmail.parusovvadim.remountreciveraudio;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
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