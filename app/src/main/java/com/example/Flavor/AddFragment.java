package com.example.Flavor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.Flavor.Models.Recipe;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class AddFragment extends Fragment {

    private TextInputEditText titleInput, descriptionInput, ingredientsInput, instructionsInput;
    private MaterialButton saveButton, cancelButton;

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference recipesRef;
    private DatabaseReference userRecipesRef;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        // Инициализация UI
        titleInput = view.findViewById(R.id.titleInput);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        ingredientsInput = view.findViewById(R.id.ingredientsInput);
        instructionsInput = view.findViewById(R.id.instructionsInput);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        recipesRef = FirebaseDatabase.getInstance().getReference("Recipes");
        userRecipesRef = FirebaseDatabase.getInstance().getReference("UserRecipes");

        // Обработчики
        saveButton.setOnClickListener(v -> checkAuthAndSave());
        cancelButton.setOnClickListener(v -> goBack());

        return view;
    }

    private void checkAuthAndSave() {
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            showAuthRequiredDialog();
        } else {
            saveRecipeToFirebase();
        }
    }

    private void showAuthRequiredDialog() {
        androidx.appcompat.app.AlertDialog.Builder dialog =
                new androidx.appcompat.app.AlertDialog.Builder(getContext());
        dialog.setTitle("Требуется авторизация");
        dialog.setMessage("Чтобы добавить рецепт, нужно войти в аккаунт. Перейти в профиль?");
        dialog.setPositiveButton("Войти", (dialogInterface, i) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showProfileFragment();
            }
        });
        dialog.setNegativeButton("Отмена", null);
        dialog.show();
    }

    private void saveRecipeToFirebase() {
        // Получаем данные
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String ingredients = ingredientsInput.getText().toString().trim();
        String instructions = instructionsInput.getText().toString().trim();

        // Валидация
        if (title.isEmpty()) {
            titleInput.setError("Введите название рецепта");
            titleInput.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            descriptionInput.setError("Введите описание");
            descriptionInput.requestFocus();
            return;
        }

        if (ingredients.isEmpty()) {
            ingredientsInput.setError("Введите ингредиенты");
            ingredientsInput.requestFocus();
            return;
        }

        if (instructions.isEmpty()) {
            instructionsInput.setError("Введите инструкцию");
            instructionsInput.requestFocus();
            return;
        }

        // Сохраняем
        saveButton.setEnabled(false);
        saveButton.setText("Сохранение...");

        String recipeId = recipesRef.push().getKey();
        String userId = currentUser.getUid();
        String userEmail = currentUser.getEmail();
        long timestamp = System.currentTimeMillis();

        // Создаём Map с данными рецепта
        Map<String, Object> recipe = new HashMap<>();
        recipe.put("id", recipeId);
        recipe.put("title", title);
        recipe.put("description", description);
        recipe.put("ingredients", ingredients);
        recipe.put("instructions", instructions);
        recipe.put("userId", userId);
        recipe.put("userEmail", userEmail);
        recipe.put("timestamp", timestamp);

        if (recipeId != null) {
            // Сохраняем в общую коллекцию Recipes
            recipesRef.child(recipeId).setValue(recipe)
                    .addOnSuccessListener(aVoid -> {
                        // Сохраняем связку пользователь-рецепт
                        saveUserRecipeReference(userId, recipeId);
                    })
                    .addOnFailureListener(e -> {
                        saveButton.setEnabled(true);
                        saveButton.setText("Сохранить");
                        Snackbar.make(getView(), "Ошибка: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveUserRecipeReference(String userId, String recipeId) {
        // Сохраняем ссылку на рецепт для пользователя
        Map<String, Object> userRecipe = new HashMap<>();
        userRecipe.put("recipeId", recipeId);
        userRecipe.put("savedAt", System.currentTimeMillis());

        userRecipesRef.child(userId).child(recipeId).setValue(userRecipe)
                .addOnSuccessListener(aVoid -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Сохранить");
                    Toast.makeText(getContext(), "Рецепт добавлен!", Toast.LENGTH_SHORT).show();
                    clearForm();

                    // Обновляем ленту
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).refreshFeed();
                    }
                })
                .addOnFailureListener(e -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Сохранить");
                    Snackbar.make(getView(), "Ошибка при сохранении ссылки", Snackbar.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        titleInput.setText("");
        descriptionInput.setText("");
        ingredientsInput.setText("");
        instructionsInput.setText("");
        titleInput.requestFocus();
    }

    private void goBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
}