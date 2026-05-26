package com.example.Flavor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
    private AutoCompleteTextView categorySpinner;

    private String selectedCategory = "Все";

    private final String[] categories = {"Все", "Завтрак", "Обед", "Ужин", "Перекус"};

    private FirebaseAuth auth;
    private DatabaseReference recipesRef;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        titleInput = view.findViewById(R.id.titleInput);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        ingredientsInput = view.findViewById(R.id.ingredientsInput);
        instructionsInput = view.findViewById(R.id.instructionsInput);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        categorySpinner = view.findViewById(R.id.categorySpinner);

        setupCategorySpinner();

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        recipesRef = FirebaseDatabase.getInstance().getReference("Recipes");

        saveButton.setOnClickListener(v -> checkAuthAndSave());
        cancelButton.setOnClickListener(v -> goBack());

        return view;
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
        );

        categorySpinner.setAdapter(adapter);
        categorySpinner.setText(categories[0], false);

        categorySpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categories[position];
        });
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
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String ingredients = ingredientsInput.getText().toString().trim();
        String instructions = instructionsInput.getText().toString().trim();

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

        saveButton.setEnabled(false);
        saveButton.setText("Сохранение...");

        String recipeId = recipesRef.push().getKey();
        String userId = currentUser.getUid();
        String userEmail = currentUser.getEmail();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("id", recipeId);
        recipe.put("title", title);
        recipe.put("description", description);
        recipe.put("ingredients", ingredients);
        recipe.put("instructions", instructions);
        recipe.put("categoryId", selectedCategory);
        recipe.put("userId", userId);
        recipe.put("userEmail", userEmail);
        recipe.put("timestamp", timestamp);

        if (recipeId != null) {
            recipesRef.child(recipeId).setValue(recipe)
                    .addOnSuccessListener(aVoid -> {
                        saveButton.setEnabled(true);
                        saveButton.setText("Опубликовать");
                        Toast.makeText(getContext(), "Рецепт добавлен!", Toast.LENGTH_SHORT).show();
                        clearForm();

                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).refreshFeed();
                        }
                    })
                    .addOnFailureListener(e -> {
                        saveButton.setEnabled(true);
                        saveButton.setText("Опубликовать");
                        Snackbar.make(getView(), "Ошибка: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    });
        }
    }

    private void clearForm() {
        titleInput.setText("");
        descriptionInput.setText("");
        ingredientsInput.setText("");
        instructionsInput.setText("");
        categorySpinner.setText(categories[0], false);
        selectedCategory = categories[0];
        titleInput.requestFocus();
    }

    private void goBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
}