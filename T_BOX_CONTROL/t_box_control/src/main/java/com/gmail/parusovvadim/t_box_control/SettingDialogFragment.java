package com.gmail.parusovvadim.t_box_control;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SettingDialogFragment extends DialogFragment {

    private Switch mIsTitleTranslateSwitch;
    private Switch mIsTagTitleTranslateSwitch;

    public static SettingDialogFragment newInstance() {
        return new SettingDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_setting, null);
        mIsTitleTranslateSwitch = view.findViewById(R.id.is_tag_title_translate);
        mIsTagTitleTranslateSwitch = view.findViewById(R.id.is_title_translate);
        loudSetting();
        mIsTitleTranslateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingPreferences.setStoreIsTitleTranslate(getActivity(), isChecked);
            }
        });

        mIsTagTitleTranslateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingPreferences.setStoreIsTagTitleTranslate(getActivity(), isChecked);
            }
        });
        return new AlertDialog.Builder(getActivity()).setView(view).setTitle(R.string.setting_title).create();
    }

    private void loudSetting() {
        mIsTitleTranslateSwitch.setChecked(SettingPreferences.getStoreIsTitleTranslate(getActivity()));
        mIsTagTitleTranslateSwitch.setChecked(SettingPreferences.getStoreIsTagTitleTranslate(getActivity()));
    }
}