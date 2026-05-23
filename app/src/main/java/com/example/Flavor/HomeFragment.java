package com.example.Flavor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class HomeFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private HomePagerAdapter pagerAdapter;
    private FeedFragment feedFragment;
    private SavedFragment savedFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        pagerAdapter = new HomePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Лента");
                            break;
                        case 1:
                            tab.setText("Поиск");
                            break;
                        case 2:
                            tab.setText("Сохранённое");
                            break;
                    }
                }).attach();

        // Получаем ссылки на фрагменты после создания
        viewPager.post(() -> {
            feedFragment = (FeedFragment) pagerAdapter.getFragment(0);
            savedFragment = (SavedFragment) pagerAdapter.getFragment(2);

            // Связываем FeedFragment с SavedFragment через слушатель
            if (feedFragment != null && savedFragment != null) {
                feedFragment.setOnRecipeUnsavedListener(recipeId -> {
                    // Когда в ленте убирают лайк, обновляем SavedFragment
                    if (savedFragment != null && savedFragment.isAdded()) {
                        // SavedFragment автоматически обновится через слушатель Firebase
                        // Дополнительно можно показать уведомление
                    }
                });
            }
        });

        return view;
    }

    public void refreshFeed() {
        if (feedFragment != null) {
            feedFragment.loadRecipes();
        }
    }
}