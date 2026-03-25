package activity.amigosecreto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "amigosecreto_prefs"
        private const val KEY_ONBOARDING_CONCLUIDO = "onboarding_concluido"

        fun isOnboardingConcluido(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_CONCLUIDO, false)
        }

        fun marcarOnboardingConcluido(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ONBOARDING_CONCLUIDO, true)
                .apply()
        }

        @androidx.annotation.VisibleForTesting
        fun limparOnboarding(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ONBOARDING_CONCLUIDO, false)
                .commit()
        }
    }

    data class OnboardingPage(
        val iconRes: Int,
        val titleRes: Int,
        val descriptionRes: Int,
    )

    private val paginas = listOf(
        OnboardingPage(
            iconRes = R.drawable.ic_gift,
            titleRes = R.string.onboarding_titulo_1,
            descriptionRes = R.string.onboarding_desc_1,
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_add,
            titleRes = R.string.onboarding_titulo_2,
            descriptionRes = R.string.onboarding_desc_2,
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_person_add,
            titleRes = R.string.onboarding_titulo_3,
            descriptionRes = R.string.onboarding_desc_3,
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_dice,
            titleRes = R.string.onboarding_titulo_4,
            descriptionRes = R.string.onboarding_desc_4,
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_share,
            titleRes = R.string.onboarding_titulo_5,
            descriptionRes = R.string.onboarding_desc_5,
        ),
    )

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnAvancar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.vp_onboarding)
        tabLayout = findViewById(R.id.tab_onboarding_dots)
        btnAvancar = findViewById(R.id.btn_onboarding_avancar)

        viewPager.adapter = OnboardingAdapter(paginas)

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                atualizarBotao(position)
            }
        })

        atualizarBotao(0)

        btnAvancar.setOnClickListener {
            val posicaoAtual = viewPager.currentItem
            if (posicaoAtual < paginas.size - 1) {
                viewPager.currentItem = posicaoAtual + 1
            } else {
                concluirOnboarding()
            }
        }
    }

    private fun atualizarBotao(posicao: Int) {
        btnAvancar.text = if (posicao == paginas.size - 1) {
            getString(R.string.onboarding_btn_comecar)
        } else {
            getString(R.string.onboarding_btn_proximo)
        }
    }

    private fun concluirOnboarding() {
        // commit() garante escrita síncrona antes do finish() — evita re-exibição do onboarding
        // se o processo for morto antes do flush assíncrono do apply().
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_CONCLUIDO, true)
            .commit()
        startActivity(Intent(this, GruposActivity::class.java))
        finish()
    }

    private inner class OnboardingAdapter(
        private val paginas: List<OnboardingPage>
    ) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcone: ImageView = view.findViewById(R.id.iv_onboarding_icone)
            val tvTitulo: TextView = view.findViewById(R.id.tv_onboarding_titulo)
            val tvDescricao: TextView = view.findViewById(R.id.tv_onboarding_descricao)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = paginas.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pagina = paginas[position]
            holder.ivIcone.setImageResource(pagina.iconRes)
            holder.tvTitulo.setText(pagina.titleRes)
            holder.tvDescricao.setText(pagina.descriptionRes)
        }
    }
}
