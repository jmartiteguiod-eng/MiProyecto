package com.example.notecortes.Administrador;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notecortes.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
//La clase AdminBlockDatesActivity proporciona al rol de administración una herramienta táctica para la
// gestión del calendario laboral de la barbería. Técnicamente, abstrae la captura de datos temporales
// mediante un componente DatePickerDialog y normaliza la salida bajo el estándar numérico yyyy-MM-dd.
// Esta cadena se utiliza como identificador único y clave primaria en la colección blocked_dates de
// Cloud Firestore, asegurando la idempotencia de la base de datos (evitando duplicados) y sirviendo como
// bandera de bloqueo para el módulo de reservas de los clientes.
public class AdminBlockDatesActivity extends AppCompatActivity {

    private TextView txtFecha;
    private Button btnSeleccionarFecha, btnBloquearFecha;
    private FirebaseFirestore db;

    private String fechaSeleccionada = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_block_dates);

        txtFecha = findViewById(R.id.txtFecha);
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha);
        btnBloquearFecha = findViewById(R.id.btnBloquearFecha);

        db = FirebaseFirestore.getInstance();

        btnSeleccionarFecha.setOnClickListener(v -> mostrarDatePicker());

        btnBloquearFecha.setOnClickListener(v -> bloquearFecha());
    }

    private void mostrarDatePicker() {
        Calendar c = Calendar.getInstance();

        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {

                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    SimpleDateFormat sdf =
                            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    fechaSeleccionada = sdf.format(selected.getTime());
                    txtFecha.setText("Fecha: " + fechaSeleccionada);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void bloquearFecha() {
        if (fechaSeleccionada == null) {
            Toast.makeText(this,
                    "Selecciona una fecha",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("blocked_dates")
                .document(fechaSeleccionada)
                .set(new java.util.HashMap<String, Object>() {{
                    put("motivo", "Bloqueo administrador");
                    put("createdAt", System.currentTimeMillis());
                }})
                .addOnSuccessListener(v ->
                        Toast.makeText(this,
                                "Fecha bloqueada correctamente",
                                Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error al bloquear fecha",
                                Toast.LENGTH_LONG).show()
                );
    }
}
