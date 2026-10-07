package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DeliveryViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.OfflineBanner
import com.example.ui.components.TopNavBar
import com.example.ui.screens.AuditDetailScreen
import com.example.ui.screens.DeliveryConfirmScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RomaneioListScreen
import com.example.ui.screens.SyncQueueScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DeliveryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DeliveryApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DeliveryApp(viewModel: DeliveryViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSimulatedOffline by viewModel.isSimulatedOffline.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val offlineBannerMessage by viewModel.offlineBannerMessage.collectAsStateWithLifecycle()
    val romaneios by viewModel.romaneios.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedRomaneio by viewModel.selectedRomaneio.collectAsStateWithLifecycle()
    val selectedNfs by viewModel.selectedNfs.collectAsStateWithLifecycle()
    val allConfirmations by viewModel.allConfirmations.collectAsStateWithLifecycle()
    val selectedConfirmation by viewModel.selectedConfirmation.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val lastOnlineTimestamp by viewModel.lastOnlineTimestamp.collectAsStateWithLifecycle()

    val currentUserSession by viewModel.currentUserSession.collectAsStateWithLifecycle()
    val loginUsername by viewModel.loginUsername.collectAsStateWithLifecycle()
    val loginPassword by viewModel.loginPassword.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val isLoggingIn by viewModel.isLoggingIn.collectAsStateWithLifecycle()

    // Se estiver na tela de login, exibe a LoginScreen completa idêntica ao backoffice oficial
    if (currentScreen == ScreenDestination.LOGIN) {
        LoginScreen(
            username = loginUsername,
            password = loginPassword,
            errorMessage = loginError,
            isLoading = isLoggingIn,
            isOnline = isOnline,
            isSimulatedOffline = isSimulatedOffline,
            onUsernameChange = { viewModel.onLoginUsernameChanged(it) },
            onPasswordChange = { viewModel.onLoginPasswordChanged(it) },
            onSubmit = { viewModel.performLogin() },
            onToggleOffline = { viewModel.toggleOfflineSimulation() }
        )
        return
    }

    // BackHandler para retornar para a lista se estiver em sub-telas ou para o login se na lista
    BackHandler {
        when (currentScreen) {
            ScreenDestination.AUDITORIA_DETALHES -> viewModel.navigateTo(ScreenDestination.FILA_SINCRONIZACAO)
            ScreenDestination.FILA_SINCRONIZACAO, ScreenDestination.CONFIRMAR_ENTREGA -> viewModel.navigateTo(ScreenDestination.LISTA_ROMANEIOS)
            ScreenDestination.LISTA_ROMANEIOS -> viewModel.logout()
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        topBar = {
            TopNavBar(
                isOnline = isOnline,
                isSimulatedOffline = isSimulatedOffline,
                pendingCount = pendingCount,
                driverName = currentUserSession?.fullName ?: "WILIAN SOUSA DA SILVA",
                driverCode = "908",
                isRefreshing = isRefreshing,
                onRefreshRealData = { viewModel.refreshRealData() },
                onToggleOffline = { viewModel.toggleOfflineSimulation() },
                onOpenSyncQueue = { viewModel.navigateTo(ScreenDestination.FILA_SINCRONIZACAO) },
                onLogout = { viewModel.logout() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            // Banner de confirmação offline obrigatório
            OfflineBanner(
                message = offlineBannerMessage,
                onDismiss = { viewModel.dismissBanner() },
                onViewQueue = { viewModel.navigateTo(ScreenDestination.FILA_SINCRONIZACAO) }
            )

            when (currentScreen) {
                ScreenDestination.LISTA_ROMANEIOS -> {
                    RomaneioListScreen(
                        romaneios = romaneios,
                        searchQuery = searchQuery,
                        onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                        onSelectRomaneio = { viewModel.selectRomaneio(it) },
                        onRefresh = { viewModel.refreshRealData() },
                        isRefreshing = isRefreshing
                    )
                }

                ScreenDestination.CONFIRMAR_ENTREGA -> {
                    DeliveryConfirmScreen(
                        romaneio = selectedRomaneio,
                        nfs = selectedNfs,
                        isOnline = isOnline,
                        onBack = { viewModel.navigateTo(ScreenDestination.LISTA_ROMANEIOS) },
                        onConfirm = { tipo, romaneioId, numeroRomaneio, nfId, numeroNf, nomeRecebedor, doc, obs, lat, lng, foto, ass ->
                            viewModel.confirmDelivery(
                                tipo = tipo,
                                romaneioId = romaneioId,
                                numeroRomaneio = numeroRomaneio,
                                nfId = nfId,
                                numeroNf = numeroNf,
                                nomeRecebedor = nomeRecebedor,
                                documentoRecebedor = doc,
                                observacao = obs,
                                latitude = lat,
                                longitude = lng,
                                canhotoFotoUri = foto,
                                assinaturaPresente = ass
                            )
                        }
                    )
                }

                ScreenDestination.FILA_SINCRONIZACAO -> {
                    SyncQueueScreen(
                        confirmations = allConfirmations,
                        pendingCount = pendingCount,
                        isOnline = isOnline,
                        syncState = syncState,
                        lastSyncTimestamp = lastSyncTimestamp,
                        lastOnlineTimestamp = lastOnlineTimestamp,
                        onBack = { viewModel.navigateTo(ScreenDestination.LISTA_ROMANEIOS) },
                        onTriggerSync = { viewModel.triggerSync() },
                        onRefreshRealData = { viewModel.refreshRealData() },
                        onSelectForAudit = { viewModel.selectConfirmationForAudit(it) }
                    )
                }

                ScreenDestination.AUDITORIA_DETALHES -> {
                    AuditDetailScreen(
                        confirmation = selectedConfirmation,
                        onBack = { viewModel.navigateTo(ScreenDestination.FILA_SINCRONIZACAO) }
                    )
                }

                ScreenDestination.LOGIN -> {
                    // Já tratado no nível raiz
                }
            }
        }
    }
}
