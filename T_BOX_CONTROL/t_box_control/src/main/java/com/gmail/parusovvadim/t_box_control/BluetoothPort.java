package com.gmail.parusovvadim.t_box_control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothPort implements DataPort
{

    final static int REQUEST_ENABLE_BT = 1;

    // UUID для последовательного порта
    private final static String m_UUID = "00001101-0000-1000-8000-00805f9b34fb";

    private String m_textLog;

    private Context m_context = null;

    private BluetoothAdapter m_bluetooth = null;
    private BluetoothDevice m_device = null;

    // Поток чтения данных из сокета
    private ConnectedThread m_connectedThread = null;
    // Поток создания соединения
    private ConnectThread m_connectThread = null;

    private Runnable m_readRunnable = null;
    private byte[] m_readDataByte = new byte[0];
    private boolean m_isUARTConfigured = false;

    BluetoothPort()
    {
    }

    @Override
    public boolean isConnected()
    {
        if(m_connectThread == null) return false;
        return m_connectThread.socketIsConnect();
    }

    @Override
    public boolean isConfigured()
    {
        return m_isUARTConfigured;
    }

    @Override
    public void writeData(byte[] bytes)
    {
        if(isConfigured() && m_connectedThread != null)
        {
            m_connectedThread.write(bytes);
        }
    }

    @Override
    public void disconnect()
    {
        if(m_connectedThread != null)
        {
            m_connectedThread.cancel();
        }
    }

    @Override
    public Boolean initialisation(Context context)
    {
        m_context = context;
        // вполняем проверку наличия блютуз
        m_bluetooth = BluetoothAdapter.getDefaultAdapter();
        if(m_bluetooth == null)
        {
            m_textLog = "No bluetooth";
            m_isUARTConfigured = false;
            return false;
        }
        return true;
    }

    @Override
    public void connect()
    {
        if(m_bluetooth != null && m_bluetooth.isEnabled())
        {
            // Bluetooth включен. Работаем.
            showLostDevice();
            if(m_device != null)
            {
                m_connectThread = new ConnectThread(m_device);
                m_connectThread.run();

                if(m_connectThread.socketIsConnect())
                    m_connectedThread = new ConnectedThread(m_connectThread.getSocket());

            } else
            {
                m_isUARTConfigured = false;
                m_textLog = "No bluetooth device";
            }
        } else
        {
            // Bluetooth выключен. Предложим пользователю включить его.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            m_context.startActivity(enableBtIntent);
        }

    }

    public boolean checkConnection()
    {
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        if(ba == null) return false;

        if(!m_bluetooth.isEnabled()) return false;

        showLostDevice();

        if(m_device == null) return false;

        try
        {
            // MY_UUID это UUID, который используется и в сервере
            BluetoothSocket socket = m_device.createRfcommSocketToServiceRecord(UUID.fromString(m_UUID));
            if(socket == null) return false;

            try
            {
                // Соединяемся с устройством через сокет.
                // Метод блокирует выполнение программы до
                // установки соединения или возникновения ошибки
                socket.connect();
            } catch(IOException connectException)
            {
                // Невозможно соединиться. Закрываем сокет и выходим.
                try
                {
                    socket.close();
                } catch(IOException closeException)
                {

                }

            }

            if(socket.isConnected())
            {
                socket.close();
                return true;
            } else return false;
        } catch(IOException e)
        {
            return false;
        }
    }

    @Override
    public void setReadRunnable(Runnable runnable)
    {
        m_readRunnable = runnable;
    }

    @Override
    public void runReadData()
    {

        if(m_isUARTConfigured && m_connectedThread != null)
        {
            // управлчем соединением (в отдельном потоке)
            if(m_connectThread.socketIsConnect())
            {
                m_connectedThread.start();
                m_textLog = "Bluetooth ok";
            }
        }

    }

    @Override
    public String getTextLog()
    {
        return m_textLog;
    }

    @Override
    public byte[] getReadDataByte()
    {
        return m_readDataByte;
    }

    // Выполняем просмотр доступных устройств
    private void showLostDevice()
    {
        Set<BluetoothDevice> pairedDevices = m_bluetooth.getBondedDevices();
        m_device = null;
        // Если список спаренных устройств не пуст
        if(pairedDevices.size() > 0)
        {
            // проходимся в цикле по этому списку
            for(BluetoothDevice device : pairedDevices)
            {
                // Добавляем имена и адреса в mArrayAdapter, чтобы показать
                // через ListView
                // mArrayAdapter.add(device.getName() + "\n" + device.getAddress());

                if(device.getName().equals("T-BOX data")) m_device = device;
            }
        }
    }

    // Создаем BroadcastReceiver для ACTION_FOUND
    private final BroadcastReceiver m_receiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {

            if(intent == null) return;

            String action = intent.getAction();
            // Когда найдено новое устройство
            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Получаем объект BluetoothDevice из интента
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Добавляем имя и адрес в array adapter, чтобы показвать в ListView
                // mArrayAdapter.add(device.getName()+"\n"+ device.getAddress());
            }
        }
    };

    private class ConnectThread extends Thread
    {

        private final BluetoothSocket m_socket;
        private final BluetoothDevice m_device;

        ConnectThread(BluetoothDevice device)
        {
            // используем вспомогательную переменную, которую в дальнейшем
            // свяжем с mmSocket,
            BluetoothSocket tmp = null;
            m_device = device;

            // получаем BluetoothSocket чтобы соединиться с BluetoothDevice
            try
            {
                // MY_UUID это UUID, который используется и в сервере
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(m_UUID));

            } catch(IOException e)
            {
                m_isUARTConfigured = false;
            }
            m_socket = tmp;
        }

        @Override
        public void run()
        {
            tryConnect();
        }

        boolean socketIsConnect()
        {
            return m_socket.isConnected();
        }

        BluetoothSocket getSocket()
        {
            return m_socket;
        }

        // Пытаемся соедениться с блютуз устройством
        private void tryConnect()
        {
            // Отменяем сканирование, поскольку оно тормозит соединение
            m_bluetooth.cancelDiscovery();

            try
            {
                // Соединяемся с устройством через сокет.
                // Метод блокирует выполнение программы до
                // установки соединения или возникновения ошибки
                m_socket.connect();
            } catch(IOException connectException)
            {
                // Невозможно соединиться. Закрываем сокет и выходим.
                try
                {
                    m_socket.close();
                } catch(IOException closeException)
                {

                }

            }
        }

        /**
         * отмена ожидания сокета
         */
        public void cancel()
        {
            try
            {
                m_socket.close();
            } catch(IOException e)
            {
                m_isUARTConfigured = false;
            }
        }
    }

    private class ConnectedThread extends Thread
    {

        // Сокет соединения
        private final BluetoothSocket m_socket;
        // Поток входных данных
        private final InputStream m_inStream;
        // Поток исходящих данных
        private final OutputStream m_outStream;

        private Handler m_handler = new Handler();

        ConnectedThread(BluetoothSocket socket)
        {

            m_socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Получить входящий и исходящий потоки данных
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();

                m_isUARTConfigured = true;

            } catch(IOException e)
            {
                m_isUARTConfigured = false;
            }
            m_inStream = tmpIn;
            m_outStream = tmpOut;
        }

        // Выполняем запуск прослушки входных данных
        @Override
        public void run()
        {
            read();
        }

        /* Вызываем этот метод из главной деятельности, чтобы отправить данные
        удаленному устройству */
        void write(byte[] bytes)
        {
            try
            {
                m_outStream.write(bytes);
            } catch(IOException e)
            {
                m_isUARTConfigured = false;
            }
        }

        /* Вызываем этот метод из главной деятельности,
        чтобы разорвать соединение */
        void cancel()
        {
            try
            {
                m_socket.close();
                m_isUARTConfigured = false;
            } catch(IOException e)
            {
                m_isUARTConfigured = false;
            }
        }

        private void read()
        {
            byte[] buffer = new byte[1024];// буферный массив
            int bytes;// bytes returned from read()

            // Прослушиваем InputStream пока не произойдет исключение
            while(true)
            {
                try
                {
                    // читаем из InputStream
                    bytes = m_inStream.read(buffer);
                    // посылаем прочитанные байты главной деятельности
                    if(bytes > 0)
                    {
                        m_readDataByte = new byte[bytes];
                        System.arraycopy(buffer, 0, m_readDataByte, 0, bytes);
                        m_handler.post(m_readRunnable);
                    }
                } catch(IOException e)
                {
                    break;
                }
            }
        }
    }
}

