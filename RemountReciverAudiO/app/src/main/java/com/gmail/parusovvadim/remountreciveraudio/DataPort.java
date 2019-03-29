package com.gmail.parusovvadim.remountreciveraudio;

import android.content.Context;

public interface DataPort {

    Boolean Initialisation(Context context);

    void Connect();

    void Disconnect();

    void WriteData(byte[] bytes);

    void RunReadData();

    void SetReadRunnable(Runnable runnable);

    boolean IsConnected();

    boolean IsConfigured();

    String GetTextLog();

    byte[] GetReadDataByte();

    boolean CheckConnection();
}
