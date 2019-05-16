package com.gmail.parusovvadim.t_box_control;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.gmail.parusovvadim.encoder_uart.EncoderMainHeader;
import com.gmail.parusovvadim.encoder_uart.EncoderTimeTrack;
import com.gmail.parusovvadim.encoder_uart.EncoderTrack;
import com.gmail.parusovvadim.encoder_uart.CMD_DATA;

import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class UARTService extends Service
{
    final static String AUDIO_PLAYER = "com.gmail.parusovvadim.t_box_media_player";
    static final public int CMD_SEND_DATA = 0xAA;
    static final public int CMD_RESET = 0x00;

    static String m_showMassage = "Поиск соединения";
    int m_iteration = 0;

    // Поток отправки сообщений в port
    private SenderThread m_senderThread;

    private boolean m_isCheckConnectionStart = false;

    // класс подключения для COM
    private DataPort m_UARTPort = null;

    private boolean m_isStartThread = true;

    @Override
    public void onCreate()
    {
        super.onCreate();
        CreateUART();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(m_UARTPort.IsConfigured() && m_UARTPort.IsConnected())
        {
            NotificationRunnableService notification = new NotificationRunnableService(this);
            notification.showNotification(this, "Передача данных", "Статус Bluetooth");
            m_senderThread.AddCMD(intent);
        }
        else
        {
            NotificationRunnableService notification = new NotificationRunnableService(this);
            notification.showNotification(this, "Соединение разорвано", "Статус Bluetooth");
            if(!m_isCheckConnectionStart)
            {
                m_isCheckConnectionStart = true;
                RunCheck();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void RunCheck()
    {
        new Timer().schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                FindConnection();
            }
        }, 5000);
    }

    private void FindConnection()
    {
        if(m_UARTPort.CheckConnection())
        {
            stopSelf();
        } else
        {
            m_iteration = m_iteration % 3;
            StringBuilder msg = new StringBuilder(m_showMassage);
            for(int i = 0; i < m_iteration; i++)
                msg.append(".");

            NotificationRunnableService notification = new NotificationRunnableService(this);
            notification.showNotification(this, msg.toString(), "Подключение");

            m_iteration++;
            if(m_isCheckConnectionStart) RunCheck();

        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        m_isStartThread = false;
        m_isCheckConnectionStart = false;
        m_UARTPort.Disconnect();
        m_senderThread.interrupt(); // завершам поток
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    // Создание соединения
    private void CreateUART()
    {
        SetPort();
        CreateUARTPort();
    }

    // Выбор типа соединения
    private void SetPort()
    {
        m_UARTPort = new BluetoothPort();
    }

    private void Parser(Intent intent)
    {
        if(!m_UARTPort.IsConnected()) return;

        int cmd = intent.getIntExtra("CMD", 0);
        switch(cmd)
        {
            case CMD_DATA.TIME:
                SendTime(intent);
                break;

            case CMD_SEND_DATA:
                SendDataByte(intent);
                break;

            case CMD_DATA.SELECTED_TRACK:
                SendSelectTrack(intent);
                break;

            case CMD_RESET:
                stopSelf();
                break;
            default:
                break;
        }
    }

    // Отправка произвольных данных
    private void SendDataByte(Intent intent)
    {
        if(intent == null) return;
        byte[] data = intent.getByteArrayExtra("Data");
        m_UARTPort.WriteData(data);
    }

    private void SendTime(Intent intent)
    {
        if(intent == null) return;
        int time = intent.getIntExtra("time", 0);
        EncoderTimeTrack timeTrack = new EncoderTimeTrack();
        timeTrack.AddHeader();
        timeTrack.AddCurrentTimePosition(time);
        EncoderMainHeader mainHeader = new EncoderMainHeader(timeTrack.GetVectorByte());
        mainHeader.AddMainHeader((byte) CMD_DATA.TIME);
        m_UARTPort.WriteData(mainHeader.GetDataByte());
    }

    private void SendSelectTrack(Intent intent)
    {

        if(intent == null) return;
        int folder = intent.getIntExtra("folder", 0);
        int track = intent.getIntExtra("track", 0);
        EncoderTrack encoderTrack = new EncoderTrack(folder);
        encoderTrack.SetTrackNumber(track);

        EncoderMainHeader mainHeader = new EncoderMainHeader(encoderTrack.GetVectorByte());
        mainHeader.AddMainHeader((byte) CMD_DATA.SELECTED_TRACK);
        m_UARTPort.WriteData(mainHeader.GetDataByte());
    }

    private void CreateUARTPort()
    {
        String msg;
        if(m_UARTPort.Initialisation(this))
        {
            m_UARTPort.Connect();

            if(m_UARTPort.IsConnected())
            {
                m_UARTPort.SetReadRunnable(this::ReadCommand);
                // Запускаем прослушку команд управления
                m_UARTPort.RunReadData();

                msg = m_UARTPort.GetTextLog();
                // запускаем поток отправки
                m_isStartThread = true;
                m_senderThread = new SenderThread();
                m_senderThread.start();

            } else
            {
                msg = "Нет соединения";
            }
        } else
        {
            msg = "Error";
        }
        NotificationRunnableService notification = new NotificationRunnableService(this);
        notification.showNotification(this, msg, "Статус Bluetooth");

    }

    // Обработка пришедших команд с порта
    private void ReadCommand()
    {
        byte[] data = m_UARTPort.GetReadDataByte();

        if(data.length == 1)
        {
            m_senderThread.SetAnswer(data[0]);
            return;
        }

        if(data[2] == (byte) CMD_DATA.SELECTED_TRACK)
        {
            Vector<Byte> dataTrack = new Vector<>();
            for(int i = 5; i < data.length - 1; i++)
                dataTrack.add(data[i]);

            EncoderTrack encoderTrack = new EncoderTrack(dataTrack);
            int folder = encoderTrack.GetFolder();
            int track = encoderTrack.GetTrackNumber();

            Intent intent = new Intent();
            intent.setClassName(AUDIO_PLAYER, AUDIO_PLAYER + ".MPlayer");
            intent.putExtra("CMD", CMD_DATA.SELECTED_TRACK);
            intent.putExtra("folder", folder);
            intent.putExtra("track", track);
            startService(intent);
        }
        if(data[2] == (byte) CMD_DATA.AUX)
        {
            StartSync();
        }
    }

    private void StartSync()
    {

        Intent intent = new Intent(this, ReceiverService.class);
        intent.putExtra("CMD", CMD_DATA.AUX);
        startService(intent);
    }

    private class SenderThread extends Thread
    {
        // лист команд на выполнения
        private final PoolTaskCMD m_poolTaskCMD = new PoolTaskCMD();

        class ErrorSender
        {
            Byte m_answer = -1;
        }

        final ErrorSender m_errorSender = new ErrorSender();

        // статус ответа

        SenderThread()
        {
        }

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

        // Добавление задачи в пул
        private void AddCMD(Intent intent)
        {
            m_poolTaskCMD.addCMD(intent);
        }

        // Получение задачи из пула задач метод является блокирующим
        private Intent GetCMD() throws InterruptedException
        {
            return m_poolTaskCMD.getCMD();
        }

        @Override
        public void run()
        {
            while(m_isStartThread)
            {
                try
                {
                    Execute();
                } catch(InterruptedException e)
                {
                    m_isStartThread = false;
                }
            }
        }

        private void Execute() throws InterruptedException
        {
            Parser(GetCMD()); // получаем команду и распознаем ее
            byte answer = GetAnswer(); // проверяем ответ
        }

        // Класс синхронизации задач между потоками
        private class PoolTaskCMD
        {
            // лист команд на выполнения
            private final ArrayDeque<Intent> m_listCMD = new ArrayDeque<>();

            // Добавление задачи в пул и оповещаем другой поток о наличии данных
            private synchronized void addCMD(Intent intent)
            {
                m_listCMD.addLast(intent);
                notify();
            }

            // Получение задачи из пула задач
            private synchronized Intent getCMD() throws InterruptedException
            {

                while(m_listCMD.isEmpty()) // если очередь пуста блокируем поток пока не поступят новые данные
                    wait();

                Intent cmd = m_listCMD.getFirst();
                m_listCMD.pollFirst();

                return cmd;
            }
        }
    }

}
