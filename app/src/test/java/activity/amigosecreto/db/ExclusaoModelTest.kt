package activity.amigosecreto.db

import org.junit.Assert.*
import org.junit.Test

class ExclusaoModelTest {

    @Test
    fun construtor_padrao_valores_iniciais_sao_zero() {
        val e = Exclusao()
        assertEquals(0, e.participanteId)
        assertEquals(0, e.excluidoId)
    }

    @Test
    fun construtor_com_valores_atribui_corretamente() {
        val e = Exclusao(participanteId = 1, excluidoId = 2)
        assertEquals(1, e.participanteId)
        assertEquals(2, e.excluidoId)
    }

    @Test
    fun setters_funcionam_corretamente() {
        val e = Exclusao()
        e.participanteId = 10
        e.excluidoId = 20
        assertEquals(10, e.participanteId)
        assertEquals(20, e.excluidoId)
    }

    @Test
    fun exclusao_par_reflexivo_pode_ser_criado() {
        // Não há validação no modelo — par (x, x) é válido no nível de entidade
        val e = Exclusao(participanteId = 5, excluidoId = 5)
        assertEquals(5, e.participanteId)
        assertEquals(5, e.excluidoId)
    }

    @Test
    fun exclusao_ids_distintos_nao_sao_confundidos() {
        val e1 = Exclusao(participanteId = 1, excluidoId = 2)
        val e2 = Exclusao(participanteId = 2, excluidoId = 1)
        assertNotEquals(e1.participanteId, e1.excluidoId)
        assertEquals(e1.participanteId, e2.excluidoId)
        assertEquals(e1.excluidoId, e2.participanteId)
    }

    @Test
    fun dois_objetos_exclusao_com_mesmos_valores_sao_independentes() {
        val e1 = Exclusao(participanteId = 3, excluidoId = 4)
        val e2 = Exclusao(participanteId = 3, excluidoId = 4)
        assertEquals(e1.participanteId, e2.participanteId)
        assertEquals(e1.excluidoId, e2.excluidoId)
        // São objetos distintos
        assertNotSame(e1, e2)
    }
}
