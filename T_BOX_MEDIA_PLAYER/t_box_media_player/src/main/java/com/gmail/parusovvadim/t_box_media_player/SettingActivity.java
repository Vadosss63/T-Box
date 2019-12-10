package com.gmail.parusovvadim.t_box_media_player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.gmail.parusovvadim.media_directory.MusicFileComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.gmail.parusovvadim.media_directory.MusicFiles.MUSIC_FORMAT;

public class SettingActivity extends AppCompatActivity implements AdapterView.OnItemClickListener
{

    private ChangeFolderFragment m_changeFolderFragment = null;
    private List<String> m_pathList = null;
    private ListView m_pathView = null;
    private SettingApp m_settingApp;
    private String m_currentDir;
    private TextView m_pathTextView;
    // Компоратор для сортировки дерикторий музыкальных треков
    private final Comparator<File> m_fileComparator = new MusicFileComparator();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        m_settingApp = SettingApp.getInstance();
        m_changeFolderFragment = new ChangeFolderFragment();
        m_pathView = findViewById(R.id.pathList);
        m_pathView.setOnItemClickListener(this);
        m_pathTextView = findViewById(R.id.pathMusicFiles);

        loadSetting();
        getDir(m_settingApp.getAbsolutePath()); // выводим список файлов и папок в корневой папке системы

        createAdapterSpinner();

    }

    private void createAdapterSpinner()
    {
        final Spinner spinner = findViewById(R.id.pathStorage);
        ArrayAdapter<String> m_adapter = new ArrayAdapter<>(this, R.layout.list_item, m_settingApp.getPaths());

        spinner.setAdapter(m_adapter);
        spinner.setSelection(m_settingApp.getCurrentPathStorage());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                m_settingApp.changeCurrentPath(i);
                getDir(m_settingApp.getStorageDirectory());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {

            }
        });
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        changeStateSelectRoot();
    }

    private void changeStateSelectRoot()
    {
        FragmentTransaction m_fragmentTransaction = getSupportFragmentManager().beginTransaction();
        m_fragmentTransaction.replace(R.id.settingFragment, m_changeFolderFragment);
        m_fragmentTransaction.commit();
    }

    private void getDir(String dirPath)
    {
        m_pathTextView.setText("Путь: " + m_settingApp.getTrimmPath(dirPath)); // где мы сейчас
        m_currentDir = dirPath;

        ArrayList<NodeSettingShow> itemList = new ArrayList<>();
        m_pathList = new ArrayList<>();
        File file = new File(dirPath);

        if(file.exists())
        {

            File[] filesArray = file.listFiles(); // получаем список файлов

            // если мы не в корневой папке
            if(!dirPath.equals(m_settingApp.getStorageDirectory()))
            {
                NodeSettingShow upFolder = new NodeSettingShow();
                upFolder.title = "../";
                upFolder.isFolder = true;
                upFolder.isFolderUp = true;
                itemList.add(upFolder);
                m_pathList.add(file.getParent());
            }

            if(filesArray == null) return;

            Arrays.sort(filesArray, m_fileComparator);

            // формируем список папок и файлов для передачи адаптеру
            for(File aFilesArray : filesArray)
            {
                file = aFilesArray;
                String filename = file.getName();

                // Работаем только с доступными папками и файлами
                if(!file.isHidden() && file.canRead()) if(file.isDirectory())
                {
                    m_pathList.add(file.getPath());
                    NodeSettingShow folder = new NodeSettingShow();
                    folder.title = file.getName();
                    folder.isFolder = true;
                    folder.isFolderUp = false;
                    itemList.add(folder);

                } else
                {
                    for(String musicFormat : MUSIC_FORMAT)
                        if(filename.endsWith(musicFormat))
                        {
                            m_pathList.add(file.getPath());

                            NodeSettingShow track = new NodeSettingShow();
                            track.title = file.getName();
                            track.isFolder = false;
                            track.isFolderUp = false;

                            itemList.add(track);
                            break;
                        }
                }
            }
        } else
        {
            getDir(m_settingApp.getStorageDirectory());
            return;
        }

        // Можно выводить на экран список
        ArrayAdapter<NodeSettingShow> adapter = new SettingArrayAdapter(this, itemList);
        m_pathView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        // обработка нажатий на элементах списка
        File file = new File(m_pathList.get(position));
        // если это папка
        if(file.isDirectory())
        {
            if(file.canRead()) // если она доступна для просмотра, то заходим в неё
            {
                getDir(m_pathList.get(position));
            } else
            { // если папка закрыта, то сообщаем об этом
                new AlertDialog.Builder(this).setIcon(R.mipmap.ic_launcher).setTitle("[" + file.getName() + "] папка не доступна!").setPositiveButton("OK", (dialog, which)->{
                }).show();
            }
        }
    }

    void saveSetting()
    {
        m_settingApp.setMusicPath(m_currentDir);
        m_settingApp.saveSetting();
        m_settingApp.loadSetting();
    }

    private void loadSetting()
    {
        m_settingApp.loadSetting();
        m_currentDir = m_settingApp.getMusicPath();
    }


    class SettingArrayAdapter extends ArrayAdapter<NodeSettingShow>
    {

        SettingArrayAdapter(Context context, List<NodeSettingShow> listItem)
        {
            super(context, R.layout.music_track_item_setting, listItem);
        }

        @SuppressLint ("InflateParams")
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent)
        {
            NodeSettingShow node = getItem(position);
            if(node == null) return convertView;

            if(node.isFolder)
            {
                if(node.isFolderUp)
                {
                    convertView = getLayoutInflater().inflate(R.layout.folder_up_item, null);
                } else
                {
                    convertView = getLayoutInflater().inflate(R.layout.folder_item_setting, null);
                    TextView titleFolder = convertView.findViewById(R.id.titleFolderSetting);
                    titleFolder.setText(node.title);
                }
            } else
            {

                convertView = getLayoutInflater().inflate(R.layout.music_track_item_setting, null);
                TextView trackLabel = convertView.findViewById(R.id.titleTrackSetting);
                String title = node.title;
                trackLabel.setText(title);
            }
            return convertView;
        }
    }

    class NodeSettingShow
    {
        String title;
        boolean isFolder;
        boolean isFolderUp;
    }
}

