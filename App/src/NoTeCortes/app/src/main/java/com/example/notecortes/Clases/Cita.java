package com.example.notecortes.Clases;
/**
 * Clase de modelo (POJO) que representa conceptual y físicamente una Cita
 * dentro del ecosistema de la aplicación.
 * <p>
 * Actúa como la estructura de datos unificada para mapear los documentos de la
 * colección raíz desnormalizada {@code user_citas}. Diseñada bajo principios de
 * optimización NoSQL, separa los campos de persistencia en la nube de los atributos
 * locales volátiles empleados para la renderización dinámica en paneles de administración.
 * </p>
 * * @author Desarrollo "No Te Cortes"
 * @version 1.1
 * @see com.example.notecortes.Citas.CitasActivity
 * @see com.example.notecortes.Citas.ReservarCitaActivity
 */
public class Cita {

    private String fecha;
    private String hora;
    private String servicio;
    private String userId;
    /** * Registra el ciclo de vida o estado situacional de la cita en el sistema.
     * Valores esperados: {@code "realizada"}, {@code "no_realizada"} o {@code "pendiente"}/null.
     */
    private String estado; // Nuevo: "realizada", "no_realizada" o null/pendiente

    private transient String userName;
    /** * Nombre descriptivo del cliente.
     * Atributo temporal excluido de la persistencia directa para evitar redundancia masiva.
     */
    private transient String userPhone;
    private transient String userEmail;
    private transient String path;
    /**
     * Constructor predeterminado sin argumentos (Constructor vacío).
     * <p>
     * <b>Especificación de infraestructura NoSQL:</b> Requisito mandatorio para que el SDK
     * corporativo de Cloud Firestore pueda instanciar y rellenar reflexivamente las propiedades
     * del documento mediante la instrucción {@code document.toObject(Cita.class)}.
     * </p>
     */
    public Cita() {}

    // ===== GETTERS =====
    public String getFecha() { return fecha; }
    public String getHora() { return hora; }
    public String getServicio() { return servicio; }
    public String getUserId() { return userId; }
    public String getPath() { return path; }
    public String getEstado() { return estado; } // 🔹 Nuevo

    public String getUserName() { return userName; }
    public String getUserPhone() { return userPhone; }
    public String getUserEmail() { return userEmail; }

    // ===== SETTERS =====
    public void setUserId(String userId) { this.userId = userId; }
    public void setPath(String path) { this.path = path; }
    public void setEstado(String estado) { this.estado = estado; } // 🔹 Nuevo
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}