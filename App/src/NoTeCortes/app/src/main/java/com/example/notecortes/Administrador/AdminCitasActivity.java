package com.example.notecortes.Administrador;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notecortes.Clases.Cita;
import com.example.notecortes.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
//La clase AdminCitasActivity gestiona la agenda diaria de la barbería para los perfiles de gestión.
// Su arquitectura destaca por realizar un proceso de hidratación de datos reactiva: recupera los nodos
// de reserva filtrados por la estructura cronológica de Firestore (yyyy-MM-dd) y, mediante consultas
// asíncronas encadenadas, resuelve el modelo relacional buscando las propiedades del cliente
// (name, phone) en la colección users. Para asegurar la estabilidad de la interfaz frente a múltiples
// peticiones simultáneas, el refresco del adaptador se encapsula de forma segura dentro del hilo de
// interfaz de usuario mediante runOnUiThread.
public class AdminCitasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminCitasAdapter adapter;
    private List<Cita> listaCitas;

    private FirebaseFirestore db;

    private TextView txtFechaSeleccionada;
    private Button btnElegirFecha;

    private String fechaSeleccionada; // yyyy-MM-dd

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_citas);

        recyclerView = findViewById(R.id.recyclerCitas);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        txtFechaSeleccionada = findViewById(R.id.txtFechaSeleccionada);
        btnElegirFecha = findViewById(R.id.btnElegirFecha);

        listaCitas = new ArrayList<>();
        adapter = new AdminCitasAdapter(listaCitas, this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        btnElegirFecha.setOnClickListener(v -> mostrarSelectorFecha());
    }

    // -------------------------------------------------
    // SELECTOR DE FECHA
    // -------------------------------------------------
    private void mostrarSelectorFecha() {
        Calendar c = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    month++; // Mes empieza en 0

                    fechaSeleccionada = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year, month, dayOfMonth
                    );

                    txtFechaSeleccionada.setText("Fecha: " + fechaSeleccionada);
                    cargarCitasPorFecha(fechaSeleccionada);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // -------------------------------------------------
    // CARGAR CITAS POR FECHA
    // -------------------------------------------------
    private void cargarCitasPorFecha(String fecha) {
        db.collection("citas")
                .document(fecha)
                .collection("horas")
                .get()
                .addOnSuccessListener(query -> {
                    listaCitas.clear();
                    // Notificamos el cambio total para limpiar la vista previa
                    adapter.notifyDataSetChanged();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Cita cita = doc.toObject(Cita.class);
                        if (cita == null) continue;

                        // IMPORTANTE: Guardamos el path y datos básicos de inmediato
                        cita.setPath(doc.getReference().getPath());

                        String userId = doc.getString("userId");

                        if (userId == null || userId.isEmpty()) {
                            cita.setUserName("Sin usuario");
                            agregarCitaALista(cita);
                        } else {
                            // Buscamos el usuario
                            db.collection("users").document(userId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            cita.setUserName(userDoc.getString("name"));
                                            cita.setUserPhone(userDoc.getString("phone"));
                                            cita.setUserEmail(userDoc.getString("email"));
                                        } else {
                                            cita.setUserName("Usuario no existe");
                                        }
                                        agregarCitaALista(cita);
                                    })
                                    .addOnFailureListener(e -> {
                                        cita.setUserName("Error al cargar");
                                        agregarCitaALista(cita);
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ADMIN_CITAS", "Error citas", e));
    }

    // Método auxiliar para evitar duplicar código y errores de hilos
    private void agregarCitaALista(Cita cita) {
        listaCitas.add(cita);
        // Usamos el hilo principal para actualizar la UI por seguridad
        runOnUiThread(() -> adapter.notifyItemInserted(listaCitas.size() - 1));
    }
}
