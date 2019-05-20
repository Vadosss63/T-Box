package com.gmail.parusovvadim.t_box_control;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.gmail.parusovvadim.encoder_uart.CMD_DATA;
import com.gmail.parusovvadim.encoder_uart.EncoderByteMainHeader;
import com.gmail.parusovvadim.encoder_uart.EncoderFolders;
import com.gmail.parusovvadim.encoder_uart.EncoderMainHeader;
import com.gmail.parusovvadim.encoder_uart.EncoderTimeTrack;
import com.gmail.parusovvadim.encoder_uart.EncoderTrack;
import com.gmail.parusovvadim.encoder_uart.TranslitAUDI;
import com.gmail.parusovvadim.media_directory.MusicFiles;
import com.gmail.parusovvadim.media_directory.NodeDirectory;

import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class UARTService extends Service {

    final static String AUDIO_PLAYER = "com.gmail.parusovvadim.t_box_media_player";
    static final public int CMD_SEND_DATA = 0xAA;
    static final public int CMD_RESET = 0x00;

    // дериктория для синхронизации
    private MusicFiles m_musicFiles = null;
    static String m_showMassage = "Поиск соединения";
    int m_iteration = 0;
    // Поток отправки сообщений в port
    private SenderThread m_senderThread;
    private boolean m_isCheckConnectionStart = false;
    // класс подключения для COM
    private DataPort m_UARTPort = null;
    private boolean m_isStartThread = true;


    @Override
    public void onCreate() {
        super.onCreate();
        createUART();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (m_UARTPort.IsConfigured() && m_UARTPort.IsConnected()) {
            showNotification("Передача данных", "Статус Bluetooth");
            m_senderThread.AddCMD(intent);
        } else {
            showNotification("Соединение разорвано", "Статус Bluetooth");
            if (!m_isCheckConnectionStart) {
                m_isCheckConnectionStart = true;
                runCheck();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void showNotification(String msg, String title) {
        NotificationRunnableService notification = new NotificationRunnableService(this);
        notification.showNotification(this, msg, title);
    }

    private void runCheck() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                findConnection();
            }
        }, 5000);
    }

    private void findConnection() {
        if (m_UARTPort.CheckConnection()) {
            stopSelf();
        } else {
            m_iteration = m_iteration % 3;
            StringBuilder msg = new StringBuilder(m_showMassage);
            for (int i = 0; i < m_iteration; i++)
                msg.append(".");

            showNotification(msg.toString(), "Подключение");

            m_iteration++;
            if (m_isCheckConnectionStart) runCheck();

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        m_isStartThread = false;
        m_isCheckConnectionStart = false;
        m_UARTPort.Disconnect();
        m_senderThread.interrupt(); // завершам поток
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Создание соединения
    private void createUART() {
        setPort();
        createUARTPort();
    }

    // Выбор типа соединения
    private void setPort() {
        m_UARTPort = new BluetoothPort();
    }

    private void parser(Intent intent) {
        if (intent == null) return;
        if (!m_UARTPort.IsConnected()) return;

        int cmd = intent.getIntExtra("CMD", 0);
        switch (cmd) {
            case CMD_DATA.TIME:
                sendTime(intent);
                break;

            case CMD_SEND_DATA:
                sendDataByte(intent);
                break;

            case CMD_DATA.SELECTED_TRACK:
                sendSelectTrack(intent);
                break;

            case CMD_RESET:
                stopSelf();
                break;
            default:
                break;
        }
    }

    // Отправка произвольных данных
    private void sendDataByte(Intent intent) {
        if (intent == null) return;
        byte[] data = intent.getByteArrayExtra("Data");
        m_UARTPort.WriteData(data);
    }

    private void sendTime(Intent intent) {
        if (intent == null) return;
        int time = intent.getIntExtra("time", 0);
        EncoderTimeTrack timeTrack = new EncoderTimeTrack();
        timeTrack.AddHeader();
        timeTrack.AddCurrentTimePosition(time);
        EncoderMainHeader mainHeader = new EncoderMainHeader(timeTrack.GetVectorByte());
        mainHeader.AddMainHeader((byte) CMD_DATA.TIME);
        m_UARTPort.WriteData(mainHeader.GetDataByte());
    }

    private void sendSelectTrack(Intent intent) {
        if (intent == null) return;
        int folder = intent.getIntExtra("folder", 0);
        int track = intent.getIntExtra("track", 0);
        EncoderTrack encoderTrack = new EncoderTrack(folder);
        encoderTrack.SetTrackNumber(track);

        EncoderMainHeader mainHeader = new EncoderMainHeader(encoderTrack.GetVectorByte());
        mainHeader.AddMainHeader((byte) CMD_DATA.SELECTED_TRACK);
        m_UARTPort.WriteData(mainHeader.GetDataByte());
    }

    // Синхронизация с АУДИ
    private void startSyncTBox(String rootPath) {
        m_musicFiles = new MusicFiles(rootPath);
        sendInfoFoldersToComPort();
        sendInfoTracksToComPort();
    }

    private void sendInfoTracksToComPort() {

        Vector<NodeDirectory> folders = m_musicFiles.getFolders();
        EncoderByteMainHeader.EncoderListTracks encoderListTracks = new EncoderByteMainHeader.EncoderListTracks();

        for (NodeDirectory folder : folders) {
            encoderListTracks.AddHeader(folder.getNumber());
            Vector<NodeDirectory> tracks = m_musicFiles.getTracks(folder.getNumber());
            for (NodeDirectory track : tracks) {
                encoderListTracks.AddTrackNumber(track.getNumber() + 1);
                encoderListTracks.AddName(getTranslate(track.getName()));
            }
            encoderListTracks.AddEnd();

            // Добавляем заголовок
            EncoderMainHeader headerData = new EncoderMainHeader(encoderListTracks.GetVectorByte());
            headerData.AddMainHeader((byte) CMD_DATA.LIST_TRACK);

            m_UARTPort.WriteData(headerData.GetDataByte());
        }
    }

    private void sendInfoFoldersToComPort() {

        Vector<NodeDirectory> folders = m_musicFiles.getFolders();
        EncoderFolders encoderFolders = new EncoderFolders();
        encoderFolders.AddHeader();
        for (NodeDirectory folder : folders) {
            encoderFolders.AddName(getTranslate(folder.getName()));
            encoderFolders.AddNumber(folder.getNumber());
            encoderFolders.AddNumberTracks(folder.getNumberTracks());
            encoderFolders.AddParentNumber(folder.getParentNumber());
        }

        encoderFolders.AddEnd();
        // Добавляем заголовок
        EncoderMainHeader headerData = new EncoderMainHeader(encoderFolders.GetVectorByte());
        headerData.AddMainHeader((byte) CMD_DATA.LIST_FOLDER);

        m_UARTPort.WriteData(headerData.GetDataByte());
    }

    private String getTranslate(String msg) {
        return TranslitAUDI.translate(msg);
    }

    private void createUARTPort() {
        String msg;
        if (m_UARTPort.Initialisation(this)) {
            m_UARTPort.Connect();

            if (m_UARTPort.IsConnected()) {
                m_UARTPort.SetReadRunnable(this::readCommand);
                // Запускаем прослушку команд управления
                m_UARTPort.RunReadData();

                msg = m_UARTPort.GetTextLog();
                // запускаем поток отправки
                m_isStartThread = true;
                m_senderThread = new SenderThread();
                m_senderThread.start();

            } else {
                msg = "Нет соединения";
            }
        } else {
            msg = "Error";
        }
        showNotification(msg, "Статус Bluetooth");
    }

    // Обработка пришедших команд с порта
    private void readCommand() {
        byte[] data = m_UARTPort.GetReadDataByte();

        if (data.length == 1) {
            m_senderThread.SetAnswer(data[0]);
            return;
        }

        if (data[2] == (byte) CMD_DATA.SELECTED_TRACK) {
            Vector<Byte> dataTrack = new Vector<>();
            for (int i = 5; i < data.length - 1; i++)
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
        if (data[2] == (byte) CMD_DATA.AUX) {
            startSync();
        }
    }

    private void startSync() {
        Intent intent = new Intent(this, ReceiverService.class);
        intent.putExtra("CMD", CMD_DATA.AUX);
        startService(intent);
    }

    private class SenderThread extends Thread {
        // лист команд на выполнения
        private final PoolTaskCMD m_poolTaskCMD = new PoolTaskCMD();

        class ErrorSender {
            Byte m_answer = -1;
        }

        final ErrorSender m_errorSender = new ErrorSender();

        // статус ответа

        SenderThread() {
        }

        private void SetAnswer(byte answer) {
            synchronized (m_errorSender) {
                m_errorSender.m_answer = answer;
                m_errorSender.notify();
            }
        }

        // блокирует поток на 5 секунд
        private byte GetAnswer() throws InterruptedException {
            byte answer;
            synchronized (m_errorSender) {
                if (m_errorSender.m_answer == -1) m_errorSender.wait(5000);
                answer = m_errorSender.m_answer;
                m_errorSender.m_answer = (byte) -1; // Выполняем сброс ответа
            }
            return answer;
        }

        // Добавление задачи в пул
        private void AddCMD(Intent intent) {
            m_poolTaskCMD.addCMD(intent);
        }

        // Получение задачи из пула задач метод является блокирующим
        private Intent GetCMD() throws InterruptedException {
            return m_poolTaskCMD.getCMD();
        }

        @Override
        public void run() {
            while (m_isStartThread) {
                try {
                    Execute();
                } catch (InterruptedException e) {
                    m_isStartThread = false;
                }
            }
        }

        private void Execute() throws InterruptedException {
            parser(GetCMD()); // получаем команду и распознаем ее
            byte answer = GetAnswer(); // проверяем ответ
        }

        // Класс синхронизации задач между потоками
        private class PoolTaskCMD {
            // лист команд на выполнения
            private final ArrayDeque<Intent> m_listCMD = new ArrayDeque<>();

            // Добавление задачи в пул и оповещаем другой поток о наличии данных
            private synchronized void addCMD(Intent intent) {
                m_listCMD.addLast(intent);
                notify();
            }

            // Получение задачи из пула задач
            private synchronized Intent getCMD() throws InterruptedException {

                while (m_listCMD.isEmpty()) // если очередь пуста блокируем поток пока не поступят новые данные
                    wait();

                Intent cmd = m_listCMD.getFirst();
                m_listCMD.pollFirst();

                return cmd;
            }
        }
    }

}

/// TODO сделать отдельный анонимный класс для обработки данных из порта и отдельный класс для сброса сервиса