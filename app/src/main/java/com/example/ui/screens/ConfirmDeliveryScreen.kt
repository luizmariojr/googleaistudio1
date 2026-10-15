package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryItem
import com.example.data.model.PointDto
import com.example.ui.components.SignaturePad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeliveryScreen(
    delivery: DeliveryItem,
    isOnline: Boolean,
    onBack: () -> Unit,
    onConfirmSuccess: (receiverName: String, receiverDoc: String, relation: String, points: List<PointDto>) -> Unit,
    onReportFailure: (reason: String) -> Unit
) {
    BackHandler { onBack() }

    var receiverName by remember { mutableStateOf(delivery.recipientName) }
    var receiverDoc by remember { mutableStateOf("") }
    var selectedRelation by remember { mutableStateOf("Próprio Destinatário") }
    var signaturePoints by remember { mutableStateOf<List<PointDto>>(emptyList()) }
    var photoTaken by remember { mutableStateOf(false) }
    var showFailureDialog by remember { mutableStateOf(false) }
    var failureReason by remember { mutableStateOf("Destinatário Ausente") }

    val relationsList = listOf(
        "Próprio Destinatário",
        "Porteiro / Zelador",
        "Vizinho",
        "Familiar",
        "Colega de Trabalho"
    )

    val failureReasons = listOf(
        "Destinatário Ausente",
        "Endereço Inexistente / Incompleto",
        "Recusado pelo Destinatário",
        "Condomínio Fechado / Sem Interfone",
        "Problema de Segurança / Tráfego"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Confirmação de Entrega",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = delivery.trackingCode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("confirm_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Package & Destination Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Destinatário Original: ${delivery.recipientName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${delivery.address} ${if (delivery.complement.isNotEmpty()) " - " + delivery.complement else ""}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${delivery.neighborhood}, ${delivery.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (delivery.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nota: ${delivery.notes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Sync Mode Indicator Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnline) {
                        "Comprovante será enviado à nuvem instantaneamente."
                    } else {
                        "Sem conexão: comprovante será salvo na fila offline protegida."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOnline) Color(0xFF1B5E20) else Color(0xFFBF360C)
                )
            }

            // Failure Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dados do Recebedor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (showFailureDialog) "Voltar para Entrega" else "Relatar Insucesso",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable { showFailureDialog = !showFailureDialog }
                        .padding(4.dp)
                )
            }

            if (showFailureDialog) {
                // Failure Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Registrar Insucesso na Entrega",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Selecione o motivo da impossibilidade:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        failureReasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { failureReason = reason }
                                    .background(
                                        if (failureReason == reason) Color(0xFFFFCDD2) else Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (failureReason == reason) FontWeight.Bold else FontWeight.Normal,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onReportFailure(failureReason) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_failure_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("Confirmar Insucesso de Entrega")
                        }
                    }
                }
            } else {
                // Standard Success Form
                OutlinedTextField(
                    value = receiverName,
                    onValueChange = { receiverName = it },
                    label = { Text("Nome Completo de Quem Recebeu") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receiver_name_input"),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = receiverDoc,
                    onValueChange = { receiverDoc = it },
                    label = { Text("Documento de Identidade (RG ou CPF)") },
                    placeholder = { Text("Ex: 12.345.678-9") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receiver_doc_input"),
                    singleLine = true
                )

                // Relationship Chips
                Column {
                    Text(
                        text = "Grau de Parentesco / Vínculo:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        relationsList.take(3).forEach { rel ->
                            SuggestionChip(
                                onClick = { selectedRelation = rel },
                                label = { Text(rel, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        relationsList.drop(3).forEach { rel ->
                            SuggestionChip(
                                onClick = { selectedRelation = rel },
                                label = { Text(rel, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Interactive Signature Pad
                SignaturePad(
                    modifier = Modifier.fillMaxWidth(),
                    onSignatureChanged = { points ->
                        signaturePoints = points
                    }
                )

                // Proof Photo simulation toggle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { photoTaken = !photoTaken }
                        .testTag("photo_proof_container"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (photoTaken) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (photoTaken) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (photoTaken) "Foto do Pacote Registrada" else "Fotografar Pacote Entregue",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (photoTaken) "Anexada com geolocalização e carimbo de tempo" else "Toque para registrar foto de comprovação",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = if (photoTaken) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (photoTaken) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Final Submit Button
                val isFormValid = receiverName.isNotBlank() && receiverDoc.isNotBlank() && signaturePoints.isNotEmpty()

                Button(
                    onClick = {
                        onConfirmSuccess(
                            receiverName,
                            receiverDoc,
                            selectedRelation,
                            signaturePoints
                        )
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("finalize_delivery_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = if (isOnline) "Confirmar & Sincronizar Agora" else "Confirmar & Salvar na Fila Offline",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                if (!isFormValid) {
                    Text(
                        text = "Preencha o nome, documento e colha a assinatura para concluir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
