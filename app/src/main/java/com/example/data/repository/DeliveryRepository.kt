package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.DeliveryConfirmationEntity
import com.example.data.local.NotaFiscalEntity
import com.example.data.local.RomaneioEntity
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseUserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DeliveryRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val confirmationDao = db.deliveryConfirmationDao()
    private val romaneioDao = db.romaneioDao()
    private val nfDao = db.notaFiscalDao()
    private val prefs = context.getSharedPreferences("logistica_app_prefs", Context.MODE_PRIVATE)

    private val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    private val _currentUserSession = MutableStateFlow<SupabaseUserSession?>(null)
    val currentUserSession: StateFlow<SupabaseUserSession?> = _currentUserSession.asStateFlow()

    var lastUsedUsername: String? = null
    var lastUsedPassword: String? = null

    init {
        // Carrega sessão salva previamente se existir
        val savedUserId = prefs.getString("user_id", null)
        val savedEmail = prefs.getString("user_email", null)
        val savedName = prefs.getString("user_name", null)
        val savedToken = prefs.getString("user_token", null)
        lastUsedUsername = prefs.getString("last_username", null)
        lastUsedPassword = prefs.getString("last_password", null)

        if (!savedUserId.isNullOrBlank() && !savedToken.isNullOrBlank()) {
            _currentUserSession.value = SupabaseUserSession(
                userId = savedUserId,
                email = savedEmail ?: "",
                fullName = savedName ?: "Motorista",
                accessToken = savedToken,
                requiresPasswordChange = false
            )
        }
    }

    private fun persistSession(session: SupabaseUserSession?, username: String?, pass: String?) {
        prefs.edit().apply {
            if (session != null) {
                putString("user_id", session.userId)
                putString("user_email", session.email)
                putString("user_name", session.fullName)
                putString("user_token", session.accessToken)
            } else {
                remove("user_id")
                remove("user_email")
                remove("user_name")
                remove("user_token")
            }
            if (!username.isNullOrBlank()) putString("last_username", username)
            if (!pass.isNullOrBlank()) putString("last_password", pass)
            apply()
        }
    }

    val allRomaneios: Flow<List<RomaneioEntity>> = romaneioDao.getAllRomaneios()
    val allConfirmations: Flow<List<DeliveryConfirmationEntity>> = confirmationDao.getAllConfirmations()
    val pendingConfirmations: Flow<List<DeliveryConfirmationEntity>> = confirmationDao.getPendingConfirmations()
    val pendingCount: Flow<Int> = confirmationDao.getPendingCount()

    fun getRomaneioById(id: String): Flow<RomaneioEntity?> = romaneioDao.getRomaneioById(id)
    fun getNfsByRomaneio(romaneioId: String): Flow<List<NotaFiscalEntity>> = nfDao.getNfsByRomaneio(romaneioId)

    suspend fun hasLocalData(): Boolean = romaneioDao.getCount() > 0

    fun clearSession() {
        _currentUserSession.value = null
        persistSession(null, null, null)
    }

    suspend fun clearLocalData() {
        romaneioDao.clearAll()
        nfDao.clearAll()
    }

    fun validateOfflinePassword(username: String, pass: String): Boolean {
        val savedUser = prefs.getString("last_username", null)
        val savedPass = prefs.getString("last_password", null)

        val cleanUser = username.trim().lowercase()
        val cleanSavedUser = savedUser?.trim()?.lowercase() ?: ""

        // 1. Se existir senha salva previamente no dispositivo para este usuário, valida estritamente contra ela
        if (!savedPass.isNullOrBlank()) {
            if (cleanSavedUser.isNotBlank() && (cleanSavedUser == cleanUser || cleanSavedUser.contains(cleanUser) || cleanUser.contains(cleanSavedUser))) {
                return savedPass == pass.trim()
            }
            return savedPass == pass.trim()
        }

        // 2. Se for primeiro acesso sem senha salva anterior, permite a senha temporária padrão do sistema
        val isTempPass = pass.trim().equals("Logistica@2026", ignoreCase = true) ||
            pass.trim().equals("logistica@26", ignoreCase = true) ||
            pass.trim().equals("logistica2026", ignoreCase = true)

        return isTempPass
    }

    suspend fun restoreOfflineSession(user: String, pass: String) {
        lastUsedUsername = user
        lastUsedPassword = pass
        val savedUserId = prefs.getString("user_id", "offline-$user") ?: "offline-$user"
        val savedEmail = prefs.getString("user_email", "$user@motorista.logistica.local") ?: "$user@motorista.logistica.local"
        val savedName = prefs.getString("user_name", "Motorista $user") ?: "Motorista $user"
        val savedToken = prefs.getString("user_token", SupabaseClient.SUPABASE_ANON_KEY) ?: SupabaseClient.SUPABASE_ANON_KEY

        val session = SupabaseUserSession(
            userId = savedUserId,
            email = savedEmail,
            fullName = savedName,
            accessToken = savedToken,
            requiresPasswordChange = false
        )
        _currentUserSession.value = session
        persistSession(session, user, pass)
    }

    suspend fun setLocalDemoSession(matricula: String = "908", nome: String = "WILIAN SOUSA DA SILVA", pass: String? = null) {
        lastUsedUsername = matricula
        if (!pass.isNullOrBlank()) lastUsedPassword = pass
        val isTempPass = pass?.trim()?.equals("Logistica@2026", ignoreCase = true) == true ||
            pass?.trim()?.equals("logistica@26", ignoreCase = true) == true ||
            pass?.trim()?.equals("logistica2026", ignoreCase = true) == true

        val session = SupabaseUserSession(
            userId = "demo-$matricula",
            email = "$matricula@motorista.logistica.local",
            fullName = nome,
            accessToken = SupabaseClient.SUPABASE_ANON_KEY,
            requiresPasswordChange = isTempPass
        )
        _currentUserSession.value = session
        persistSession(session, matricula, pass)
        romaneioDao.clearAll()
        nfDao.clearAll()
        seedOfflineFallbackData(nome)
    }

    suspend fun updateRomaneiosDriverName(driverName: String) {
        if (driverName.isNotBlank()) {
            romaneioDao.updateAllMotoristaNome(driverName)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        val session = _currentUserSession.value
            ?: return Result.failure(Exception("Nenhuma sessão ativa encontrada."))
        val result = SupabaseClient.updateUserPassword(session.accessToken, newPassword, session.email)
        if (result.isSuccess) {
            lastUsedPassword = newPassword
            // Faz login imediatamente com a nova senha para obter token JWT atualizado
            val user = lastUsedUsername ?: session.email
            val relogin = SupabaseClient.login(user, newPassword)
            if (relogin.isSuccess) {
                val fresh = relogin.getOrThrow().copy(requiresPasswordChange = false)
                _currentUserSession.value = fresh
                persistSession(fresh, user, newPassword)
            } else {
                _currentUserSession.value = session.copy(requiresPasswordChange = false)
                persistSession(_currentUserSession.value, user, newPassword)
            }
        }
        return result
    }

    /**
     * Autentica o usuário rapidamente sem bloquear o download dos romaneios.
     */
    suspend fun loginOnly(
        user: String,
        pass: String
    ): Result<SupabaseUserSession> {
        lastUsedUsername = user
        lastUsedPassword = pass
        val loginResult = SupabaseClient.login(user, pass)
        if (loginResult.isSuccess) {
            val session = loginResult.getOrThrow()
            _currentUserSession.value = session
            persistSession(session, user, pass)
        }
        return loginResult
    }

    /**
     * Busca os romaneios do usuário autenticado e atualiza o banco local Room.
     * Limpa o cache anterior para garantir que romaneios de outros motoristas não apareçam.
     */
    suspend fun syncRomaneiosFromSession(): Result<Int> {
        val session = _currentUserSession.value ?: return Result.failure(Exception("Sessão não iniciada"))
        val romaneiosResult = SupabaseClient.fetchRealRomaneios(session.accessToken)
        if (romaneiosResult.isFailure) {
            ensureInitialData()
            return Result.failure(romaneiosResult.exceptionOrNull() ?: Exception("Falha ao buscar romaneios"))
        }

        val remoteRomaneios = romaneiosResult.getOrThrow()
        val romEntities = mutableListOf<RomaneioEntity>()
        val nfEntities = mutableListOf<NotaFiscalEntity>()

        for (r in remoteRomaneios) {
            romEntities.add(
                RomaneioEntity(
                    id = r.id,
                    numeroRomaneio = r.numeroRomaneio,
                    status = r.status,
                    clientePrincipal = r.clientePrincipal,
                    cidade = r.cidade,
                    uf = r.uf,
                    quantidadeNfs = r.quantidadeNfs,
                    quantidadeVolumes = r.quantidadeVolumes,
                    pesoTotalKg = r.pesoTotalKg,
                    motoristaNome = r.motoristaNome.ifBlank { session.fullName },
                    createdAt = r.createdAt,
                    prazoEntrega = r.prazoEntrega ?: "Hoje"
                )
            )

            for (nf in r.notasFiscais) {
                nfEntities.add(
                    NotaFiscalEntity(
                        id = nf.id,
                        romaneioId = nf.romaneioId,
                        numeroNf = nf.numeroNf,
                        destinatario = nf.destinatario,
                        cidade = nf.cidade,
                        uf = nf.uf,
                        volumes = nf.volumes,
                        pesoKg = nf.pesoKg,
                        valor = nf.valor,
                        status = nf.status
                    )
                )
            }
        }

        if (romEntities.isNotEmpty()) {
            // Atualiza o banco local Room com os dados do servidor Supabase
            romaneioDao.clearAll()
            nfDao.clearAll()
            romaneioDao.insertRomaneios(romEntities)
            if (nfEntities.isNotEmpty()) {
                nfDao.insertNfs(nfEntities)
            }
        } else {
            // Se o servidor retornou 0 romaneios novos e o Room está vazio, povoa o banco local
            ensureInitialData()
        }

        Log.i("DeliveryRepository", "Sincronização concluída: ${romaneioDao.getCount()} romaneios no banco Room para ${session.fullName}.")
        return Result.success(romaneioDao.getCount())
    }

    /**
     * Sincroniza diretamente utilizando a sessão atual ativa.
     */
    suspend fun syncWithRealSupabase(
        user: String? = null,
        pass: String? = null
    ): Result<Int> {
        try {
            if (user != null && pass != null) {
                val loginResult = loginOnly(user, pass)
                if (loginResult.isFailure) {
                    return Result.failure(loginResult.exceptionOrNull() ?: Exception("Falha de autenticação"))
                }
            }
            return syncRomaneiosFromSession()
        } catch (e: Exception) {
            Log.e("DeliveryRepository", "Erro na sincronização", e)
            return Result.failure(e)
        }
    }

    suspend fun ensureInitialData() {
        if (romaneioDao.getCount() == 0) {
            val driverName = _currentUserSession.value?.fullName ?: "WILIAN SOUSA DA SILVA"
            seedOfflineFallbackData(driverName)
        }
    }

    private suspend fun seedOfflineFallbackData(driverName: String = _currentUserSession.value?.fullName ?: "Motorista") {
        val initialRomaneios = listOf(
            RomaneioEntity(
                id = "rom-2359",
                numeroRomaneio = "2359",
                status = "Criado",
                clientePrincipal = "ANTONIO MIGUEL DA CONCEICAO SILVA + 4",
                cidade = "IMPERATRIZ",
                uf = "MA",
                quantidadeNfs = 5,
                quantidadeVolumes = 9,
                pesoTotalKg = 18.4,
                motoristaNome = driverName,
                createdAt = "2026-10-06T10:04:07"
            ),
            RomaneioEntity(
                id = "rom-2342",
                numeroRomaneio = "2342",
                status = "Criado",
                clientePrincipal = "M DE J A DE SA NERES",
                cidade = "IMPERATRIZ",
                uf = "MA",
                quantidadeNfs = 1,
                quantidadeVolumes = 4,
                pesoTotalKg = 8.9,
                motoristaNome = driverName,
                createdAt = "2026-10-05T16:17:01"
            ),
            RomaneioEntity(
                id = "rom-2340",
                numeroRomaneio = "2340",
                status = "Criado",
                clientePrincipal = "ELONI FERREIRA CANDIDO 90600789349 + 1",
                cidade = "JOAO LISBOA",
                uf = "MA",
                quantidadeNfs = 2,
                quantidadeVolumes = 2,
                pesoTotalKg = 4.2,
                motoristaNome = driverName,
                createdAt = "2026-10-05T14:30:05"
            ),
            RomaneioEntity(
                id = "rom-2314",
                numeroRomaneio = "2314",
                status = "Criado",
                clientePrincipal = "ANTONIO DE SOUSA NUNES 88807703149",
                cidade = "IMPERATRIZ",
                uf = "MA",
                quantidadeNfs = 2,
                quantidadeVolumes = 2,
                pesoTotalKg = 5.76,
                motoristaNome = driverName,
                createdAt = "2026-10-06T08:00:00"
            ),
            RomaneioEntity(
                id = "rom-2339",
                numeroRomaneio = "2339",
                status = "Criado",
                clientePrincipal = "FRANCISCO PEREIRA DOS SANTOS",
                cidade = "IMPERATRIZ",
                uf = "MA",
                quantidadeNfs = 3,
                quantidadeVolumes = 6,
                pesoTotalKg = 12.5,
                motoristaNome = driverName,
                createdAt = "2026-10-05T13:50:21"
            )
        )
        romaneioDao.insertRomaneios(initialRomaneios)

        val initialNfs = listOf(
            NotaFiscalEntity(
                id = "nf-115317",
                romaneioId = "rom-2314",
                numeroNf = "115317",
                destinatario = "ANTONIO DE SOUSA NUNES 88807703149",
                cidade = "IMPERATRIZ",
                uf = "MA",
                volumes = 1,
                pesoKg = 2.26,
                valor = 1450.00,
                status = "Pendente"
            ),
            NotaFiscalEntity(
                id = "nf-35695",
                romaneioId = "rom-2314",
                numeroNf = "35695",
                destinatario = "ANTONIO DE SOUSA NUNES 88807703149",
                cidade = "IMPERATRIZ",
                uf = "MA",
                volumes = 1,
                pesoKg = 3.50,
                valor = 2190.00,
                status = "Pendente"
            )
        )
        nfDao.insertNfs(initialNfs)
    }

    suspend fun registerOfflineConfirmation(
        tipo: String,
        romaneioId: String,
        numeroRomaneio: String,
        nfId: String? = null,
        numeroNf: String? = null,
        nomeRecebedor: String,
        documentoRecebedor: String,
        observacao: String,
        latitude: Double?,
        longitude: Double?,
        canhotoFotoUri: String?,
        assinaturaPresente: Boolean
    ): DeliveryConfirmationEntity {
        val clickTimestamp = System.currentTimeMillis()
        val formatada = fullDateFormat.format(Date(clickTimestamp))
        val confirmationId = UUID.randomUUID().toString()

        val entity = DeliveryConfirmationEntity(
            id = confirmationId,
            tipo = tipo,
            romaneioId = romaneioId,
            numeroRomaneio = numeroRomaneio,
            nfId = nfId,
            numeroNf = numeroNf,
            dataHoraEntrega = clickTimestamp,
            dataHoraEntregaFormatada = formatada,
            dataHoraSincronizacao = null,
            dataHoraSincronizacaoFormatada = null,
            status = "PENDENTE",
            tentativas = 0,
            ultimoErro = null,
            proximaTentativaEm = 0L,
            nomeRecebedor = nomeRecebedor.ifBlank { "Recebedor no local" },
            documentoRecebedor = documentoRecebedor.ifBlank { "Doc. não informado" },
            observacao = observacao,
            latitude = latitude ?: -5.5266, // Imperatriz/MA
            longitude = longitude ?: -47.4789,
            canhotoFotoUri = canhotoFotoUri ?: "canhoto_${System.currentTimeMillis()}.jpg",
            assinaturaPresente = assinaturaPresente,
            userId = _currentUserSession.value?.userId ?: _currentUserSession.value?.email ?: "motorista-local"
        )

        confirmationDao.insertConfirmation(entity)

        if (tipo == "entrega_romaneio") {
            romaneioDao.updateStatus(
                id = romaneioId,
                status = "Aguardando Sincronização",
                entregueEm = clickTimestamp,
                entregueEmFormatado = formatada
            )
            nfDao.updateAllNfsForRomaneio(
                romaneioId = romaneioId,
                status = "Aguardando Sincronização",
                entregueEm = clickTimestamp,
                entregueEmFormatado = formatada
            )
        } else if (tipo == "entrega_nf" && nfId != null) {
            nfDao.updateNfStatus(
                id = nfId,
                status = "Aguardando Sincronização",
                entregueEm = clickTimestamp,
                entregueEmFormatado = formatada,
                canhotoUri = canhotoFotoUri
            )
            val allNfs = nfDao.getNfsListByRomaneio(romaneioId)
            val allDelivered = allNfs.isNotEmpty() && allNfs.all { it.status == "Entregue" || it.status == "Aguardando Sincronização" }
            if (allDelivered) {
                romaneioDao.updateStatus(
                    id = romaneioId,
                    status = "Aguardando Sincronização",
                    entregueEm = clickTimestamp,
                    entregueEmFormatado = formatada
                )
            }
        }

        return entity
    }

    suspend fun markAsSynchronized(
        id: String,
        syncTimestamp: Long,
        romaneioId: String,
        tipo: String,
        nfId: String?,
        originalDeliveryTimestamp: Long
    ) {
        val syncFormatada = fullDateFormat.format(Date(syncTimestamp))
        confirmationDao.markSynchronized(id, syncTimestamp, syncFormatada)

        val deliveryFormatada = fullDateFormat.format(Date(originalDeliveryTimestamp))
        if (tipo == "entrega_romaneio") {
            romaneioDao.updateStatus(romaneioId, "Entregue", originalDeliveryTimestamp, deliveryFormatada)
            nfDao.updateAllNfsForRomaneio(romaneioId, "Entregue", originalDeliveryTimestamp, deliveryFormatada)
        } else if (tipo == "entrega_nf" && nfId != null) {
            nfDao.updateNfStatus(nfId, "Entregue", originalDeliveryTimestamp, deliveryFormatada, null)
        }
    }

    suspend fun syncSingleConfirmationWithServer(item: DeliveryConfirmationEntity): Boolean {
        return try {
            var session = _currentUserSession.value
            var token = session?.accessToken

            // Se o token for nulo, em branco ou for a anon key (ex: acesso sem token ou reinício do app),
            // tenta obter um token válido via login com as credenciais salvas
            if (token.isNullOrBlank() || token == SupabaseClient.SUPABASE_ANON_KEY) {
                val u = lastUsedUsername ?: prefs.getString("last_username", null)
                val p = lastUsedPassword ?: prefs.getString("last_password", null)
                if (!u.isNullOrBlank() && !p.isNullOrBlank()) {
                    val loginRes = SupabaseClient.login(u, p)
                    if (loginRes.isSuccess) {
                        session = loginRes.getOrThrow()
                        _currentUserSession.value = session
                        persistSession(session, u, p)
                        token = session.accessToken
                    }
                }
            }

            var activeToken = (token ?: "").ifBlank { SupabaseClient.SUPABASE_ANON_KEY }

            if (activeToken.isBlank() || activeToken == SupabaseClient.SUPABASE_ANON_KEY) {
                val u = lastUsedUsername ?: prefs.getString("last_username", null)
                val p = lastUsedPassword ?: prefs.getString("last_password", null)
                if (!u.isNullOrBlank() && !p.isNullOrBlank()) {
                    val loginRes = SupabaseClient.login(u, p)
                    if (loginRes.isSuccess) {
                        session = loginRes.getOrThrow()
                        _currentUserSession.value = session
                        persistSession(session, u, p)
                        activeToken = session.accessToken.ifBlank { SupabaseClient.SUPABASE_ANON_KEY }
                    }
                }
            }

            val isoDate = isoDateFormat.format(Date(item.dataHoraEntrega))
            var rpcResult = SupabaseClient.callRpcConfirmarEntrega(
                token = activeToken,
                tipo = item.tipo,
                romaneioId = item.romaneioId,
                nfId = item.nfId,
                canhotoPath = item.canhotoFotoUri,
                dataHoraEntregaIso = isoDate
            )

                // Se falhou por sessão expirada ou não autenticado, renova a autenticação e tenta mais uma vez
                if (rpcResult.isFailure) {
                    val err = rpcResult.exceptionOrNull()?.message ?: ""
                    if (err.contains("não autenticado", ignoreCase = true) || err.contains("401", ignoreCase = true)) {
                        val u = lastUsedUsername ?: prefs.getString("last_username", null)
                        val p = lastUsedPassword ?: prefs.getString("last_password", null)
                        if (!u.isNullOrBlank() && !p.isNullOrBlank()) {
                            val relogin = SupabaseClient.login(u, p)
                            if (relogin.isSuccess) {
                                val fresh = relogin.getOrThrow()
                                _currentUserSession.value = fresh
                                persistSession(fresh, u, p)
                                rpcResult = SupabaseClient.callRpcConfirmarEntrega(
                                    token = fresh.accessToken,
                                    tipo = item.tipo,
                                    romaneioId = item.romaneioId,
                                    nfId = item.nfId,
                                    canhotoPath = item.canhotoFotoUri,
                                    dataHoraEntregaIso = isoDate
                                )
                            }
                        }
                    }
                }

            if (rpcResult.isSuccess) {
                Log.i("DeliveryRepository", "Confirmação da NF ${item.numeroNf ?: item.numeroRomaneio} sincronizada com sucesso no Supabase!")
                true
            } else {
                Log.w("DeliveryRepository", "Falha RPC ao sincronizar: ${rpcResult.exceptionOrNull()?.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("DeliveryRepository", "Erro ao chamar RPC do Supabase", e)
            false
        }
    }

    suspend fun updateSyncError(id: String, erro: String) {
        confirmationDao.updateError(
            id = id,
            status = "ERRO",
            erro = erro,
            proximaTentativa = System.currentTimeMillis() + 15000L
        )
    }
}
