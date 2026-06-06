package com.example.notecortes.Administrador;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notecortes.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
//La clase ManagePromotionsActivity completa el ecosistema de marketing de la aplicación implementando
// las operaciones de lectura, modificación y eliminación sobre la colección promotions de Cloud
// Firestore. Su principal fuerte técnico radica en la optimización de vistas mediante Programación de
// Interfaces Dinámicas (In-Memory View Building): el formulario de edición se genera e inyecta
// dinámicamente en tiempo de ejecución dentro de un contenedor AlertDialog, prescindiendo de layouts
// XML redundantes. Los cambios de estado binarios se resuelven mediante el uso de un componente Switch
// y la consistencia de datos local se asegura forzando llamadas cíclicas al método de descarga tras
//  cada operación asíncrona exitosa.
public class ManagePromotionsActivity extends AppCompatActivity {

    private ListView listViewPromos;
    private FirebaseFirestore db;

    private ArrayList<String> promoNames = new ArrayList<>();
    private ArrayList<DocumentSnapshot> promoDocs = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Asegúrate de que el layout se llame activity_manage_promotions
        setContentView(R.layout.activity_manage_promotions);

        listViewPromos = findViewById(R.id.listViewPromos);
        db = FirebaseFirestore.getInstance();

        //adapter = new ArrayAdapter<>(
                //this,
                //android.R.layout.simple_list_item_1,
                //promoNames
        //);
        adapter = new ArrayAdapter<>(
                this,
                R.layout.item_promo,      // 1. Usamos tu layout personalizado
                R.id.textNombrePromo,     // 2. Indicamos qué TextView recibe el texto (el azul)
                promoNames                // 3. Tu lista de datos
        );

        listViewPromos.setAdapter(adapter);

        loadPromotions();

        listViewPromos.setOnItemClickListener((parent, view, position, id) ->
                showPromoOptions(position)
        );
    }

    private void loadPromotions() {
        db.collection("promotions")
                .get()
                .addOnSuccessListener(query -> {
                    promoNames.clear();
                    promoDocs.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        promoDocs.add(doc);

                        String desc = doc.getString("descripcion");
                        Boolean activa = doc.getBoolean("activa");

                        if (desc == null) desc = "Sin descripción";
                        if (activa == null) activa = false;

                        String estado = activa ? "🟢 ACTIVA" : "🔴 NO ACTIVA";
                        promoNames.add(desc + "\n" + estado);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error cargando promociones", Toast.LENGTH_LONG).show()
                );
    }

    private void showPromoOptions(int position) {
        String[] options = {"Modificar", "Borrar"};

        new AlertDialog.Builder(this)
                .setTitle("Opciones de Promoción")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(position);
                    } else {
                        confirmDelete(position);
                    }
                })
                .show();
    }

    private void showEditDialog(int position) {
        DocumentSnapshot doc = promoDocs.get(position);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Campo Descripción
        TextView txtDesc = new TextView(this);
        txtDesc.setText("Descripción:");
        layout.addView(txtDesc);

        EditText edtDesc = new EditText(this);
        edtDesc.setText(doc.getString("descripcion"));
        layout.addView(edtDesc);

        // Switch para Activa/Inactiva
        Switch swActiva = new Switch(this);
        swActiva.setText("¿Promoción activa?");
        swActiva.setPadding(0, 30, 0, 30);
        Boolean estadoActual = doc.getBoolean("activa");
        swActiva.setChecked(estadoActual != null && estadoActual);
        layout.addView(swActiva);

        new AlertDialog.Builder(this)
                .setTitle("Modificar promoción")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevaDesc = edtDesc.getText().toString().trim();
                    boolean nuevoEstado = swActiva.isChecked();

                    if (nuevaDesc.isEmpty()) {
                        Toast.makeText(this, "La descripción no puede estar vacía", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("descripcion", nuevaDesc);
                    update.put("activa", nuevoEstado);

                    doc.getReference().update(update)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Promoción actualizada", Toast.LENGTH_SHORT).show();
                                loadPromotions();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al actualizar", Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDelete(int position) {
        DocumentSnapshot doc = promoDocs.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Eliminar promoción")
                .setMessage("¿Estás seguro?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    doc.getReference().delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Eliminada", Toast.LENGTH_SHORT).show();
                                loadPromotions();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al borrar", Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}