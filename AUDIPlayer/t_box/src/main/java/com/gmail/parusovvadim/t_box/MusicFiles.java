package com.gmail.parusovvadim.t_box;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

public class MusicFiles {
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

    Vector<NodeDirectory> getFolders() {
        return m_mapFolders;
    }

    MusicFiles(String rootPathFolder) {
        getAllFiles(rootPathFolder, 0);
    }

    NodeDirectory getParentFolder(NodeDirectory childFolder) {
        if (childFolder == null) return null;

        if (childFolder.getParentNumber() == 0) return null;

        return m_mapFolders.get(childFolder.getParentNumber() - 1);
    }

    boolean isEmpty() {
        return m_mapFolders.isEmpty();
    }

    String getPathTrack(int parentNumber, int number) {
        if (m_mapTracks.containsKey(parentNumber)) {
            Vector<NodeDirectory> listTracks = m_mapTracks.get(parentNumber);
            if (number < listTracks.size()) {
                NodeDirectory track = listTracks.get(number);
                return track.getPathDir();
            }
        }
        return "";
    }

    NodeDirectory getTrack(int parentNumber, int number) {
        if (m_mapTracks.containsKey(parentNumber)) {
            Vector<NodeDirectory> listTracks = m_mapTracks.get(parentNumber);
            if (number < listTracks.size() && number >= 0) {
                return listTracks.get(number);
            }
        }
        return null;
    }

    Vector<NodeDirectory> getFolders(int parentFolder) {
        if (m_mapChaldeanFolders.containsKey(parentFolder))
            return m_mapChaldeanFolders.get(parentFolder);

        return new Vector<>();
    }

    Vector<NodeDirectory> getAllFiles(int parentFolder) {
        Vector<NodeDirectory> dataFales = new Vector<NodeDirectory>();
        if (m_mapChaldeanFolders.containsKey(parentFolder))
            dataFales.addAll(m_mapChaldeanFolders.get(parentFolder));

        if (m_mapTracks.containsKey(parentFolder)) dataFales.addAll(m_mapTracks.get(parentFolder));

        return dataFales;
    }

    Vector<NodeDirectory> getTracks(int parentFolder) {
        if (m_mapTracks.containsKey(parentFolder)) return m_mapTracks.get(parentFolder);

        return new Vector<NodeDirectory>();
    }

    int getParentNumber(String dirPath) {
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
            if (filename.endsWith(".mp3") || filename.endsWith(".flac") || filename.endsWith(".m4a")|| filename.endsWith(".wma") || filename.endsWith(".ogg")) {
                Track track = new Track(filename);
                track.setNumber(numberTracks);
                track.setParentNumber(parentFolder.getNumber());
                track.setPath(file.getPath());
                mapTracks.add(track);
                m_mapPaths.put(file.getPath(), track);
                numberTracks++;
            }
        }
        // Устаналиваем количество треков в папке
        parentFolder.setNumberTracks(numberTracks);
        m_mapTracks.put(parentFolder.getNumber(), mapTracks);
        m_mapPaths.put(parentFolder.getPathDir(), parentFolder);
    }

    // Компоратор для сортировки дерикторий музыкальных треков
    private Comparator<? super File> m_fileComparator = (Comparator<File>) (file1, file2) -> {

        if (file1.isDirectory() && !file2.isDirectory()) return -1;

        if (file2.isDirectory() && !file1.isDirectory()) return 1;

        String pathLowerCaseFile1 = file1.getName().toLowerCase();
        String pathLowerCaseFile2 = file2.getName().toLowerCase();
        return String.valueOf(pathLowerCaseFile1).compareTo(pathLowerCaseFile2);
    };
}
