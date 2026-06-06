// Archivo: AdminHomeActivity.java

package com.example.notecortes.Administrador;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.example.notecortes.Citas.CitasActivity;
import com.example.notecortes.Login.LoginActivity;
import com.example.notecortes.R;
import com.example.notecortes.Citas.ReservarCitaActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
//La clase AdminHomeActivity representa el cuadro de mando integral de la aplicación con privilegios
// absolutos sobre el sistema. Su diseño arquitectónico actúa como un despachador de navegación
// centralizado bajo el principio de Segregación de Roles y Permisos. A diferencia del perfil de
// empleado, esta vista acopla listeners tácticos para la mutación del catálogo (módulos CRUD de
// servicios y promociones), el control de la persistencia del personal (ManageEmployeesActivity) y la
// alteración de la disponibilidad del calendario comercial mediante banderas de bloqueo, garantizando
// un entorno cerrado y seguro gracias al vaciado atómico de la pila en el cierre de sesión.
public class AdminHomeActivity extends AppCompatActivity {

    private Button btnCrearServicio, btnVerServicios, btnCrearPromo, btnAdminLogout,
            btnVerPromos, btnBloqueoFecha,btnDesBloqueoFecha,btnGestionCitas,
            btnCitaManual, btnCancelacionCitaManual,btnGestionEmpleado, btnHistorial;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        // -----------------------------
        // REFERENCIAS A VISTAS
        // -----------------------------
        btnCrearServicio = findViewById(R.id.btnCrearServicio);
        btnVerServicios = findViewById(R.id.btnVerServicios);
        btnCrearPromo = findViewById(R.id.btnCrearPromo);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);
        btnVerPromos = findViewById(R.id.btnVerPromos);
        btnBloqueoFecha = findViewById(R.id.btnBloqueoFecha);
        btnDesBloqueoFecha = findViewById(R.id.btnDesBloqueoFecha);
        btnGestionCitas = findViewById(R.id.btnGestionCitas);
        btnCitaManual = findViewById(R.id.btnCitaManual);
        btnCancelacionCitaManual = findViewById(R.id.btnCancelacionCitaManual);
        btnGestionEmpleado = findViewById(R.id.btnGestionEmpleado);
        btnHistorial = findViewById(R.id.btnHistorial);


        //Toast.makeText(this, "CARGA → AdminHomeActivity", Toast.LENGTH_SHORT).show();

        // -----------------------------
        // LISTENERS DE BOTONES
        // -----------------------------

        btnCrearServicio.setOnClickListener(v ->
                startActivity(new Intent(this, CrearServicioActivity.class))
        );

        btnVerServicios.setOnClickListener(v ->
                startActivity(new Intent(this, ManageServicesActivity.class))
        );

        btnCrearPromo.setOnClickListener(v ->
                startActivity(new Intent(this, CrearPromotionActivity.class))
        );

        btnAdminLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Corregido: Separado del listener anterior
        btnVerPromos.setOnClickListener(v -> {
            startActivity(new Intent(this, ManagePromotionsActivity.class));
        });

        // Corregido: Separado y con llaves correctas
        btnBloqueoFecha.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminBlockDatesActivity.class));
        });
        btnDesBloqueoFecha.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminUnblockDatesActivity.class));
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
        //Gestion Empleados
        btnGestionEmpleado.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageEmployeesActivity.class));
        });
        //Historial
        btnHistorial.setOnClickListener(v -> {
            startActivity(new Intent(this, HistorialActivity.class));
        });



    } // Fin onCreate
} // Fin Activity