package com.example.Flavor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.Flavor.Models.Recipe;
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

public class SavedFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private List<Recipe> savedRecipeList = new ArrayList<>();

    private FirebaseAuth auth;
    private DatabaseReference recipesRef;
    private DatabaseReference savedRef;
    private FirebaseUser currentUser;

    // Храним ID сохранённых рецептов для быстрого доступа
    private List<String> savedRecipeIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved, container, false);

        recyclerView = view.findViewById(R.id.savedRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyText = view.findViewById(R.id.emptyText);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        recipesRef = FirebaseDatabase.getInstance().getReference("Recipes");
        savedRef = FirebaseDatabase.getInstance().getReference("SavedRecipes");

        adapter = new RecipeAdapter(savedRecipeList,
                recipe -> openRecipeDetail(recipe),
                (recipe, position, isLiked) -> {
                    // Если убрали лайк в SavedFragment - удаляем
                    if (!isLiked) {
                        removeFromSaved(recipe, position);
                    }
                }
        );

        recyclerView.setAdapter(adapter);

        if (currentUser != null) {
            // Слушаем изменения в SavedRecipes в реальном времени
            listenToSavedChanges();
        } else {
            showEmpty(true);
            emptyText.setText("Войдите в аккаунт,\nчтобы увидеть сохранённые рецепты");
        }

        return view;
    }

    private void listenToSavedChanges() {
        String userId = currentUser.getUid();

        // Слушаем изменения в списке сохранённых рецептов
        savedRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                savedRecipeIds.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    savedRecipeIds.add(dataSnapshot.getKey());
                }

                // Загружаем актуальные рецепты
                loadSavedRecipes();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSavedRecipes() {
        showLoading(true);

        if (savedRecipeIds.isEmpty()) {
            savedRecipeList.clear();
            adapter.updateRecipes(savedRecipeList);
            showLoading(false);
            showEmpty(true);
            return;
        }

        recipesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                savedRecipeList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String recipeId = dataSnapshot.getKey();

                    if (savedRecipeIds.contains(recipeId)) {
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                        if (map != null) {
                            Recipe recipe = new Recipe(map);
                            recipe.setId(recipeId);
                            recipe.setSaved(true);
                            savedRecipeList.add(0, recipe);
                        }
                    }
                }

                adapter.updateRecipes(savedRecipeList);
                showLoading(false);
                showEmpty(savedRecipeList.isEmpty());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(getContext(), "Ошибка загрузки рецептов", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFromSaved(Recipe recipe, int position) {
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        savedRef.child(userId).child(recipe.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Рецепт автоматически удалится из списка через слушатель

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка удаления", Toast.LENGTH_SHORT).show();
                    adapter.updateLikeStatus(position, true);
                });
    }

    private void openRecipeDetail(Recipe recipe) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openRecipeDetail(recipe);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmpty(boolean isEmpty) {
        if (emptyText != null) {
            emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }
}