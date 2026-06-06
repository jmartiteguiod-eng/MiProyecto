package com.example.notecortes.Adaptador;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notecortes.R;
import com.example.notecortes.Clases.Service;

import java.util.List;
//Este adaptador gestiona el catálogo dinámico de servicios disponibles en la barbería. Destaca
// por aplicar técnicas de formateo de datos en tiempo real dentro del método onBindViewHolder:
//  encapsula el precio bajo el estándar monetario europeo con dos decimales fijos mediante String.format
//  e incorpora elementos visuales contextuales (como el indicador de tiempo) para optimizar la legibilidad
//  y la experiencia de usuario.
public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {
    private List<Service> serviceList;

    public ServiceAdapter(List<Service> serviceList) {
        this.serviceList = serviceList;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service2, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        Service service = serviceList.get(position);
        holder.tvNombre.setText(service.getNombre());
        holder.tvDuracion.setText("🕒 " + service.getDuracion() + " min");
        holder.tvPrecio.setText(String.format("%.2f€", service.getPrecio()));
    }

    @Override
    public int getItemCount() { return serviceList.size(); }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDuracion, tvPrecio;
        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvDuracion = itemView.findViewById(R.id.tvDuracion);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
        }
    }
}
