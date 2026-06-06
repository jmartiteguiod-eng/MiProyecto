package com.example.notecortes.Administrador;

import androidx.activity.ComponentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.notecortes.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
//La clase EditarPromotionActivity implementa el flujo de actualización completa (Update) para el
// módulo de marketing estratégico. El componente destaca por su enfoque en la programación defensiva:
// utiliza la introspección de tipos en tiempo de ejecución mediante el operador instanceof para garantizar
// una deserialización polimórfica segura del atributo precio. Adicionalmente, desacopla la edición del
// formulario mediante persistencia por fusión (SetOptions.merge()) de la conmutación de disponibilidad
// comercial, la cual se resuelve mediante actualizaciones atómicas independientes sobre el flag activa
// para optimizar la reactividad del backend.
public class EditarPromotionActivity extends ComponentActivity {

    private static final String TAG = "EditarPromo";
    private EditText edtDescripcion, edtPrecio, edtDirigidoA, edtFechaInicio, edtFechaFin;
    private Button btnGuardarCambios, btnActivar, btnDesactivar, btnCancelar;

    private FirebaseFirestore db;
    private String promoId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_promotion);

        db = FirebaseFirestore.getInstance();

        // Obtener ID de la promoción
        promoId = getIntent().getStringExtra("promoId");
        if (promoId == null || promoId.isEmpty()) {
            Toast.makeText(this, "Error: ID de promoción no recibido", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Vincular vistas
        edtDescripcion = findViewById(R.id.edtDescripcionPromo);
        edtPrecio = findViewById(R.id.edtPrecioPromo);
        edtDirigidoA = findViewById(R.id.edtDirigidoA);
        edtFechaInicio = findViewById(R.id.edtFechaInicio);
        edtFechaFin = findViewById(R.id.edtFechaFin);

        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnActivar = findViewById(R.id.btnActivar);
        btnDesactivar = findViewById(R.id.btnDesactivar);
        btnCancelar = findViewById(R.id.btnCancelar);

        cargarPromocion();

        // Configurar botones
        btnGuardarCambios.setOnClickListener(v -> guardarCambios());
        btnActivar.setOnClickListener(v -> cambiarEstado(true));
        btnDesactivar.setOnClickListener(v -> cambiarEstado(false));
        btnCancelar.setOnClickListener(v -> finish()); // Cancelar suele ser solo cerrar la pantalla
    }

    private void cargarPromocion() {
        db.collection("promotions")
                .document(promoId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        edtDescripcion.setText(doc.getString("descripcion"));

                        // EXTRACCIÓN SEGURA DE PRECIO (Evita errores de tipo de dato)
                        Object precioObj = doc.get("precio");
                        if (precioObj instanceof Number) {
                            edtPrecio.setText(String.valueOf(((Number) precioObj).doubleValue()));
                        } else {
                            edtPrecio.setText("0.0");
                        }

                        edtDirigidoA.setText(doc.getString("dirigidoA"));
                        edtFechaInicio.setText(doc.getString("fechaInicio"));
                        edtFechaFin.setText(doc.getString("fechaFin"));
                    } else {
                        Toast.makeText(this, "La promoción ya no existe", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al cargar", e));
    }

    private void guardarCambios() {
        String descripcion = edtDescripcion.getText().toString().trim();
        String dirigidoA = edtDirigidoA.getText().toString().trim();
        String fechaInicio = edtFechaInicio.getText().toString().trim();
        String fechaFin = edtFechaFin.getText().toString().trim();
        String precioStr = edtPrecio.getText().toString().trim();

        if (descripcion.isEmpty() || dirigidoA.isEmpty() || fechaInicio.isEmpty() || fechaFin.isEmpty() || precioStr.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "El precio debe ser un número válido", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("descripcion", descripcion);
        updates.put("precio", precio);
        updates.put("dirigidoA", dirigidoA);
        updates.put("fechaInicio", fechaInicio);
        updates.put("fechaFin", fechaFin);

        db.collection("promotions")
                .document(promoId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cambios guardados correctamente", Toast.LENGTH_SHORT).show();
                    finish(); // Opcional: cerrar al guardar
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void cambiarEstado(boolean activa) {
        db.collection("promotions")
                .document(promoId)
                .update("activa", activa)
                .addOnSuccessListener(aVoid -> {
                    String msg = activa ? "Promoción Activada" : "Promoción Desactivada";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al cambiar estado", Toast.LENGTH_SHORT).show());
    }
}