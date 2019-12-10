package com.gmail.parusovvadim.t_box_control;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.gmail.parusovvadim.encoder_uart.CMD_DATA;
import com.gmail.parusovvadim.encoder_uart.EncoderMainHeader;
import com.gmail.parusovvadim.encoder_uart.EncoderTimeTrack;
import com.gmail.parusovvadim.encoder_uart.EncoderTrack;

import java.util.ArrayDeque;
import java.util.Vector;

public class UARTService extends Service {
    private static final String CMD = "CMD";
    private static final String DATA = "Data";
    private static final int CMD_SEND_DATA = 0xAA;
    private static final int CMD_RESET = 0x00;
    // Поток отправки сообщений в port
    private SenderThread mSenderThread;
    // класс подключения для COM
    private DataPort mUARTPort = null;
    private volatile boolean mIsStartThread = true;

    public static Intent newSendDataIntent(Context context, byte[] data) {
        Intent intent = new Intent(context, UARTService.class);
        intent.putExtra(CMD, CMD_SEND_DATA);
        intent.putExtra(DATA, data);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createUART();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mUARTPort.isConfigured() && mUARTPort.isConnected()) {
            mSenderThread.addCMD(intent);
            showNotification("Соединено с T-BOX data", "Статус Bluetooth");
        } else {
            Intent intentUART = ReceiverService.newIntent(this);
            stopService(intentUART);
            stopSelf();
            Log.i("BluetoothReceiver", "UART stop");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsStartThread = false;
        try {
            if (mSenderThread != null)
                if (mSenderThread.isAlive()) mSenderThread.interrupt(); // завершам поток
        } catch (RuntimeException e) {
            e.fillInStackTrace();
        }

        mSenderThread = null;
        Log.i("UARTService", "onDestroy: ");
        try {
            mUARTPort.disconnect();
        } catch (RuntimeException e) {
            e.fillInStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showNotification(String msg, String title) {
        NotificationRunnableService notification = new NotificationRunnableService(this);
        notification.showNotification(this, msg, title);
    }

    // Создание соединения
    private void createUART() {
        setPort();
        createUARTPort();
    }

    // Выбор типа соединения
    private void setPort() {
        mUARTPort = new BluetoothPort();
    }

    private void parser(Intent intent) {
        if (intent == null) return;

        if (!mUARTPort.isConnected()) return;

        int cmd = intent.getIntExtra(CMD, 0);
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
        byte[] data = intent.getByteArrayExtra(DATA);
        mUARTPort.writeData(data);
    }

    private void sendTime(Intent intent) {
        if (intent == null) return;
        int time = intent.getIntExtra("time", 0);
        EncoderTimeTrack timeTrack = new EncoderTimeTrack();
        timeTrack.addHeader();
        timeTrack.addCurrentTimePosition(time);
        EncoderMainHeader mainHeader = new EncoderMainHeader(timeTrack.getVectorByte());
        mainHeader.addMainHeader((byte) CMD_DATA.TIME);
        mUARTPort.writeData(mainHeader.getDataByte());
    }

    private void sendSelectTrack(Intent intent) {
        if (intent == null) return;
        int folder = intent.getIntExtra("folder", 0);
        int track = intent.getIntExtra("track", 0);
        EncoderTrack encoderTrack = new EncoderTrack(folder);
        encoderTrack.setTrackNumber(track);
        EncoderMainHeader mainHeader = new EncoderMainHeader(encoderTrack.getVectorByte());
        mainHeader.addMainHeader((byte) CMD_DATA.SELECTED_TRACK);
        mUARTPort.writeData(mainHeader.getDataByte());
    }

    private void createUARTPort() {
        String msg;
        if (mUARTPort.initialisation(this)) {
            mUARTPort.connect();

            if (mUARTPort.isConnected()) {
                mUARTPort.setReadRunnable(this::readCommand);
                // Запускаем прослушку команд управления
                mUARTPort.runReadData();

                msg = mUARTPort.getTextLog();
                // запускаем поток отправки
                mIsStartThread = true;
                mSenderThread = new SenderThread();
                mSenderThread.start();

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
        byte[] data = mUARTPort.getReadDataByte();

        if (data.length == 1) {
            mSenderThread.setAnswer(data[0]);
            return;
        }

        if (data[2] == (byte) CMD_DATA.SELECTED_TRACK) {
            Vector<Byte> dataTrack = new Vector<>();
            for (int i = 5; i < data.length - 1; i++)
                dataTrack.add(data[i]);

            EncoderTrack encoderTrack = new EncoderTrack(dataTrack);
            int folder = encoderTrack.getFolder();
            int track = encoderTrack.getTrackNumber() - 1;

            Intent intent = new Intent(this, ReceiverService.class);
            intent.putExtra(CMD, CMD_DATA.SELECTED_TRACK);
            intent.putExtra("folder", folder);
            intent.putExtra("track", track);
            StartService.start(this, intent);
            return;
        }
        if (data[2] == (byte) 12) {
            int isShuffle = data[5];
            Intent intent = new Intent(this, ReceiverService.class);
            intent.putExtra(CMD, 12);
            intent.putExtra("isShuffle", isShuffle);
            StartService.start(this, intent);
            return;
        }

        if (data[2] == (byte) CMD_DATA.AUX) {
            startSync();
        }
    }

    private void startSync() {
        Intent intent = new Intent(this, ReceiverService.class);
        intent.putExtra(CMD, CMD_DATA.AUX);
        StartService.start(this, intent);
    }

    private class SenderThread extends Thread {
        class ErrorSender {
            Byte m_answer = -1;
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

        // лист команд на выполнения
        private final PoolTaskCMD m_poolTaskCMD = new PoolTaskCMD();

        // статус ответа
        private final ErrorSender m_errorSender = new ErrorSender();


        SenderThread() {
        }

        @Override
        public void run() {
            try {
                while (mIsStartThread) execute();
            } catch (InterruptedException e) {
                Log.d("ThreadPool", "Error");
                mIsStartThread = false;
            }
        }

        private void execute() throws InterruptedException {
            parser(getCMD()); // получаем команду и распознаем ее
            getAnswer(); // проверяем ответ
        }

        private void setAnswer(byte answer) {
            synchronized (m_errorSender) {
                m_errorSender.m_answer = answer;
                m_errorSender.notify();
            }
        }

        // блокирует поток на 5 секунд
        private byte getAnswer() throws InterruptedException {
            byte answer;
            synchronized (m_errorSender) {
                if (m_errorSender.m_answer == -1) m_errorSender.wait(5000);
                answer = m_errorSender.m_answer;
                m_errorSender.m_answer = (byte) -1; // Выполняем сброс ответа
            }
            return answer;
        }

        // Добавление задачи в пул
        private void addCMD(Intent intent) {
            m_poolTaskCMD.addCMD(intent);
        }

        // Получение задачи из пула задач метод является блокирующим
        private Intent getCMD() throws InterruptedException {
            return m_poolTaskCMD.getCMD();
        }

    }

}