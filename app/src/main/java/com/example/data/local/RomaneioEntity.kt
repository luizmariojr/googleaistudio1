package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "romaneios_cache")
data class RomaneioEntity(
    @PrimaryKey
    val id: String,
    val numeroRomaneio: String,
    val status: String = "Criado", // "Criado" | "Entregue" | "Aguardando Sincronização"
    val clientePrincipal: String,
    val cidade: String,
    val uf: String,
    val quantidadeNfs: Int = 1,
    val quantidadeVolumes: Int,
    val pesoTotalKg: Double,
    val motoristaNome: String = "WILIAN SOUSA DA SILVA",
    val prazoEntrega: String = "Hoje",
    val permiteCanhotoUnico: Boolean = true,
    val entregueEm: Long? = null,
    val entregueEmFormatado: String? = null,
    val retiradaConfirmadaEm: Long? = null,
    val observacaoGeral: String? = null,
    val createdAt: String = ""
)

@Entity(tableName = "notas_fiscais_cache")
data class NotaFiscalEntity(
    @PrimaryKey
    val id: String,
    val romaneioId: String,
    val numeroNf: String,
    val destinatario: String,
    val cidade: String,
    val uf: String,
    val volumes: Int,
    val pesoKg: Double = 1.0,
    val valor: Double = 0.0,
    val status: String = "Pendente", // "Pendente" | "Entregue" | "Aguardando Sincronização"
    val canhotoFotoUri: String? = null,
    val entregueEm: Long? = null,
    val entregueEmFormatado: String? = null,
    val avariaRegistrada: Boolean = false,
    val redespachoRegistrado: Boolean = false
)

fun RomaneioEntity.getDataFormatada(): String {
    val raw = createdAt.trim()
    if (raw.isNotBlank()) {
        try {
            val datePart = raw.take(10)
            val parts = datePart.split("-")
            if (parts.size == 3) {
                return "${parts[2]}/${parts[1]}/${parts[0]}"
            }
        } catch (_: Exception) {}
    }
    val prazo = prazoEntrega.trim()
    if (prazo.isNotBlank() && prazo.contains("-")) {
        try {
            val parts = prazo.take(10).split("-")
            if (parts.size == 3) {
                return "${parts[2]}/${parts[1]}/${parts[0]}"
            }
        } catch (_: Exception) {}
    }
    return if (prazo.isNotBlank()) prazo else "Hoje"
}

fun RomaneioEntity.getDataComHoraFormatada(): String {
    val raw = createdAt.trim()
    if (raw.isNotBlank()) {
        try {
            val datePart = raw.take(10)
            val parts = datePart.split("-")
            val timePart = if (raw.length >= 16 && raw.contains("T")) {
                raw.substring(11, 16)
            } else ""
            if (parts.size == 3) {
                val dmy = "${parts[2]}/${parts[1]}/${parts[0]}"
                return if (timePart.isNotBlank()) "$dmy $timePart" else dmy
            }
        } catch (_: Exception) {}
    }
    return getDataFormatada()
}

fun RomaneioEntity.getPrazoFormatado(): String? {
    val prazo = prazoEntrega.trim()
    if (prazo.isBlank() || prazo.equals("null", ignoreCase = true)) return null
    if (prazo.contains("-")) {
        try {
            val parts = prazo.take(10).split("-")
            if (parts.size == 3) {
                return "${parts[2]}/${parts[1]}/${parts[0]}"
            }
        } catch (_: Exception) {}
    }
    return prazo
}

/**
 * Organiza e limpa o nome do cliente:
 * - Remove números de CPF soltos (ex: 88807703149)
 * - Converte CAIXA ALTA para formato legível (Title Case)
 * - Mantém indicativo de múltiplos clientes/NFs (ex: + 4) de forma elegante
 */
fun formatarNomeCliente(nomeBruto: String): String {
    if (nomeBruto.isBlank()) return "Cliente"

    var texto = nomeBruto.trim()
    // Identifica sufixos como "+ 4", "+ 1"
    var sufixo = ""
    val regexSufixo = Regex("""\+\s*(\d+)""")
    val matchSufixo = regexSufixo.find(texto)
    if (matchSufixo != null) {
        val qtd = matchSufixo.groupValues[1]
        sufixo = " (+$qtd ${if (qtd == "1") "NF" else "NFs"})"
        texto = texto.replace(matchSufixo.value, "").trim()
    }

    // Remove CPFs ou números com 11 ou mais dígitos
    texto = texto.replace(Regex("""\b\d{11,}\b"""), "").trim()
    // Remove múltiplos espaços
    texto = texto.replace(Regex("""\s+"""), " ").trim()

    if (texto.isBlank()) return "Cliente$sufixo"

    // Converte para Title Case mantendo preposições minúsculas em português
    val preposicoes = setOf("de", "da", "do", "das", "dos", "e", "em", "para", "com")
    val palavras = texto.lowercase().split(" ")
    val formatado = palavras.mapIndexed { index, p ->
        if (p.isBlank()) ""
        else if (index > 0 && preposicoes.contains(p)) p
        else p.replaceFirstChar { it.uppercase() }
    }.filter { it.isNotBlank() }.joinToString(" ")

    return "$formatado$sufixo"
}

fun RomaneioEntity.getClienteFormatado(): String = formatarNomeCliente(clientePrincipal)

/**
 * Retorna o timestamp em milissegundos da data de criação ou prazo do romaneio.
 * Suporta formatos ISO-8601 (yyyy-MM-ddTHH:mm:ss... ou yyyy-MM-dd) e datas especiais como "Hoje".
 */
fun RomaneioEntity.getDateMillis(): Long? {
    val raw = createdAt.trim()
    if (raw.isNotBlank()) {
        try {
            val isoPart = raw.take(10)
            if (isoPart.contains("-") && isoPart.length == 10) {
                val parts = isoPart.split("-")
                if (parts.size == 3) {
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                        set(java.util.Calendar.YEAR, parts[0].toInt())
                        set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                        set(java.util.Calendar.DAY_OF_MONTH, parts[2].toInt())
                        set(java.util.Calendar.HOUR_OF_DAY, 12)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    return cal.timeInMillis
                }
            }
        } catch (_: Exception) {}
    }

    val prazo = prazoEntrega.trim()
    if (prazo.isNotBlank() && prazo.contains("-")) {
        try {
            val isoPart = prazo.take(10)
            val parts = isoPart.split("-")
            if (parts.size == 3) {
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    set(java.util.Calendar.YEAR, parts[0].toInt())
                    set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                    set(java.util.Calendar.DAY_OF_MONTH, parts[2].toInt())
                    set(java.util.Calendar.HOUR_OF_DAY, 12)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }
        } catch (_: Exception) {}
    }

    if (prazo.equals("Hoje", ignoreCase = true)) {
        return System.currentTimeMillis()
    }

    return null
}

/**
 * Verifica com precisão se o romaneio está dentro dos últimos N dias a partir de agora.
 */
fun RomaneioEntity.isWithinDays(days: Int): Boolean {
    val millis = getDateMillis() ?: return true // se não tiver data, inclui para segurança
    val threshold = System.currentTimeMillis() - (days.toLong() * 24L * 60L * 60L * 1000L)
    return millis >= threshold
}

