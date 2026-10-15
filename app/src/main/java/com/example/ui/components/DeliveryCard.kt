package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryItem
import com.example.data.model.DeliveryStatus
import com.example.data.model.SyncStatus

@Composable
fun DeliveryCard(
    delivery: DeliveryItem,
    onStartTransit: () -> Unit,
    onConfirmDelivery: () -> Unit,
    onViewReceipt: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .testTag("delivery_card_${delivery.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Tracking & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = delivery.trackingCode,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                StatusBadge(status = delivery.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recipient & Package
            Text(
                text = delivery.recipientName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = delivery.packageDescription + " • " + "${delivery.packageWeightKg} kg",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Address
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "${delivery.address} ${if (delivery.complement.isNotEmpty()) "- " + delivery.complement else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${delivery.neighborhood}, ${delivery.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Window
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Janela: ${delivery.scheduledTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Proof Sync Indicator if delivered
            if (delivery.status == DeliveryStatus.DELIVERED && delivery.proof != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SyncProofBadge(syncStatus = delivery.proof.syncStatus)
            }

            if (delivery.status == DeliveryStatus.FAILED && delivery.failureReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Motivo: ${delivery.failureReason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (delivery.status) {
                    DeliveryStatus.PENDING -> {
                        OutlinedButton(
                            onClick = onStartTransit,
                            modifier = Modifier.testTag("start_transit_button_${delivery.id}")
                        ) {
                            Text("Iniciar Rota")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onConfirmDelivery,
                            modifier = Modifier.testTag("confirm_delivery_button_${delivery.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text("Dar Baixa")
                        }
                    }

                    DeliveryStatus.IN_TRANSIT -> {
                        Button(
                            onClick = onConfirmDelivery,
                            modifier = Modifier.testTag("confirm_delivery_button_${delivery.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text("Confirmar Entrega")
                        }
                    }

                    DeliveryStatus.DELIVERED -> {
                        OutlinedButton(
                            onClick = onViewReceipt,
                            modifier = Modifier.testTag("view_receipt_button_${delivery.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text("Ver Comprovante")
                        }
                    }

                    DeliveryStatus.FAILED -> {
                        OutlinedButton(
                            onClick = onConfirmDelivery,
                            modifier = Modifier.testTag("retry_delivery_button_${delivery.id}")
                        ) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DeliveryStatus) {
    val (bgColor, textColor, label) = when (status) {
        DeliveryStatus.PENDING -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendente")
        DeliveryStatus.IN_TRANSIT -> Triple(Color(0xFFE1F5FE), Color(0xFF0277BD), "Em Rota")
        DeliveryStatus.DELIVERED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Entregue")
        DeliveryStatus.FAILED -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Insucesso")
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SyncProofBadge(syncStatus: SyncStatus) {
    val (icon, text, tint) = when (syncStatus) {
        SyncStatus.SYNCED -> Triple(Icons.Default.CloudDone, "Comprovante sincronizado na nuvem", Color(0xFF2E7D32))
        SyncStatus.PENDING_SYNC -> Triple(Icons.Default.CloudUpload, "Comprovante na fila local offline", Color(0xFFE65100))
        SyncStatus.SYNCING -> Triple(Icons.Default.CloudUpload, "Sincronizando com o servidor...", Color(0xFF0277BD))
        SyncStatus.SYNC_ERROR -> Triple(Icons.Default.ErrorOutline, "Falha de envio na rede", Color(0xFFC62828))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(tint.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}
