package com.example.notecortes.Adaptador;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notecortes.Clases.CitaModel;
import com.example.notecortes.R;

import java.util.List;
//Este adaptador se encarga de pintar de forma eficiente el historial o la lista de reservas del
// usuario en la barbería, reciclando las vistas en memoria gracias al patrón ViewHolder y mapeando los
// atributos de CitaModel en el diseño item_cita.
public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.ViewHolder> {

    private List<CitaModel> listaCitas;

    public CitaAdapter(List<CitaModel> listaCitas) {
        this.listaCitas = listaCitas;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cita, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CitaModel cita = listaCitas.get(position);

        holder.txtServicio.setText(cita.getServicio());
        holder.txtFecha.setText(cita.getFecha());
        holder.txtHora.setText(cita.getHora());
        holder.txtEstado.setText(cita.getEstado());
    }

    @Override
    public int getItemCount() {
        return listaCitas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtServicio, txtFecha, txtHora, txtEstado;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtServicio = itemView.findViewById(R.id.txtServicio);
            txtFecha = itemView.findViewById(R.id.txtFecha);
            txtHora = itemView.findViewById(R.id.txtHora);
            txtEstado = itemView.findViewById(R.id.txtEstado);
        }
    }
}
