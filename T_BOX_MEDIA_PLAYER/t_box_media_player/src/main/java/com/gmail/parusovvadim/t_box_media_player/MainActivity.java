package com.gmail.parusovvadim.t_box_media_player;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.gmail.parusovvadim.media_directory.Folder;
import com.gmail.parusovvadim.media_directory.MusicFiles;
import com.gmail.parusovvadim.media_directory.NodeDirectory;
import com.gmail.parusovvadim.media_directory.TrackInfo;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String EXTRA_CMD = "com.gmail.parusovvadim.t_box_media_player.CMD";
    private static final String BROADCAST_ACTION = "com.gmail.parusovvadim.t_box_media_player";
    private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 1;

    private static final int CMD_EXIT = -1;

    private ControllerPlayerFragment m_controllerPlayerFragment = null;

    // ресивер для приема данных от сервиса
    private BroadcastReceiver m_broadcastReceiver = null;

    // дериктория для воспроизведения
    private MusicFiles m_musicFiles = null;
    // Текущая деректория показа
    private NodeDirectory m_currentDirectory = null;
    // Текущий выбранный трек
    private NodeDirectory m_currentTrack = null;

    private final Folder m_backFolder = new Folder("Вверх");

    private final SettingApp m_settingApp = SettingApp.getInstance();

    // адаптер
    private ArrayAdapter<NodeDirectory> m_adapterPlayList = null;

    private ListView m_mainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_settingApp.initSetting(this);
        setContentView(R.layout.activity_main);

        Button scrollButton = findViewById(R.id.scrollButton);
        scrollButton.setOnClickListener(v -> scrollDown());

        final Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> exitApp());
        final Button menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingActivity.class);
            startActivityForResult(intent, 1);
        });

    }

    // Выделение воспроизводимого элемента и выключение приложения со шторки
    private void createBroadcast() {
        // создаем BroadcastReceiver
        m_broadcastReceiver = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                int task = getCMD(intent);
                if (task == CMD_EXIT) exitApp();
            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(m_broadcastReceiver, intentFilter);
    }

    public void selectCurrentTrack(int folder, int track) {

        NodeDirectory trackNode = m_musicFiles.getTrack(folder, track);

        if (trackNode != null) {
            if (m_currentTrack != trackNode) {
                m_currentTrack = trackNode;
                m_adapterPlayList.notifyDataSetChanged();
                scrollToSelectTrack();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // дерегистрируем (выключаем) BroadcastReceiver
        if (m_broadcastReceiver != null) unregisterReceiver(m_broadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionStatus != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {

            if (m_currentTrack == null) {
                m_mainView = findViewById(R.id.playList);
                loudSettings();
                createBroadcast();

            } else {
                scrollToSelectTrack();
            }
        }
    }

    private void exitApp() {
        stopService(new Intent(this, MPlayer.class));
        finish();
        System.exit(0);
    }

    private void createAdapter() {
        if (m_musicFiles.getFolders().isEmpty()) {
            if (m_adapterPlayList != null) m_adapterPlayList.clear();
            return;
        }
        m_currentDirectory = m_musicFiles.getFolders().get(0);

        m_adapterPlayList = new ArrayAdapter<NodeDirectory>(this, R.layout.music_track_item, m_musicFiles.getAllFiles(1)) {

            @SuppressLint("InflateParams")
            @NotNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                NodeDirectory node = getItem(position);
                if (node == null) return convertView;

                if (node.isFolder()) {
                    if (node.isFolderUp()) {
                        convertView = getLayoutInflater().inflate(R.layout.folder_up_item, null);
                    } else {
                        convertView = getLayoutInflater().inflate(R.layout.folder_item, null);
                        TextView titleFolder = convertView.findViewById(R.id.titleFolder);
                        titleFolder.setText(node.getName());
                    }
                } else {

                    convertView = getLayoutInflater().inflate(R.layout.music_track_item, null);

                    TextView trackLabel = convertView.findViewById(R.id.titleTrack);
                    TextView sizeTimeLabel = convertView.findViewById(R.id.sizeTime);
                    ImageView imageView = convertView.findViewById(R.id.trackSelected);

                    String title = node.getName();
                    trackLabel.setText(title);

                    TrackInfo trackInfo = (TrackInfo) node;

                    sizeTimeLabel.setText(getStringTime(trackInfo.getDuration()));

                    boolean isSelected = m_currentTrack == node;

                    imageView.setSelected(isSelected);
                    trackLabel.setSelected(isSelected);
                    sizeTimeLabel.setSelected(isSelected);

                }
                return convertView;
            }
        };

        m_mainView.setAdapter(m_adapterPlayList);
        m_mainView.setOnItemClickListener(this);
        scrollToSelectTrack();
    }

    private void openDirectory() {
        m_adapterPlayList.clear();
        Vector<NodeDirectory> files = new Vector<>();
        NodeDirectory back = m_musicFiles.getParentFolder(m_currentDirectory);
        if (back != null) {
            m_backFolder.setPath(back.getPathDir());
            m_backFolder.setNumber(back.getNumber());
            m_backFolder.setParentNumber(back.getParentNumber());
            m_backFolder.setIsFolderUp(true);
            files.add(m_backFolder);
        }

        files.addAll(m_musicFiles.getAllFiles(m_currentDirectory.getNumber()));
        m_adapterPlayList.addAll(files);
    }

    private void scrollToSelectTrack() {
        int scrollPos = m_adapterPlayList.getPosition(m_currentTrack);
        m_mainView.smoothScrollToPosition(scrollPos);
        m_adapterPlayList.notifyDataSetChanged();
    }

    private void scrollDown() {
        if (m_mainView == null || m_adapterPlayList == null) return;

        m_mainView.smoothScrollByOffset(5);
        m_adapterPlayList.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {   // обработка нажатий на элементах списка
        NodeDirectory nodeDirectory = (NodeDirectory) (parent.getItemAtPosition(position));
        // пока у нас есть треки мы их воспроизводим
        if (nodeDirectory.isFolder()) {
            if (nodeDirectory.isFolderUp())
                m_currentDirectory = m_musicFiles.getFolders().get(nodeDirectory.getNumber() - 1);
            else m_currentDirectory = nodeDirectory;
            openDirectory();
        } else {
            m_currentTrack = nodeDirectory;
            selectedTrack();
            m_adapterPlayList.notifyDataSetChanged();
        }
    }

    // Изменение дериктории воспроизведени
    private void changeRoot() {
        m_musicFiles = MusicFiles.getInstance();
        m_musicFiles.setPathRoot(m_settingApp.getAbsolutePath(), new ReaderTrackInfo());
        Intent intent = MPlayer.newIntentChangeRoot(this);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (resultCode == RESULT_OK) loudSettings();
    }

    private void loudSettings() {
        changeRoot();
        createAdapter();
        if (m_controllerPlayerFragment == null) {
            m_controllerPlayerFragment = new ControllerPlayerFragment();
            changeStateController();
        }
    }

    // Отправка выбранного трека
    private void selectedTrack() {
        int folder = m_currentTrack.getParentNumber();
        int track = m_currentTrack.getNumber();
        m_controllerPlayerFragment.selectTrack(folder, track);
    }

    private void changeStateController() {

        FragmentManager fm = getSupportFragmentManager();
//        m_controllerPlayerFragment = (ControllerPlayerFragment) fm.findFragmentById(R.id.mainFragment);
        //  Fragment fragment = fm.findFragmentById(R.id.mainFragment);
        fm.beginTransaction().add(R.id.mainFragment, m_controllerPlayerFragment).commit();
    }

    private String getStringTime(int mSec) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("m:ss");
        return sdf.format(new Date(mSec));
    }

    // Форммирование интента выход
    public static Intent newIntentExit() {
        Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
        intent.putExtra(EXTRA_CMD, CMD_EXIT);
        return intent;
    }

    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, MainActivity.class);
    }

    private int getCMD(Intent intent) {
        return intent.getIntExtra(EXTRA_CMD, 0);
    }
}