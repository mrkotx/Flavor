package com.example.Flavor;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.view.View;
import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.Flavor.Models.Recipe;

public class MainActivity extends AppCompatActivity {

    private MaterialButton home_button;
    private MaterialButton add_button;
    private MaterialButton profile_button;

    private FrameLayout contentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        home_button = findViewById(R.id.home_button);
        add_button = findViewById(R.id.add_button);
        profile_button = findViewById(R.id.profile_button);

        contentContainer = findViewById(R.id.contentContainer);

        home_button.setOnClickListener(v -> showHomeFragment());
        add_button.setOnClickListener(v -> showAddFragment());
        profile_button.setOnClickListener(v -> showProfileFragment());

        showHomeFragment();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void showHomeFragment() {
        HomeFragment fragment = new HomeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, fragment)
                .commit();
        updateButtonState(home_button);
    }

    private void showAddFragment() {
        AddFragment fragment = new AddFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, fragment)
                .commit();
        updateButtonState(add_button);
    }

    public void openRecipeDetail(Recipe recipe) {
        RecipeDetailFragment fragment = RecipeDetailFragment.newInstance(recipe);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void refreshFeed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.contentContainer);
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).refreshFeed();
        }
    }

    public void showProfileFragment() {
        ProfileFragment fragment = new ProfileFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, fragment)
                .commit();
        updateButtonState(profile_button);
    }

    private void updateButtonState(MaterialButton activeButton) {
        MaterialButton[] buttons = {home_button, add_button, profile_button};
        for (MaterialButton button : buttons) {
            if (button == activeButton) {
                button.setBackgroundTintList(getColorStateList(R.color.active_button));
            } else {
                button.setBackgroundTintList(getColorStateList(R.color.inactive_button));
            }
        }
    }
}
