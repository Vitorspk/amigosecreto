package activity.amigosecreto.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import activity.amigosecreto.R;
import activity.amigosecreto.db.Participante;

/**
 * Modern RecyclerView Adapter for Participantes with animations
 */
public class ParticipantesRecyclerAdapter extends RecyclerView.Adapter<ParticipantesRecyclerAdapter.ViewHolder> {

    private Context context;
    private List<Participante> participantes;
    private OnItemClickListener listener;
    private int lastPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(Participante participante);
        void onRemoveClick(Participante participante);
        void onShareClick(Participante participante);
    }

    public ParticipantesRecyclerAdapter(Context context, List<Participante> participantes) {
        this.context = context;
        this.participantes = participantes;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_participante, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Participante participante = participantes.get(position);
        holder.bind(participante);

        // Apply animation
        if (position > lastPosition) {
            holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.card_appear));
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return participantes.size();
    }

    public void updateList(List<Participante> newList) {
        this.participantes = newList;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNome, tvEmail, tvNumero, tvAvatar;
        ImageButton btnRemover, btnShare;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNome = itemView.findViewById(R.id.tv_nome);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvNumero = itemView.findViewById(R.id.tv_numero);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            btnRemover = itemView.findViewById(R.id.btn_remover);
            btnShare = itemView.findViewById(R.id.btn_share);
        }

        void bind(Participante participante) {
            tvNome.setText(participante.getNome());
            tvNumero.setText(String.valueOf(getAdapterPosition() + 1));

            // Avatar with first letter
            String inicial = participante.getNome().substring(0, 1).toUpperCase();
            tvAvatar.setText(inicial);

            // Status
            if (participante.isEnviado()) {
                tvEmail.setText("✓ Enviado");
            } else {
                tvEmail.setText("Pendente");
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(participante);
                }
            });

            btnRemover.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveClick(participante);
                }
            });

            btnShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(participante);
                }
            });
        }
    }
}