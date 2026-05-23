package com.example.Flavor;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.Flavor.Models.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileFragment extends Fragment {

    // Профиль и БД (новые кнопки)
    private MaterialButton login_button;
    private MaterialButton register_button;
    RelativeLayout root_element;
    RelativeLayout sign_root_element;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Профиль и БД (новые кнопки)
        login_button = view.findViewById(R.id.login_button);
        register_button = view.findViewById(R.id.register_button);
        root_element = view.findViewById(R.id.root_element);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference("Users");

        register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterWindow();
            }
        });

        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginWindow();
            }
        });

        return view;
    }

    private void showRegisterWindow() {
        // Импорт xml Fragment_register
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View fragment_register = inflater.inflate(R.layout.fragment_register, null);
        dialog.setView(fragment_register);

        // Объявление переменных
        TextInputEditText nickname = fragment_register.findViewById(R.id.nickname);
        TextInputEditText email = fragment_register.findViewById(R.id.email);
        TextInputEditText password = fragment_register.findViewById(R.id.password);
        TextInputEditText confirm_password = fragment_register.findViewById(R.id.confirm_password);
        MaterialButton register_confirm = fragment_register.findViewById(R.id.register_confirm);
        TextView i_have_account = fragment_register.findViewById(R.id.i_have_account);
        MaterialButton dismiss_reg = fragment_register.findViewById(R.id.dismiss_reg);

        AlertDialog alertDialog = dialog.create();
        alertDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        alertDialog.show();

        // Используем кнопку из XML вместо setPositiveButton
        register_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(nickname.getText().toString())) {
                    Snackbar.make(root_element, "Введите никнейм", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(email.getText().toString())) {
                    Snackbar.make(root_element, "Введите почту", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (password.getText().toString().length() < 5) {
                    Snackbar.make(root_element, "Пароль не менее 5 символов", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (!confirm_password.getText().toString().equals(password.getText().toString())) {
                    Snackbar.make(root_element, "Пароли не совпадают", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                // Регистрация
                auth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                User user = new User();
                                user.setNickname(nickname.getText().toString());
                                user.setEmail(email.getText().toString());
                                user.setPassword(password.getText().toString());

                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Snackbar.make(root_element, "Пользователь добавлен!", Snackbar.LENGTH_SHORT).show();
                                                alertDialog.dismiss();
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Snackbar.make(root_element, "Ошибка: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        });
            }
        });

        dismiss_reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }
    private void showLoginWindow() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View fragment_signin = inflater.inflate(R.layout.fragment_signin, null);
        dialog.setView(fragment_signin);

        TextInputEditText email = fragment_signin.findViewById(R.id.email);
        TextInputEditText password = fragment_signin.findViewById(R.id.password);
        MaterialButton login_confirm = fragment_signin.findViewById(R.id.login_confirm);
        TextView i_have_account = fragment_signin.findViewById(R.id.i_have_account);
        MaterialButton dismiss_sign = fragment_signin.findViewById(R.id.dismiss_sign);

        AlertDialog alertDialog = dialog.create();
        alertDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        alertDialog.show();

        login_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(email.getText().toString())) {
                    Snackbar.make(root_element, "Введите почту", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password.getText().toString())) {
                    Snackbar.make(root_element, "Введите пароль", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                Snackbar.make(root_element, "Вход выполнен!", Snackbar.LENGTH_SHORT).show();
                                alertDialog.dismiss();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Snackbar.make(root_element, "Ошибка: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        });
                }
            });
        dismiss_sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }
}