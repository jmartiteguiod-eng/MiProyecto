package com.example.notecortes.Clases;
/**
 * Clase de modelo (POJO) que representa una franja horaria o bloque de tiempo
 * individual en la agenda de la aplicación.
 * <p>
 * Se utiliza como la entidad de datos unitaria (bloques rígidos de 30 minutos)
 * que alimenta tanto a la matriz visual del RecyclerView en la interfaz de usuario,
 * como a las operaciones de validación lógica de disponibilidad en los flujos de reserva.
 * </p>
 * * @author Desarrollo "No Te Cortes"
 * @version 1.0
 * @see com.example.notecortes.Adaptador.TimeSlotAdapter
 * @see com.example.notecortes.Citas.ReservarCitaActivity
 */
public class TimeSlot {
    /** * Representación textual de la hora de inicio de la franja temporal.
     * Almacenada bajo el formato de reloj de 24 horas (ej. "09:00", "15:30").
     */
    public String time;     // "09:00"
    /** * Flag que determina la disponibilidad comercial de la franja.
     * Evalúa si el slot está libre en la base de datos (true) u ocupado por otra cita (false).
     */
    public boolean available; // libre o no
    public boolean isPast;    // es hora pasada respecto a hoy
    public boolean isSelected; // seleccionado por el usuario

    public TimeSlot() { }

    public TimeSlot(String time, boolean available, boolean isPast) {
        this.time = time;
        this.available = available;
        this.isPast = isPast;
        this.isSelected = false;
    }
}

