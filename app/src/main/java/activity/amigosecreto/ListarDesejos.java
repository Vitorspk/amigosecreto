package activity.amigosecreto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import com.purplebrain.adbuddiz.sdk.AdBuddiz;
import com.purplebrain.adbuddiz.sdk.AdBuddizLogLevel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import activity.amigosecreto.db.Desejo;
import activity.amigosecreto.db.DesejoDAO;


public class ListarDesejos extends Activity implements AdapterView.OnItemClickListener{

    private ListView lv_desejos;
    private ListarDesejosAdapter adapter;
    private List<Desejo> listaDesejos;
    private List<Desejo> lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar_desejos);
        final Activity Activity = this;
        listaDesejos = new ArrayList<Desejo>();
        adapter = new ListarDesejosAdapter(this, listaDesejos);
        lv_desejos = (ListView) findViewById(R.id.lv_desejos);
        lv_desejos.setOnItemClickListener(this);
        lv_desejos.setAdapter(adapter);
        AdBuddiz.setLogLevel(AdBuddizLogLevel.Info);
        AdBuddiz.setPublisherKey("7ba590d3-0e58-41b4-b5f3-3325b059309d");
        AdBuddiz.cacheAds(Activity);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                To call whenever you want to display an Ad.
                Parameter is the current activity
                */
                AdBuddiz.showAd(Activity);
            }
        });
}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdBuddiz.onDestroy(); // to minimize memory footprint
    }
    @Override
    protected void onStart(){
        super.onStart();
        try {
            DesejoDAO dao = new DesejoDAO(this);
            dao.open();
            lista = dao.listar();
            listaDesejos.clear();
            listaDesejos.addAll(lista);
            dao.close();
            adapter.notifyDataSetChanged();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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
        for(Desejo d : lista){
            sb.append("Desejo: " + d.getProduto() + ls);
            sb.append("Preco: de " + nf.format(d.getPrecoMinimo()) + " ate " + nf.format(d.getPrecoMaximo()) + ls);
            String lojas = d.getLojas().replace(ls, ", ");
            sb.append("Onde encontrar: " + lojas + ls + ls);
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, "Minha lista de desejos" + ls);
        intent.putExtra(Intent.EXTRA_TEXT,sb.toString());
        ShareActionProvider mShareActionProvider = (ShareActionProvider) menu.findItem(R.id.menu_compartilhar).getActionProvider();
        mShareActionProvider.setShareIntent(intent);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.menu_novo:
                Intent intent = new Intent(this, InserirDesejoActivity.class);
                startActivity(intent);
                return true;
            case android.R.id.home:
                //Metodo finish() vai encerrar essa activity
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Desejo desejo = (Desejo) ((TextView)view).getTag();
        Intent intent = new Intent(this, DetalheDesejoActivity.class);
        intent.putExtra("desejo", desejo);
        startActivity(intent);
    }
    private class ListarDesejosAdapter extends BaseAdapter {
        private Context ctx;
        private List<Desejo> produtos;

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
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = new TextView(ctx);
            }
            ((TextView) view).setText(produtos.get(position).getProduto());
            ((TextView) view).setTextAppearance(ctx, android.R.style.TextAppearance_Large);
            view.setTag(produtos.get(position));
            return view;
        }
    }

}
