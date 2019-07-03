package com.gmail.parusovvadim.t_box_control;

import java.util.Timer;
import java.util.TimerTask;

public class TimerExecute {
    private Timer m_timer;
    private Runnable m_timerTask;

    public TimerExecute(Runnable timerTask) {
        m_timerTask = timerTask;
    }

    // остановка таймера
    public void stop() {
        if (m_timer != null) {
            m_timer.cancel();
            m_timer.purge();
            m_timer = null;
        }
    }

    public void start(int sec) {
        stop();
        m_timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                m_timerTask.run();
            }
        };
        m_timer.schedule(timerTask, 1000, sec * 1000);
    }

}
