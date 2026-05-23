package com.example.Flavor;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.HashMap;
import java.util.Map;

public class HomePagerAdapter extends FragmentStateAdapter {

    private Map<Integer, Fragment> fragmentMap = new HashMap<>();

    public HomePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new FeedFragment();
                break;
            case 1:
                fragment = new SearchFragment();
                break;
            case 2:
                fragment = new SavedFragment();
                break;
            default:
                fragment = new FeedFragment();
        }
        fragmentMap.put(position, fragment);
        return fragment;
    }

    // Добавь этот метод для доступа к фрагментам
    public Fragment getFragment(int position) {
        return fragmentMap.get(position);
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}