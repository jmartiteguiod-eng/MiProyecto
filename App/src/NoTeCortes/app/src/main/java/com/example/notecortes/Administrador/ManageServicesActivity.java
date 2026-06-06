package com.example.notecortes.Administrador;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.notecortes.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
//La clase ManageServicesActivity centraliza la gestión del inventario operativo y catálogo de la
// barbería. Su arquitectura destaca por aplicar restricciones de entrada por tipo de datos
// (InputType.TYPE_CLASS_NUMBER) combinadas con bloques de control de excepciones try-catch para asegurar
// que las mutaciones sobre los campos numéricos de duracion y precio sean estables y no corrompan el
// esquema NoSQL. Al igual que el módulo de marketing, implementa la persistencia de cambios directamente
// por referencia de puntero (doc.getReference().update()), forzando una recarga síncrona en cascada para
// mantener la interfaz actualizada
public class ManageServicesActivity extends AppCompatActivity {

    private ListView listViewServices;
    private FirebaseFirestore db;

    private ArrayList<String> serviceNames = new ArrayList<>();
    private ArrayList<DocumentSnapshot> serviceDocs = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_services);

        listViewServices = findViewById(R.id.listViewServices);
        db = FirebaseFirestore.getInstance();

        //adapter = new ArrayAdapter<>(
               // this,
                //android.R.layout.simple_list_item_1,
                //serviceNames
        //);
        adapter = new ArrayAdapter<>(
                this,
                R.layout.item_promo,       // El diseño de la tarjeta
                R.id.textNombrePromo,      // El TextView que se pintará de azul
                serviceNames               // Tu lista de servicios (nombres)
        );





        listViewServices.setAdapter(adapter);

        loadServices();

        listViewServices.setOnItemClickListener((parent, view, position, id) ->
                showServiceOptions(position)
        );
    }

    // ------------------------------------------------
    // CARGAR SERVICIOS
    // ------------------------------------------------
    private void loadServices() {
        db.collection("services")
                .get()
                .addOnSuccessListener(query -> {
                    serviceNames.clear();
                    serviceDocs.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        serviceDocs.add(doc);

                        String nombre = doc.getString("nombre");
                        Long duracion = doc.getLong("duracion");
                        Long precio = doc.getLong("precio");

                        if (nombre == null) nombre = "Sin nombre";
                        if (duracion == null) duracion = 0L;
                        if (precio == null) precio = 0L;

                        String texto = nombre +
                                " - " + duracion + " min - " +
                                precio + " €";

                        serviceNames.add(texto);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error cargando servicios", Toast.LENGTH_LONG).show()
                );
    }

    // ------------------------------------------------
    // OPCIONES
    // ------------------------------------------------
    private void showServiceOptions(int position) {
        String[] options = {"Modificar", "Borrar"};

        new AlertDialog.Builder(this)
                .setTitle("Servicio")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(position);
                    } else {
                        confirmDelete(position);
                    }
                })
                .show();
    }

    // ------------------------------------------------
    // MODIFICAR SERVICIO
    // ------------------------------------------------
    private void showEditDialog(int position) {
        DocumentSnapshot doc = serviceDocs.get(position);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 10);

        EditText edtNombre = new EditText(this);
        edtNombre.setHint("Nombre del servicio");
        edtNombre.setText(doc.getString("nombre"));
        layout.addView(edtNombre);

        EditText edtDuracion = new EditText(this);
        edtDuracion.setHint("Duración (minutos)");
        edtDuracion.setInputType(InputType.TYPE_CLASS_NUMBER);
        Long duracion = doc.getLong("duracion");
        if (duracion != null) edtDuracion.setText(String.valueOf(duracion));
        layout.addView(edtDuracion);

        EditText edtPrecio = new EditText(this);
        edtPrecio.setHint("Precio (€)");
        edtPrecio.setInputType(InputType.TYPE_CLASS_NUMBER);
        Long precio = doc.getLong("precio");
        if (precio != null) edtPrecio.setText(String.valueOf(precio));
        layout.addView(edtPrecio);

        new AlertDialog.Builder(this)
                .setTitle("Modificar servicio")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {

                    String nombre = edtNombre.getText().toString().trim();
                    String duracionStr = edtDuracion.getText().toString().trim();
                    String precioStr = edtPrecio.getText().toString().trim();

                    if (nombre.isEmpty() || duracionStr.isEmpty() || precioStr.isEmpty()) {
                        Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int duracionMin;
                    int precioEur;

                    try {
                        duracionMin = Integer.parseInt(duracionStr);
                        precioEur = Integer.parseInt(precioStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Duración o precio inválidos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("nombre", nombre);
                    update.put("duracion", duracionMin);
                    update.put("precio", precioEur);

                    doc.getReference().update(update)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Servicio actualizado", Toast.LENGTH_SHORT).show();
                                loadServices();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al actualizar", Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ------------------------------------------------
    // BORRAR SERVICIO
    // ------------------------------------------------
    private void confirmDelete(int position) {
        DocumentSnapshot doc = serviceDocs.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Eliminar servicio")
                .setMessage("¿Seguro que deseas eliminar este servicio?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    doc.getReference().delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Servicio eliminado", Toast.LENGTH_SHORT).show();
                                loadServices();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al borrar", Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
