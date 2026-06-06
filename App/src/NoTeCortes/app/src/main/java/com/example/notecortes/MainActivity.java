package com.example.notecortes;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.example.notecortes.Login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import androidx.activity.OnBackPressedCallback;
import android.os.Looper;
//La MainActivity actúa como la Splash Screen o pantalla de bienvenida de la aplicación.
// Su propósito es doble: por un lado, asienta la imagen de marca de la barbería durante 5 segundos y,
// por otro, inicializa el entorno de Firebase. Utiliza técnicas de vaciado de pila (Flags de Intent) y
// control estricto del ciclo de vida (onDestroy) para garantizar que la transición hacia el módulo de
// Login sea asíncrona, limpia y segura, impidiendo bucles de navegación infinitos al pulsar el botón
// físico de retroceso
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable = this::navigate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handler.removeCallbacks(runnable);
                finishAndRemoveTask();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        handler.postDelayed(runnable, 5000);
    }

    private void navigate() {
        if (isFinishing() || isDestroyed()) return;

        // IMPORTANTE: Siempre a LoginActivity.
        // Ella se encarga de ver si hay sesión y mandar a HomeActivity, Admin o Empleado.
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // SEGURIDAD: Si la actividad se destruye, cancelamos el hilo para que no salte después
        handler.removeCallbacks(runnable);
    }
}