package activity.amigosecreto.util

object ParticipanteAcoesLayout {
    private const val LARGURA_MINIMA_SEIS_COLUNAS_DP = 360
    private const val COLUNAS_LARGURA_AMPLA = 6
    private const val COLUNAS_LARGURA_ESTREITA = 3

    fun colunas(screenWidthDp: Int): Int =
        if (screenWidthDp >= LARGURA_MINIMA_SEIS_COLUNAS_DP) COLUNAS_LARGURA_AMPLA else COLUNAS_LARGURA_ESTREITA
}
