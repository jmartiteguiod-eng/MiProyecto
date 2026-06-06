package com.example.notecortes.Cliente;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.notecortes.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
//No activa
public class ReservarCitaActivity2 extends AppCompatActivity {

    private TextView tvServices;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reserva_cita2);

        tvServices = findViewById(R.id.tvServices);

        loadServicesFromFirestore();
    }

    private void loadServicesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("services")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder sb = new StringBuilder();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String nombre = doc.getString("nombre");
                        if (nombre != null) {
                            sb.append(nombre).append("\n");
                            Log.d("Firestore", "Servicio: " + nombre);
                        } else {
                            Log.w("Firestore", "Documento sin campo 'nombre': " + doc.getId());
                        }
                    }
                    tvServices.setText(sb.toString());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al obtener servicios", e);
                    tvServices.setText("Error al cargar servicios");
                });
    }
}
