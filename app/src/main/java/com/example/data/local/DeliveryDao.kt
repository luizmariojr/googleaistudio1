package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryConfirmationDao {
    @Query("SELECT * FROM offline_confirmations ORDER BY dataHoraEntrega DESC")
    fun getAllConfirmations(): Flow<List<DeliveryConfirmationEntity>>

    @Query("SELECT * FROM offline_confirmations WHERE status != 'SINCRONIZADO' ORDER BY dataHoraEntrega ASC")
    fun getPendingConfirmations(): Flow<List<DeliveryConfirmationEntity>>

    @Query("SELECT COUNT(*) FROM offline_confirmations WHERE status != 'SINCRONIZADO'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM offline_confirmations WHERE id = :id")
    suspend fun getById(id: String): DeliveryConfirmationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfirmation(entity: DeliveryConfirmationEntity)

    @Update
    suspend fun updateConfirmation(entity: DeliveryConfirmationEntity)

    @Query("UPDATE offline_confirmations SET status = :status, ultimoErro = :erro, tentativas = tentativas + 1, proximaTentativaEm = :proximaTentativa WHERE id = :id")
    suspend fun updateError(id: String, status: String, erro: String?, proximaTentativa: Long)

    @Query("UPDATE offline_confirmations SET status = 'SINCRONIZADO', dataHoraSincronizacao = :syncTime, dataHoraSincronizacaoFormatada = :syncFormatada, ultimoErro = null WHERE id = :id")
    suspend fun markSynchronized(id: String, syncTime: Long, syncFormatada: String)

    @Query("DELETE FROM offline_confirmations WHERE id = :id")
    suspend fun deleteConfirmation(id: String)
}

@Dao
interface RomaneioDao {
    @Query("SELECT * FROM romaneios_cache ORDER BY CAST(numeroRomaneio AS INTEGER) DESC")
    fun getAllRomaneios(): Flow<List<RomaneioEntity>>

    @Query("SELECT * FROM romaneios_cache WHERE id = :id")
    fun getRomaneioById(id: String): Flow<RomaneioEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRomaneios(list: List<RomaneioEntity>)

    @Query("UPDATE romaneios_cache SET status = :status, entregueEm = :entregueEm, entregueEmFormatado = :entregueEmFormatado WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, entregueEm: Long?, entregueEmFormatado: String?)

    @Query("SELECT COUNT(*) FROM romaneios_cache")
    suspend fun getCount(): Int
}

@Dao
interface NotaFiscalDao {
    @Query("SELECT * FROM notas_fiscais_cache WHERE romaneioId = :romaneioId")
    fun getNfsByRomaneio(romaneioId: String): Flow<List<NotaFiscalEntity>>

    @Query("SELECT * FROM notas_fiscais_cache WHERE romaneioId = :romaneioId")
    suspend fun getNfsListByRomaneio(romaneioId: String): List<NotaFiscalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNfs(list: List<NotaFiscalEntity>)

    @Query("UPDATE notas_fiscais_cache SET status = :status, entregueEm = :entregueEm, entregueEmFormatado = :entregueEmFormatado, canhotoFotoUri = :canhotoUri WHERE id = :id")
    suspend fun updateNfStatus(id: String, status: String, entregueEm: Long?, entregueEmFormatado: String?, canhotoUri: String?)

    @Query("UPDATE notas_fiscais_cache SET status = :status, entregueEm = :entregueEm, entregueEmFormatado = :entregueEmFormatado WHERE romaneioId = :romaneioId")
    suspend fun updateAllNfsForRomaneio(romaneioId: String, status: String, entregueEm: Long?, entregueEmFormatado: String?)

    @Query("UPDATE notas_fiscais_cache SET canhotoFotoUri = :fotoUri WHERE id = :id")
    suspend fun updateCanhotoFoto(id: String, fotoUri: String)

    @Query("UPDATE notas_fiscais_cache SET avariaRegistrada = 1 WHERE id = :id")
    suspend fun markAvaria(id: String)

    @Query("UPDATE notas_fiscais_cache SET redespachoRegistrado = 1 WHERE id = :id")
    suspend fun markRedespacho(id: String)
}
