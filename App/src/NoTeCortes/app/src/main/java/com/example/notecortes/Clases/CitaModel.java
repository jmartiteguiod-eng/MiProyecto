package com.example.notecortes.Clases;
//La clase CitaModel es una entidad de datos puramente estructural que encapsula las propiedades
// de una reserva (servicio, fecha, hora y estado). Su diseño incluye un constructor explícito sin
// argumentos, requisito indispensable para asegurar la deserialización automática y directa de documentos
// JSON provenientes de Cloud Firestore hacia objetos nativos de Java.
public class CitaModel {

    private String servicio;
    private String fecha;
    private String hora;
    private String estado;

    public CitaModel() {
    }

    public String getServicio() {
        return servicio;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHora() {
        return hora;
    }

    public String getEstado() {
        return estado;
    }
}
