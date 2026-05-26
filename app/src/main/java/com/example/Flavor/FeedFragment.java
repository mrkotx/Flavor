package com.example.Flavor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.Flavor.Models.Recipe;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeedFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private List<Recipe> recipeList = new ArrayList<>();
    private String currentCategory = "Все";
    private LinearLayout categoriesContainer;

    private FirebaseAuth auth;
    private DatabaseReference recipesRef;
    private DatabaseReference savedRef;
    private FirebaseUser currentUser;

    private OnRecipeUnsavedListener unsavedListener;

    public interface OnRecipeUnsavedListener {
        void onRecipeUnsaved(String recipeId);
    }

    public void setOnRecipeUnsavedListener(OnRecipeUnsavedListener listener) {
        this.unsavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        recyclerView = view.findViewById(R.id.feedRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyText = view.findViewById(R.id.emptyText);
        categoriesContainer = view.findViewById(R.id.categoriesContainer);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        recipesRef = FirebaseDatabase.getInstance().getReference("Recipes");
        savedRef = FirebaseDatabase.getInstance().getReference("SavedRecipes");

        adapter = new RecipeAdapter(recipeList,
                recipe -> openRecipeDetail(recipe),
                (recipe, position, isLiked) -> {
                    if (currentUser == null) {
                        Toast.makeText(getContext(), "Войдите в аккаунт, чтобы сохранять рецепты", Toast.LENGTH_SHORT).show();
                        adapter.updateLikeStatus(position, false);
                        return;
                    }
                    if (isLiked) {
                        saveToSavedRecipes(recipe, position);
                    } else {
                        removeFromSavedRecipes(recipe, position);
                    }
                }
        );

        recyclerView.setAdapter(adapter);
        setupCategories();
        loadRecipes();

        return view;
    }

    private void setupCategories() {
        String[] categories = {"Все", "Завтрак", "Обед", "Ужин", "Перекус"};

        for (String category : categories) {
            addCategoryButton(category);
        }
    }

    private void addCategoryButton(String categoryName) {
        MaterialButton button = new MaterialButton(getContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(16);
        button.setLayoutParams(params);

        button.setText(categoryName);
        button.setTag(categoryName);
        button.setCornerRadius(24);
        button.setPadding(32, 12, 32, 12);
        button.setAllCaps(false);

        button.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.inactive_button));
        button.setTextColor(ContextCompat.getColor(getContext(), R.color.text_category));

        button.setOnClickListener(v -> {
            for (int i = 0; i < categoriesContainer.getChildCount(); i++) {
                View child = categoriesContainer.getChildAt(i);
                if (child instanceof MaterialButton) {
                    MaterialButton btn = (MaterialButton) child;
                    btn.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.inactive_button));
                    btn.setTextColor(ContextCompat.getColor(getContext(), R.color.text_category));
                }
            }

            button.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.active_button));
            button.setTextColor(ContextCompat.getColor(getContext(), R.color.white));

            currentCategory = categoryName;
            filterRecipes();
        });

        categoriesContainer.addView(button);

        if (categoryName.equals("Все")) {
            button.performClick();
        }
    }

    private void filterRecipes() {
        if (currentCategory.equals("Все")) {
            adapter.updateRecipes(recipeList);
            checkEmpty();
            return;
        }

        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : recipeList) {
            String recipeCategory = recipe.getCategoryId();
            if (currentCategory.equals(recipeCategory)) {
                filtered.add(recipe);
            }
        }
        adapter.updateRecipes(filtered);
        checkEmpty();
    }

    public void loadRecipes() {
        showLoading(true);

        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipeList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map != null) {
                        Recipe recipe = new Recipe(map);
                        recipe.setId(dataSnapshot.getKey());
                        recipeList.add(0, recipe);
                    }
                }
                checkSavedStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(getContext(), "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkSavedStatus() {
        if (currentUser == null) {
            adapter.updateRecipes(recipeList);
            showLoading(false);
            checkEmpty();
            return;
        }

        savedRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (Recipe recipe : recipeList) {
                    recipe.setSaved(snapshot.hasChild(recipe.getId()));
                }
                filterRecipes();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                filterRecipes();
                showLoading(false);
            }
        });
    }

    private void saveToSavedRecipes(Recipe recipe, int position) {
        savedRef.child(currentUser.getUid()).child(recipe.getId()).setValue(true)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                    adapter.updateLikeStatus(position, false);
                });
    }

    private void removeFromSavedRecipes(Recipe recipe, int position) {
        savedRef.child(currentUser.getUid()).child(recipe.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (unsavedListener != null) {
                        unsavedListener.onRecipeUnsaved(recipe.getId());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                    adapter.updateLikeStatus(position, true);
                });
    }

    private void openRecipeDetail(Recipe recipe) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openRecipeDetail(recipe);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showEmpty(boolean isEmpty) {
        if (emptyText != null) emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void checkEmpty() {
        showEmpty(adapter.getItemCount() == 0);
    }
}