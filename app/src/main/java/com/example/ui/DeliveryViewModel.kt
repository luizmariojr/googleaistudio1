package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DeliveryConfirmationEntity
import com.example.data.local.NotaFiscalEntity
import com.example.data.local.RomaneioEntity
import com.example.data.remote.SupabaseUserSession
import com.example.data.repository.DeliveryRepository
import com.example.sync.SyncManager
import com.example.sync.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    LOGIN,
    LISTA_ROMANEIOS,
    CONFIRMAR_ENTREGA,
    FILA_SINCRONIZACAO,
    AUDITORIA_DETALHES,
    TROCA_SENHA_TEMPORARIA
}

class DeliveryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = DeliveryRepository(application, database)
    val syncManager = SyncManager(application, repository, viewModelScope)

    val romaneios: StateFlow<List<RomaneioEntity>> = repository.allRomaneios
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConfirmations: StateFlow<List<DeliveryConfirmationEntity>> = repository.allConfirmations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingConfirmations: StateFlow<List<DeliveryConfirmationEntity>> = repository.pendingConfirmations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isOnline: StateFlow<Boolean> = syncManager.isOnline
    val isSimulatedOffline: StateFlow<Boolean> = syncManager.isSimulatedOffline
    val syncState: StateFlow<SyncState> = syncManager.syncState
    val lastSyncTimestamp: StateFlow<Long?> = syncManager.lastSyncTimestamp
    val lastOnlineTimestamp: StateFlow<Long?> = syncManager.lastOnlineTimestamp
    val currentUserSession: StateFlow<SupabaseUserSession?> = repository.currentUserSession

    private val _currentScreen = MutableStateFlow(ScreenDestination.LOGIN)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _loginUsername = MutableStateFlow("")
    val loginUsername: StateFlow<String> = _loginUsername.asStateFlow()

    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    fun onLoginUsernameChanged(username: String) {
        _loginUsername.value = username
        _loginError.value = null
    }

    fun onLoginPasswordChanged(password: String) {
        _loginPassword.value = password
        _loginError.value = null
    }

    fun performLogin() {
        val user = _loginUsername.value.trim()
        val pass = _loginPassword.value.trim()
        if (user.isBlank() || pass.isBlank()) {
            _loginError.value = "Preencha a matrícula/CNPJ e a senha."
            return
        }

        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null
            try {
                if (syncManager.isOnline.value) {
                    // Autenticação online diretamente no Supabase Auth (busca a senha cadastrada no banco Supabase)
                    val result = repository.loginOnly(user, pass)
                    if (result.isSuccess) {
                        val session = result.getOrThrow()
                        if (session.requiresPasswordChange) {
                            // Se a senha for padrão/temporária (ex: Logistica@2026), exige redefinição no Supabase
                            _currentScreen.value = ScreenDestination.TROCA_SENHA_TEMPORARIA
                            _isLoggingIn.value = false
                        } else {
                            _currentScreen.value = ScreenDestination.LISTA_ROMANEIOS
                            _isLoggingIn.value = false

                            // Dispara a sincronização de romaneios em segundo plano de forma assíncrona
                            launch(Dispatchers.IO) {
                                try {
                                    repository.syncRomaneiosFromSession()
                                    syncManager.recordSyncTime()
                                } catch (e: Exception) {
                                    Log.w("DeliveryViewModel", "Sincronização em background falhou", e)
                                }
                            }
                        }
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Falha ao autenticar no Supabase."
                        _loginError.value = msg
                        _isLoggingIn.value = false
                    }
                } else {
                    // Modo offline: valida a senha informada contra a última informação salva do banco offline
                    val isValidOffline = repository.validateOfflinePassword(user, pass)
                    if (isValidOffline) {
                        val isTempPass = pass.equals("Logistica@2026", ignoreCase = true) ||
                            pass.equals("logistica@26", ignoreCase = true) ||
                            pass.equals("logistica2026", ignoreCase = true)

                        if (isTempPass) {
                            repository.setLocalDemoSession(
                                matricula = user.ifBlank { "908" },
                                nome = if (user == "908" || user.isBlank()) "WILIAN SOUSA DA SILVA" else "Motorista $user",
                                pass = pass
                            )
                            _currentScreen.value = ScreenDestination.TROCA_SENHA_TEMPORARIA
                        } else {
                            repository.restoreOfflineSession(user, pass)
                            _currentScreen.value = ScreenDestination.LISTA_ROMANEIOS
                            _offlineBannerMessage.value = "Acesso em modo offline. Confirmações serão salvas localmente."
                        }
                        _isLoggingIn.value = false
                    } else {
                        _loginError.value = "Senha offline incorreta. Informe a última senha cadastrada ou conecte-se à internet para autenticar no Supabase."
                        _isLoggingIn.value = false
                    }
                }
            } catch (e: Exception) {
                _loginError.value = e.message ?: "Erro ao realizar login"
                _isLoggingIn.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearSession()
            repository.clearLocalData()
        }
        _loginPassword.value = ""
        _selectedRomaneio.value = null
        _selectedNfs.value = emptyList()
        _currentScreen.value = ScreenDestination.LOGIN
    }

    private val _isUpdatingPassword = MutableStateFlow(false)
    val isUpdatingPassword: StateFlow<Boolean> = _isUpdatingPassword.asStateFlow()

    private val _updatePasswordError = MutableStateFlow<String?>(null)
    val updatePasswordError: StateFlow<String?> = _updatePasswordError.asStateFlow()

    fun updateTemporaryPassword(newPassword: String) {
        viewModelScope.launch {
            _isUpdatingPassword.value = true
            _updatePasswordError.value = null
            try {
                val res = repository.updatePassword(newPassword)
                if (res.isSuccess) {
                    // Atualiza a senha armazenada localmente no formulário para a nova senha
                    _loginPassword.value = newPassword
                    _isUpdatingPassword.value = false
                    _currentScreen.value = ScreenDestination.LISTA_ROMANEIOS

                    // Inicia sincronização de romaneios em segundo plano
                    launch(Dispatchers.IO) {
                        try {
                            repository.syncRomaneiosFromSession()
                            syncManager.recordSyncTime()
                        } catch (e: Exception) {
                            Log.w("DeliveryViewModel", "Sincronização em background falhou", e)
                        }
                    }
                } else {
                    _updatePasswordError.value = res.exceptionOrNull()?.message ?: "Erro ao atualizar senha."
                    _isUpdatingPassword.value = false
                }
            } catch (e: Exception) {
                _updatePasswordError.value = e.message ?: "Erro ao salvar nova senha."
                _isUpdatingPassword.value = false
            }
        }
    }

    fun cancelPasswordChange() {
        logout()
    }

    private val _selectedRomaneio = MutableStateFlow<RomaneioEntity?>(null)
    val selectedRomaneio: StateFlow<RomaneioEntity?> = _selectedRomaneio.asStateFlow()

    private val _selectedNfs = MutableStateFlow<List<NotaFiscalEntity>>(emptyList())
    val selectedNfs: StateFlow<List<NotaFiscalEntity>> = _selectedNfs.asStateFlow()

    private val _selectedConfirmation = MutableStateFlow<DeliveryConfirmationEntity?>(null)
    val selectedConfirmation: StateFlow<DeliveryConfirmationEntity?> = _selectedConfirmation.asStateFlow()

    private val _offlineBannerMessage = MutableStateFlow<String?>(null)
    val offlineBannerMessage: StateFlow<String?> = _offlineBannerMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialData()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun selectRomaneio(romaneio: RomaneioEntity) {
        _selectedRomaneio.value = romaneio
        viewModelScope.launch {
            repository.getNfsByRomaneio(romaneio.id).collect { nfs ->
                _selectedNfs.value = nfs
            }
        }
        _currentScreen.value = ScreenDestination.CONFIRMAR_ENTREGA
    }

    fun selectConfirmationForAudit(confirmation: DeliveryConfirmationEntity) {
        _selectedConfirmation.value = confirmation
        _currentScreen.value = ScreenDestination.AUDITORIA_DETALHES
    }

    fun dismissBanner() {
        _offlineBannerMessage.value = null
    }

    fun toggleOfflineSimulation() {
        syncManager.toggleOfflineSimulation()
    }

    fun triggerSync() {
        syncManager.triggerSync("Ação manual do motorista")
    }

    fun confirmDelivery(
        tipo: String, // "entrega_romaneio" | "entrega_nf"
        romaneioId: String,
        numeroRomaneio: String,
        nfId: String? = null,
        numeroNf: String? = null,
        nomeRecebedor: String,
        documentoRecebedor: String,
        observacao: String,
        latitude: Double? = -23.55052,
        longitude: Double? = -46.633308,
        canhotoFotoUri: String? = "foto_canhoto_salva.jpg",
        assinaturaPresente: Boolean = false
    ) {
        // Mensagem de confirmação exibida imediatamente:
        _offlineBannerMessage.value = "Entrega confirmada neste dispositivo. Aguardando sincronização."

        viewModelScope.launch {
            val confirmation = repository.registerOfflineConfirmation(
                tipo = tipo,
                romaneioId = romaneioId,
                numeroRomaneio = numeroRomaneio,
                nfId = nfId,
                numeroNf = numeroNf,
                nomeRecebedor = nomeRecebedor,
                documentoRecebedor = documentoRecebedor,
                observacao = observacao,
                latitude = latitude,
                longitude = longitude,
                canhotoFotoUri = canhotoFotoUri,
                assinaturaPresente = assinaturaPresente
            )

            // Se estiver online no momento, tenta sincronizar em background.
            // Se estiver sem conexão, mantém seguro no dispositivo aguardando internet!
            if (syncManager.isOnline.value) {
                syncManager.triggerSync("Confirmação realizada enquanto conectado")
            }

            // Permanece na tela do romaneio que está sendo confirmado
            _currentScreen.value = ScreenDestination.CONFIRMAR_ENTREGA
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshRealData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val res = repository.syncRomaneiosFromSession()
                if (res.isSuccess) {
                    syncManager.recordSyncTime()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
