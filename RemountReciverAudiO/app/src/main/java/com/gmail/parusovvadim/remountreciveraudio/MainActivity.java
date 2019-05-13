package com.gmail.parusovvadim.remountreciveraudio;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CreateActions();
        Sync();
        if (Build.BRAND.equalsIgnoreCase("xiaomi")) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void CreateActions() {

        FloatingActionButton play = findViewById(R.id.play);
        play.setOnClickListener(view -> SendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, -1));
        FloatingActionButton prev = findViewById(R.id.prev);
        prev.setOnClickListener(view -> SendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, -1));

        FloatingActionButton next = findViewById(R.id.next);
        next.setOnClickListener(view -> SendKey(KeyEvent.KEYCODE_MEDIA_NEXT, -1));

        FloatingActionButton rew = findViewById(R.id.rew);
        rew.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // нажатие
                    SendKey(KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.ACTION_DOWN);

                    break;
                case MotionEvent.ACTION_MOVE: // движение

                    break;
                case MotionEvent.ACTION_UP: // отпускание
                case MotionEvent.ACTION_CANCEL:
                    SendKey(KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.ACTION_UP);
                    break;
            }
            return true;
        });

        FloatingActionButton media_ff = findViewById(R.id.media_ff);
        media_ff.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // нажатие
                    SendKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.ACTION_DOWN);

                    break;
                case MotionEvent.ACTION_MOVE: // движение

                    break;
                case MotionEvent.ACTION_UP: // отпускание
                case MotionEvent.ACTION_CANCEL:
                    SendKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.ACTION_UP);
                    break;
            }
            return true;
        });

        FloatingActionButton sync = findViewById(R.id.sync);
        sync.setOnClickListener(view -> Sync());
    }

    private void Sync() {

        Intent intent = new Intent(this, ReceiverService.class);
        intent.putExtra("CMD", ReceiverService.CMD_SYNC);
        startService(intent);
    }

    private void SendKey(int keycodeMedia, int action) {
        Intent intent = new Intent(this, ReceiverService.class);
        intent.putExtra("CMD", ReceiverService.CMD_MEDIA_KEY);
        intent.putExtra("keycodeMedia", keycodeMedia);
        intent.putExtra("action", action);
        startService(intent);
    }

}
