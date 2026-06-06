package com.example.notecortes.Administrador;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notecortes.Clases.Cita;
import com.example.notecortes.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
//El AdminCitasAdapter es un controlador de interfaz bidireccional y reactivo. Destaca por optimizar
// el rendimiento visual mediante la reutilización de drawables nativos de Android modificados en tiempo
// de ejecución con filtros cromáticos (setColorFilter). Además, implementa un flujo de escritura atómico
// en Cloud Firestore aprovechando la persistencia del puntero jerárquico (getPath()) del modelo. El
// refresco de la UI se gestiona de forma selectiva mediante notifyItemChanged, minimizando la sobrecarga
// de renderizado en el dispositivo y ofreciendo un control de estados contextual ágil para el operario a
// través de un AlertDialog
public class AdminCitasAdapter extends RecyclerView.Adapter<AdminCitasAdapter.ViewHolder> {

    private final List<Cita> lista;
    private final Context context;

    public AdminCitasAdapter(List<Cita> lista, Context context) {
        this.lista = lista;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_cita_admin, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cita cita = lista.get(position);

        holder.txtFecha.setText("Fecha: " + cita.getFecha());
        holder.txtHora.setText("Hora: " + cita.getHora());
        holder.txtServicio.setText("Servicio: " + cita.getServicio());
        holder.txtCliente.setText("Cliente: " + cita.getUserName());
        holder.txtTelefono.setText("Tel: " + cita.getUserPhone());
        holder.txtEmail.setText("Email: " + cita.getUserEmail());

        // --- Lógica Visual del Estado ---
        if ("realizada".equals(cita.getEstado())) {
            holder.imgEstado.setImageResource(android.R.drawable.checkbox_on_background); // Stick verde (default android)
            holder.imgEstado.setColorFilter(Color.GREEN);
            holder.imgEstado.setVisibility(View.VISIBLE);
        } else if ("no_realizada".equals(cita.getEstado())) {
            holder.imgEstado.setImageResource(android.R.drawable.ic_delete); // X roja (default android)
            holder.imgEstado.setColorFilter(Color.RED);
            holder.imgEstado.setVisibility(View.VISIBLE);
        } else {
            holder.imgEstado.setVisibility(View.GONE);
        }

        // --- Clic para cambiar estado ---
        holder.itemView.setOnClickListener(v -> {
            String[] opciones = {"Realizada", "No realizada", "Pendiente"};
            new AlertDialog.Builder(context)
                    .setTitle("Estado de la cita")
                    .setItems(opciones, (dialog, which) -> {
                        String nuevoEstado;
                        if (which == 0) nuevoEstado = "realizada";
                        else if (which == 1) nuevoEstado = "no_realizada";
                        else nuevoEstado = null;

                        actualizarEstadoEnFirebase(cita, nuevoEstado, position);
                    })
                    .show();
        });
    }

    private void actualizarEstadoEnFirebase(Cita cita, String nuevoEstado, int position) {
        if (cita.getPath() == null) return;

        FirebaseFirestore.getInstance().document(cita.getPath())
                .update("estado", nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    cita.setEstado(nuevoEstado);
                    notifyItemChanged(position);
                    Toast.makeText(context, "Estado actualizado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtFecha, txtHora, txtServicio, txtCliente, txtTelefono, txtEmail;
        ImageView imgEstado;

        ViewHolder(View itemView) {
            super(itemView);
            txtFecha = itemView.findViewById(R.id.txtFecha);
            txtHora = itemView.findViewById(R.id.txtHora);
            txtServicio = itemView.findViewById(R.id.txtServicio);
            txtCliente = itemView.findViewById(R.id.txtCliente);
            txtTelefono = itemView.findViewById(R.id.txtTelefono);
            txtEmail = itemView.findViewById(R.id.txtEmail);
            imgEstado = itemView.findViewById(R.id.imgEstado);
        }
    }
}