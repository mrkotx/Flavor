package com.example.Flavor;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.Flavor.Models.Recipe;
import com.example.Flavor.Models.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
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

public class ProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextView userNameText;
    private TextView userEmailText;
    private TextView recipesCountText;
    private TextView likesCountText;
    private RecyclerView userRecipesRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;

    // Кнопки
    private MaterialButton loginButton;
    private MaterialButton registerButton;
    private MaterialButton logoutButton;
    private View profileInfoContainer;
    private LinearLayout rootElement;

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference usersRef;  // ← БЫЛО usersRef
    private DatabaseReference recipesRef;
    private DatabaseReference savedRef;
    private FirebaseUser currentUser;

    // Адаптер
    private RecipeAdapter userRecipesAdapter;
    private List<Recipe> userRecipesList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Инициализация UI
        profileImage = view.findViewById(R.id.profile_image);
        userNameText = view.findViewById(R.id.user_name);
        userEmailText = view.findViewById(R.id.user_email);
        recipesCountText = view.findViewById(R.id.recipes_count);
        likesCountText = view.findViewById(R.id.likes_count);
        userRecipesRecyclerView = view.findViewById(R.id.user_recipes_recycler);
        progressBar = view.findViewById(R.id.progressBar);
        emptyText = view.findViewById(R.id.emptyText);

        loginButton = view.findViewById(R.id.login_button);
        registerButton = view.findViewById(R.id.register_button);
        logoutButton = view.findViewById(R.id.logout_button);
        profileInfoContainer = view.findViewById(R.id.profile_info_container);
        rootElement = view.findViewById(R.id.root_element);

        // Firebase
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        recipesRef = FirebaseDatabase.getInstance().getReference("Recipes");
        savedRef = FirebaseDatabase.getInstance().getReference("SavedRecipes");

        // Настройка RecyclerView
        userRecipesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userRecipesAdapter = new RecipeAdapter(userRecipesList,
                recipe -> openRecipeDetail(recipe),        // OnRecipeClickListener
                (recipe, position, isLiked) -> {           // OnRecipeLikeListener
                    if (currentUser != null) {
                        updateRecipeLike(recipe, position, isLiked);
                    }
                }
        );
        userRecipesRecyclerView.setAdapter(userRecipesAdapter);

        // Кнопки
        loginButton.setOnClickListener(v -> showLoginWindow());
        registerButton.setOnClickListener(v -> showRegisterWindow());
        logoutButton.setOnClickListener(v -> logout());

        // Проверяем состояние авторизации
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
            loginButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String email = currentUser.getEmail();
        userEmailText.setText(email);

        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nickname = snapshot.child("nickname").getValue(String.class);
                if (nickname != null && !nickname.isEmpty()) {
                    userNameText.setText(nickname);
                } else {
                    userNameText.setText("Пользователь");
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

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                            if (map != null) {
                                Recipe recipe = new Recipe(map);
                                recipe.setId(dataSnapshot.getKey());
                                recipe.setSaved(false);
                                userRecipesList.add(0, recipe);
                            }
                        }

                        // Обновляем счётчик рецептов
                        recipesCountText.setText(String.valueOf(userRecipesList.size()));

                        userRecipesAdapter.updateRecipes(userRecipesList);
                        showLoading(false);

                        if (userRecipesList.isEmpty()) {
                            showEmpty(true);
                            emptyText.setText("У вас пока нет рецептов\nДобавьте первый рецепт!");
                        } else {
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

        String userId = currentUser.getUid();

        savedRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                likesCountText.setText(String.valueOf(count));
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
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Добавлено в сохранённые", Toast.LENGTH_SHORT).show();
                        loadUserLikesCount();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                        userRecipesAdapter.updateLikeStatus(position, false);
                    });
        } else {
            savedRef.child(userId).child(recipe.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Удалено из сохранённых", Toast.LENGTH_SHORT).show();
                        loadUserLikesCount();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                        userRecipesAdapter.updateLikeStatus(position, true);
                    });
        }
    }

    private void openRecipeDetail(Recipe recipe) {
        Toast.makeText(getContext(), "Открыть: " + recipe.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: открыть детали рецепта
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
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (userRecipesRecyclerView != null) {
            userRecipesRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmpty(boolean isEmpty) {
        if (emptyText != null) {
            emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
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
        alertDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        alertDialog.show();

        register_confirm.setOnClickListener(v -> {
            if (TextUtils.isEmpty(nickname.getText().toString())) {
                Snackbar.make(rootElement, "Введите никнейм", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(email.getText().toString())) {
                Snackbar.make(rootElement, "Введите почту", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (password.getText().toString().length() < 5) {
                Snackbar.make(rootElement, "Пароль не менее 5 символов", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (!confirm_password.getText().toString().equals(password.getText().toString())) {
                Snackbar.make(rootElement, "Пароли не совпадают", Snackbar.LENGTH_SHORT).show();
                return;
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
                    .addOnFailureListener(e -> {
                        Snackbar.make(rootElement, "Ошибка: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    });
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
        alertDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        alertDialog.show();

        login_confirm.setOnClickListener(v -> {
            if (TextUtils.isEmpty(email.getText().toString())) {
                Snackbar.make(rootElement, "Введите почту", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password.getText().toString())) {
                Snackbar.make(rootElement, "Введите пароль", Snackbar.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnSuccessListener(authResult -> {
                        Snackbar.make(rootElement, "Вход выполнен!", Snackbar.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                        checkAuthState();
                    })
                    .addOnFailureListener(e -> {
                        Snackbar.make(rootElement, "Ошибка: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    });
        });

        dismiss_sign.setOnClickListener(v -> alertDialog.dismiss());
    }
}