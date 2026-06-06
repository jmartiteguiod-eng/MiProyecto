package com.example.notecortes.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color; // Necesario para los colores
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.example.notecortes.Cliente.HomeActivity;
import com.example.notecortes.R;
import com.google.android.material.snackbar.Snackbar; // Necesario para Snackbar
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
//La clase RegisterActivity gestiona el alta asíncrona de clientes en dos fases atómicas mediante el
// SDK de Firebase: primero efectúa la creación del perfil en Firebase Auth tras validar la robustez
// de la clave mediante expresiones regulares (Regex); y segundo, persiste el documento del usuario en
// Cloud Firestore. Arquitectónicamente, el código implementa un modelo de Validación por Administración
// (activo = false), lo que impide el acceso inmediato del usuario hasta que un rol administrativo
// autorice su ingreso, mitigando así el riesgo de registros fraudulentos.
public class RegisterActivity extends AppCompatActivity {

    private EditText name, lastname, phone, address, city, email, password;
    private CheckBox notifications;
    private Button btnRegister, btnBackLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        name = findViewById(R.id.name);
        lastname = findViewById(R.id.lastname);
        phone = findViewById(R.id.phone);
        address = findViewById(R.id.address);
        city = findViewById(R.id.city);
        notifications = findViewById(R.id.notifications);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackLogin = findViewById(R.id.btnBackLogin);

        btnRegister.setOnClickListener(v -> registerUser());
        btnBackLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String nameTxt = name.getText().toString().trim();
        String lastTxt = lastname.getText().toString().trim();
        String phoneTxt = phone.getText().toString().trim();
        String addressTxt = address.getText().toString().trim();
        String cityTxt = city.getText().toString().trim();
        String emailTxt = email.getText().toString().trim();
        String passTxt = password.getText().toString().trim();
        boolean notif = notifications.isChecked();

        // 1. VALIDACIÓN CAMPOS VACÍOS (Rojo #D32F2F)
        if (nameTxt.isEmpty() || lastTxt.isEmpty() || phoneTxt.isEmpty() ||
                addressTxt.isEmpty() || cityTxt.isEmpty() || emailTxt.isEmpty() ||
                passTxt.isEmpty()) {
            showMsgCustom("Por favor, rellena todos los campos.", "#D32F2F");
            return;
        }

        // 2. VALIDACIÓN DE CONTRASEÑA SEGURA (RETO REGEX)
        if (!esContrasenaSegura(passTxt)) {
            showMsgCustom("La contraseña debe tener al menos 8 caracteres, incluir una mayúscula, un número y un símbolo (@#$%^&+=!).", "#D32F2F");
            return;
        }

        mAuth.createUserWithEmailAndPassword(emailTxt, passTxt)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            String uid = user.getUid();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", nameTxt);
                            userData.put("lastname", lastTxt);
                            userData.put("phone", phoneTxt);
                            userData.put("address", addressTxt);
                            userData.put("city", cityTxt);
                            userData.put("notifications", notif);
                            userData.put("email", emailTxt);
                            userData.put("createdAt", System.currentTimeMillis());
                            userData.put("activo", false);
                            userData.put("rol", "cliente");
                            userData.put("isAdmin", false);

                            db.collection("users").document(uid)
                                    .set(userData)
                                    .addOnSuccessListener(unused -> {
                                        Log.d(TAG, "Registro exitoso en Firestore");
                                        // ÉXITO: Azul corporativo (#0D47A1)
                                        showMsgCustom("Registro completado. Espera activación administrativa.", "#0D47A1");

                                        // Retardo opcional para que vean el mensaje antes de cambiar de pantalla
                                        btnRegister.postDelayed(() -> {
                                            startActivity(new Intent(this, LoginActivity.class));
                                            //startActivity(new Intent(this, HomeActivity.class));
                                            finish();
                                        }, 2000);
                                    })
                                    .addOnFailureListener(e -> {
                                        showMsgCustom("Error al guardar datos: " + e.getMessage(), "#D32F2F");
                                    });
                        }
                    } else {
                        // Gestión de errores de Auth (Mismo criterio que en Login)
                        showRegisterError(task.getException() != null ? task.getException().getMessage() : "Error desconocido");
                    }
                });
    }

    // MÉTODO PARA VALIDAR CONTRASEÑA CON EXPRESIÓN REGULAR
    private boolean esContrasenaSegura(String pass) {
        // Al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        return pass != null && pass.matches(regex);
    }

    // MÉTODO PARA ERRORES DE REGISTRO (Siguiendo el criterio de traducción)
    private void showRegisterError(String error) {
        String e = (error != null) ? error.toLowerCase() : "";
        String mensajeEspanol;

        if (e.contains("already in use") || e.contains("email-already-exists")) {
            mensajeEspanol = "Este correo electrónico ya está registrado.";
        } else if (e.contains("password") || e.contains("weak-password")) {
            mensajeEspanol = "La contraseña es demasiado débil (mín. 6 caracteres).";
        } else if (e.contains("badly formatted") || e.contains("invalid-email")) {
            mensajeEspanol = "El formato del correo es incorrecto.";
        } else {
            mensajeEspanol = "Error al registrar: " + error;
        }

        showMsgCustom(mensajeEspanol, "#D32F2F");
    }

    // MÉTODO AUXILIAR PARA SNACKBARS PERSONALIZADOS
    private void showMsgCustom(String mensaje, String colorHex) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(Color.parseColor(colorHex));
        snackbar.setTextColor(Color.WHITE);
        snackbar.setAction("OK", v -> snackbar.dismiss());
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }
}