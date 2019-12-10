package com.gmail.parusovvadim.t_box_media_player;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class ChangeFolderFragment extends Fragment
{
    private View m_view = null;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if(m_view == null)
        {
            m_view = inflater.inflate(R.layout.fragment_dialog, null);
            CreateButtons();
        }
        return m_view;
    }

    private void CreateButtons()
    {
        Button previousButton = m_view.findViewById(R.id.okButton);
        previousButton.setOnClickListener((View v)->{
            SettingActivity sa = ((SettingActivity) getActivity());
            sa.saveSetting();
            Intent intent = new Intent();
            sa.setResult(AppCompatActivity.RESULT_OK, intent);
            sa.finish();
        });

        Button playButton = m_view.findViewById(R.id.cancelButton);
        playButton.setOnClickListener((View v)->{
            SettingActivity sa = ((SettingActivity) getActivity());
            Intent intent = new Intent();
            sa.setResult(AppCompatActivity.RESULT_CANCELED, intent);
            sa.finish();
        });

    }
}
