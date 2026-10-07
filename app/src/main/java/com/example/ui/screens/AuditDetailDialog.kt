package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DeliveryConfirmationEntity
import com.example.data.local.formatarNomeCliente
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandNavy

@Composable
fun AuditDetailScreen(
    confirmation: DeliveryConfirmationEntity?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (confirmation == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Registro não encontrado.")
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("audit_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Auditoria da Confirmação",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Comprovante Digital & Fila Offline",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Principal de Carimbo de Tempo (Timestamp Audit)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auditoria de Datas e Horários",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Data e Hora da Entrega
                    AuditRow(
                        label = "Data/Hora da Entrega (Dispositivo)",
                        value = confirmation.dataHoraEntregaFormatada,
                        highlightColor = BrandEmerald,
                        description = "Momento em que o usuário clicou em confirmar no aparelho. Esta é a data que passa a valer juridicamente e operacionalmente."
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // 2. Data e Hora da Sincronização
                    AuditRow(
                        label = "Data/Hora da Sincronização (Servidor)",
                        value = confirmation.dataHoraSincronizacaoFormatada ?: "Pendente de Sincronização",
                        highlightColor = if (confirmation.dataHoraSincronizacao != null) BrandBlue else BrandAmber,
                        description = "Momento em que o backend recebeu o registro quando a internet voltou. Tratado puramente como dado técnico de auditoria."
                    )
                }
            }

            // Metadados da Confirmação
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Dados do Comprovante Digital",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    MetaItem("Romaneio", "#${confirmation.numeroRomaneio}")
                    if (confirmation.numeroNf != null) {
                        MetaItem("Nota Fiscal", "#${confirmation.numeroNf}")
                    }
                    MetaItem("Recebedor", formatarNomeCliente(confirmation.nomeRecebedor))
                    MetaItem("Documento (RG/CPF)", confirmation.documentoRecebedor)
                    MetaItem("Observação", confirmation.observacao.ifBlank { "Nenhuma observação" })
                    MetaItem(
                        "Geolocalização (GPS)",
                        "${confirmation.latitude ?: -22.9064}, ${confirmation.longitude ?: -47.0616}"
                    )
                    MetaItem("Foto do Canhoto", confirmation.canhotoFotoUri ?: "Armazenado localmente")
                    MetaItem(
                        "Assinatura na Tela",
                        if (confirmation.assinaturaPresente) "Coletada com sucesso" else "Não coletada"
                    )
                    MetaItem("Chave de Idempotência (UUID)", confirmation.id)
                    MetaItem("Tentativas de Envio", "${confirmation.tentativas}")
                }
            }

            // Visualização do Payload JSON que é enviado ao Supabase RPC
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Payload Enviado ao Servidor (RPC)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = """
{
  "p_idempotency_key": "${confirmation.id}",
  "p_romaneio_id": "${confirmation.romaneioId}",
  "p_data_hora_entrega": "${confirmation.dataHoraEntregaFormatada}",
  "p_timestamp_entrega_ms": ${confirmation.dataHoraEntrega},
  "p_recebedor_nome": "${confirmation.nomeRecebedor}",
  "p_recebedor_doc": "${confirmation.documentoRecebedor}",
  "p_latitude": ${confirmation.latitude},
  "p_longitude": ${confirmation.longitude},
  "p_canhoto_path": "${confirmation.canhotoFotoUri}",
  "p_observacao": "${confirmation.observacao}"
}
                        """.trimIndent(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF6EE7B7),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AuditRow(
    label: String,
    value: String,
    highlightColor: Color,
    description: String
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = highlightColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 11.sp,
            color = Color.Gray,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun MetaItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
