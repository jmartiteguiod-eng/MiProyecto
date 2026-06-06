package com.example.notecortes.Cliente;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.notecortes.Citas.CitasActivity;
import com.example.notecortes.Login.LoginActivity;
import com.example.notecortes.Login.PerfilActivity;
import com.example.notecortes.R;
import com.example.notecortes.Citas.ReservarCitaActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
//La HomeActivity actúa como el panel neurálgico del cliente. Su arquitectura implementa un consumo
// asíncrono en loadUserName para recuperar de forma reactiva el perfil del usuario desde Cloud Firestore,
// gestionando excepciones de conexión mediante cadenas de texto de seguridad (Fallback). Además,
// desacopla la navegación secundaria mediante un componente BottomNavigationView que procesa los
// eventos táctiles del menú inferior a través de selectores condicionales (setOnItemSelectedListener),
// asegurando un flujo fluido por las distintas vistas operativas de la aplicación
public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textWelcome = findViewById(R.id.textWelcome);
        Button btnLogout = findViewById(R.id.btnLogout);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        //Toast.makeText(this, "CARGA → HomeActivity", Toast.LENGTH_SHORT).show();

        loadUserName();

        btnLogout.setOnClickListener(view -> {
            mAuth.signOut();
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        });

        Button btnCitas = findViewById(R.id.btnCitas);
        Button btnReservar = findViewById(R.id.btnReservar);
        Button btnPromos = findViewById(R.id.btnPromos);
        btnCitas.setOnClickListener(v ->
                startActivity(new Intent(this, CitasActivity.class))
        );

        btnReservar.setOnClickListener(v ->
                startActivity(new Intent(this, ReservarCitaActivity.class))
        );

        btnPromos.setOnClickListener(v ->
                startActivity(new Intent(this, ClientPromotionsActivity.class))
        );


        //Button btnCitas = findViewById(R.id.btnCitas);

        btnCitas.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ListaServiciosActivity.class))
        );

        bottomNav.setSelectedItemId(R.id.nav_history);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history){

                startActivity(new Intent(this, ClienteHistorialActivity.class));
                return true;
            }

            if (id == R.id.nav_citas) {
                startActivity(new Intent(this, CitasActivity.class));
                return true;
            }

            if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                return true;
            }

            return false;
        });
    }

    private void loadUserName() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            textWelcome.setText("Hola, Invitado");
            return;
        }

        String uid = user.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        if (name != null) {
                            textWelcome.setText("Hola, " + name);
                        } else {
                            textWelcome.setText("Hola, Usuario");
                        }
                    } else {
                        textWelcome.setText("Hola, Usuario no encontrado");
                    }
                })
                .addOnFailureListener(e -> {
                    textWelcome.setText("Hola, Error al cargar tu nombre");
                });
    }
}
