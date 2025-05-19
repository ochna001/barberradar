package com.example.barberradar.ui.admin;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.barberradar.ui.admin.tabs.AllShopsFragment;
import com.example.barberradar.ui.admin.tabs.PendingShopsFragment;
import com.example.barberradar.ui.admin.tabs.UsersFragment;

public class AdminPagerAdapter extends FragmentStateAdapter {

    public AdminPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return the appropriate fragment for each tab
        switch (position) {
            case 0:
                return new PendingShopsFragment();
            case 1:
                return new UsersFragment();
            case 2:
                return new AllShopsFragment();
            default:
                return new PendingShopsFragment();
        }
    }

    @Override
    public int getItemCount() {
        // Return the number of tabs
        return 3;
    }
}
