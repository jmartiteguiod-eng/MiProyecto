package com.example.notecortes.Clases;
//La clase Service representa la entidad del catálogo de prestaciones de la barbería. A diferencia de un
// modelo de lectura plano, este diseño implementa de forma estricta el patrón JavaBean completo
// (encapsulación, constructor sin argumentos, getters y setters). La inclusión de los métodos mutadores
// (Setters) es un requisito de arquitectura crítico que permite a la API de Cloud Firestore realizar la
// reflexión y el mapeo bidireccional de datos de forma automática y transparente.
public class Service {
    private String nombre;
    private long duracion;
    private double precio;

    // 1. Constructor vacío obligatorio para Firebase
    public Service() {}

    // 2. Constructor completo
    public Service(String nombre, long duracion, double precio) {
        this.nombre = nombre;
        this.duracion = duracion;
        this.precio = precio;
    }

    // 3. Getters (Para leer los datos en el Adapter)
    public String getNombre() { return nombre; }
    public long getDuracion() { return duracion; }
    public double getPrecio() { return precio; }

    // 4. Setters (¡IMPORTANTES! Sin estos Firestore no puede escribir los datos en el objeto)
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDuracion(long duracion) { this.duracion = duracion; }
    public void setPrecio(double precio) { this.precio = precio; }
}