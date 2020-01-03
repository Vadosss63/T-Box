package com.gmail.parusovvadim.t_box_control;

import android.content.Context;

import androidx.preference.PreferenceManager;

public class SettingPreferences {
    private static final String PREF_TITLE_TRANSLATE = "isTitleTranslate";
    private static final String PREF_TAG_TITLE_TRANSLATE = "isTagTitleTranslate";

    public static Boolean getStoreIsTitleTranslate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_TITLE_TRANSLATE, true);
    }

    public static Boolean getStoreIsTagTitleTranslate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_TAG_TITLE_TRANSLATE, false);
    }

    public static void setStoreIsTitleTranslate(Context context, Boolean isTitleTranslate) {

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_TITLE_TRANSLATE, isTitleTranslate)
                .apply();
    }

    public static void setStoreIsTagTitleTranslate(Context context, Boolean isTagTitleTranslate) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_TAG_TITLE_TRANSLATE, isTagTitleTranslate)
                .apply();
    }
}
