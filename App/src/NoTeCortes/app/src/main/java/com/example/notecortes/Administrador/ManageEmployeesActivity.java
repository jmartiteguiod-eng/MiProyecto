package com.example.notecortes.Administrador;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.notecortes.R;
import com.google.firebase.auth.FirebaseAuth; // Necesario para la validación de identidad
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
//La clase ManageEmployeesActivity constituye el núcleo de administración de accesos, roles y control de
// identidades (IAM) de la aplicación. Arquitectónicamente, se destaca por implementar patrones de
// control defensivo en dos capas: previene la pérdida de gobernanza del sistema impidiendo la autodegradación
// de credenciales del operador activo y restringe cualquier mutación sobre cuentas raíz mediante un filtrado
// semántico estricto en el campo email. La consistencia visual se mantiene acoplada al backend mediante un
// addSnapshotListener, asegurando auditorías reactivas y en tiempo real del estado operacional de las
// cuentas del negocio
public class ManageEmployeesActivity extends AppCompatActivity {

    private ListView listView;
    private EditText inputBusqueda;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth; // Para identificar al admin actual
    private ArrayList<String> listaNombres = new ArrayList<>();
    private ArrayList<DocumentSnapshot> listaDocs = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_employees);

        listView = findViewById(R.id.listEmployees);
        inputBusqueda = findViewById(R.id.inputSearch);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // Inicialización de Auth

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaNombres);
        listView.setAdapter(adapter);

        cargarUsuarios("");

        inputBusqueda.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cargarUsuarios(s.toString().toLowerCase());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((p, v, pos, id) -> mostrarOpciones(pos));
    }

    private void cargarUsuarios(String filtroEmail) {
        Query query = db.collection("users").orderBy("email");

        query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("FIRESTORE", "Error al leer: " + error.getMessage());
                return;
            }

            listaNombres.clear();
            listaDocs.clear();

            if (value != null) {
                for (DocumentSnapshot d : value) {
                    String email = d.getString("email");
                    if (email != null && email.toLowerCase().contains(filtroEmail)) {
                        listaDocs.add(d);

                        boolean activo = d.getBoolean("activo") != null ? d.getBoolean("activo") : false;
                        String rol = d.getString("rol") != null ? d.getString("rol") : "cliente";
                        String nombre = d.getString("name") != null ? d.getString("name") : "Sin nombre";
                        boolean isAdmin = d.getBoolean("isAdmin") != null && d.getBoolean("isAdmin");

                        String etiquetaAdmin = isAdmin ? " [ADMIN]" : "";
                        String icono = activo ? "✔️" : "❌";

                        listaNombres.add(icono + etiquetaAdmin + " [" + rol.toUpperCase() + "] " + nombre + "\n" + email);
                    }
                }
            }
            adapter.notifyDataSetChanged();
        });
    }
/**
    private void mostrarOpciones(int pos) {
        if (pos >= listaDocs.size()) return;

        DocumentSnapshot doc = listaDocs.get(pos);
        String idDoc = doc.getId();
        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // VALIDACIÓN DE SEGURIDAD: Evitar que el administrador se edite a sí mismo
        if (idDoc.equals(currentUid)) {
            Toast.makeText(this, "No puedes modificar tu propio perfil desde aquí", Toast.LENGTH_SHORT).show();
            return;
        }

        String rolActual = doc.getString("rol") != null ? doc.getString("rol") : "cliente";
        boolean estaActivo = doc.getBoolean("activo") != null ? doc.getBoolean("activo") : false;
        String nombre = doc.getString("name") != null ? doc.getString("name") : "Usuario";

        String opcionRol = "empleado".equals(rolActual) ? "Convertir en CLIENTE" : "Convertir en EMPLEADO";
        String opcionActivo = estaActivo ? "Dar de BAJA (Bloquear)" : "Dar de ALTA (Permitir)";

        String[] opciones = {opcionRol, opcionActivo, "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Gestionar: " + nombre);
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                String nuevoRol = "empleado".equals(rolActual) ? "cliente" : "empleado";
                boolean nuevoAdminStatus = "empleado".equals(nuevoRol);

                Map<String, Object> actualizaciones = new HashMap<>();
                actualizaciones.put("rol", nuevoRol);
                actualizaciones.put("isAdmin", nuevoAdminStatus);

                actualizarCamposMultiples(idDoc, actualizaciones);

            } else if (which == 1) {
                actualizarUsuarioSencillo(idDoc, "activo", !estaActivo);
            }
        });
        builder.show();
    }
*/
//incluida opcion convertir en administrador
private void mostrarOpciones(int pos) {
    if (pos >= listaDocs.size()) return;

    DocumentSnapshot doc = listaDocs.get(pos);
    String idDoc = doc.getId();
    String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

    // Obtenemos el email para la validación de cuenta maestra
    String emailUsuario = doc.getString("email") != null ? doc.getString("email") : "";

    // 1. SEGURIDAD: No permitir que el admin se modifique a sí mismo
    if (idDoc.equals(currentUid)) {
        com.google.android.material.snackbar.Snackbar snack = com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                "No puedes modificar tu propio perfil",
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        );
        snack.setBackgroundTint(android.graphics.Color.parseColor("#D32F2F")); // Rojo corporativo
        snack.setTextColor(android.graphics.Color.WHITE);
        snack.show();
        return;
    }

    // 2. PROTECCIÓN POR ESTRUCTURA: No dejar modificar cuentas maestras (admin@...)
    if (emailUsuario.toLowerCase().contains("admin@")) {
        com.google.android.material.snackbar.Snackbar snack = com.google.android.material.snackbar.Snackbar.make(
                findViewById(android.R.id.content),
                "Acceso Denegado: Cuenta raíz protegida",
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        );
        snack.setBackgroundTint(android.graphics.Color.parseColor("#D32F2F")); // Rojo corporativo
        snack.setTextColor(android.graphics.Color.WHITE);
        snack.setAction("OK", v -> snack.dismiss());
        snack.setActionTextColor(android.graphics.Color.YELLOW);
        snack.show();
        return;
    }

    String rolActual = doc.getString("rol") != null ? doc.getString("rol") : "cliente";
    boolean estaActivo = doc.getBoolean("activo") != null ? doc.getBoolean("activo") : false;
    String nombre = doc.getString("name") != null ? doc.getString("name") : "Usuario";

    // Definimos las opciones del menú
    String[] opciones = {
            "Asignar rol: CLIENTE",
            "Asignar rol: EMPLEADO",
            "Asignar rol: ADMINISTRADOR",
            estaActivo ? "Dar de BAJA (Bloquear)" : "Dar de ALTA (Permitir)",
            "Cancelar"
    };

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Gestionar: " + nombre);
    builder.setItems(opciones, (dialog, which) -> {
        Map<String, Object> actualizaciones = new HashMap<>();

        switch (which) {
            case 0: // CLIENTE
                actualizaciones.put("rol", "cliente");
                actualizaciones.put("isAdmin", false);
                actualizarCamposMultiples(idDoc, actualizaciones);
                break;
            case 1: // EMPLEADO
                actualizaciones.put("rol", "empleado");
                //actualizaciones.put("isAdmin", false);
                actualizaciones.put("isAdmin", true);
                actualizarCamposMultiples(idDoc, actualizaciones);
                break;
            case 2: // ADMINISTRADOR
                actualizaciones.put("rol", "administrador");
                actualizaciones.put("isAdmin", true);
                actualizarCamposMultiples(idDoc, actualizaciones);
                break;
            case 3: // ALTA/BAJA
                actualizarUsuarioSencillo(idDoc, "activo", !estaActivo);
                break;
        }
    });
    builder.show();
}
    private void actualizarCamposMultiples(String idDoc, Map<String, Object> campos) {
        db.collection("users").document(idDoc)
                .update(campos)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Rol y permisos actualizados", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void actualizarUsuarioSencillo(String idDoc, String campo, Object nuevoValor) {
        db.collection("users").document(idDoc)
                .update(campo, nuevoValor)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}