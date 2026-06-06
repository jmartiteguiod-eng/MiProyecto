package com.example.notecortes.Login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.example.notecortes.Cliente.HomeActivity;
import com.example.notecortes.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
//La clase ForceChangePasswordActivity es un módulo de seguridad transaccional. Aplica políticas de
// robustez criptográfica mediante filtrado por expresiones regulares (Regex) en el lado del cliente.
// Tras validar los campos, actualiza las credenciales en Firebase Auth, revoca de forma síncrona el
// flag de bloqueo en Firestore, y sanitiza los identificadores para purgar la colección password_resets.
// El flujo concluye invalidando el token de sesión (signOut) y redirigiendo al usuario a la vista de
// login mediante un vaciado completo de la pila de actividades.
public class ForceChangePasswordActivity extends AppCompatActivity {

    private EditText newPasswordEt, confirmPasswordEt;
    private Button btnChange;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_force_change_password);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        newPasswordEt = findViewById(R.id.newPassword);
        confirmPasswordEt = findViewById(R.id.confirmPassword);
        btnChange = findViewById(R.id.btnChangePassword);

        uid = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");

        // Si el email no llega por el Intent, intentamos recuperarlo de la sesión actual
        if ((email == null || email.isEmpty()) && mAuth.getCurrentUser() != null) {
            email = mAuth.getCurrentUser().getEmail();
        }

        btnChange.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String newPass = newPasswordEt.getText().toString().trim();
        String confirm = confirmPasswordEt.getText().toString().trim();

        // 1. REGEX DE SEGURIDAD: 8 caracteres, 1 Mayúscula, 1 Número, 1 Símbolo
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!/.*])(?=\\S+$).{8,}$";

        // VALIDACIONES INICIALES
        if (TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirm)) {
            mostrarMensaje("Por favor, rellena ambos campos.", true);
            return;
        }

        if (!newPass.equals(confirm)) {
            mostrarMensaje("Las contraseñas introducidas no coinciden.", true);
            return;
        }

        // VALIDACIÓN DE ROBUSTEZ (REGEX)
        if (!newPass.matches(passwordRegex)) {
            mostrarMensaje("Mínimo 8 caracteres, una mayúscula, un número y un símbolo.", true);
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mostrarMensaje("Sesión no válida. Vuelve a intentarlo.", true);
            finish();
            return;
        }

        // Bloqueamos botón para evitar múltiples envíos
        btnChange.setEnabled(false);

        // PROCESO DE ACTUALIZACIÓN EN FIREBASE AUTH
        user.updatePassword(newPass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (uid == null) uid = user.getUid();

                        // 1. Actualizamos el flag de cambio forzado en Firestore
                        db.collection("users").document(uid)
                                .update("forcePasswordChange", false)
                                .addOnCompleteListener(updateTask -> {

                                    // 2. Limpieza del rastro en password_resets
                                    if (email != null && !email.isEmpty()) {
                                        String key = sanitizeEmailKey(email);
                                        db.collection("password_resets").document(key).delete();
                                    }

                                    mostrarMensaje("¡Perfecto! Contraseña actualizada correctamente.", false);

                                    // Delay para que de tiempo a leer el éxito (Verde) antes de redirigir
                                    btnChange.postDelayed(() -> {
                                        //startActivity(new Intent(ForceChangePasswordActivity.this, HomeActivity.class));
                                        //mAuth.signOut();//Nuevo
                                        //finish();
                                        //startActivity(new Intent(ForceChangePasswordActivity.this, LoginActivity.class));

                                        mAuth.signOut();
                                        Intent intent = new Intent(ForceChangePasswordActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();



                                    }, 2000);
                                })
                                .addOnFailureListener(e -> {
                                    // Fallo en Firestore pero éxito en Auth
                                    mostrarMensaje("Contraseña cambiada, pero hubo un error de sincronización.", true);
                                    btnChange.postDelayed(() -> {
                                        //startActivity(new Intent(ForceChangePasswordActivity.this, HomeActivity.class));
                                        //mAuth.signOut();//Nuevo
                                        //finish();
                                        //startActivity(new Intent(ForceChangePasswordActivity.this, LoginActivity.class));

                                        mAuth.signOut();
                                        Intent intent = new Intent(ForceChangePasswordActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    }, 2000);
                                });
                    } else {
                        btnChange.setEnabled(true);
                        // Gestión de error técnico de Firebase
                        String error = (task.getException() != null) ? task.getException().getMessage() : "";
                        procesarErrorFirebase(error);
                    }
                });
    }

    private void procesarErrorFirebase(String error) {
        String mensajeEspanol;
        String e = (error != null) ? error.toLowerCase() : "";

        if (e.contains("recent login") || e.contains("re-authenticate")) {
            mensajeEspanol = "Por seguridad, vuelve a iniciar sesión para cambiar la clave.";
        } else if (e.contains("network") || e.contains("connection")) {
            mensajeEspanol = "Sin conexión a internet.";
        } else if (e.contains("weak-password")) {
            mensajeEspanol = "La contraseña no cumple los requisitos mínimos de Firebase.";
        } else {
            mensajeEspanol = "No se pudo actualizar: " + error;
        }
        mostrarMensaje(mensajeEspanol, true);
    }

    private void mostrarMensaje(String texto, boolean esError) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), texto, Snackbar.LENGTH_LONG);

        if (esError) {
            snackbar.setBackgroundTint(Color.parseColor("#D32F2F")); // Rojo No Te Cortes
        } else {
            snackbar.setBackgroundTint(Color.parseColor("#2E7D32")); // Verde Éxito
        }

        snackbar.setTextColor(Color.WHITE);
        snackbar.setAction("OK", v -> snackbar.dismiss());
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }

    private String sanitizeEmailKey(@NonNull String email) {
        return email.replace(".", "_").replace("@", "_at_").toLowerCase().trim();
    }
}