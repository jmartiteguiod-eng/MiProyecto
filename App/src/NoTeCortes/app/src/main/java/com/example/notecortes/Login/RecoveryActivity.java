package com.example.notecortes.Login;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import com.example.notecortes.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
//La clase RecoveryActivity gestiona el flujo asíncrono para la recuperación de accesos mediante
// el método sendPasswordResetEmail del SDK de Firebase. El componente destaca por su enfoque en la
//  experiencia de usuario (UX) y el manejo de excepciones: procesa y traduce en tiempo de ejecución
//  las respuestas de error nativas del backend hacia cadenas semánticas claras en español, y utiliza
//  un temporizador diferido de 3 segundos antes de invocar el método finish(), asegurando que el cliente
//   reciba la confirmación visual de forma óptima antes de retornar automáticamente a la pantalla de
//   login.
public class RecoveryActivity extends AppCompatActivity {

    private EditText emailRecover;
    private Button btnSendRecover;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recover_password);

        mAuth = FirebaseAuth.getInstance();

        emailRecover = findViewById(R.id.emailRecover);
        btnSendRecover = findViewById(R.id.btnSendRecover);

        btnSendRecover.setOnClickListener(v -> sendRecoveryEmail());
    }

    private void sendRecoveryEmail() {
        String email = emailRecover.getText().toString().trim();

        if (email.isEmpty()) {
            mostrarMensaje("Introduce tu correo electrónico", true);
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Mensaje de éxito en AZUL corporativo
                        mostrarMensaje("Correo enviado. Revisa bandeja de entrada y SPAM.", false);

                        // Cerramos la actividad después de un breve delay para que vean el mensaje
                        emailRecover.postDelayed(this::finish, 3000);
                    } else {
                        // Gestión de error traducido
                        String error = (task.getException() != null) ? task.getException().getMessage() : "";
                        procesarErrorFirebase(error);
                    }
                });
    }

    private void procesarErrorFirebase(String error) {
        String mensajeEspanol;
        String e = (error != null) ? error.toLowerCase() : "";

        if (e.contains("no user record") || e.contains("user not found")) {
            mensajeEspanol = "No existe ninguna cuenta asociada a este correo.";
        } else if (e.contains("badly formatted") || e.contains("format") || e.contains("invalid email")) {
            mensajeEspanol = "El formato del correo electrónico es inválido.";
        } else if (e.contains("network") || e.contains("connection")) {
            mensajeEspanol = "Sin conexión a internet.";
        } else if (e.contains("too many requests")) {
            mensajeEspanol = "Demasiados intentos. Inténtalo más tarde.";
        } else {
            mensajeEspanol = "Error: No se pudo enviar el correo de recuperación.";
        }

        mostrarMensaje(mensajeEspanol, true);
    }

    private void mostrarMensaje(String texto, boolean esError) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), texto, Snackbar.LENGTH_LONG);

        if (esError) {
            snackbar.setBackgroundTint(Color.parseColor("#D32F2F")); // Rojo No Te Cortes
        } else {
            snackbar.setBackgroundTint(Color.parseColor("#0D47A1")); // Azul No Te Cortes
        }

        snackbar.setTextColor(Color.WHITE);
        snackbar.setAction("OK", v -> snackbar.dismiss());
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }
}