package com.example.notecortes.Empleado;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.example.notecortes.Administrador.AdminCitasActivity;
import com.example.notecortes.Administrador.HistorialActivity;
import com.example.notecortes.Citas.CitasActivity;
import com.example.notecortes.Login.LoginActivity;
import com.example.notecortes.Administrador.ManagePromotionsActivity;
import com.example.notecortes.Administrador.ManageServicesActivity;
import com.example.notecortes.R;
import com.example.notecortes.Citas.ReservarCitaActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
//La clase EmpleHomeActivity es el panel principal de operaciones para los trabajadores de la barbería
// 'No Te Cortes'. Aplica el principio de segregación de funciones: oculta las herramientas de edición
// masiva (reservadas al administrador) y expone funcionalidades tácticas como la visualización del
// catálogo, la auditoría del historial y la gestión manual de citas (altas y bajas) para dar soporte a
// clientes telefónicos o presenciales, garantizando un flujo cerrado mediante cierres de sesión atómicos
public class EmpleHomeActivity extends AppCompatActivity {

    private Button  btnAdminLogout, btnVerPromos,   btnGestionCitas,
            btnCitaManual, btnCancelacionCitaManual,  btnHistorial, btnVerServicios;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emple_home);

        // -----------------------------
        // REFERENCIAS A VISTAS
        // -----------------------------
        //btnCrearServicio = findViewById(R.id.btnCrearServicio);
        btnVerServicios = findViewById(R.id.btnVerServicios);
       // btnCrearPromo = findViewById(R.id.btnCrearPromo);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);
        btnVerPromos = findViewById(R.id.btnVerPromos);
        //btnBloqueoFecha = findViewById(R.id.btnBloqueoFecha);
        //btnDesBloqueoFecha = findViewById(R.id.btnDesBloqueoFecha);
        btnGestionCitas = findViewById(R.id.btnGestionCitas);
        btnCitaManual = findViewById(R.id.btnCitaManual);
        btnCancelacionCitaManual = findViewById(R.id.btnCancelacionCitaManual);
        //btnGestionEmpleado = findViewById(R.id.btnGestionEmpleado);
        btnHistorial = findViewById(R.id.btnHistorial);


        //Toast.makeText(this, "CARGA → AdminHomeActivity", Toast.LENGTH_SHORT).show();

        // -----------------------------
        // LISTENERS DE BOTONES
        // -----------------------------


        btnAdminLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        btnVerServicios.setOnClickListener(v ->
                startActivity(new Intent(this, ManageServicesActivity.class))
        );

        // Corregido: Separado del listener anterior
        btnVerPromos.setOnClickListener(v -> {
            startActivity(new Intent(this, ManagePromotionsActivity.class));
        });

        //Gestion de citas clientes
        btnGestionCitas.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminCitasActivity.class));
        });

        //Cita Manual
        btnCitaManual.setOnClickListener(v -> {
            startActivity(new Intent(this, ReservarCitaActivity.class));
        });
        //Cancelacion Cita Manual
        btnCancelacionCitaManual.setOnClickListener(v -> {
            startActivity(new Intent(this, CitasActivity.class));
        });

        //Historial
        btnHistorial.setOnClickListener(v -> {
            startActivity(new Intent(this, HistorialActivity.class));
        });


    } // Fin onCreate
}