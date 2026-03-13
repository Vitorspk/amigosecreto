package activity.amigosecreto;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;
import activity.amigosecreto.db.Participante;

public class ParticipanteDesejosActivity extends AppCompatActivity {

    private static final int REQUEST_EDIT_DESEJO = 100;

    private Participante participante;
    private DesejoDAO desejoDAO;
    private List<Desejo> listaDesejos;

    private ListView lvDesejos;
    private TextView tvPresentesCount;
    private View layoutEmpty;
    private DesejosAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_participante_desejos);

        // Receber participante via Intent
        participante = (Participante) getIntent().getSerializableExtra("participante");
        if (participante == null) {
            Toast.makeText(this, R.string.error_participant_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar DAO
        desejoDAO = new DesejoDAO(this);
        desejoDAO.open();

        // Inicializar views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(participante.getNome() + " 🎁");
        toolbar.setNavigationOnClickListener(v -> finish());

        lvDesejos = findViewById(R.id.lv_desejos);
        tvPresentesCount = findViewById(R.id.tv_presentes_count);
        layoutEmpty = findViewById(R.id.layout_empty);
        MaterialButton btnAddDesejo = findViewById(R.id.btn_add_desejo);

        // Configurar adapter
        listaDesejos = new ArrayList<>();
        adapter = new DesejosAdapter(this, listaDesejos);
        lvDesejos.setAdapter(adapter);

        // Botão adicionar desejo
        btnAddDesejo.setOnClickListener(v -> mostrarDialogAdicionarDesejo());

        // Carregar desejos
        carregarDesejos();
    }

    private void carregarDesejos() {
        listaDesejos.clear();
        List<Desejo> desejos = desejoDAO.listarPorParticipante(participante.getId());
        listaDesejos.addAll(desejos);
        adapter.notifyDataSetChanged();

        // Atualizar contador
        tvPresentesCount.setText(getString(R.string.label_wishes_count_format, listaDesejos.size()));

        // Mostrar/ocultar empty state
        if (listaDesejos.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            lvDesejos.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            lvDesejos.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarDialogAdicionarDesejo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_desejo, null);

        final TextInputEditText etProduto = view.findViewById(R.id.et_produto);
        final TextInputEditText etCategoria = view.findViewById(R.id.et_categoria);
        final TextInputEditText etPrecoMin = view.findViewById(R.id.et_preco_minimo);
        final TextInputEditText etPrecoMax = view.findViewById(R.id.et_preco_maximo);
        final TextInputEditText etLojas = view.findViewById(R.id.et_lojas);

        builder.setView(view)
                .setTitle(R.string.dialog_add_wish_title)
                .setPositiveButton(R.string.button_save, null)
                .setNegativeButton(R.string.button_cancel, null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent auto-dismiss on validation failure
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String produto = etProduto.getText() != null ? etProduto.getText().toString().trim() : "";

                if (produto.isEmpty()) {
                    Toast.makeText(ParticipanteDesejosActivity.this, R.string.error_product_name_required, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    Desejo desejo = new Desejo();
                    desejo.setId(desejoDAO.proximoId());
                    desejo.setProduto(produto);

                    if (etCategoria != null && etCategoria.getText() != null) {
                        desejo.setCategoria(etCategoria.getText().toString().trim());
                    }

                    // Tratar preços - substituir vírgula por ponto para parseDouble
                    String precoMinStr = (etPrecoMin != null && etPrecoMin.getText() != null) ? etPrecoMin.getText().toString().trim().replace(",", ".") : "";
                    String precoMaxStr = (etPrecoMax != null && etPrecoMax.getText() != null) ? etPrecoMax.getText().toString().trim().replace(",", ".") : "";
                    desejo.setPrecoMinimo(precoMinStr.isEmpty() ? 0 : Double.parseDouble(precoMinStr));
                    desejo.setPrecoMaximo(precoMaxStr.isEmpty() ? 0 : Double.parseDouble(precoMaxStr));

                    if (etLojas != null && etLojas.getText() != null) {
                        desejo.setLojas(etLojas.getText().toString().trim());
                    }

                    desejo.setParticipanteId(participante.getId());

                    desejoDAO.inserir(desejo);
                    Toast.makeText(ParticipanteDesejosActivity.this, R.string.toast_wish_added, Toast.LENGTH_SHORT).show();
                    carregarDesejos();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(ParticipanteDesejosActivity.this, R.string.error_invalid_price, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (Exception e) {
                    Toast.makeText(ParticipanteDesejosActivity.this, getString(R.string.error_generic_format, e.getMessage()), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (desejoDAO != null) {
            desejoDAO.close();
        }
    }

    // Adapter para lista de desejos
    private class DesejosAdapter extends BaseAdapter {
        private Context context;
        private List<Desejo> desejos;

        public DesejosAdapter(Context context, List<Desejo> desejos) {
            this.context = context;
            this.desejos = desejos;
        }

        @Override
        public int getCount() {
            return desejos.size();
        }

        @Override
        public Object getItem(int position) {
            return desejos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_desejo, parent, false);
            }

            Desejo desejo = desejos.get(position);

            TextView tvProduto = convertView.findViewById(R.id.tv_item_produto);
            TextView tvCategoria = convertView.findViewById(R.id.tv_item_categoria);
            TextView tvPreco = convertView.findViewById(R.id.tv_item_preco);

            tvProduto.setText(desejo.getProduto());
            tvCategoria.setText(desejo.getCategoria());

            // Formatar preço
            if (desejo.getPrecoMinimo() > 0 || desejo.getPrecoMaximo() > 0) {
                String precoTexto = "R$ " + String.format("%.2f", desejo.getPrecoMinimo())
                        + " - R$ " + String.format("%.2f", desejo.getPrecoMaximo());
                tvPreco.setText(precoTexto);
            } else {
                tvPreco.setText("");
            }

            // Click para editar ou remover
            convertView.setOnClickListener(v -> mostrarOpcoesDesejo(desejo));

            return convertView;
        }
    }

    private void mostrarOpcoesDesejo(Desejo desejo) {
        // Inflar o layout customizado
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_opcoes_desejo, null);

        TextView tvProdutoNome = dialogView.findViewById(R.id.tv_produto_nome);
        com.google.android.material.card.MaterialCardView cardEditar = dialogView.findViewById(R.id.card_editar);
        com.google.android.material.card.MaterialCardView cardRemover = dialogView.findViewById(R.id.card_remover);
        com.google.android.material.button.MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_cancelar);

        tvProdutoNome.setText(desejo.getProduto());

        // Criar o dialog
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Tornar o fundo transparente para mostrar os cantos arredondados
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Listener do card Editar
        cardEditar.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, AlterarDesejoActivity.class);
            intent.putExtra("desejo", desejo);
            startActivityForResult(intent, REQUEST_EDIT_DESEJO);
        });

        // Listener do card Remover
        cardRemover.setOnClickListener(v -> {
            dialog.dismiss();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_remove_wish_title)
                    .setMessage(getString(R.string.dialog_remove_wish_message_format, desejo.getProduto()))
                    .setPositiveButton(R.string.button_remove_yes, (d, w) -> {
                        desejoDAO.remover(desejo);
                        Toast.makeText(this, R.string.toast_wish_removed, Toast.LENGTH_SHORT).show();
                        carregarDesejos();
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show();
        });

        // Listener do botão Cancelar
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_DESEJO && resultCode == RESULT_OK) {
            carregarDesejos();
        }
    }
}
