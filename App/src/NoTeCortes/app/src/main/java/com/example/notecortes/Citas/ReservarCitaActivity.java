package com.example.notecortes.Citas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notecortes.R;
import com.example.notecortes.Clases.TimeSlot;
import com.example.notecortes.Adaptador.TimeSlotAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
/**
 * Actividad núcleo encargada de procesar el flujo de reserva de citas en la aplicación.
 * <p>
 * Controla un sistema dinámico de gestión por franjas de tiempo (slots de 30 minutos).
 * Evalúa en tiempo real las restricciones de red, el bloqueo perimetral de fechas,
 * la concurrencia horaria frente a situaciones de overbooking y la desnormalización
 * transaccional orientada a rendimiento en Cloud Firestore.
 * </p>
 * * @author Desarrollo "No Te Cortes"
 * @version 1.0
 * @see CitasActivity
 * @see TimeSlot
 */
public class ReservarCitaActivity extends AppCompatActivity {

    private TextView tvFechaSeleccionada, tvPeriodo;
    private Button btnPickDate, btnConfirm, btnPrev, btnNext, btnWeek, btnMonth;
    private RecyclerView rvTimeSlots;
    private Spinner spinnerService, spinnerDuration;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String selectedDate;
    private List<TimeSlot> slots;
    private TimeSlotAdapter adapter;

    private boolean viewByWeek = true;
    private Calendar currentPageStart;
    private final Map<String, Integer> services = new HashMap<>();
    /**
     * Inicializa el ciclo de vida de la actividad, vincula el mapa de vistas,
     * inicializa los adaptadores gráficos y fuerza la zona horaria a "Europe/Madrid"
     * para mitigar desfases con el servidor cloud de Firebase.
     *
     * @param savedInstanceState Contenedor de datos que preserva el estado técnico de la UI.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservar_cita_prof);

        // Vinculación de vistas (Tu código original)
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);
        btnPickDate = findViewById(R.id.btnPickDate);
        rvTimeSlots = findViewById(R.id.rvTimeSlots);
        btnConfirm = findViewById(R.id.btnConfirm);
        spinnerService = findViewById(R.id.spinnerService);
        spinnerDuration = findViewById(R.id.spinnerDuration);
        tvPeriodo = findViewById(R.id.tvPeriodo);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnWeek = findViewById(R.id.btnWeek);
        btnMonth = findViewById(R.id.btnMonth);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadServicesFromFirestore();

        // Spinner Duración (MI código original)
        ArrayAdapter<String> durAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>() {{
            add("30"); add("45"); add("60");
        }});
        durAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDuration.setAdapter(durAdapter);

        // Recycler Setup
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 3));
        slots = generateDefaultTimeSlots();
        adapter = new TimeSlotAdapter(slots, (slot, pos) -> onSlotClicked(slot, pos));
        rvTimeSlots.setAdapter(adapter);

        // Forzamos Madrid para evitar el desfase de 1:30h
        currentPageStart = Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid"));
        startOfWeek(currentPageStart);
        updatePeriodoLabel();

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnConfirm.setOnClickListener(v -> confirmReservation(v));

        // Navegación (Respetando tu lógica de actualización)
        btnWeek.setOnClickListener(v -> { viewByWeek = true; startOfWeek(currentPageStart); updatePeriodoLabel(); resetSelection(); refreshSlotsVisual(selectedDate); });
        btnMonth.setOnClickListener(v -> { viewByWeek = false; startOfMonth(currentPageStart); updatePeriodoLabel(); resetSelection(); refreshSlotsVisual(selectedDate); });
        btnPrev.setOnClickListener(v -> {
            if (viewByWeek) currentPageStart.add(Calendar.WEEK_OF_YEAR, -1);
            else currentPageStart.add(Calendar.MONTH, -1);
            updatePeriodoLabel();
            refreshSlotsVisual(selectedDate);
        });
        btnNext.setOnClickListener(v -> {
            if (viewByWeek) currentPageStart.add(Calendar.WEEK_OF_YEAR, 1);
            else currentPageStart.add(Calendar.MONTH, 1);
            updatePeriodoLabel();
            refreshSlotsVisual(selectedDate);
        });

        // Cargar hoy por defecto para que no salga vacío
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentPageStart.getTime());
        checkDateBlockedAndLoad(selectedDate);
    }
    /**
     * Intercepta la pulsación sobre una celda del horario y calcula cuántos bloques continuos
     * de 30 minutos se necesitan en cascada según la duración específica del servicio.
     * <p>
     * Valida si el rango resultante excede la hora de cierre o si colisiona con slots inactivos,
     * ocupados o pasados (control de tiempo de cortesía de 15 minutos).
     * </p>
     *
     * @param slot     Instancia del bloque horario sobre el que se hace foco táctil.
     * @param position Índice lineal de la celda pulsada dentro de la lista global del adaptador.
     */
    private void onSlotClicked(TimeSlot slot, int position) {
        if (spinnerService.getSelectedItem() == null) return;

        // 1. Obtener la duración real del mapa de servicios
        String servicioName = spinnerService.getSelectedItem().toString();
        Integer durationObj = services.get(servicioName);
        int duration = (durationObj != null) ? durationObj : 30;
        int slotsNeeded = (int) Math.ceil(duration / 30.0);

        // 2. Limpiar selección visual previa
        for (TimeSlot s : slots) s.isSelected = false;

        // 3. Validar disponibilidad de bloque completo (CASCADA)
        boolean canSelect = true;
        if (position + slotsNeeded > slots.size()) {
            canSelect = false; // Se sale del horario de cierre
        } else {
            for (int i = 0; i < slotsNeeded; i++) {
                TimeSlot s = slots.get(position + i);
                if (!s.available || s.isPast) {
                    canSelect = false;
                    break;
                }
            }
        }

        // 4. Seleccionar visualmente todos los slots necesarios si hay hueco
        if (canSelect) {
            for (int i = 0; i < slotsNeeded; i++) {
                slots.get(position + i).isSelected = true;
            }
        } else {
            //Snackbar.make(rvTimeSlots, "No hay hueco suficiente para este servicio", Snackbar.LENGTH_SHORT).show();
            // 1. Creamos el Snackbar (sin el .show() al final)
            Snackbar snackbar = Snackbar.make(rvTimeSlots, "No hay hueco suficiente para este servicio", Snackbar.LENGTH_SHORT);
            // 2. Le aplicamos tu fondo rojo y tus letras amarillas
            snackbar.setBackgroundTint(androidx.core.content.ContextCompat.getColor(this, R.color.rojo_alerta));
            snackbar.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.amarillo_alerta));

// 3. Lo mostramos en pantalla
            snackbar.show();

        }
        adapter.notifyDataSetChanged();
    }
    /**
     * Evalúa si una fecha objetivo se encuentra registrada en la colección estructural
     * de jornadas deshabilitadas o festivas (nodo {@code blocked_dates}).
     * <p>
     * Realiza un control asíncrono previo del hardware de red. Si la fecha está bloqueada,
     * limpia el adaptador e inhabilita las celdas; en caso contrario, inicializa el catálogo
     * estándar y dispara la consulta de reservas concurrentes.
     * </p>
     *
     * @param fecha Cadena de texto que representa el día a evaluar (yyyy-MM-dd).
     */
    private void checkDateBlockedAndLoad(String fecha) {
        // 🔍 CONTROL DE RED - FONDO ROJO, LETRAS AMARILLAS
        if (!isNetworkAvailable()) {
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "No hay conexión. No se pueden cargar los horarios.", Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(Color.parseColor("#D32F2F")); // Rojo Alerta
            snackbar.setTextColor(Color.YELLOW); // Letras amarillas
            snackbar.show();
            return;
        }

        db.collection("blocked_dates").document(fecha).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        selectedDate = null;
                        tvFechaSeleccionada.setText("DÍA BLOQUEADO");
                        slots.clear(); // Vaciar para que no pinchen en nada
                        adapter.notifyDataSetChanged();
                    } else {
                        selectedDate = fecha;
                        tvFechaSeleccionada.setText("Fecha: " + selectedDate);
                        slots.clear();
                        slots.addAll(generateDefaultTimeSlots());
                        refreshSlotsVisual(selectedDate);
                        fetchOccupiedSlots(selectedDate);
                    }
                });
    }
    /**
     * Consume de forma asíncrona los identificadores de documentos almacenados en la subcolección
     * {@code citas/{fecha}/horas/}, mutando el flag de disponibilidad del modelo a falso.
     *
     * @param fecha Cadena representativa de la jornada de consulta.
     */
    private void fetchOccupiedSlots(String fecha) {
        db.collection("citas").document(fecha).collection("horas").get()
                .addOnSuccessListener(qs -> {
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String horaDoc = doc.getId().replace("_", ":");
                        for (TimeSlot s : slots) {
                            if (s.time.equals(horaDoc)) {
                                s.available = false;
                                break;
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
    /**
     * Evalúa de forma estricta si una hora dada corresponde al pasado cronológico,
     * asumiendo un margen transaccionalizado de 15 minutos de cortesía comercial.
     *
     * @param fecha     Fecha actual evaluada.
     * @param timeHHmm Cadena horaria con formato numérico de 24 horas (HH:mm).
     * @return true si la franja ya ha vencido en tiempo real en la localización peninsular; false en caso contrario.
     */
    private boolean isSlotPastForDate(String fecha, String timeHHmm) {
        try {
            TimeZone tz = TimeZone.getTimeZone("Europe/Madrid");
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            fmt.setTimeZone(tz);

            Calendar ahora = Calendar.getInstance(tz);
            Calendar sel = Calendar.getInstance(tz);
            sel.setTime(fmt.parse(fecha));

            // Comparación de días
            if (sel.get(Calendar.YEAR) < ahora.get(Calendar.YEAR)) return true;
            if (sel.get(Calendar.YEAR) > ahora.get(Calendar.YEAR)) return false;
            if (sel.get(Calendar.DAY_OF_YEAR) < ahora.get(Calendar.DAY_OF_YEAR)) return true;
            if (sel.get(Calendar.DAY_OF_YEAR) > ahora.get(Calendar.DAY_OF_YEAR)) return false;

            // Si es hoy, comparamos minutos totales + 15 de cortesía
            int h = Integer.parseInt(timeHHmm.split(":")[0]);
            int m = Integer.parseInt(timeHHmm.split(":")[1]);
            int totalSlot = (h * 60) + m;
            int totalAhora = (ahora.get(Calendar.HOUR_OF_DAY) * 60) + ahora.get(Calendar.MINUTE) + 15;
            //int totalAhora = (15 * 60); // Simulamos que son horas
            return totalSlot <= totalAhora;
        } catch (Exception e) { return false; }
    }
    /**
     * Orquesta el entorno transaccional atómico distribuído para confirmar la reserva.
     * <p>
     * Se abre un bloque {@code db.runTransaction()} aislado en el servidor cloud que:
     * 1) Evalúa de forma secuencial la existencia previa de cada id_slot seleccionado (control doble de overbooking).
     * 2) Escribe de manera atómica N instancias en la colección global de control de agenda.
     * 3) Desnormaliza los datos persistiendo el objeto Cita consolidado dentro de {@code user_citas}
     * con una latencia O(1) de cara al consumo del perfil móvil.
     * </p>
     *
     * @param vistaBoton Referencia del elemento visual que dispara la acción para el anclaje de notificaciones.
     */
    private void confirmReservation(View vistaBoton) {
        // 🔍 CONTROL DE RED AL CONFIRMAR - FONDO ROJO, LETRAS AMARILLAS
        if (!isNetworkAvailable()) {
            Snackbar snackbar = Snackbar.make(vistaBoton, "Sin conexión a internet. No se puede realizar la reserva.", Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(Color.parseColor("#D32F2F")); // Rojo Alerta
            snackbar.setTextColor(Color.YELLOW); // Letras amarillas
            snackbar.show();
            return;
        }

        if (selectedDate == null) return;

        int startIndex = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isSelected) {
                startIndex = i;
                break;
            }
        }
 /**
//evitar el crash
        if (startIndex == -1) {
            Snackbar.make(vistaBoton, "Selecciona una hora", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String servicioName = spinnerService.getSelectedItem().toString();
        int duration = services.get(servicioName);
        int slotsNeeded = (int) Math.ceil(duration / 30.0);

        List<String> slotIds = new ArrayList<>();
        for (int i = 0; i < slotsNeeded; i++) {
            slotIds.add(slots.get(startIndex + i).time.replace(":", "_"));
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        //final crash
        */
        //Nuevo
        if (startIndex == -1) {
            Snackbar.make(vistaBoton, "Selecciona una hora", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String servicioName = spinnerService.getSelectedItem().toString();
        int duration = services.get(servicioName);
        int slotsNeeded = (int) Math.ceil(duration / 30.0);

        // =========================================================================
        // Evita que el bucle intente leer posiciones que no existen en la lista
        // =========================================================================
        if (startIndex + slotsNeeded > slots.size()) {
            Snackbar snackbar = Snackbar.make(vistaBoton, "El servicio excede el horario de cierre de la barbería", Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(androidx.core.content.ContextCompat.getColor(this, R.color.rojo_alerta));
            snackbar.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.amarillo_alerta));
            snackbar.show();
            return; // Frenamos la ejecución para que no tire el bucle 'for' de abajo
        }
        // =========================================================================

        List<String> slotIds = new ArrayList<>();
        for (int i = 0; i < slotsNeeded; i++) {
            slotIds.add(slots.get(startIndex + i).time.replace(":", "_"));
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;


        db.runTransaction(transaction -> {
            for (String sid : slotIds) {
                DocumentReference dr = db.collection("citas").document(selectedDate).collection("horas").document(sid);
                if (transaction.get(dr).exists()) throw new RuntimeException("Hora ocupada");
            }

            for (String sid : slotIds) {
                DocumentReference dr = db.collection("citas").document(selectedDate).collection("horas").document(sid);
                Map<String, Object> d = new HashMap<>();
                d.put("userId", user.getUid());
                d.put("servicio", servicioName);
                d.put("fecha", selectedDate);
                d.put("hora", sid.replace("_", ":"));
                transaction.set(dr, d);
            }

            // Guardar en user_citas usando el ID de la primera hora para evitar duplicados
            DocumentReference userRef = db.collection("user_citas").document(user.getUid())
                    .collection("citas").document(selectedDate + "_" + slotIds.get(0));

            Map<String, Object> uData = new HashMap<>();
            uData.put("fecha", selectedDate);
            uData.put("servicio", servicioName);
            uData.put("slots", slotIds);
            uData.put("hora", slotIds.get(0).replace("_", ":"));
            transaction.set(userRef, uData);

            return null;
        }).addOnSuccessListener(v -> {
            //Snackbar.make(vistaBoton, "¡Reserva exitosa!", Snackbar.LENGTH_SHORT).show();
            //fetchOccupiedSlots(selectedDate);
            Snackbar snackbar = Snackbar.make(vistaBoton, "¡Reserva exitosa!", Snackbar.LENGTH_SHORT);
            snackbar.setBackgroundTint(getColor(R.color.verde_exito));
            snackbar.setTextColor(getColor(R.color.amarillo_alerta));
            snackbar.show();
            fetchOccupiedSlots(selectedDate);

        }).addOnFailureListener(e -> Snackbar.make(vistaBoton, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
    }
    /**
     * Genera la colección algorítmica de franjas por defecto que modelan la jornada laboral de la estética.
     * <p>
     * Comprende un horario continuado de 10 horas operativas estructurado desde las 08:00 hasta las 18:30
     * en intervalos rígidos de 30 minutos.
     * </p>
     *
     * @return Una lista indexada de objetos del tipo {@link TimeSlot}.
     */
    private List<TimeSlot> generateDefaultTimeSlots() {
        List<TimeSlot> list = new ArrayList<>();
        for (int h = 8; h <= 18; h++) {
            list.add(new TimeSlot(String.format(Locale.getDefault(), "%02d:00", h), true, false));
            list.add(new TimeSlot(String.format(Locale.getDefault(), "%02d:30", h), true, false));
        }
        return list;
    }
    /**
     * Actualiza analíticamente el flag cronológico {@code isPast} de los bloques locales
     * basándose en la fecha seleccionada del control.
     *
     * @param date Fecha contra la que se computará el vencimiento actual del reloj.
     */
    private void refreshSlotsVisual(String date) {
        if (slots == null) return;
        for (TimeSlot s : slots) {
            s.isPast = (date != null) ? isSlotPastForDate(date, s.time) : false;
        }
        adapter.notifyDataSetChanged();
    }

    private void resetSelection() {
        selectedDate = null;
        tvFechaSeleccionada.setText("Fecha: —");
    }
    /**
     * Instancia y proyecta el cuadro de diálogo modal nativo Picker de fechas del framework de Android,
     * sincronizado de manera forzada bajo el huso huso horario de Madrid.
     */
    private void showDatePicker() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid"));
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, day);
            checkDateBlockedAndLoad(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(sel.getTime()));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
    /**
     * Configura el Calendar de navegación forzando su índice al lunes de la semana actual.
     * @param cal Instancia temporal de destino.
     */
    private void startOfWeek(Calendar cal) { cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); }
    private void startOfMonth(Calendar cal) { cal.set(Calendar.DAY_OF_MONTH, 1); }
    /**
     * Modifica dinámicamente la vista del texto de control del periodo temporal activo del visor de reservas.
     */
    private void updatePeriodoLabel() {
        SimpleDateFormat fmt = new SimpleDateFormat(viewByWeek ? "yyyy-MM-dd ' (semana)'" : "yyyy-MM", Locale.getDefault());
        tvPeriodo.setText(fmt.format(currentPageStart.getTime()));
    }
    /**
     * Consume asíncronamente el catálogo maestro desnormalizado desde la colección raíz {@code services}.
     * <p>
     * Recupera las propiedades relativas al nombre e indexa su duración en minutos en el mapa en memoria,
     * alimentando de manera dinámica el adaptador del Spinner visual.
     * </p>
     */
    private void loadServicesFromFirestore() {
        //  CONTROL DE RED AL INICIAR - FONDO ROJO, LETRAS AMARILLAS
        if (!isNetworkAvailable()) {
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Error de red: No se pudo cargar el catálogo de servicios.", Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(Color.parseColor("#D32F2F")); // Rojo Alerta
            snackbar.setTextColor(Color.YELLOW); // Letras amarillas
            snackbar.show();
            return;
        }

        db.collection("services").get().addOnSuccessListener(qs -> {
            List<String> names = new ArrayList<>();
            for (DocumentSnapshot doc : qs.getDocuments()) {
                String n = doc.getString("nombre");
                Long d = doc.getLong("duracion");
                if (n != null) {
                    services.put(n, d != null ? d.intValue() : 30);
                    names.add(n);
                }
            }
            ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            spinnerService.setAdapter(a);
        });
    }
    /**
     * Realiza una auditoría a nivel de hardware físico del chipset de comunicaciones del smartphone.
     * <p>
     * Interroga al {@link ConnectivityManager} activo evaluando la existencia de canales de transporte
     * de datos válidos ya sea por redes celulares de datos móviles (3G/4G/5G) o redes locales inalámbricas Wi-Fi.
     * </p>
     *
     * @return true si el canal de comunicaciones está operativo y disponible; false si el dispositivo carece de red.
     */
    // MÉTODO AUXILIAR DE RED (Hardware Check)
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            }
        }
        return false;
    }
}