package activity.amigosecreto.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guarda arquitetural: nenhuma tela **alcançável** pode usar os DAOs legados.
 *
 * O `MySQLiteOpenHelper` está congelado em `DATABASE_VERSION = 10`. Abrir com ele o banco que
 * o Room migrou para 13 dispara `onDowngrade()` e, em seguida, `setVersion(10)` — rebaixando o
 * `user_version`. No start seguinte o Room re-executa a `MIGRATION_10_11`, que recria
 * `participante` sem as colunas da v12 e perde `confirmou_presente`, `foi_notificado` e
 * `observacoes`.
 *
 * Não dá para testar isso ponta a ponta sem infraestrutura de teste do Hilt (o projeto não tem),
 * então o invariante é verificado sobre o código-fonte. Os quatro arquivos tolerados abaixo são
 * telas inalcançáveis — nenhum `Intent` as inicia — e somem quando forem removidas; quando isso
 * acontecer, esvazie a lista.
 */
class DaosLegadosGuardTest {

    private val daosLegados = listOf("DesejoDAO", "ParticipanteDAO", "GrupoDAO", "SorteioDAO")

    /** Telas inalcançáveis que ainda usam DAO legado — ver tabela em CLAUDE.md. */
    private val toleradosPorSeremCodigoMorto = setOf(
        "ListarDesejos.kt",
        "DetalheDesejoActivity.kt",
        "RevelarAmigoActivity.kt",
        "VisualizarDesejosActivity.kt",
    )

    private fun raizDasActivities(): File =
        listOf(
            File("src/main/java/activity/amigosecreto"),
            File("app/src/main/java/activity/amigosecreto"),
        ).firstOrNull { it.isDirectory }
            ?: throw IllegalStateException("fonte de produção não encontrada a partir de ${File("").absolutePath}")

    @Test
    fun nenhuma_tela_alcancavel_instancia_DAO_legado() {
        val raiz = raizDasActivities()
        val activities = raiz.listFiles { f -> f.isFile && f.name.endsWith(".kt") }.orEmpty()
        assertTrue("nenhuma Activity encontrada — o teste não estaria verificando nada", activities.isNotEmpty())

        val infratores = activities.filter { arquivo ->
            arquivo.name !in toleradosPorSeremCodigoMorto &&
                daosLegados.any { dao -> arquivo.readText().contains("$dao(") }
        }.map { it.name }

        assertEquals(
            "estas telas alcançáveis instanciam DAO legado e rebaixariam o user_version: $infratores",
            emptyList<String>(), infratores
        )
    }

    @Test
    fun a_lista_de_tolerados_nao_tem_entrada_obsoleta() {
        // Se uma tela tolerada deixar de existir ou de usar DAO legado, a lista precisa encolher —
        // senão ela mascararia um uso novo em um arquivo de mesmo nome no futuro.
        val raiz = raizDasActivities()
        val obsoletos = toleradosPorSeremCodigoMorto.filter { nome ->
            val f = File(raiz, nome)
            !f.isFile || daosLegados.none { dao -> f.readText().contains("$dao(") }
        }
        assertEquals(
            "remova da lista de tolerados: $obsoletos",
            emptyList<String>(), obsoletos
        )
    }
}
