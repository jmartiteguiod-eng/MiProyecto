package com.example.notecortes.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.notecortes.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
//La clase PerfilActivity gestiona el ciclo de persistencia de los datos maestros del cliente.
// Implementa un patrón de lectura asíncrona en cargarPerfil para poblar los campos del formulario
// desde Cloud Firestore y una operación de actualización selectiva mediante el método .update().
//  Esta última técnica es crítica para la seguridad del sistema, ya que modifica exclusivamente los
//  atributos de perfil expuestos al usuario, salvaguardando campos sensibles de control de acceso como
//  los roles o los estados de activación de la cuenta.
public class PerfilActivity extends AppCompatActivity {

    private EditText editName, editLastname, editPhone, editAddress, editCity, editEmail;
    private CheckBox checkNotifications;
    private Button btnGuardarPerfil, btnCerrarSesion;
            //btnReservarCita,

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        uid = user.getUid();

        editName = findViewById(R.id.editName);
        editLastname = findViewById(R.id.editLastname);
        editPhone = findViewById(R.id.editPhone);
        editAddress = findViewById(R.id.editAddress);
        editCity = findViewById(R.id.editCity);
        editEmail = findViewById(R.id.editEmail);
        checkNotifications = findViewById(R.id.checkNotifications);

        btnGuardarPerfil = findViewById(R.id.btnGuardarPerfil);
        //btnReservarCita = findViewById(R.id.btnReservarCita);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        cargarPerfil();

        btnGuardarPerfil.setOnClickListener(v -> guardarPerfil());

        //btnReservarCita.setOnClickListener(v ->
               // startActivity(new Intent(this, CitasActivity.class))
       // );

        btnCerrarSesion.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void cargarPerfil() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        editName.setText(doc.getString("name"));
                        editLastname.setText(doc.getString("lastname"));
                        editPhone.setText(doc.getString("phone"));
                        editAddress.setText(doc.getString("address"));
                        editCity.setText(doc.getString("city"));
                        editEmail.setText(doc.getString("email"));

                        Boolean notif = doc.getBoolean("notifications");
                        if (notif != null) {
                            checkNotifications.setChecked(notif);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                );
    }

    private void guardarPerfil() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", editName.getText().toString().trim());
        data.put("lastname", editLastname.getText().toString().trim());
        data.put("phone", editPhone.getText().toString().trim());
        data.put("address", editAddress.getText().toString().trim());
        data.put("city", editCity.getText().toString().trim());
        data.put("notifications", checkNotifications.isChecked());

        db.collection("users").document(uid)
                .update(data)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                );
    }
}
