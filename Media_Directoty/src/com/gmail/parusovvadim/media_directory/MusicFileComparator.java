package com.gmail.parusovvadim.media_directory;

import java.io.File;
import java.util.Comparator;

public class MusicFileComparator implements Comparator<File> {

    @Override
    public int compare(File file1, File file2) {
        if (file1.isDirectory() && !file2.isDirectory()) return -1;

        if (file2.isDirectory() && !file1.isDirectory()) return 1;

        String pathLowerCaseFile1 = file1.getName().toLowerCase();
        String pathLowerCaseFile2 = file2.getName().toLowerCase();
        return pathLowerCaseFile1.compareTo(pathLowerCaseFile2);
    }
}
