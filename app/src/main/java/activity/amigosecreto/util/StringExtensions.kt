package activity.amigosecreto.util

/**
 * Converte uma String nullable para o texto de avatar de uma letra:
 * pega o primeiro caractere não-branco, converte para maiúscula.
 * Retorna "?" se a string for nula, em branco ou vazia.
 *
 * Uso: participante.nome.toAvatarText()
 */
fun String?.toAvatarText(): String =
    this?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
