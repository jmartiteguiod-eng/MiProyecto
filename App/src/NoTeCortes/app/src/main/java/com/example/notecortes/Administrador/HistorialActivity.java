package com.example.notecortes.Administrador;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notecortes.Clases.Cita;
import com.example.notecortes.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//La clase HistorialActivity actúa como el módulo de auditoría de operaciones de la barbería. A nivel
// arquitectónico, rompe la restricción jerárquica del modelo NoSQL de Firestore implementando una
// Consulta de Grupo de Colecciones (collectionGroup). Esta técnica permite indexar de forma transversal
// todas las subcolecciones independientes de horas en una sola petición síncrona al servidor, aplicando
// un filtro de pertenencia asociativa (whereIn) para segregar las citas cerradas de las activas.
// Además, optimiza la base de código del proyecto al reutilizar de forma polimórfica el AdminCitasAdapter
// para la renderización del histórico.
public class HistorialActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminCitasAdapter adapter;
    private List<Cita> listaHistorico;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial); // Necesitas crear este layout simple

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerHistorial);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listaHistorico = new ArrayList<>();
        // Reutilizamos tu adaptador
        adapter = new AdminCitasAdapter(listaHistorico, this);
        recyclerView.setAdapter(adapter);

        cargarHistorial();
    }

    private void cargarHistorial() {
        // Buscamos en todas las subcolecciones "horas" donde el estado no sea null
        db.collectionGroup("horas")
                .whereIn("estado", Arrays.asList("realizada", "no_realizada"))
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaHistorico.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Cita cita = doc.toObject(Cita.class);
                        if (cita != null) {
                            cita.setPath(doc.getReference().getPath());

                            // Cargamos el nombre del usuario (igual que en AdminCitasActivity)
                            cargarDatosUsuario(cita);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("HISTORIAL", "Error: " + e.getMessage()));
    }

    private void cargarDatosUsuario(Cita cita) {
        if (cita.getUserId() == null) {
            listaHistorico.add(cita);
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("users").document(cita.getUserId()).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        cita.setUserName(userDoc.getString("name"));
                        cita.setUserPhone(userDoc.getString("phone"));
                    }
                    listaHistorico.add(cita);
                    adapter.notifyDataSetChanged();
                });
    }
}