package com.example.notecortes.Administrador;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.notecortes.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
//La clase AdminUnblockDatesActivity cierra el ciclo de gestión de disponibilidad de la
// agenda del negocio, encargándose de la eliminación de restricciones temporales. Su
// arquitectura destaca por implementar un patrón de indexación paralela en memoria: sincroniza una
// estructura de cadenas tipadas para alimentar el ArrayAdapter visual con una colección de instantáneas
// (DocumentSnapshot) en segundo plano. Esto permite que, tras la confirmación de una alerta de seguridad
// (AlertDialog), el sistema ejecute una baja física en Cloud Firestore mediante una resolución directa
// por referencia de puntero (doc.getReference().delete()), refrescando la interfaz de manera inmediata.
public class AdminUnblockDatesActivity extends AppCompatActivity {

    private ListView listViewFechas;
    private FirebaseFirestore db;

    private ArrayList<String> fechas = new ArrayList<>();
    private ArrayList<DocumentSnapshot> docs = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_unblock_dates);

        listViewFechas = findViewById(R.id.listViewFechas);
        db = FirebaseFirestore.getInstance();

        //adapter = new ArrayAdapter<>(
          //      this,
           //     android.R.layout.simple_list_item_1,
            //    fechas
        //);

        adapter = new ArrayAdapter<String>(this, R.layout.item_fecha_bloqueada, R.id.textFechaItem, fechas);
        listViewFechas.setAdapter(adapter);


        //);

        listViewFechas.setAdapter(adapter);

        cargarFechasBloqueadas();

        listViewFechas.setOnItemClickListener((parent, view, position, id) ->
                confirmarDesbloqueo(position)
        );
    }

    // ------------------------------------------------
    // CARGAR FECHAS BLOQUEADAS
    // ------------------------------------------------
    private void cargarFechasBloqueadas() {
        db.collection("blocked_dates")
                .get()
                .addOnSuccessListener(query -> {
                    fechas.clear();
                    docs.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        docs.add(doc);
                        fechas.add(doc.getId()); // El ID ES la fecha
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error cargando fechas",
                                Toast.LENGTH_LONG).show()
                );
    }

    // ------------------------------------------------
    // CONFIRMAR DESBLOQUEO
    // ------------------------------------------------

    private void confirmarDesbloqueo(int position) {
        DocumentSnapshot doc = docs.get(position);
        String fecha = doc.getId();

        new AlertDialog.Builder(this)
                .setTitle("Desbloquear fecha")
                .setMessage("¿Quieres desbloquear el día " + fecha + "?")
                .setPositiveButton("Desbloquear", (d, w) -> {

                    doc.getReference().delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this,
                                        "Fecha desbloqueada",
                                        Toast.LENGTH_SHORT).show();
                                cargarFechasBloqueadas();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Error al desbloquear",
                                            Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }





}
