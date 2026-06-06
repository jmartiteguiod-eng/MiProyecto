package com.example.notecortes;
import android.app.Application;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import com.example.notecortes.Login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
//La clase MyApp extiende de Application para gestionar el ciclo de vida global de la app.
// Hemos implementado un patron de diseño de Centralización de Excepciones No Controladas
// (UncaughtExceptionHandler). Su objetivo es aportar una tolerancia a fallos de nivel empresarial:
// si ocurre un error crítico imprevisto en cualquier hilo de ejecución, el sistema intercepta el crash,
// fuerza el cierre de sesión en Firebase para proteger la integridad de los datos del usuario, destruye
// el proceso corrupto en la memoria del dispositivo y redirige el flujo de forma automática hacia la
// LoginActivity de manera transparente y limpia.
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {

            Log.e("APP_CRASH", "Crash detectado. Forzando logout", throwable);

            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception e) {
                Log.e("APP_CRASH", "Error al cerrar sesión", e);
            }

            // Reiniciar a LoginActivity
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            );
            startActivity(intent);

            // Matar el proceso para evitar estado corrupto
            Process.killProcess(Process.myPid());
            System.exit(1);
        });
    }
}
