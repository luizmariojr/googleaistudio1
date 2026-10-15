package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun NetworkBanner(
    isOnline: Boolean,
    isSyncing: Boolean,
    pendingQueueCount: Int,
    onToggleNetwork: (Boolean) -> Unit,
    onTriggerSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("network_banner_surface"),
        color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                                shape = CircleShape
                            )
                    )

                    Icon(
                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = if (isOnline) "Conectado" else "Offline",
                        tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.size(20.dp)
                    )

                    Column {
                        Text(
                            text = if (isOnline) "Modo Conectado (Online)" else "Modo Offline (Sem Sinal)",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isOnline) Color(0xFF1B5E20) else Color(0xFFBF360C)
                        )
                        Text(
                            text = if (isOnline) {
                                if (pendingQueueCount > 0) "$pendingQueueCount comprovante(s) pendentes" else "Dados sincronizados com o servidor"
                            } else {
                                "$pendingQueueCount comprovante(s) na fila local protegida"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF37474F)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = isOnline,
                        onCheckedChange = onToggleNetwork,
                        modifier = Modifier.testTag("network_toggle_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2E7D32),
                            checkedTrackColor = Color(0xFFA5D6A7),
                            uncheckedThumbColor = Color(0xFFE65100),
                            uncheckedTrackColor = Color(0xFFFFCC80)
                        )
                    )
                }
            }

            AnimatedVisibility(visible = pendingQueueCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOnline) "Pronto para descarregar fila no servidor" else "Comprovantes salvos localmente sem perda de dados",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF455A64)
                    )

                    if (isOnline) {
                        TextButton(
                            onClick = onTriggerSync,
                            enabled = !isSyncing,
                            modifier = Modifier.testTag("sync_now_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 4.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Enviando...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 4.dp)
                                )
                                Text("Sincronizar")
                            }
                        }
                    }
                }
            }
        }
    }
}
