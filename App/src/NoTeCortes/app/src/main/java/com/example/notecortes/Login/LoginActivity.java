package com.example.notecortes.Login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.notecortes.Administrador.AdminHomeActivity;
import com.example.notecortes.Cliente.HomeActivity;
import com.example.notecortes.Empleado.EmpleHomeActivity;
import com.example.notecortes.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
//La clase LoginActivity centraliza la autenticación, la autorización por roles y la seguridad
// perimetral de la aplicación. En lugar de delegar el acceso ciegamente en Firebase Auth, implementa
// un control de estado síncrono que evalúa el flag activo en Firestore y audita directivas de cambio
// forzado de credenciales de forma cruzada. Adicionalmente, se encarga de la persistencia del token de
// Firebase Cloud Messaging (fcmToken) en cada inicio de sesión, garantizando la disponibilidad del canal
// de comunicación push con el dispositivo.
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailLogin, passwordLogin;
    private Button btnLogin, btnGoRegister, btnRecoverPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        emailLogin = findViewById(R.id.emailLogin);
        passwordLogin = findViewById(R.id.passwordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);
        btnRecoverPassword = findViewById(R.id.btnRecoverPassword);

        // Listeners
        btnLogin.setOnClickListener(v -> loginUser(v));
        btnGoRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnRecoverPassword.setOnClickListener(v -> sendRecoveryEmail(v));
    }

    private void loginUser(View view) {
        String mail = emailLogin.getText().toString().trim();
        String pass = passwordLogin.getText().toString().trim();

        if (mail.isEmpty() || pass.isEmpty()) {
            showMsgCustom(view, "Completa todos los campos", "#D32F2F");
            return;
        }

        // --- VALIDACIÓN DE CONEXIÓN REAL ---
        if (!isNetworkAvailable()) {
            // El Toast es la única forma de asegurar que el mensaje salga si el layout falla
            Toast.makeText(this, "No tienes conexión a Internet", Toast.LENGTH_LONG).show();
            showMsgCustom(view, "Sin conexión a internet", "#D32F2F");
            return;
        }

        mAuth.signInWithEmailAndPassword(mail, pass)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showLoginError(view, task.getException() != null
                                ? task.getException().getMessage()
                                : "Error desconocido");
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        checkUserStatusAndRole(user, view);
                    }
                });
    }

    private void checkUserStatusAndRole(FirebaseUser user, View view) {
        String uid = user.getUid();
        String email = user.getEmail();

        db.collection("users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        Log.e(TAG, "Documento no encontrado. Cerrando sesión.");
                        mAuth.signOut();
                        showMsgCustom(view, "Error: Cuenta no configurada.", "#D32F2F");
                        return;
                    }

                    DocumentSnapshot doc = task.getResult();

                    // 1. VALIDACIÓN DE ESTADO ACTIVO
                    Boolean isActive = doc.getBoolean("activo");
                    if (isActive == null || !isActive) {
                        mAuth.signOut();
                        Log.w(TAG, "Intento de acceso bloqueado: " + email);
                        showMsgCustom(view, "Cuenta desactivada. Contacta con administración.", "#D32F2F");
                        return;
                    }

                    actualizarTokenFCM(uid);

                    // 2. OBTENER ROL E ISADMIN
                    String rol = doc.getString("rol");
                    if (rol == null) rol = "cliente";

                    Boolean isAdminCheck = doc.getBoolean("isAdmin");
                    boolean isAdmin = (isAdminCheck != null && isAdminCheck);

                    // 3. REDIRECCIÓN SEGÚN ROL CON CHEQUEO DE CONTRASEÑA
                    if (isAdmin && "empleado".equals(rol)) {
                        checkForcePasswordChange(uid, email, doc, EmpleHomeActivity.class, view);
                    }
                    else if (isAdmin) {
                        checkForcePasswordChange(uid, email, doc, AdminHomeActivity.class, view);
                    }
                    else {
                        checkForcePasswordChange(uid, email, doc, HomeActivity.class, view);
                    }
                });
    }

    private void checkForcePasswordChange(String uid, String email, DocumentSnapshot userDoc, Class<?> destinationActivity, View view) {
        boolean forceChangeField = userDoc != null
                && userDoc.contains("forcePasswordChange")
                && Boolean.TRUE.equals(userDoc.getBoolean("forcePasswordChange"));

        String sanitizedEmail = sanitizeEmailKey(email);
        db.collection("password_resets").document(sanitizedEmail)
                .get()
                .addOnSuccessListener(resetDoc -> {
                    if (forceChangeField || resetDoc.exists()) {
                        goToForcePassword(uid, email);
                    } else {
                        startActivity(new Intent(LoginActivity.this, destinationActivity));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    mAuth.signOut();
                    showMsgCustom(view, "Error de validación de seguridad. Revisa tu conexión.", "#D32F2F");
                    Log.e(TAG, "Error en checkForcePasswordChange: " + e.getMessage());
                });
    }

    private void sendRecoveryEmail(View view) {
        String email = emailLogin.getText().toString().trim();
        if (email.isEmpty()) {
            showMsgCustom(view, "Introduce tu correo", "#D32F2F");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String key = sanitizeEmailKey(email);
                        HashMap<String, Object> resetData = new HashMap<>();
                        resetData.put("requestedAt", System.currentTimeMillis());

                        db.collection("password_resets").document(key)
                                .set(resetData)
                                .addOnSuccessListener(aVoid -> {
                                    showMsgCustom(view, "Correo de recuperación enviado.", "#0D47A1");
                                });
                    } else {
                        showMsgCustom(view, "Error: " + task.getException().getMessage(), "#D32F2F");
                    }
                });
    }

    private void goToForcePassword(String uid, String email) {
        Intent i = new Intent(LoginActivity.this, ForceChangePasswordActivity.class);
        i.putExtra("uid", uid);
        i.putExtra("email", email);
        startActivity(i);
        finish();
    }

    private void showLoginError(View view, String error) {
        String mensajeEspanol;
        String e = (error != null) ? error.toLowerCase() : "";

        if (e.contains("incorrect") || e.contains("malformed") || e.contains("expired") || e.contains("credential")) {
            mensajeEspanol = "El correo o la contraseña no son correctos.";
        } else if (e.contains("badly formatted") || e.contains("format") || e.contains("invalid email")) {
            mensajeEspanol = "El formato del correo electrónico es inválido.";
        } else if (e.contains("network") || e.contains("connection")) {
            mensajeEspanol = "Sin conexión a internet.";
        } else if (e.contains("too many requests")) {
            mensajeEspanol = "Demasiados intentos. Inténtalo más tarde.";
        } else {
            mensajeEspanol = "Error de acceso: " + error;
        }

        showMsgCustom(view, mensajeEspanol, "#D32F2F");
    }

    private void showMsgCustom(View view, String mensaje, String colorHex) {
        try {
            // Intentamos con Snackbar anclado a la vista del botón
            Snackbar snackbar = Snackbar.make(view, mensaje, Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(Color.parseColor(colorHex));
            snackbar.setTextColor(Color.WHITE);
            snackbar.setAction("OK", v -> snackbar.dismiss());
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        } catch (Exception e) {
            // Si el Snackbar falla por el layout, el Toast siempre funciona
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private String sanitizeEmailKey(@NonNull String email) {
        return email.replace(".", "_").replace("@", "_at_").toLowerCase();
    }

    private void actualizarTokenFCM(String uid) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;
                    String token = task.getResult();
                    db.collection("users").document(uid)
                            .update("fcmToken", token)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Token FCM actualizado"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error al guardar token", e));
                });
    }
}