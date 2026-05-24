package com.example.Flavor;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchFragment extends Fragment {

    private TextInputEditText searchInput;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;

    private RecipeAdapter adapter;
    private List<Recipe> allRecipes = new ArrayList<>();
    private List<Recipe> filteredRecipes = new ArrayList<>();

    private FirebaseAuth auth;
    private DatabaseReference recipesRef;
    private DatabaseReference savedRef;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        searchInput = view.findViewById(R.id.searchInput);
        recyclerView = view.findViewById(R.id.searchRecyclerView);
        progressBar = view.findViewById(R.id.searchProgressBar);
        emptyText = view.findViewById(R.id.searchEmptyText);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        recipesRef = FirebaseDatabase.getInstance().getReference("Recipes");
        savedRef = FirebaseDatabase.getInstance().getReference("SavedRecipes");

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecipeAdapter(filteredRecipes,
                recipe -> openRecipeDetail(recipe),
                (recipe, position, isLiked) -> {
                    if (currentUser == null) {
                        Toast.makeText(getContext(), "Войдите в аккаунт, чтобы сохранять рецепты", Toast.LENGTH_SHORT).show();
                        adapter.updateLikeStatus(position, false);
                        return;
                    }
                    if (isLiked) {
                        savedRef.child(currentUser.getUid()).child(recipe.getId()).setValue(true);
                    } else {
                        savedRef.child(currentUser.getUid()).child(recipe.getId()).removeValue();
                    }
                });
        recyclerView.setAdapter(adapter);

        loadAllRecipes();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecipes(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadAllRecipes() {
        progressBar.setVisibility(View.VISIBLE);
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRecipes.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) ds.getValue();
                    if (map != null) {
                        Recipe recipe = new Recipe(map);
                        recipe.setId(ds.getKey());
                        allRecipes.add(0, recipe);
                    }
                }
                checkSavedStatus();
                progressBar.setVisibility(View.GONE);
                // Re-filter if there's text
                String query = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
                filterRecipes(query);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void checkSavedStatus() {
        if (currentUser == null) return;
        savedRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (Recipe recipe : allRecipes) {
                    recipe.setSaved(snapshot.hasChild(recipe.getId()));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterRecipes(String query) {
        filteredRecipes.clear();
        if (query.isEmpty()) {
            emptyText.setText("Начните вводить название рецепта");
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            adapter.updateRecipes(filteredRecipes);
            return;
        }

        String lower = query.toLowerCase(Locale.getDefault());
        for (Recipe recipe : allRecipes) {
            if (recipe.getTitle() != null && recipe.getTitle().toLowerCase(Locale.getDefault()).contains(lower)) {
                filteredRecipes.add(recipe);
            }
        }

        if (filteredRecipes.isEmpty()) {
            emptyText.setText("Рецепты не найдены");
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        adapter.updateRecipes(filteredRecipes);
    }

    private void openRecipeDetail(Recipe recipe) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openRecipeDetail(recipe);
        }
    }
}
