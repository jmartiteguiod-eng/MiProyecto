package com.example.notecortes.Cliente;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notecortes.R;
import com.example.notecortes.Clases.Service;
import com.example.notecortes.Adaptador.ServiceAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
//La clase ListaServiciosActivity actúa como el controlador de la vista del catálogo comercial de la
// barbería. A nivel de arquitectura, implementa un consumo desacoplado en el método cargarServicios
//  que explota la conversión reflexiva del SDK de Cloud Firestore mediante toObject(). Esto nos permite
//  mapear estructuras JSON de la nube en objetos de tipo Service en tiempo de ejecución de manera
//  automatizada. Además, incluye un control visual estricto para ocultar la barra de soporte nativa
//  (SupportActionBar) en favor de una experiencia de usuario (UX) más limpia, homogénea y minimalista
public class ListaServiciosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ServiceAdapter adapter;
    private List<Service> serviceList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_servicios);

        // Ocultar Action Bar para un look más moderno si es necesario
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        recyclerView = findViewById(R.id.rvServicios);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        serviceList = new ArrayList<>();
        adapter = new ServiceAdapter(serviceList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        cargarServicios();
    }

    private void cargarServicios() {
        db.collection("services")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    serviceList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Mapeo automático de Firestore a objeto Service
                        Service service = doc.toObject(Service.class);
                        serviceList.add(service);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al conectar con Firestore", Toast.LENGTH_SHORT).show();
                });
    }
}
