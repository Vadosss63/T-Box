package com.gmail.parusovvadim.t_box_media_player;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.storage.StorageManager;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

class SettingApp {
    static final private SettingApp m_ourInstance = new SettingApp();
    private Context m_context;

    // Путь к папке смузыкой
    private final static String SAVED_MUSIC_PATH = "music_path";
    // Выбор внутреннего или внешнего накопителя
    private final static String SAVED_STORAGE_DIRECTORY = "storage_directory";
    private final String m_defaultPath = "/Music";

    // корневая папка
    private SharedPreferences m_setting;
    private String m_pathMusicFiles;
    private String m_storageDirectory;
    private int m_currentPathStorage = 0;
    private List<String> m_paths;


    private SettingApp() {
    }

    public static SettingApp getInstance() {
        return m_ourInstance;
    }

    public void initSetting(Context context) {
        m_context = context;
        m_paths = getAllPaths();
        changeCurrentPath(0);
        loadSetting();
    }

    int getCurrentPathStorage() {
        return m_currentPathStorage;
    }

    List<String> getPaths() {
        return m_paths;
    }

    void changeCurrentPath(int val) {

        if (val < m_paths.size()) {
            m_storageDirectory = m_paths.get(val);
            m_currentPathStorage = val;
        }
    }

    String getTrimmPath(String path) {
        return path.replace(m_storageDirectory, "");
    }

    String getStorageDirectory() {
        return m_storageDirectory;
    }

    void setMusicPath(String path) {
        m_pathMusicFiles = m_pathMusicFiles.replace(m_storageDirectory, "");
        m_pathMusicFiles = path;
    }

    String getAbsolutePath() {
        if (m_pathMusicFiles.isEmpty()) {
            m_pathMusicFiles = m_defaultPath;
        }

        return m_storageDirectory + m_pathMusicFiles;
    }

    String getMusicPath() {
        return m_pathMusicFiles;
    }

    void saveSetting() {
        m_setting = m_context.getSharedPreferences("Setting", MODE_PRIVATE);
        SharedPreferences.Editor ed = m_setting.edit();
        ed.putString(SAVED_MUSIC_PATH, m_pathMusicFiles);
        ed.putInt(SAVED_STORAGE_DIRECTORY, m_currentPathStorage);
        ed.apply();
    }

    void loadSetting() {

        m_setting = m_context.getSharedPreferences("Setting", MODE_PRIVATE);
        String savedText = m_setting.getString(SAVED_MUSIC_PATH, "");
        m_pathMusicFiles = m_defaultPath;
        File file = new File(savedText);

        // если это папка
        if (file.isDirectory()) {
            m_currentPathStorage = m_setting.getInt(SAVED_STORAGE_DIRECTORY, 0);
            changeCurrentPath(m_currentPathStorage);
            m_pathMusicFiles = getTrimmPath(Objects.requireNonNull(savedText));
        }
    }

    private StorageManager getStorageManager() {
        return (StorageManager) m_context.getSystemService(Context.STORAGE_SERVICE);
    }

    private List<String> getAllPaths() {
        List<String> allPath = new ArrayList<>();

        try {

            Class<?> storageVolumeClass = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = getStorageManager().getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClass.getMethod("getPath");
            Method getState = storageVolumeClass.getMethod("getState");
            Object getVolumeResult = getVolumeList.invoke(getStorageManager());

            final int length = Array.getLength(getVolumeResult);

            for (int i = 0; i < length; i++) {
                Object storageVolumeElem = Array.get(getVolumeResult, i);
                String mountStatus = (String) getState.invoke(storageVolumeElem);
                if (mountStatus != null && mountStatus.equals("mounted")) {
                    String path = (String) getPath.invoke(storageVolumeElem);
                    if (path != null) {
                        allPath.add(path);
                    }
                }
            }

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return allPath;
    }

}
