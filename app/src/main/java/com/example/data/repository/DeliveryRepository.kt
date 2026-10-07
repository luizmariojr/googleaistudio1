package com.example.data.repository

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

class DeliveryRepository(private val db: AppDatabase) {
    private val confirmationDao = db.deliveryConfirmationDao()
    private val romaneioDao = db.romaneioDao()
    private val nfDao = db.notaFiscalDao()

    private val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    private val _currentUserSession = MutableStateFlow<SupabaseUserSession?>(null)
    val currentUserSession: StateFlow<SupabaseUserSession?> = _currentUserSession.asStateFlow()

    val allRomaneios: Flow<List<RomaneioEntity>> = romaneioDao.getAllRomaneios()
    val allConfirmations: Flow<List<DeliveryConfirmationEntity>> = confirmationDao.getAllConfirmations()
    val pendingConfirmations: Flow<List<DeliveryConfirmationEntity>> = confirmationDao.getPendingConfirmations()
    val pendingCount: Flow<Int> = confirmationDao.getPendingCount()

    fun getRomaneioById(id: String): Flow<RomaneioEntity?> = romaneioDao.getRomaneioById(id)
    fun getNfsByRomaneio(romaneioId: String): Flow<List<NotaFiscalEntity>> = nfDao.getNfsByRomaneio(romaneioId)

    suspend fun hasLocalData(): Boolean = romaneioDao.getCount() > 0

    fun clearSession() {
        _currentUserSession.value = null
    }

    /**
     * Autentica o usuário rapidamente sem bloquear o download dos romaneios.
     */
    suspend fun loginOnly(
        user: String,
        pass: String
    ): Result<SupabaseUserSession> {
        val loginResult = SupabaseClient.login(user, pass)
        if (loginResult.isSuccess) {
            _currentUserSession.value = loginResult.getOrThrow()
        }
        return loginResult
    }

    /**
     * Busca os romaneios do usuário autenticado e atualiza o banco local Room.
     */
    suspend fun syncRomaneiosFromSession(): Result<Int> {
        val session = _currentUserSession.value ?: return Result.failure(Exception("Sessão não iniciada"))
        val romaneiosResult = SupabaseClient.fetchRealRomaneios(session.accessToken)
        if (romaneiosResult.isFailure) {
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
            romaneioDao.insertRomaneios(romEntities)
        }
        if (nfEntities.isNotEmpty()) {
            nfDao.insertNfs(nfEntities)
        }

        Log.i("DeliveryRepository", "Sincronização real concluída: ${romEntities.size} romaneios gravados no Room.")
        return Result.success(romEntities.size)
    }

    /**
     * Sincroniza diretamente com a base de dados real do Supabase
     * utilizando as credenciais informadas.
     */
    suspend fun syncWithRealSupabase(
        user: String = "908",
        pass: String = "wl908m"
    ): Result<Int> {
        try {
            Log.i("DeliveryRepository", "Iniciando autenticação no Supabase para usuário: $user")
            val loginResult = loginOnly(user, pass)
            if (loginResult.isFailure) {
                return Result.failure(loginResult.exceptionOrNull() ?: Exception("Falha de autenticação"))
            }
            return syncRomaneiosFromSession()
        } catch (e: Exception) {
            Log.e("DeliveryRepository", "Erro na sincronização com a base real", e)
            return Result.failure(e)
        }
    }

    suspend fun ensureInitialData() {
        // Se banco local estiver vazio, tenta sincronizar com o Supabase ou usa o seed de fallback
        if (romaneioDao.getCount() == 0) {
            val syncRes = syncWithRealSupabase("908", "wl908m")
            if (syncRes.isFailure) {
                // Fallback para os dados conhecidos das capturas se estiver sem sinal na primeira inicialização
                seedOfflineFallbackData()
            }
        } else {
            // Se já tem dados locais, atualiza silenciosamente em background com os dados mais recentes do Supabase
            try {
                syncWithRealSupabase("908", "wl908m")
            } catch (_: Exception) {}
        }
    }

    private suspend fun seedOfflineFallbackData() {
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
                motoristaNome = "WILIAN SOUSA DA SILVA",
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
                motoristaNome = "WILIAN SOUSA DA SILVA",
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
                motoristaNome = "WILIAN SOUSA DA SILVA",
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
                motoristaNome = "WILIAN SOUSA DA SILVA",
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
                motoristaNome = "WILIAN SOUSA DA SILVA",
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
            userId = _currentUserSession.value?.userId ?: "WILIAN SOUSA DA SILVA"
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
            val session = _currentUserSession.value
            val token = session?.accessToken ?: run {
                // Se o token não estiver em cache, reautentica
                val login = SupabaseClient.login("908", "wl908m")
                if (login.isSuccess) {
                    _currentUserSession.value = login.getOrThrow()
                    login.getOrThrow().accessToken
                } else null
            }

            if (token != null) {
                val isoDate = isoDateFormat.format(Date(item.dataHoraEntrega))
                val rpcResult = SupabaseClient.callRpcConfirmarEntrega(
                    token = token,
                    tipo = item.tipo,
                    romaneioId = item.romaneioId,
                    nfId = item.nfId,
                    canhotoPath = item.canhotoFotoUri,
                    dataHoraEntregaIso = isoDate
                )
                rpcResult.isSuccess
            } else {
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
