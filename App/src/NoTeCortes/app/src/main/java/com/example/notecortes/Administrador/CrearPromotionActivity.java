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
//La clase CrearPromotionActivity encapsula la operación de escritura e inserción del modelo de
// marketing de la aplicación. Arquitectónicamente, delega la creación de la clave primaria en el
// motor de Cloud Firestore mediante el método .add(), lo que genera identificadores atómicos únicos y
//  aleatorios. El documento se inicializa de forma estricta con el flag de control de estado
//  activa = true, inyectando la promoción en caliente dentro del flujo de datos indexado que consume
//  la interfaz del cliente
public class CrearPromotionActivity extends AppCompatActivity {

    private EditText edtDescripcion;
    private Button btnGuardarPromo;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_promotion);

        // Solo inicializamos la descripción y el botón
        edtDescripcion = findViewById(R.id.edtDescripcion);
        btnGuardarPromo = findViewById(R.id.btnGuardarPromo);

        db = FirebaseFirestore.getInstance();

        btnGuardarPromo.setOnClickListener(v -> guardarPromocion());
    }

    private void guardarPromocion() {
        String descripcion = edtDescripcion.getText().toString().trim();

        if (descripcion.isEmpty()) {
            Toast.makeText(this, "La descripción es obligatoria", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creamos el objeto para Firestore
        Map<String, Object> promocion = new HashMap<>();
        promocion.put("descripcion", descripcion);
        promocion.put("activa", true); // <--- Aquí se pone true por defecto

        db.collection("promotions")
                .add(promocion)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Promoción creada", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_LONG).show());
    }
}