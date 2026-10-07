package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DeliveryConfirmationEntity
import com.example.sync.SyncState
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkHeader
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceCardBorder
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusAmberBorder
import com.example.ui.theme.StatusAmberText
import com.example.ui.theme.StatusEmeraldBg
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
    lastSyncTimestamp: Long? = null,
    lastOnlineTimestamp: Long? = null,
    onBack: () -> Unit,
    onTriggerSync: () -> Unit,
    onRefreshRealData: () -> Unit = {},
    onSelectForAudit: (DeliveryConfirmationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSyncing = syncState is SyncState.Syncing

    val dateFormat = remember {
        SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale("pt", "BR"))
    }
    val formattedLastSync = remember(lastSyncTimestamp) {
        if (lastSyncTimestamp != null && lastSyncTimestamp > 0L) {
            dateFormat.format(Date(lastSyncTimestamp))
        } else {
            "Sincronização inicial pendente"
        }
    }
    val formattedLastOnline = remember(lastOnlineTimestamp, isOnline) {
        if (isOnline) {
            "Conectado agora à rede (Online)"
        } else if (lastOnlineTimestamp != null && lastOnlineTimestamp > 0L) {
            dateFormat.format(Date(lastOnlineTimestamp))
        } else {
            "Sem conexão registrada"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Barra superior
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkHeader,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("queue_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Fila de Sincronização",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isOnline) "Rede conectada ao servidor" else "Offline (guardando no aparelho)",
                            fontSize = 11.sp,
                            color = if (isOnline) StatusEmeraldText else StatusAmberText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onTriggerSync,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("manual_sync_button"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        disabledContainerColor = Color(0xFF1E2C4F)
                    )
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isSyncing) "Sincronizando..." else "Sincronizar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Card de Conectividade e Horários Solicitados
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isOnline) BrandBlue.copy(alpha = 0.4f) else StatusAmberBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ESTADO DA CONEXÃO & SINCRONISMO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isOnline) StatusEmeraldBg else StatusAmberBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isOnline) StatusEmeraldText.copy(alpha = 0.4f) else StatusAmberBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                        contentDescription = null,
                                        tint = if (isOnline) StatusEmeraldText else StatusAmberText,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isOnline) "Online" else "Offline",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnline) StatusEmeraldText else StatusAmberText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Horário da última sincronização
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Horário da última sincronização:",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = formattedLastSync,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Último horário que teve rede
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isOnline) StatusEmeraldBg else Color(0xFF7F1D1D).copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isOnline) StatusEmeraldText else Color(0xFFFCA5A5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Último horário que teve rede:",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = formattedLastOnline,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) StatusEmeraldText else Color(0xFFFCA5A5)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Botão para atualizar lista completa de romaneios do servidor
                        OutlinedButton(
                            onClick = onRefreshRealData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recarregar todos os romaneios da nuvem",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resumo de contadores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Aguardando Envio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$pendingCount",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (pendingCount > 0) StatusAmberText else TextMuted
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Sincronizados",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${confirmations.count { it.status == "SINCRONIZADO" }}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = StatusEmeraldText
                            )
                        }
                    }
                }
            }

            // Banner explicativo do protocolo de segurança
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F1A30),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Garantia de Não Perda de Dados",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Uma confirmação offline JAMAIS é apagada da memória do aparelho antes que o servidor de backend confirme a gravação. Se houver falha de sinal no trajeto, ela permanece segura no banco local para reenvio automático.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Registros de Confirmação no Aparelho",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }

            if (confirmations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma confirmação registrada ainda.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(confirmations, key = { it.id }) { item ->
                    SyncQueueItemCard(
                        item = item,
                        onClick = { onSelectForAudit(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SyncQueueItemCard(
    item: DeliveryConfirmationEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSync = item.status == "SINCRONIZADO"
    val isSending = item.status == "SINCRONIZANDO"
    val isError = item.status == "ERRO"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("queue_item_${item.numeroRomaneio}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Linha do Topo: Romaneio e Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ROMANEIO #${item.numeroRomaneio}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Recebedor: ${item.nomeRecebedor}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isSync -> StatusEmeraldBg
                        isSending -> BrandBlue.copy(alpha = 0.2f)
                        isError -> Color(0xFF450A0A)
                        else -> StatusAmberBg
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isSync -> StatusEmeraldText.copy(alpha = 0.4f)
                            isSending -> BrandBlue.copy(alpha = 0.4f)
                            isError -> Color(0xFFDC2626).copy(alpha = 0.4f)
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
                                isSync -> Icons.Default.CloudDone
                                isSending -> Icons.Default.CloudSync
                                isError -> Icons.Default.ErrorOutline
                                else -> Icons.Default.HourglassEmpty
                            },
                            contentDescription = null,
                            tint = when {
                                isSync -> StatusEmeraldText
                                isSending -> BrandBlue
                                isError -> Color(0xFFF87171)
                                else -> StatusAmberText
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isSync -> "Sincronizado"
                                isSending -> "Enviando..."
                                isError -> "Erro de Envio"
                                else -> "Pendente na Fila"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSync -> StatusEmeraldText
                                isSending -> BrandBlue
                                isError -> Color(0xFFF87171)
                                else -> StatusAmberText
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // REQUISITO CRÍTICO: Tabela visual destacando os dois conceitos separados
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0A0F1D),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2C4F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Conceito 1: Data/Hora da Entrega
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusEmeraldText)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Data/Hora da Entrega:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = item.dataHoraEntregaFormatada,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = StatusEmeraldText
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E2C4F))

                    // Conceito 2: Data/Hora da Sincronização
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSync) BrandBlue else StatusAmberText)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Data/Hora de Sincronização:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = item.dataHoraSincronizacaoFormatada ?: "Aguardando envio ao servidor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSync) BrandBlue else StatusAmberText
                        )
                    }
                }
            }

            if (item.ultimoErro != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aviso: ${item.ultimoErro}",
                    fontSize = 11.sp,
                    color = Color(0xFFF87171),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${item.id.take(8)}... (UUID Único)",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "Toque para ver auditoria >",
                    fontSize = 11.sp,
                    color = BrandBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
