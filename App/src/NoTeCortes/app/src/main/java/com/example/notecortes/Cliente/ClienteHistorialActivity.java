package com.example.notecortes.Cliente;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notecortes.Clases.Cita;
import com.example.notecortes.Adaptador.ClienteHistorialAdapter;
import com.example.notecortes.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
//La clase ClienteHistorialActivity controla la interfaz del histórico de servicios del usuario.
// Técnicamente, destaca por implementar una consulta de arquitectura avanzada en Cloud Firestore
// basada en collectionGroup. Esto permite indexar y aplanar subcolecciones distribuidas de manera
// jerárquica en una sola petición atómica. Además, el flujo de datos aplica cláusulas de filtrado
// selectivo whereIn y algoritmos de ordenación cronológica inversa en memoria mediante expresiones
// Lambda para optimizar la experiencia de usuario (UX)
public class ClienteHistorialActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ClienteHistorialAdapter adapter;
    private List<Cita> listaHistorico;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cliente_historial);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerHistorial);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listaHistorico = new ArrayList<>();
        adapter = new ClienteHistorialAdapter(listaHistorico);
        recyclerView.setAdapter(adapter);

        if (mAuth.getCurrentUser() != null) {
            cargarHistorial(mAuth.getCurrentUser().getUid());
        } else {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void cargarHistorial(String userId) {
        // Consultamos solo las citas donde el userId coincida con el actual
        db.collectionGroup("horas")
                .whereEqualTo("userId", userId)
                .whereIn("estado", Arrays.asList("realizada", "no_realizada"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaHistorico.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Cita cita = doc.toObject(Cita.class);
                        if (cita != null) {
                            listaHistorico.add(cita);
                        }
                    }

                    // Ordenar por fecha (más reciente primero)
                    Collections.sort(listaHistorico, (c1, c2) -> c2.getFecha().compareTo(c1.getFecha()));

                    adapter.notifyDataSetChanged();

                    if (listaHistorico.isEmpty()) {
                        Toast.makeText(this, "No tienes citas en tu historial", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HISTORIAL", "Error: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                });
    }
}