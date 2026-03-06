package activity.amigosecreto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import activity.amigosecreto.db.Participante;
import activity.amigosecreto.db.ParticipanteDAO;
import activity.amigosecreto.db.DesejoDAO;

public class RevelarAmigoActivity extends AppCompatActivity {

    private Participante participante;
    private Participante amigoSorteado;
    private ParticipanteDAO dao;
    private DesejoDAO desejoDAO;
    private boolean revelado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_revelar_amigo);

        // Aplica padding de sistema no root para que o conteudo nao fique
        // atras da status bar ou navigation bar (edge-to-edge no Android 15+).
        // Os valores originais do XML sao capturados fora do lambda porque o listener
        // pode ser invocado multiplas vezes (ex: rotacao) e setPadding sobrescreveria
        // os insets acumulados em chamadas anteriores.
        View rootView = findViewById(R.id.root_revelar);
        if (rootView != null) {
            final int padLeft = rootView.getPaddingLeft();
            final int padRight = rootView.getPaddingRight();
            final int padTop = rootView.getPaddingTop();
            final int padBottom = rootView.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                        padLeft,
                        padTop + systemBars.top,
                        padRight,
                        padBottom + systemBars.bottom);
                return insets;
            });
        }

        participante = (Participante) getIntent().getSerializableExtra("participante");
        dao = new ParticipanteDAO(this);
        desejoDAO = new DesejoDAO(this);

        TextView tvNomeUsuario = findViewById(R.id.tv_nome_usuario);
        final TextView tvAmigoSorteado = findViewById(R.id.tv_amigo_sorteado);
        final View layoutRevelado = findViewById(R.id.layout_revelado);
        final View layoutEscondido = findViewById(R.id.layout_escondido);
        View cardRevelacao = findViewById(R.id.card_revelacao);
        View btnVoltar = findViewById(R.id.btn_voltar);
        final MaterialButton btnVerDesejos = findViewById(R.id.btn_ver_desejos);

        if (participante != null) {
            tvNomeUsuario.setText(participante.getNome());

            if (participante.getAmigoSorteadoId() != null && participante.getAmigoSorteadoId() > 0) {
                dao.open();
                String nomeAmigo = dao.getNomeAmigoSorteado(participante.getAmigoSorteadoId());

                // Buscar o participante sorteado completo
                amigoSorteado = dao.buscarPorId(participante.getAmigoSorteadoId());
                dao.close();

                tvAmigoSorteado.setText(nomeAmigo);
            } else {
                tvAmigoSorteado.setText("Sorteio não realizado");
            }
        }

        // Botão ver desejos
        btnVerDesejos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (amigoSorteado != null) {
                    desejoDAO.open();
                    int countDesejos = desejoDAO.contarDesejosPorParticipante(amigoSorteado.getId());
                    desejoDAO.close();

                    if (countDesejos > 0) {
                        Intent intent = new Intent(RevelarAmigoActivity.this, VisualizarDesejosActivity.class);
                        intent.putExtra("participante", amigoSorteado);
                        intent.putExtra("somente_visualizar", true);
                        startActivity(intent);
                    } else {
                        Toast.makeText(RevelarAmigoActivity.this, amigoSorteado.getNome() + " ainda não adicionou desejos", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RevelarAmigoActivity.this, "Sorteio não realizado", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cardRevelacao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!revelado) {
                    if (participante.getAmigoSorteadoId() != null && participante.getAmigoSorteadoId() > 0) {
                        layoutEscondido.setVisibility(View.GONE);
                        layoutRevelado.setVisibility(View.VISIBLE);
                        btnVerDesejos.setVisibility(View.VISIBLE);
                        revelado = true;
                    } else {
                        Toast.makeText(RevelarAmigoActivity.this, "O sorteio ainda não foi feito!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    layoutEscondido.setVisibility(View.VISIBLE);
                    layoutRevelado.setVisibility(View.GONE);
                    btnVerDesejos.setVisibility(View.GONE);
                    revelado = false;
                }
            }
        });

        btnVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
