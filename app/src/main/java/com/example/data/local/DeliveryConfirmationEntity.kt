package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_confirmations")
data class DeliveryConfirmationEntity(
    @PrimaryKey
    val id: String, // UUID único de idempotência
    val tipo: String, // "entrega_romaneio" | "entrega_nf" | "retirada"
    val romaneioId: String,
    val numeroRomaneio: String,
    val nfId: String? = null,
    val numeroNf: String? = null,
    val dataHoraEntrega: Long, // Timestamp real do momento em que o usuário clicou (ex: 14:37)
    val dataHoraEntregaFormatada: String, // "06/10/2026 14:37:12"
    val dataHoraSincronizacao: Long? = null, // Timestamp do momento em que o servidor confirmou (ex: 16:10)
    val dataHoraSincronizacaoFormatada: String? = null,
    val status: String, // "PENDENTE" | "SINCRONIZANDO" | "SINCRONIZADO" | "ERRO"
    val tentativas: Int = 0,
    val ultimoErro: String? = null,
    val proximaTentativaEm: Long = 0L,
    val nomeRecebedor: String = "",
    val documentoRecebedor: String = "",
    val observacao: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val canhotoFotoUri: String? = null,
    val assinaturaPresente: Boolean = false,
    val userId: String = "motorista-app-01"
)
