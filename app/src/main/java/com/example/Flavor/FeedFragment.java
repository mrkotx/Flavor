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

public class FeedFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private List<Recipe> recipeList = new ArrayList<>();

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference recipesRef;
    private DatabaseReference savedRef;
    private FirebaseUser currentUser;

    // Слушатель для обновления SavedFragment
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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        recipesRef = FirebaseDatabase.getInstance().getReference("Recipes");
        savedRef = FirebaseDatabase.getInstance().getReference("SavedRecipes");

        // Настройка адаптера
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

        loadRecipes();

        return view;
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

                // Проверяем статус сохранения для каждого рецепта
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

        String userId = currentUser.getUid();

        savedRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (Recipe recipe : recipeList) {
                    recipe.setSaved(snapshot.hasChild(recipe.getId()));
                }
                adapter.updateRecipes(recipeList);
                showLoading(false);
                checkEmpty();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                adapter.updateRecipes(recipeList);
                showLoading(false);
                checkEmpty();
            }
        });
    }

    private void saveToSavedRecipes(Recipe recipe, int position) {
        String userId = currentUser.getUid();

        savedRef.child(userId).child(recipe.getId()).setValue(true)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                    adapter.updateLikeStatus(position, false);
                });
    }

    private void removeFromSavedRecipes(Recipe recipe, int position) {
        String userId = currentUser.getUid();

        savedRef.child(userId).child(recipe.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {


                    // Уведомляем SavedFragment об удалении
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
        Toast.makeText(getContext(), "Открыть: " + recipe.getTitle(), Toast.LENGTH_SHORT).show();
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

    private void checkEmpty() {
        showEmpty(recipeList.isEmpty());
    }
}