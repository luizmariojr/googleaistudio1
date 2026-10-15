package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DeliveryConfirmationEntity
import com.example.sync.SyncState
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkHeader
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceCardBorder
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusAmberBorder
import com.example.ui.theme.StatusAmberText
import com.example.ui.theme.StatusEmeraldBg
import com.example.ui.theme.StatusEmeraldBorder
import com.example.ui.theme.StatusEmeraldText
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncQueueScreen(
    confirmations: List<DeliveryConfirmationEntity>,
    pendingCount: Int,
    isOnline: Boolean,
    syncState: SyncState,
    lastSyncTimestamp: Long?,
    lastOnlineTimestamp: Long?,
    onBack: () -> Unit,
    onTriggerSync: () -> Unit,
    onRefreshRealData: () -> Unit,
    onSelectForAudit: (DeliveryConfirmationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSyncing = syncState is SyncState.Syncing
    val timeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Cabeçalho da Tela
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkHeader,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("sync_queue_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fila de Sincronização & Auditoria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${confirmations.size} registros • $pendingCount pendente(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onRefreshRealData,
                    enabled = isOnline && !isSyncing,
                    modifier = Modifier.testTag("sync_queue_refresh_server")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar do Servidor",
                        tint = if (isOnline) BrandBlue else TextMuted
                    )
                }
            }
        }

        // Cartão de Status e Conectividade
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (isOnline) BrandEmerald else BrandAmber,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "ONLINE COM A CENTRAL" else "MODO OFFLINE (SEGURO)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) StatusEmeraldText else StatusAmberText
                        )
                    }

                    if (pendingCount > 0) {
                        Surface(
                            color = StatusAmberBg,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusAmberBorder)
                        ) {
                            Text(
                                text = "$pendingCount Pendente(s)",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusAmberText
                            )
                        }
                    } else {
                        Surface(
                            color = StatusEmeraldBg,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusEmeraldBorder)
                        ) {
                            Text(
                                text = "Tudo em Dia",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusEmeraldText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Estado de Sincronização Dinâmico
                when (syncState) {
                    is SyncState.Syncing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = BrandBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Enviando: ${syncState.currentItem} (${syncState.progress}/${syncState.total})...",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    is SyncState.Failed -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = BrandRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Falha no envio: ${syncState.error}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    is SyncState.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusEmeraldText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = syncState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusEmeraldText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    else -> {}
                }

                // Timestamps de Auditoria
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (lastSyncTimestamp != null && lastSyncTimestamp > 0) {
                        Text(
                            text = "Último envio central: ${timeFormatter.format(Date(lastSyncTimestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (lastOnlineTimestamp != null && lastOnlineTimestamp > 0) {
                        Text(
                            text = "Último sinal de rede: ${timeFormatter.format(Date(lastOnlineTimestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botão de Disparo Manual
                Button(
                    onClick = onTriggerSync,
                    enabled = isOnline && !isSyncing && pendingCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_queue_trigger_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        disabledContainerColor = DarkSurfaceCardBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transmitindo para Central...", color = TextPrimary)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (pendingCount > 0) "Enviar $pendingCount Baixa(s) Pendente(s)" else "Nenhuma Baixa Pendente"
                        )
                    }
                }
            }
        }

        // Card Explicativo de Segurança & Auditoria Jurídica
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = BrandNavy,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = BrandEmerald,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Auditoria Jurídica: O momento real do aceite offline no aparelho fica gravado de forma imutável, mesmo se o envio à central ocorrer horas depois.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Lista de Comprovantes
        if (confirmations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = BrandEmerald
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Fila Vazia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nenhuma confirmação de entrega pendente ou registrada localmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("sync_queue_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(confirmations, key = { it.id }) { item ->
                    DeliveryConfirmationCard(
                        confirmation = item,
                        onClick = { onSelectForAudit(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryConfirmationCard(
    confirmation: DeliveryConfirmationEntity,
    onClick: () -> Unit
) {
    val isPendente = confirmation.status == "PENDENTE"
    val isSincronizado = confirmation.status == "SINCRONIZADO"
    val isErro = confirmation.status == "ERRO"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("queue_item_${confirmation.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPendente) StatusAmberBorder.copy(alpha = 0.6f) else DarkSurfaceCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Linha Superior: Tipo, NF/Romaneio e Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (confirmation.tipo) {
                            "entrega_romaneio" -> "Romaneio Completo: ${confirmation.numeroRomaneio}"
                            "entrega_nf" -> "Nota Fiscal: ${confirmation.numeroNf ?: "-"}"
                            else -> "Retirada: ${confirmation.numeroRomaneio}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (confirmation.numeroNf != null && confirmation.tipo == "entrega_romaneio") {
                        Text(
                            text = "NF #${confirmation.numeroNf}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Badge de Status
                Surface(
                    color = when {
                        isSincronizado -> StatusEmeraldBg
                        isErro -> BrandRed.copy(alpha = 0.2f)
                        else -> StatusAmberBg
                    },
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isSincronizado -> StatusEmeraldBorder
                            isErro -> BrandRed
                            else -> StatusAmberBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isSincronizado -> Icons.Default.CheckCircle
                                isErro -> Icons.Default.CloudOff
                                else -> Icons.Default.CloudUpload
                            },
                            contentDescription = null,
                            tint = when {
                                isSincronizado -> StatusEmeraldText
                                isErro -> BrandRed
                                else -> StatusAmberText
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isSincronizado -> "SINCRONIZADO"
                                isErro -> "ERRO"
                                else -> "OFFLINE PENDENTE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSincronizado -> StatusEmeraldText
                                isErro -> BrandRed
                                else -> StatusAmberText
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Detalhes da Baixa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recebedor: ${confirmation.nomeRecebedor.ifBlank { "Não informado" }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (confirmation.documentoRecebedor.isNotBlank()) {
                        Text(
                            text = "Doc: ${confirmation.documentoRecebedor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Badges de Evidências
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!confirmation.canhotoFotoUri.isNullOrBlank()) {
                        Surface(
                            color = BrandBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "📸 Foto",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (confirmation.assinaturaPresente) {
                        Surface(
                            color = BrandEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "✍️ Assinatura",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusEmeraldText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamps de Auditoria
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandNavy.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Momento no Aparelho: ${confirmation.dataHoraEntregaFormatada}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (confirmation.dataHoraSincronizacaoFormatada != null) {
                        "Enviado à Central: ${confirmation.dataHoraSincronizacaoFormatada}"
                    } else {
                        "Enviado à Central: Aguardando conexão"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSincronizado) StatusEmeraldText else TextMuted
                )
            }

            if (!confirmation.ultimoErro.isNullOrBlank() && isErro) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Erro: ${confirmation.ultimoErro} (Tentativas: ${confirmation.tentativas})",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandRed
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dica de clique para auditoria
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toque para ver auditoria completa ›",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandBlue
                )
            }
        }
    }
}
