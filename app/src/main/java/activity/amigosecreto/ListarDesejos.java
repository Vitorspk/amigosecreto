package activity.amigosecreto;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.view.MenuItemCompat;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;


public class ListarDesejos extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private ListView lv_desejos;
    private ListarDesejosAdapter adapter;
    private List<Desejo> listaDesejos;
    private List<Desejo> lista;
    private TextView tv_empty;
    private FloatingActionButton fabNovo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar_desejos);
        
        listaDesejos = new ArrayList<Desejo>();
        adapter = new ListarDesejosAdapter(this, listaDesejos);
        lv_desejos = (ListView) findViewById(R.id.lv_desejos);
        tv_empty = (TextView) findViewById(R.id.tv_empty);
        fabNovo = (FloatingActionButton) findViewById(R.id.fab_novo);
        
        if (lv_desejos != null) {
            lv_desejos.setEmptyView(tv_empty);
            lv_desejos.setOnItemClickListener(this);
            lv_desejos.setAdapter(adapter);
        }
        
        if (fabNovo != null) {
            fabNovo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ListarDesejos.this, InserirDesejoActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        carregarLista();
    }

    private void carregarLista() {
        try {
            DesejoDAO dao = new DesejoDAO(this);
            dao.open();
            lista = dao.listar();
            listaDesejos.clear();
            if (lista != null) {
                listaDesejos.addAll(lista);
            }
            dao.close();
            adapter.notifyDataSetChanged();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.listar_desejos, menu);
        compartilharLista(menu);
        return true;
    }

    private void compartilharLista(Menu menu){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        StringBuilder sb = new StringBuilder();
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        String ls = System.getProperty("line.separator");
        if (lista != null && !lista.isEmpty()) {
            for(Desejo d : lista){
                sb.append("Desejo: ").append(d.getProduto()).append(ls);
                sb.append("Preço: de ").append(nf.format(d.getPrecoMinimo())).append(" até ").append(nf.format(d.getPrecoMaximo())).append(ls);
                String lojas = d.getLojas().replace(ls, ", ");
                sb.append("Onde encontrar: ").append(lojas).append(ls).append(ls);
            }
        } else {
            sb.append("Minha lista de desejos está vazia!");
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, "Minha lista de desejos");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        MenuItem item = menu.findItem(R.id.menu_compartilhar);
        if (item != null) {
            ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(intent);
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.menu_novo) {
            Intent intent = new Intent(this, InserirDesejoActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Desejo desejo = listaDesejos.get(position);
        Intent intent = new Intent(this, DetalheDesejoActivity.class);
        intent.putExtra("desejo", desejo);
        startActivity(intent);
    }

    private class ListarDesejosAdapter extends BaseAdapter {
        private Context ctx;
        private List<Desejo> produtos;
        private NumberFormat nf = NumberFormat.getCurrencyInstance();

        ListarDesejosAdapter(Context ctx, List<Desejo> produtos) {
            this.ctx = ctx;
            this.produtos = produtos;
        }
        @Override
        public int getCount() {
            return produtos.size();
        }

        @Override
        public Object getItem(int position) {
            return produtos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(ctx).inflate(R.layout.item_desejo, parent, false);
                holder = new ViewHolder();
                holder.tvProduto = convertView.findViewById(R.id.tv_item_produto);
                holder.tvCategoria = convertView.findViewById(R.id.tv_item_categoria);
                holder.tvPreco = convertView.findViewById(R.id.tv_item_preco);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Desejo desejo = produtos.get(position);

            if (holder.tvProduto != null) holder.tvProduto.setText(desejo.getProduto());
            if (holder.tvCategoria != null) holder.tvCategoria.setText(desejo.getCategoria());
            if (holder.tvPreco != null) holder.tvPreco.setText(String.format("%s - %s", nf.format(desejo.getPrecoMinimo()), nf.format(desejo.getPrecoMaximo())));

            return convertView;
        }

        private static class ViewHolder {
            TextView tvProduto;
            TextView tvCategoria;
            TextView tvPreco;
        }
    }

}
