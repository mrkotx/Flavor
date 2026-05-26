package com.example.Flavor;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.Flavor.Models.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecipeDetailFragment extends Fragment {

    private static final String ARG_RECIPE = "recipe";

    private Recipe recipe;
    private FirebaseAuth auth;
    private DatabaseReference savedRef;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;

    private ImageButton likeButton;
    private boolean isSaved = false;

    public static RecipeDetailFragment newInstance(Recipe recipe) {
        RecipeDetailFragment fragment = new RecipeDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RECIPE, recipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_detail, container, false);

        if (getArguments() != null) {
            recipe = (Recipe) getArguments().getSerializable(ARG_RECIPE);
        }

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        savedRef = FirebaseDatabase.getInstance().getReference("SavedRecipes");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Bind views
        TextView detailTitle = view.findViewById(R.id.detailTitle);
        TextView detailDescription = view.findViewById(R.id.detailDescription);
        TextView detailIngredients = view.findViewById(R.id.detailIngredients);
        TextView detailInstructions = view.findViewById(R.id.detailInstructions);
        TextView authorName = view.findViewById(R.id.authorName);
        TextView detailDate = view.findViewById(R.id.detailDate);
        TextView detailCategory = view.findViewById(R.id.detailCategory);

        likeButton = view.findViewById(R.id.likeButtonDetail);
        ImageButton backButton = view.findViewById(R.id.backButton);

        if (recipe != null) {
            detailCategory.setText(recipe.getCategoryId());
            detailCategory.setVisibility(View.VISIBLE);
            android.util.Log.d("RecipeDetail", "Recipe ID: " + recipe.getId());
            android.util.Log.d("RecipeDetail", "Category from getCategoryId(): " + recipe.getCategoryId());
            detailTitle.setText(recipe.getTitle());
            detailDescription.setText(recipe.getDescription());

            // Format ingredients with bullet points
            String ingredients = recipe.getIngredients();
            if (!TextUtils.isEmpty(ingredients)) {
                StringBuilder formatted = new StringBuilder();
                String[] lines = ingredients.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        formatted.append("• ").append(line.trim()).append("\n");
                    }
                }
                detailIngredients.setText(formatted.toString().trim());
            }

            detailInstructions.setText(recipe.getInstructions());

            // Date
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", new Locale("ru"));
            detailDate.setText(sdf.format(new Date(recipe.getTimestamp())));

            // Load author info
            loadAuthorInfo(recipe.getUserId(), authorName);

            // Check saved status
            isSaved = recipe.isSaved();
            updateLikeIcon();

            // Check from Firebase
            if (currentUser != null) {
                savedRef.child(currentUser.getUid()).child(recipe.getId())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                isSaved = snapshot.exists();
                                updateLikeIcon();
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }
        }

        likeButton.setOnClickListener(v -> toggleSave());
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    private void loadAuthorInfo(String userId, TextView authorName) {
        if (TextUtils.isEmpty(userId)) {
            authorName.setText("Аноним");
            return;
        }
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nickname = snapshot.child("nickname").getValue(String.class);
                if (!TextUtils.isEmpty(nickname)) {
                    authorName.setText(nickname);
                } else {
                    String email = snapshot.child("email").getValue(String.class);
                    authorName.setText(email != null ? email : "Пользователь");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                authorName.setText("Пользователь");
            }
        });
    }

    private void updateLikeIcon() {
        if (likeButton != null) {
            likeButton.setImageResource(isSaved ? R.drawable.heart_filled : R.drawable.heart_outline);
        }
    }

    private void toggleSave() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Войдите в аккаунт, чтобы сохранять рецепты", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        isSaved = !isSaved;
        updateLikeIcon();
        if (isSaved) {
            savedRef.child(userId).child(recipe.getId()).setValue(true);
        } else {
            savedRef.child(userId).child(recipe.getId()).removeValue();
        }
    }
}
