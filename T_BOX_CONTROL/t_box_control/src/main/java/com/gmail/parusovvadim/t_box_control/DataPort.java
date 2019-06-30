package com.gmail.parusovvadim.t_box_control;

import android.content.Context;

public interface DataPort {

    Boolean initialisation(Context context);

    void connect();

    void disconnect();

    void writeData(byte[] bytes);

    void runReadData();

    void setReadRunnable(Runnable runnable);

    boolean isConnected();

    boolean isConfigured();

    String getTextLog();

    byte[] getReadDataByte();

    boolean checkConnection();
}
