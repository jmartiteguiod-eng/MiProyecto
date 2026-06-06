package com.example.notecortes.Adaptador;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notecortes.Clases.TimeSlot;
import com.example.notecortes.R;

import java.util.List;
/**
 * Adaptador personalizado para el componente {@link RecyclerView} encargado de renderizar
 * la matriz bidimensional de franjas horarias (slots de tiempo) en la agenda.
 * <p>
 * Implementa el patrón estructural ViewHolder para optimizar el reciclaje de memoria en la interfaz.
 * Además, actúa como un controlador visual asumiendo una máquina de estados estricta para alterar el
 * color, la opacidad (alpha) y la interactividad táctil de cada celda según el contexto de su modelo.
 * </p>
 * * @author Desarrollo "No Te Cortes"
 * @version 1.0
 * @see RecyclerView.Adapter
 * @see TimeSlot
 */
public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.VH> {

    public interface Listener { void onSlotClicked(TimeSlot slot, int position); }

    private final List<TimeSlot> slots;
    private final Listener listener;
    /**
     * Constructor único para inicializar el adaptador con su respectivo origen de datos e interfaz de escucha.
     *
     * @param slots    Lista de franjas temporales que alimentarán el RecyclerView.
     * @param listener Implementación de la interfaz encargada de procesar las pulsaciones sobre los slots.
     */
    public TimeSlotAdapter(List<TimeSlot> slots, Listener listener) {
        this.slots = slots;
        this.listener = listener;
    }
    /**
     * Instancia y desinfla el archivo de diseño XML individual (layout) para construir la estructura de la celda.
     * <p>
     * Este método se ejecuta únicamente cuando el RecyclerView necesita inicializar un nuevo contenedor
     * de vistas (ViewHolder) debido a que no existen celdas disponibles para su reutilización en memoria cache.
     * </p>
     *
     * @param parent   El grupo de vistas principal (ViewGroup) al que se añadirá la nueva celda tras acoplarse.
     * @param viewType Entero identificador que define el tipo de vista estructural de la celda (no utilizado en este caso uniforme).
     * @return Una nueva instancia estructurada del contenedor {@link VH}.
     */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
        return new VH(v);
    }
    /**
     * Enlaza y mapea de forma controlada el estado lógico del modelo {@link TimeSlot} sobre los elementos gráficos
     * de la interfaz de usuario en la posición dada.
     * <p>
     * Implementa un sistema jerárquico de filtros y estilos basado en los atributos booleanos del objeto:
     * </p>
     * <ul>
     * <li><b>Vencida (isPast):</b> Reduce la opacidad al 40%, tiñe el texto en gris oscuro e inactiva los eventos táctiles.</li>
     * <li><b>Ocupada (!available):</b> Reduce la opacidad al 35%, tiñe en gris neutro e inhabilita su foco.</li>
     * <li><b>Seleccionada (isSelected):</b> Fuerza opacidad máxima, colorea el texto en blanco y establece un fondo sólido azul.</li>
     * <li><b>Disponible (Defecto):</b> Fuerza opacidad máxima, activa eventos táctiles, texto negro y fondo completamente transparente.</li>
     * </ul>
     *
     * @param holder   Instancia del contenedor ViewHolder que aloja las referencias a los widgets de la celda.
     * @param position Índice relativo de la estructura de datos que se procesará en el ciclo actual de refresco de pantalla.
     */
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TimeSlot s = slots.get(position);
        holder.tvTime.setText(s.time);

        // Colores:
        // - pasada: gris claro
        // - hoy disponible: verde claro tint
        // - ocupada: alpha baja (deshabilitada)
        // - seleccionada: azul
        if (s.isPast) {
            holder.itemView.setAlpha(0.4f);
            holder.tvTime.setTextColor(Color.DKGRAY);
            holder.itemView.setEnabled(false);
        } else if (!s.available) {
            holder.itemView.setAlpha(0.35f);
            holder.tvTime.setEnabled(false);
            holder.tvTime.setTextColor(Color.GRAY);
        } else if (s.isSelected) {
            holder.itemView.setAlpha(1f);
            holder.tvTime.setTextColor(Color.WHITE);
            holder.itemView.setBackgroundColor(0xff3b82f6); // azul
        } else {
            holder.itemView.setAlpha(1f);
            holder.itemView.setEnabled(true);
            // today color or default
            holder.tvTime.setTextColor(Color.BLACK);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> {
            if (s.available && !s.isPast && listener != null) listener.onSlotClicked(s, position);
        });
    }
    /**
     * Informa al motor de renderizado del RecyclerView la dimensión numérica de la colección actual de datos.
     *
     * @return El número total de franjas de tiempo almacenadas en la lista del adaptador.
     */
    @Override
    public int getItemCount() { return slots.size(); }
    /**
     * Clase estática contenedora de vistas (Pattern ViewHolder) que encapsula y cachea de forma directa
     * las referencias a los controles de la interfaz gráfica de cada celda individual.
     * <p>
     * Mitiga la necesidad de realizar llamadas repetitivas e ineficientes al método nativo {@code findViewById}
     * durante los procesos continuos de scroll e interpolación de celdas.
     * </p>
     */
    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime;
        VH(View v) {
            super(v);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}
