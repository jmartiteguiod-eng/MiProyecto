package com.example.notecortes.Adaptador;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notecortes.Clases.Cita;
import com.example.notecortes.R;

import java.util.List;
//Este adaptador gestiona el historial de citas del cliente. A diferencia de un adaptador plano,
// este incluye lógica de control de nulos para hacer la app más robusta y un sistema de renderizado
// cromático dinámico en onBindViewHolder. Gracias a esto, el usuario puede identificar de un solo vistazo
// el estado de sus citas pasadas mediante un código de colores (verde para las completadas y rojo para las
// no asistidas o canceladas).
public class ClienteHistorialAdapter extends RecyclerView.Adapter<ClienteHistorialAdapter.ViewHolder> {

    private List<Cita> lista;

    public ClienteHistorialAdapter(List<Cita> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cita_historial, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cita cita = lista.get(position);

        holder.txtFecha.setText(cita.getFecha());
        holder.txtServicio.setText("Servicio: " + (cita.getServicio() != null ? cita.getServicio() : "No especificado"));
        holder.txtHora.setText("Hora: " + cita.getHora()); // Asegúrate que Cita tenga getHora()

        String estado = cita.getEstado() != null ? cita.getEstado() : "desconocido";
        holder.txtEstado.setText(estado.toUpperCase());

        // Colores según el estado
        if (estado.equalsIgnoreCase("realizada")) {
            holder.txtEstado.setTextColor(Color.parseColor("#388E3C")); // Verde
        } else if (estado.equalsIgnoreCase("no_realizada")) {
            holder.txtEstado.setTextColor(Color.parseColor("#D32F2F")); // Rojo
        } else {
            holder.txtEstado.setTextColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtFecha, txtServicio, txtEstado, txtHora;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFecha = itemView.findViewById(R.id.txtFechaCita);
            txtServicio = itemView.findViewById(R.id.txtServicioCita);
            txtEstado = itemView.findViewById(R.id.txtEstadoCita);
            txtHora = itemView.findViewById(R.id.txtHoraCita);
        }
    }
}