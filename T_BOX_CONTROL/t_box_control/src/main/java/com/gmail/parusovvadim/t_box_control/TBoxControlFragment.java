package com.gmail.parusovvadim.t_box_control;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TBoxControlFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.BRAND.equalsIgnoreCase("xiaomi")) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tbox_control, container, false);
        createActions(view);
        sync();
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createActions(View viewFragment) {

        FloatingActionButton play = viewFragment.findViewById(R.id.play);
        play.setOnClickListener(view -> sendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, -1));
        FloatingActionButton prev = viewFragment.findViewById(R.id.prev);
        prev.setOnClickListener(view -> sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, -1));

        FloatingActionButton next = viewFragment.findViewById(R.id.next);
        next.setOnClickListener(view -> sendKey(KeyEvent.KEYCODE_MEDIA_NEXT, -1));

        FloatingActionButton rew = viewFragment.findViewById(R.id.rew);
        rew.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // нажатие
                    sendKey(KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.ACTION_DOWN);

                    break;
                case MotionEvent.ACTION_MOVE: // движение

                    break;
                case MotionEvent.ACTION_UP: // отпускание
                case MotionEvent.ACTION_CANCEL:
                    sendKey(KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.ACTION_UP);
                    break;
            }
            return true;
        });

        FloatingActionButton media_ff = viewFragment.findViewById(R.id.media_ff);
        media_ff.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // нажатие
                    sendKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.ACTION_DOWN);

                    break;
                case MotionEvent.ACTION_MOVE: // движение

                    break;
                case MotionEvent.ACTION_UP: // отпускание
                case MotionEvent.ACTION_CANCEL:
                    sendKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.ACTION_UP);
                    break;
            }
            return true;
        });

        FloatingActionButton sync = viewFragment.findViewById(R.id.sync);
        sync.setOnClickListener(view -> sync());

        FloatingActionButton exit = viewFragment.findViewById(R.id.exit);
        exit.setOnClickListener(view -> exitApp());

    }

    private void exitApp() {
        Intent intent = ReceiverService.newIntent(getActivity());
        getActivity().stopService(intent);
        Intent intentUART = new Intent(getActivity(), UARTService.class);
        getActivity().stopService(intentUART);
        getActivity().finish();
    }

    private void sync() {
        StartService.start(getActivity(), ReceiverService.newSyncIntent(getActivity()));
    }

    private void sendKey(int keycodeMedia, int action) {
        Intent intent = ReceiverService.newPressKeyIntent(getActivity(), keycodeMedia, action);
        StartService.start(getActivity(), intent);
    }
}
