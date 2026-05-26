package com.example.Flavor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.Flavor.Models.Recipe;
import com.example.Flavor.Models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextView userNameText;
    private TextView userEmailText;
    private TextView recipesCountText;
    private TextView likesCountText;
    private RecyclerView userRecipesRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView myRecipesTitle;

    private MaterialButton loginButton;
    private MaterialButton registerButton;
    private MaterialButton logoutButton;
    private View profileInfoContainer;
    private LinearLayout rootElement;

    private AutoCompleteTextView editCategory;
    private String[] categories = {"Все", "Завтрак", "Обед", "Ужин", "Перекус"};
    private String selectedCategory;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private DatabaseReference recipesRef;
    private DatabaseReference savedRef;
    private StorageReference storageRef;
    private FirebaseUser currentUser;

    private RecipeAdapter userRecipesAdapter;
    private List<Recipe> userRecipesList = new ArrayList<>();

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        uploadProfilePhoto(imageUri);
                    }
                });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImage = view.findViewById(R.id.profile_image);
        userNameText = view.findViewById(R.id.user_name);
        userEmailText = view.findViewById(R.id.user_email);
        recipesCountText = view.findViewById(R.id.recipes_count);
        likesCountText = view.findViewById(R.id.likes_count);
        userRecipesRecyclerView = view.findViewById(R.id.user_recipes_recycler);
        progressBar = view.findViewById(R.id.progressBar);
        emptyText = view.findViewById(R.id.emptyText);
        myRecipesTitle = view.findViewById(R.id.my_recipes_title);

        loginButton = view.findViewById(R.id.login_button);
        registerButton = view.findViewById(R.id.register_button);
        logoutButton = view.findViewById(R.id.logout_button);
        profileInfoContainer = view.findViewById(R.id.profile_info_container);
        rootElement = view.findViewById(R.id.root_element);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        recipesRef = FirebaseDatabase.getInstance().getReference("Recipes");
        savedRef = FirebaseDatabase.getInstance().getReference("SavedRecipes");
        storageRef = FirebaseStorage.getInstance().getReference("profile_photos");

        userRecipesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userRecipesAdapter = new RecipeAdapter(userRecipesList,
                recipe -> showEditRecipeDialog(recipe),
                (recipe, position, isLiked) -> {
                    if (currentUser != null) updateRecipeLike(recipe, position, isLiked);
                }
        );
        userRecipesRecyclerView.setAdapter(userRecipesAdapter);

        // Tap avatar to change photo
        profileImage.setOnClickListener(v -> {
            if (currentUser != null) pickImage();
        });

        loginButton.setOnClickListener(v -> showLoginWindow());
        registerButton.setOnClickListener(v -> showRegisterWindow());
        logoutButton.setOnClickListener(v -> logout());

        checkAuthState();

        return view;
    }

    private void checkAuthState() {
        currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            showProfileContent(true);
            loadUserData();
            loadUserRecipes();
            loadUserLikesCount();
        } else {
            showProfileContent(false);
        }
    }

    private void showProfileContent(boolean isAuthorized) {
        if (isAuthorized) {
            profileInfoContainer.setVisibility(View.VISIBLE);
            userRecipesRecyclerView.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
        } else {
            profileInfoContainer.setVisibility(View.GONE);
            userRecipesRecyclerView.setVisibility(View.GONE);
            myRecipesTitle.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        userEmailText.setText(currentUser.getEmail());

        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nickname = snapshot.child("nickname").getValue(String.class);
                userNameText.setText(!TextUtils.isEmpty(nickname) ? nickname : "Пользователь");

                // Load avatar
                String photoUrl = snapshot.child("photoUrl").getValue(String.class);
                if (!TextUtils.isEmpty(photoUrl) && getContext() != null) {
                    Glide.with(getContext()).load(photoUrl).circleCrop()
                            .placeholder(R.drawable.profile_icon).into(profileImage);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userNameText.setText("Пользователь");
            }
        });
    }

    private void loadUserRecipes() {
        if (currentUser == null) return;
        showLoading(true);
        String userId = currentUser.getUid();

        recipesRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userRecipesList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Map<String, Object> map = (Map<String, Object>) ds.getValue();
                            if (map != null) {
                                Recipe recipe = new Recipe(map);
                                recipe.setId(ds.getKey());
                                recipe.setSaved(false);
                                userRecipesList.add(0, recipe);
                            }
                        }

                        recipesCountText.setText(String.valueOf(userRecipesList.size()));
                        userRecipesAdapter.updateRecipes(userRecipesList);
                        showLoading(false);

                        // Fix: always show title when user is logged in
                        if (userRecipesList.isEmpty()) {
                            myRecipesTitle.setVisibility(View.VISIBLE);
                            emptyText.setText("У вас пока нет рецептов\nДобавьте первый рецепт!");
                            showEmpty(true);
                        } else {
                            myRecipesTitle.setVisibility(View.VISIBLE);
                            showEmpty(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        showEmpty(true);
                        emptyText.setText("Ошибка загрузки");
                    }
                });
    }

    private void loadUserLikesCount() {
        if (currentUser == null) return;
        savedRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                likesCountText.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                likesCountText.setText("0");
            }
        });
    }

    private void updateRecipeLike(Recipe recipe, int position, boolean isLiked) {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        if (isLiked) {
            savedRef.child(userId).child(recipe.getId()).setValue(true)
                    .addOnSuccessListener(v -> loadUserLikesCount())
                    .addOnFailureListener(e -> userRecipesAdapter.updateLikeStatus(position, false));
        } else {
            savedRef.child(userId).child(recipe.getId()).removeValue()
                    .addOnSuccessListener(v -> loadUserLikesCount())
                    .addOnFailureListener(e -> userRecipesAdapter.updateLikeStatus(position, true));
        }
    }

    private void showEditRecipeDialog(Recipe recipe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.fragment_edit_recipe, null);
        builder.setView(dialogView);

        TextInputEditText editTitle = dialogView.findViewById(R.id.editTitle);
        TextInputEditText editDescription = dialogView.findViewById(R.id.editDescription);
        TextInputEditText editIngredients = dialogView.findViewById(R.id.editIngredients);
        TextInputEditText editInstructions = dialogView.findViewById(R.id.editInstructions);
        AutoCompleteTextView editCategory = dialogView.findViewById(R.id.editCategory);
        MaterialButton saveEditButton = dialogView.findViewById(R.id.saveEditButton);
        MaterialButton deleteButton = dialogView.findViewById(R.id.deleteButton);

        editTitle.setText(recipe.getTitle());
        editDescription.setText(recipe.getDescription());
        editIngredients.setText(recipe.getIngredients());
        editInstructions.setText(recipe.getInstructions());

        // Настройка выбора категории
        String[] categories = {"Все", "Завтрак", "Обед", "Ужин", "Перекус"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        editCategory.setAdapter(categoryAdapter);

        String currentCategory = recipe.getCategoryId();
        if (currentCategory == null || currentCategory.isEmpty()) {
            currentCategory = "Все";
        }
        editCategory.setText(currentCategory, false);

        final String[] selectedCategory = {currentCategory};
        editCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory[0] = categories[position];
        });

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.show();

        saveEditButton.setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();
            String description = editDescription.getText().toString().trim();
            String ingredients = editIngredients.getText().toString().trim();
            String instructions = editInstructions.getText().toString().trim();

            if (TextUtils.isEmpty(title)) { editTitle.setError("Введите название"); return; }
            if (TextUtils.isEmpty(description)) { editDescription.setError("Введите описание"); return; }
            if (TextUtils.isEmpty(ingredients)) { editIngredients.setError("Введите ингредиенты"); return; }
            if (TextUtils.isEmpty(instructions)) { editInstructions.setError("Введите инструкцию"); return; }

            Map<String, Object> updates = new HashMap<>();
            updates.put("title", title);
            updates.put("description", description);
            updates.put("ingredients", ingredients);
            updates.put("instructions", instructions);
            updates.put("categoryId", selectedCategory[0]);

            recipesRef.child(recipe.getId()).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Рецепт обновлён!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Удалить рецепт?")
                    .setMessage("Вы уверены, что хотите удалить «" + recipe.getTitle() + "»?")
                    .setPositiveButton("Удалить", (d, which) -> {
                        recipesRef.child(recipe.getId()).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Рецепт удалён", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfilePhoto(Uri imageUri) {
        if (currentUser == null || imageUri == null) return;

        Toast.makeText(getContext(), "Загрузка фото...", Toast.LENGTH_SHORT).show();
        StorageReference photoRef = storageRef.child(currentUser.getUid() + ".jpg");

        photoRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String photoUrl = uri.toString();
                        // Save URL to Realtime DB
                        usersRef.child(currentUser.getUid()).child("photoUrl").setValue(photoUrl)
                                .addOnSuccessListener(aVoid -> {
                                    if (getContext() != null) {
                                        Glide.with(getContext()).load(photoUrl).circleCrop()
                                                .placeholder(R.drawable.profile_icon).into(profileImage);
                                        Toast.makeText(getContext(), "Фото обновлено!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки фото: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        auth.signOut();
        currentUser = null;
        showProfileContent(false);
        userRecipesList.clear();
        userRecipesAdapter.updateRecipes(userRecipesList);
        Toast.makeText(getContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (userRecipesRecyclerView != null) userRecipesRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showEmpty(boolean isEmpty) {
        if (emptyText != null) emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showRegisterWindow() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View fragment_register = inflater.inflate(R.layout.fragment_register, null);
        dialog.setView(fragment_register);

        TextInputEditText nickname = fragment_register.findViewById(R.id.nickname);
        TextInputEditText email = fragment_register.findViewById(R.id.email);
        TextInputEditText password = fragment_register.findViewById(R.id.password);
        TextInputEditText confirm_password = fragment_register.findViewById(R.id.confirm_password);
        MaterialButton register_confirm = fragment_register.findViewById(R.id.register_confirm);
        MaterialButton dismiss_reg = fragment_register.findViewById(R.id.dismiss_reg);

        AlertDialog alertDialog = dialog.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        alertDialog.show();

        register_confirm.setOnClickListener(v -> {
            if (TextUtils.isEmpty(nickname.getText().toString())) {
                Snackbar.make(rootElement, "Введите никнейм", Snackbar.LENGTH_SHORT).show(); return;
            }
            if (TextUtils.isEmpty(email.getText().toString())) {
                Snackbar.make(rootElement, "Введите почту", Snackbar.LENGTH_SHORT).show(); return;
            }
            if (password.getText().toString().length() < 5) {
                Snackbar.make(rootElement, "Пароль не менее 5 символов", Snackbar.LENGTH_SHORT).show(); return;
            }
            if (!confirm_password.getText().toString().equals(password.getText().toString())) {
                Snackbar.make(rootElement, "Пароли не совпадают", Snackbar.LENGTH_SHORT).show(); return;
            }

            auth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnSuccessListener(authResult -> {
                        User user = new User();
                        user.setNickname(nickname.getText().toString());
                        user.setEmail(email.getText().toString());
                        usersRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    Snackbar.make(rootElement, "Пользователь добавлен!", Snackbar.LENGTH_SHORT).show();
                                    alertDialog.dismiss();
                                    checkAuthState();
                                });
                    })
                    .addOnFailureListener(e -> Snackbar.make(rootElement, "Ошибка: " + e.getMessage(), Snackbar.LENGTH_SHORT).show());
        });

        dismiss_reg.setOnClickListener(v -> alertDialog.dismiss());
    }

    private void showLoginWindow() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View fragment_signin = inflater.inflate(R.layout.fragment_signin, null);
        dialog.setView(fragment_signin);

        TextInputEditText email = fragment_signin.findViewById(R.id.email);
        TextInputEditText password = fragment_signin.findViewById(R.id.password);
        MaterialButton login_confirm = fragment_signin.findViewById(R.id.login_confirm);
        MaterialButton dismiss_sign = fragment_signin.findViewById(R.id.dismiss_sign);

        AlertDialog alertDialog = dialog.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        alertDialog.show();

        login_confirm.setOnClickListener(v -> {
            if (TextUtils.isEmpty(email.getText().toString())) {
                Snackbar.make(rootElement, "Введите почту", Snackbar.LENGTH_SHORT).show(); return;
            }
            if (TextUtils.isEmpty(password.getText().toString())) {
                Snackbar.make(rootElement, "Введите пароль", Snackbar.LENGTH_SHORT).show(); return;
            }
            auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnSuccessListener(authResult -> {
                        Snackbar.make(rootElement, "Вход выполнен!", Snackbar.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                        checkAuthState();
                    })
                    .addOnFailureListener(e -> Snackbar.make(rootElement, "Ошибка: " + e.getMessage(), Snackbar.LENGTH_SHORT).show());
        });

        dismiss_sign.setOnClickListener(v -> alertDialog.dismiss());
    }
}
