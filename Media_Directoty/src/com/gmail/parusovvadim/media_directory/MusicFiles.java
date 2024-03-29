package com.gmail.parusovvadim.media_directory;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

public class MusicFiles {
    // Поддерживаемые форматы
    public static final String[] MUSIC_FORMAT = new String[]{".mp3", ".flac", ".m4a", ".wma", ".ogg"};
    // Компоратор для сортировки дерикторий музыкальных треков
    private static Comparator<File> m_fileComparator = new MusicFileComparator();

    private static MusicFiles m_ourInstance = new MusicFiles();


    // Номер для новой папки
    private int m_newFolderNumber = 0;
    // Мап для хранения папок
    private Vector<NodeDirectory> m_mapFolders = new Vector<>();
    // Мап для хранения треков
    private HashMap<Integer, Vector<NodeDirectory>> m_mapTracks = new HashMap<>();
    // Мап для папок
    private HashMap<Integer, Vector<NodeDirectory>> m_mapChaldeanFolders = new HashMap<>();
    // Мар с для доступа к по пути
    private HashMap<String, NodeDirectory> m_mapPaths = new HashMap<>();

    private String m_rootPathFolder = "";

    private void CleanFiles() {
        // Номер для новой папки
        m_newFolderNumber = 0;
        m_mapFolders = new Vector<>();
        m_mapTracks = new HashMap<>();
        m_mapChaldeanFolders = new HashMap<>();
        m_mapPaths = new HashMap<>();
    }

    private MusicFiles() {
    }

    public static MusicFiles getInstance() {
        return m_ourInstance;
    }

    public void setPathRoot(String rootPathFolder) {
        if (!m_rootPathFolder.isEmpty() && m_rootPathFolder.equals(rootPathFolder)) return;
        m_rootPathFolder = rootPathFolder;
        CleanFiles();
        IReaderTrackInfo.setReaderTrackInfo(null);
        getAllFiles(rootPathFolder, 0);
    }

    public void setPathRoot(String rootPathFolder, TrackInfo readerTrackInfo) {

        if (!m_rootPathFolder.isEmpty() && m_rootPathFolder.equals(rootPathFolder)) return;
        m_rootPathFolder = rootPathFolder;
        CleanFiles();
        IReaderTrackInfo.setReaderTrackInfo(readerTrackInfo);
        getAllFiles(rootPathFolder, 0);
        //Запуск прогрузки информации
        LouderThread louderThread = new LouderThread();
        louderThread.start();
    }

    private class LouderThread extends Thread {
        @Override
        public void run() {
            loudInfo();
        }
    }

    // выполнение загрузки данных
    private void loudInfo() {
        Vector<NodeDirectory> folders = getFolders();
        for (NodeDirectory folder : folders) {
            Vector<NodeDirectory> tracks = getTracks(folder.getNumber());
            for (NodeDirectory track : tracks)
                ((TrackInfo) track).initInfo();
        }
    }


    public Vector<NodeDirectory> getFolders() {
        return m_mapFolders;
    }

    public NodeDirectory getParentFolder(NodeDirectory childFolder) {
        if (childFolder == null) return null;

        if (childFolder.getParentNumber() == 0) return null;

        return m_mapFolders.get(childFolder.getParentNumber() - 1);
    }

    public boolean isEmpty() {
        return m_mapFolders.isEmpty();
    }

    public String getPathTrack(int parentNumber, int number) {
        if (m_mapTracks.containsKey(parentNumber)) {
            Vector<NodeDirectory> listTracks = m_mapTracks.get(parentNumber);
            if (number < listTracks.size()) {
                NodeDirectory track = listTracks.get(number);
                return track.getPathDir();
            }
        }
        return "";
    }

    public NodeDirectory getTrack(int parentNumber, int number) {
        if (m_mapTracks.containsKey(parentNumber)) {
            Vector<NodeDirectory> listTracks = m_mapTracks.get(parentNumber);
            if (number < listTracks.size() && number >= 0) {
                return listTracks.get(number);
            }
        }
        return null;
    }

    public Vector<NodeDirectory> getFolders(int parentFolder) {
        if (m_mapChaldeanFolders.containsKey(parentFolder))
            return m_mapChaldeanFolders.get(parentFolder);
        return new Vector<>();
    }

    public Vector<NodeDirectory> getAllFiles(int parentFolder) {
        Vector<NodeDirectory> dataFales = new Vector<NodeDirectory>();
        if (m_mapChaldeanFolders.containsKey(parentFolder))
            dataFales.addAll(m_mapChaldeanFolders.get(parentFolder));

        if (m_mapTracks.containsKey(parentFolder)) dataFales.addAll(m_mapTracks.get(parentFolder));

        return dataFales;
    }

    public Vector<NodeDirectory> getTracks(int parentFolder) {
        if (m_mapTracks.containsKey(parentFolder)) return m_mapTracks.get(parentFolder);

        return new Vector<NodeDirectory>();
    }

    public int getParentNumber(String dirPath) {
        NodeDirectory node = m_mapPaths.get(dirPath);
        return (node != null) ? node.getParentNumber() : -1;
    }

    private int getNumber(String dirPath) {
        NodeDirectory node = m_mapPaths.get(dirPath);
        return (node != null) ? node.getNumber() : -1;
    }

    private int getNumberTracks(int number) {
        NodeDirectory folder = m_mapFolders.get(number);
        return folder.getNumberTracks();
    }

    public int getNumberTracks(String dirPath) {
        return getNumberTracks(getNumber(dirPath));
    }

    // выполняет чтение папок с музыкой
    private void getAllFiles(String dirPath, int parentIndex) {
        // Читаем дерикторию Получаем список файлов
        File parentFile = new File(dirPath);

        if (!parentFile.exists())
            return;

        File[] listFiles = new File(dirPath).listFiles();

        if (listFiles == null) return;

        if (listFiles.length == 0)
            return;

        Arrays.sort(listFiles, m_fileComparator);

        // Заполняем родительскую папку
        Folder parentFolder = new Folder(parentFile.getName());
        // устонавливаем родительскую папку
        parentFolder.setParentNumber(parentIndex);
        m_newFolderNumber++;
        // устонавливаем номер папки
        parentFolder.setNumber(m_newFolderNumber);
        // устанавливаем путь к папке
        parentFolder.setPath(parentFile.getPath());
        // количество дорожек в папке
        int numberTracks = 0;

        // Задаем папку
        m_mapFolders.add(parentFolder);

        // Задаем нулевую папу
        if (!m_mapChaldeanFolders.containsKey(parentIndex)) {
            Vector<NodeDirectory> list = new Vector<>();
            m_mapChaldeanFolders.put(parentIndex, list);
        }
        m_mapChaldeanFolders.get(parentIndex).add(parentFolder);

        Vector<NodeDirectory> mapTracks = new Vector<>();
        // формируем список папок и файлов
        for (File file : listFiles) {
            // Работаем только с доступными папками и файлами
            if (file.isHidden() && !file.canRead()) continue;

            // проверяем файл дериктория???
            if (file.isDirectory()) {
                // спускаемся рекурсивно читая вложенное содержимое
                getAllFiles(file.getPath(), parentFolder.getNumber());
                continue;
            }
            // проверяем типы файлов
            String filename = file.getName();

            for (String musicFormat : MUSIC_FORMAT) {
                if (filename.endsWith(musicFormat)) {
                    filename = filename.replace(musicFormat, "");
                    Track track = new Track(filename);
                    track.setNumber(numberTracks);
                    track.setParentNumber(parentFolder.getNumber());
                    track.setPath(file.getPath());
                    mapTracks.add(track);
                    m_mapPaths.put(file.getPath(), track);
                    numberTracks++;
                    break;
                }
            }

        }
        // Устаналиваем количество треков в папке
        parentFolder.setNumberTracks(numberTracks);
        m_mapTracks.put(parentFolder.getNumber(), mapTracks);
        m_mapPaths.put(parentFolder.getPathDir(), parentFolder);
    }

}
