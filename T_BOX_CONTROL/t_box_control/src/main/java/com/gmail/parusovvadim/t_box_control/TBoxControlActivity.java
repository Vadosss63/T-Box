package com.gmail.parusovvadim.t_box_control;

import androidx.fragment.app.Fragment;

public class TBoxControlActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new TBoxControlFragment();
    }
}