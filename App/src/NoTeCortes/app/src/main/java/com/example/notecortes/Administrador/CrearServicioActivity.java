package com.example.notecortes.Administrador;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.notecortes.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
//La clase CrearServicioActivity implementa la persistencia de inserción para el catálogo comercial
// de la barbería. A nivel arquitectónico, destaca por realizar una conversión de tipos explícita
// (Parsing) en el lado del cliente, transformando las entradas de texto de la interfaz en tipos
// numéricos primitivos (Integer y Double) equivalentes a los atributos de nuestra entidad Service.
// El documento se añade a Cloud Firestore delegando la clave primaria mediante .add(), finalizando el
// ciclo de vida de la actividad con finish() para optimizar el flujo de navegación del administrador
public class CrearServicioActivity extends AppCompatActivity {

    private EditText edtNombreServicio, edtDuracion, edtPrecio;
    private Button btnGuardarServicio;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_servicio);

        edtNombreServicio = findViewById(R.id.edtNombreServicio);
        edtDuracion = findViewById(R.id.edtDuracion);
        edtPrecio = findViewById(R.id.edtPrecio);
        btnGuardarServicio = findViewById(R.id.btnGuardarServicio);

        db = FirebaseFirestore.getInstance();

        btnGuardarServicio.setOnClickListener(v -> guardarServicio());
    }

    private void guardarServicio() {

        String nombre = edtNombreServicio.getText().toString().trim();
        String duracion = edtDuracion.getText().toString().trim();
        String precio = edtPrecio.getText().toString().trim();

        if (nombre.isEmpty() || duracion.isEmpty() || precio.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> servicio = new HashMap<>();
        servicio.put("nombre", nombre);
        servicio.put("duracion", Integer.parseInt(duracion));
        servicio.put("precio", Double.parseDouble(precio));

        db.collection("services")
                .add(servicio)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Servicio creado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al crear servicio", Toast.LENGTH_LONG).show());
    }
}
