package com.example.notecortes.Citas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.notecortes.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/**
 * Actividad encargada de gestionar la interfaz de usuario para la visualización,
 * control técnico e historial de citas activas de un cliente.
 * <p>
 * Implementa el flujo de cancelación mediante operaciones transaccionales
 * asíncronas en Cloud Firestore, asegurando la consistencia física entre las
 * subcolecciones desnormalizadas del usuario y el estado global de la agenda.
 * </p>
 * * @author Desarrollo "No Te Cortes"
 * @version 1.0
 * @see ReservarCitaActivity
 */

public class CitasActivity extends AppCompatActivity {

    private ListView listViewCitas;
    private Button btnCancelar;
    private ArrayAdapter<String> adapter;

    // Lista de Strings para mostrar en el ListView
    private ArrayList<String> listaCitasDisplay;

    // Mapeo para guardar el ID de la cita (fecha_hora_inicio) real de Firestore
    /** * Diccionario de acoplamiento que mapea cada texto visible del ListView (Key) con el ID real
     * del documento en Cloud Firestore (Value). Evita la dependencia de índices volátiles en la UI.
     */
    private HashMap<String, String> citasIdMap;
    private String selectedCitaId = null; // ID de la cita seleccionada para cancelar
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "CitasActivity";
    /**
     * Inicializa el ciclo de vida de la actividad, vincula los elementos del layout,
     * inicializa los contenedores de datos y configura los escuchas de selección de la lista.
     *
     * @param savedInstanceState Contenedor de datos que preserva el estado previo de la interfaz si la actividad se recrea.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citas);

        listViewCitas = findViewById(R.id.listViewCitas);
        btnCancelar = findViewById(R.id.btnCancelarCita);

        listaCitasDisplay = new ArrayList<>();
        citasIdMap = new HashMap<>();

        // En tu CitasActivity.java
        adapter = new ArrayAdapter<String>(this, R.layout.item_cita2, android.R.id.text1, listaCitasDisplay);

        listViewCitas.setAdapter(adapter);
        listViewCitas.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        cargarCitasUsuario();

        // Manejar la selección de la cita
        listViewCitas.setOnItemClickListener((parent, view, position, id) -> {
            // El elemento seleccionado en el ListView
            String selectedItemText = listaCitasDisplay.get(position);
            // El ID real de la cita que se usará para la cancelación
            selectedCitaId = citasIdMap.get(selectedItemText);

            if (selectedCitaId != null) {
                btnCancelar.setEnabled(true);
            }
        });

        btnCancelar.setOnClickListener(v -> {
            if (selectedCitaId == null) {
                Snackbar snackbar = Snackbar.make(listViewCitas, "Selecciona una cita para cancelar.", Snackbar.LENGTH_SHORT);
                snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.rojo_alerta));
                snackbar.setTextColor(ContextCompat.getColor(this, R.color.amarillo_alerta));
                snackbar.show();
                return;
            }

            new AlertDialog.Builder(CitasActivity.this)
                    .setTitle("Confirmar cancelación")
                    .setMessage("¿Estás seguro de que quieres cancelar la cita seleccionada?")
                    .setPositiveButton("Sí, cancelar", (dialog, which) -> cancelarCita(selectedCitaId))
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        btnCancelar.setEnabled(false); // Deshabilitar inicialmente
    }

    // =========================================================================
    // 1. CARGAR CITAS
    // =========================================================================
    /**
     * Realiza una consulta asíncrona a Cloud Firestore para recuperar el listado de citas
     * vinculadas al identificador del usuario (UID) autenticado.
     * <p>
     * Consume los datos desde la subcolección {@code user_citas/{uid}/citas}. Procesa dinámicamente
     * el volumen de bloques temporales reservados (slots de 30 minutos) para proyectar la duración estimada,
     * recupera el estado transaccional y formatea las cadenas legibles en el control de la interfaz.
     * </p>
     */
    private void cargarCitasUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Snackbar snackbar = Snackbar.make(listViewCitas, "Usuario no autenticado", Snackbar.LENGTH_SHORT);
            snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.rojo_alerta));
            snackbar.setTextColor(ContextCompat.getColor(this, R.color.amarillo_alerta));
            snackbar.show();
            finish();
            return;
        }

        String uid = user.getUid();

        // Consulta a la colección específica del usuario
        db.collection("user_citas").document(uid).collection("citas")
                .orderBy("fecha")
                .get()
                .addOnSuccessListener((QuerySnapshot qs) -> {

                    listaCitasDisplay.clear();
                    citasIdMap.clear();
                    selectedCitaId = null;
                    btnCancelar.setEnabled(false);
                    listViewCitas.clearChoices();

                    if (qs.isEmpty()) {
                        listaCitasDisplay.add("No tienes ninguna cita registrada.");
                    } else {
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            // El ID suele ser la hora (ej: "16_00") o fecha_hora (ej: "2026-01-18_16_00")
                            String id = doc.getId();

                            String servicio = safeGetString(doc, "servicio", "—");
                            String fecha = safeGetString(doc, "fecha", "—");
                            String estado = safeGetString(doc, "estado", "—");

                            // 1. Intentamos obtener el campo "hora" que vimos en tu Firebase
                            String horaInicio = safeGetString(doc, "hora", "—");

                            // 2. Si el campo "hora" no existe (devuelve —), la extraemos del ID del documento
                            if (horaInicio.equals("—")) {
                                if (id.contains("_")) {
                                    String[] partes = id.split("_");
                                    // Si el ID es solo la hora (ej: "16_00")
                                    if (partes.length == 2 && id.length() <= 5) {
                                        horaInicio = partes[0] + ":" + partes[1];
                                    }
                                    // Si el ID incluye la fecha (ej: "2026-01-18_16_00")
                                    else if (partes.length >= 3) {
                                        // Tomamos las últimas dos partes que corresponden a HH y mm
                                        horaInicio = partes[partes.length - 2] + ":" + partes[partes.length - 1];
                                    }
                                }
                            }

                            // Cálculo de duración NUEVO
                            List<String> totalSlots = (List<String>) doc.get("slots");
                            int numSlots = (totalSlots != null) ? totalSlots.size() : 1;
                            String duracionEstimada = (numSlots * 30) + "m";

                            String texto = String.format(
                                    "Servicio: %s\nFecha: %s | Hora: %s\nDuración: %s | Estado: %s",
                                    servicio, fecha, horaInicio, duracionEstimada, estado
                            );

                            listaCitasDisplay.add(texto);
                            citasIdMap.put(texto, id);
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    listaCitasDisplay.clear();
                    listaCitasDisplay.add("Error al cargar citas: " + e.getMessage());
                    adapter.notifyDataSetChanged();
                    Log.e(TAG, "Error cargando citas: ", e);

                    Snackbar snackbar = Snackbar.make(listViewCitas, "Error al cargar citas.", Snackbar.LENGTH_LONG);
                    snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.rojo_alerta));
                    snackbar.setTextColor(ContextCompat.getColor(this, R.color.amarillo_alerta));
                    snackbar.show();
                });
    }


    // =========================================================================
    // 2. CANCELAR CITA (Transacción)
    // =========================================================================
    /**
     * Procesa la baja y liberación de una cita del sistema por medio de una transacción atómica.
     * <p>
     * El método ejecuta una lectura inicial para obtener la fecha de reserva y el listado de identificadores
     * de bloques de tiempo asociados. Posteriormente, ejecuta un entorno transaccional aislado que garantiza:
     * </p>
     * <ul>
     * <li>El borrado físico secuencial de cada franja horaria ocupada en {@code citas/{fecha}/horas/{id_slot}}.</li>
     * <li>La eliminación íntegra del documento duplicado en el perfil privado del usuario.</li>
     * </ul>
     * Evita problemas de concurrencia o estados indeterminados en la agenda en tiempo real.
     *
     * @param citaId Identificador único del documento de la cita objetivo que se desea dar de baja.
     */
    private void cancelarCita(String citaId) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Snackbar snackbar = Snackbar.make(listViewCitas, "Usuario no autenticado", Snackbar.LENGTH_SHORT);
            snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.rojo_alerta));
            snackbar.setTextColor(ContextCompat.getColor(this, R.color.amarillo_alerta));
            snackbar.show();
            return;
        }

        String uid = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Referencia a la cita del usuario
        DocumentReference citaRef = db.collection("user_citas")
                .document(uid)
                .collection("citas")
                .document(citaId);

        citaRef.get().addOnSuccessListener(citaDoc -> {

            if (!citaDoc.exists()) {
                Snackbar snackbar = Snackbar.make(listViewCitas, "La cita no existe", Snackbar.LENGTH_SHORT);
                snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.rojo_alerta));
                snackbar.setTextColor(ContextCompat.getColor(this, R.color.amarillo_alerta));
                snackbar.show();
                return;
            }

            String fecha = citaDoc.getString("fecha");
            List<String> slots = (List<String>) citaDoc.get("slots"); // usar lista de slots reservados

            if (fecha == null || slots == null || slots.isEmpty()) {
                Snackbar snackbar = Snackbar.make(listViewCitas, "Datos insuficientes para la cancelación", Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.rojo_alerta));
                snackbar.setTextColor(ContextCompat.getColor(this, R.color.amarillo_alerta));
                snackbar.show();
                return;
            }

            db.runTransaction(transaction -> {

                //Liberar todos los slots de la cita
                for (String sid : slots) {
                    DocumentReference dr = db.collection("citas")
                            .document(fecha)
                            .collection("horas")
                            .document(sid);
                    transaction.delete(dr);
                }

                // Borrar la cita del usuario
                transaction.delete(citaRef);

                return null;

            }).addOnSuccessListener(v -> {
                Snackbar snackbar = Snackbar.make(listViewCitas, "Cita cancelada correctamente", Snackbar.LENGTH_SHORT);
                snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.verde_exito));
                snackbar.setTextColor(ContextCompat.getColor(this, R.color.amarillo_alerta));
                snackbar.show();
            }).addOnFailureListener(e -> {
                Snackbar snackbar = Snackbar.make(listViewCitas, "Error al cancelar: " + e.getMessage(), Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.rojo_alerta));
                snackbar.setTextColor(ContextCompat.getColor(this, R.color.amarillo_alerta));
                snackbar.show();
            });

        }).addOnFailureListener(e -> {
            Snackbar snackbar = Snackbar.make(listViewCitas, "Error al leer la cita: " + e.getMessage(), Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.rojo_alerta));
            snackbar.setTextColor(ContextCompat.getColor(this, R.color.amarillo_alerta));
            snackbar.show();
        });
    }

    // Helper seguro para obtener strings
    /**
     * Lee de forma controlada propiedades del tipo String dentro de un mapa de datos de Firestore.
     * Evita excepciones por desajustes de cast o referencias nulas si el servidor devuelve campos vacíos.
     *
     * @param doc      Instancia del documento recuperado que contiene los pares clave-valor.
     * @param key      Nombre de la propiedad o nodo de datos que se desea evaluar.
     * @param fallback Cadena de caracteres alternativa que se asignará si la extracción es inválida.
     * @return El valor String almacenado o, en su defecto, la cadena de contingencia (fallback).
     */

    private String safeGetString(@NonNull DocumentSnapshot doc, String key, String fallback) {
        Object o = doc.get(key);
        return (o instanceof String) ? (String) o : fallback;
    }
}