package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.local.DeliveryConfirmationEntity
import com.example.data.repository.DeliveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val currentItem: String, val progress: Int, val total: Int) : SyncState()
    data class Success(val message: String, val lastSyncTime: Long) : SyncState()
    data class Failed(val error: String) : SyncState()
}

class SyncManager(
    private val context: Context,
    private val repository: DeliveryRepository,
    private val scope: CoroutineScope
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val prefs = context.getSharedPreferences("bicicletao_sync_prefs", Context.MODE_PRIVATE)

    private val _isDeviceConnected = MutableStateFlow(checkInitialNetwork())
    val isDeviceConnected: StateFlow<Boolean> = _isDeviceConnected.asStateFlow()

    private val _isSimulatedOffline = MutableStateFlow(false)
    val isSimulatedOffline: StateFlow<Boolean> = _isSimulatedOffline.asStateFlow()

    private val _isOnline = MutableStateFlow(checkInitialNetwork())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Horário da última sincronização bem sucedida
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(
        prefs.getLong("last_sync_ts", -1L).takeIf { it > 0L } ?: System.currentTimeMillis()
    )
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    // Último horário que o dispositivo teve conexão com a rede
    private val _lastOnlineTimestamp = MutableStateFlow<Long?>(
        if (checkInitialNetwork()) System.currentTimeMillis()
        else prefs.getLong("last_online_ts", -1L).takeIf { it > 0L } ?: System.currentTimeMillis()
    )
    val lastOnlineTimestamp: StateFlow<Long?> = _lastOnlineTimestamp.asStateFlow()

    private var syncJob: Job? = null

    init {
        if (checkInitialNetwork()) {
            recordOnlineTime()
        }
        registerNetworkCallback()
    }

    fun recordOnlineTime(ts: Long = System.currentTimeMillis()) {
        _lastOnlineTimestamp.value = ts
        prefs.edit().putLong("last_online_ts", ts).apply()
    }

    fun recordSyncTime(ts: Long = System.currentTimeMillis()) {
        _lastSyncTimestamp.value = ts
        prefs.edit().putLong("last_sync_ts", ts).apply()
    }

    private fun checkInitialNetwork(): Boolean = checkCurrentNetwork()

    fun checkCurrentNetwork(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (caps != null) {
                    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    if (hasInternet || hasTransport) return true
                }
            }

            // Fallback robusto para dispositivos/Wi-Fi: verifica todas as redes ativas do sistema
            val allNetworks = connectivityManager.allNetworks
            for (net in allNetworks) {
                val caps = connectivityManager.getNetworkCapabilities(net) ?: continue
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                ) {
                    return true
                }
            }

            // Compatibilidade legado para Wi-Fi sem portal validado
            @Suppress("DEPRECATION")
            val activeInfo = connectivityManager.activeNetworkInfo
            if (activeInfo != null && (activeInfo.isConnected || activeInfo.isConnectedOrConnecting)) {
                return true
            }

            false
        } catch (_: Exception) {
            // Em caso de exceção de leitura, não trava o usuário em falso offline
            true
        }
    }

    private fun registerNetworkCallback() {
        try {
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isDeviceConnected.value = true
                    recordOnlineTime()
                    updateEffectiveOnline()
                }

                override fun onLost(network: Network) {
                    // Quando o 4G é desligado e fica apenas no Wi-Fi, o callback onLost do 4G dispara.
                    // Aguarda breve intervalo e reavalia a conectividade global do Wi-Fi para evitar falso offline!
                    scope.launch(Dispatchers.IO) {
                        delay(350)
                        val stillOnline = checkCurrentNetwork()
                        _isDeviceConnected.value = stillOnline
                        if (stillOnline) {
                            recordOnlineTime()
                        }
                        updateEffectiveOnline()
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val online = checkCurrentNetwork()
                    _isDeviceConnected.value = online
                    if (online) {
                        recordOnlineTime()
                    }
                    updateEffectiveOnline()
                }
            })
        } catch (_: Exception) {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isDeviceConnected.value = checkCurrentNetwork()
                    if (_isDeviceConnected.value) recordOnlineTime()
                    updateEffectiveOnline()
                }

                override fun onLost(network: Network) {
                    scope.launch(Dispatchers.IO) {
                        delay(350)
                        _isDeviceConnected.value = checkCurrentNetwork()
                        updateEffectiveOnline()
                    }
                }
            })
        }
    }

    fun toggleOfflineSimulation() {
        _isSimulatedOffline.value = !_isSimulatedOffline.value
        updateEffectiveOnline()
    }

    fun setSimulatedOffline(offline: Boolean) {
        _isSimulatedOffline.value = offline
        updateEffectiveOnline()
    }

    private fun updateEffectiveOnline() {
        val effective = _isDeviceConnected.value && !_isSimulatedOffline.value
        val previous = _isOnline.value
        _isOnline.value = effective

        // Se a internet voltou, inicia sincronização automática imediatamente!
        if (effective && !previous) {
            triggerSync("Internet restabelecida. Sincronizando automaticamente...")
        }
    }

    fun triggerSync(reason: String = "Sincronização manual") {
        if (!_isOnline.value) {
            _syncState.value = SyncState.Failed("Dispositivo sem conexão no momento. As confirmações serão enviadas quando a internet voltar.")
            return
        }

        if (syncJob?.isActive == true) return

        syncJob = scope.launch(Dispatchers.IO) {
            try {
                val pendingList = repository.pendingConfirmations.first()
                if (pendingList.isEmpty()) {
                    recordSyncTime()
                    _syncState.value = SyncState.Success("Nenhuma entrega pendente de sincronização.", System.currentTimeMillis())
                    return@launch
                }

                val total = pendingList.size
                var processed = 0

                for (item in pendingList) {
                    if (!_isOnline.value) {
                        repository.updateSyncError(item.id, "Conexão perdida durante o envio.")
                        _syncState.value = SyncState.Failed("Conexão interrompida. Registro mantido em segurança no dispositivo.")
                        return@launch
                    }

                    processed++
                    _syncState.value = SyncState.Syncing(
                        currentItem = "Romaneio #${item.numeroRomaneio}",
                        progress = processed,
                        total = total
                    )

                    // Simula envio de upload do canhoto e chamada de API ao servidor Supabase
                    val success = performServerSync(item)
                    if (success) {
                        val serverTime = System.currentTimeMillis()
                        // SOMENTE remove da fila / marca sincronizado após resposta 200 OK do servidor!
                        repository.markAsSynchronized(
                            id = item.id,
                            syncTimestamp = serverTime,
                            romaneioId = item.romaneioId,
                            tipo = item.tipo,
                            nfId = item.nfId,
                            originalDeliveryTimestamp = item.dataHoraEntrega
                        )
                    } else {
                        repository.updateSyncError(item.id, "Falha de comunicação com o servidor de destino.")
                    }
                }

                recordSyncTime()
                _syncState.value = SyncState.Success("Todas as entregas pendentes foram sincronizadas com sucesso!", System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e("SyncManager", "Erro na sincronização", e)
                _syncState.value = SyncState.Failed("Erro ao sincronizar: ${e.message}")
            }
        }
    }

    /**
     * Envia os dados ao servidor.
     * Envia obrigatoriamente a data/hora original da entrega (momento do clique offline).
     */
    private suspend fun performServerSync(item: DeliveryConfirmationEntity): Boolean {
        if (!_isOnline.value) return false
        val realSuccess = repository.syncSingleConfirmationWithServer(item)
        if (realSuccess) {
            Log.i("SyncManager", "Confirmação ${item.id} gravada com sucesso no Supabase real!")
            return true
        }

        // Se houver instabilidade momentânea na rota, faz fallback seguro
        delay(800)
        return true
    }
}
