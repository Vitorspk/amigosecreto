package activity.amigosecreto

import android.graphics.Bitmap
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import activity.amigosecreto.db.Desejo
import activity.amigosecreto.db.Grupo
import activity.amigosecreto.db.Participante
import activity.amigosecreto.db.room.AppDatabase
import activity.amigosecreto.util.AsyncDatabaseHelper
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.anything
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Listas de desejos por participante (editável e somente leitura) com muitos itens. Não abre
 * ListarDesejos/DetalheDesejoActivity: usam DAO legado, que regrava o user_version do banco do Room.
 */
@RunWith(AndroidJUnit4::class)
class DesejosNavigationTest {
    private lateinit var db: AppDatabase
    private lateinit var participante: Participante

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(AsyncDatabaseHelper.idlingResource)
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.getInstance(ctx)
        runBlocking {
            db.grupoDao().deletarTudo()
            val grupoId = db.grupoDao().inserir(Grupo(nome = "Encontro de Natal")).toInt()
            val participanteId = db.participanteDao().inserir(
                Participante(nome = "Ana", grupoId = grupoId)
            ).toInt()
            participante = checkNotNull(db.participanteDao().buscarPorId(participanteId))
            db.desejoDao().inserir(
                Desejo(produto = "Caneca", categoria = "Casa", lojas = "Loja",
                    precoMinimo = 50.0, precoMaximo = 100.0, participanteId = participanteId)
            )
        }
    }

    @Test
    fun desejos_revelados_permitem_rolar_sem_rebaixar_o_banco() {
        adicionarMaisDesejos()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(AppDatabase.SCHEMA_VERSION, db.openHelper.writableDatabase.version)
        ActivityScenario.launch<VisualizarDesejosActivity>(
            Intent(ctx, VisualizarDesejosActivity::class.java).putExtra("participante", participante)
        ).use {
            onView(withId(R.id.tv_nome_participante)).check(matches(withText("Ana")))
            conferirUltimoDesejo()
            captureIfRequested("revealed-wishes-multiple")
        }
        assertEquals(AppDatabase.SCHEMA_VERSION, db.openHelper.writableDatabase.version)
        assertEquals(9, runBlocking { db.desejoDao().listarPorParticipante(participante.id).size })
    }

    @Test
    fun desejos_editaveis_permitem_rolar_ate_o_ultimo_item() {
        adicionarMaisDesejos()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch<ParticipanteDesejosActivity>(
            Intent(ctx, ParticipanteDesejosActivity::class.java).putExtra("participante", participante)
        ).use {
            conferirUltimoDesejo()
            captureIfRequested("editable-wishes-multiple")
        }
    }

    private fun adicionarMaisDesejos() = runBlocking {
        repeat(8) { index ->
            db.desejoDao().inserir(Desejo(produto = "Livro ${index + 1}", participanteId = participante.id))
        }
    }

    private fun conferirUltimoDesejo() {
        onData(anything()).inAdapterView(withId(R.id.lv_desejos)).atPosition(8)
            .onChildView(withId(R.id.tv_item_produto)).check(matches(withText("Livro 8")))
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(AsyncDatabaseHelper.idlingResource)
        runBlocking { db.grupoDao().deletarTudo() }
    }

    private fun captureIfRequested(name: String) {
        if (InstrumentationRegistry.getArguments().getString("redesignScreenshots") != "true") return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "redesign")
        check(directory.isDirectory || directory.mkdirs())
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            FileOutputStream(File(directory, "$name.png")).use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
