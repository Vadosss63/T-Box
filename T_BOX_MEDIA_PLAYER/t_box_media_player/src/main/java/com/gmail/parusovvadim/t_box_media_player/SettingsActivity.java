package com.gmail.parusovvadim.t_box_media_player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.gmail.parusovvadim.media_directory.MusicFileComparator;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.gmail.parusovvadim.media_directory.MusicFiles.MUSIC_FORMAT;

public class SettingsActivity extends AppCompatActivity
{
    class NodeSettingShow
    {
        String title;
        boolean isFolder;
        boolean isFolderUp;
    }

    class SettingArrayAdapter extends ArrayAdapter<NodeSettingShow>
    {

        SettingArrayAdapter(Context context, List<NodeSettingShow> listItem)
        {
            super(context, R.layout.music_track_item_setting, listItem);
        }

        @SuppressLint ("InflateParams")
        @NotNull
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

    private List<String> m_pathList = null;
    private ListView m_pathView = null;
    private SettingApp m_settingApps;
    //    private String m_currentDir;
//    private TextView m_pathTextView;
    // Компоратор для сортировки дерикторий музыкальных треков
    private final Comparator<File> m_fileComparator = new MusicFileComparator();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        m_settingApps = SettingApp.getInstance();
        m_settingApps.loadSetting();
        getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        m_pathView = findViewById(R.id.pathListSettings);

        getDir(m_settingApps.getAbsolutePath());

    }

    private void getDir(String dirPath)
    {
//        m_pathTextView.setText("Путь: " + m_settingApp.getTrimmPath(dirPath)); // где мы сейчас
//        m_currentDir = dirPath;

        ArrayList<NodeSettingShow> itemList = new ArrayList<>();
        m_pathList = new ArrayList<>();
        File file = new File(dirPath);

        if(file.exists())
        {

            File[] filesArray = file.listFiles(); // получаем список файлов

            // если мы не в корневой папке
            if(!dirPath.equals(m_settingApps.getStorageDirectory()))
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
            getDir(m_settingApps.getStorageDirectory());
            return;
        }

        // Можно выводить на экран список
        ArrayAdapter<NodeSettingShow> adapter = new SettingArrayAdapter(this, itemList);
        m_pathView.setAdapter(adapter);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
    {
        ListPreference m_listPreference;
        private SettingApp m_settingApp;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            m_settingApp = SettingApp.getInstance();
            m_listPreference = findPreference("pathStorage");
            List<String> paths = m_settingApp.getPaths();
            CharSequence[] cs = paths.toArray(new CharSequence[0]);
            m_listPreference.setEntryValues(cs);
            m_listPreference.setEntries(cs);
            m_listPreference.setValue(m_settingApp.getStorageDirectory());
        }


    }
}