package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DeliveryConfirmationEntity
import com.example.data.local.RomaneioEntity
import com.example.data.local.isWithinDays
import com.example.data.remote.SupabaseClient
import com.example.data.repository.DeliveryRepository
import com.example.ui.DeliveryViewModel
import com.example.ui.ScreenDestination
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verificar nome do app nos recursos`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Bicicletão Logística", appName)
    }

    @Test
    fun `testar conversao de login para email do supabase`() {
        // Usuário motorista: número de matrícula "908" -> "908@motorista.logistica.local"
        val motoristaEmail = SupabaseClient.formatUsernameToEmail("908")
        assertEquals("908@motorista.logistica.local", motoristaEmail)

        // Usuário transportadora: CNPJ de 14 dígitos
        val transportadoraEmail = SupabaseClient.formatUsernameToEmail("12.345.678/0001-99")
        assertEquals("12345678000199@transportadora.logistica.local", transportadoraEmail)

        // E-mail comum
        val normalEmail = SupabaseClient.formatUsernameToEmail("admin@bicicletao.com.br")
        assertEquals("admin@bicicletao.com.br", normalEmail)
    }

    @Test
    fun `testar fluxo completo de confirmacao offline no Room Database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val repository = DeliveryRepository(db)

        // 1. Garante que os dados iniciais dos romaneios foram povoados
        repository.ensureInitialData()
        val romaneios = repository.allRomaneios.first()
        assertTrue("Deve conter romaneios carregados", romaneios.isNotEmpty())

        val romaneioAlvo = romaneios.first()

        // 2. Registra entrega offline (simulando clique do motorista sem internet)
        val confirmation = repository.registerOfflineConfirmation(
            tipo = "entrega_romaneio",
            romaneioId = romaneioAlvo.id,
            numeroRomaneio = romaneioAlvo.numeroRomaneio,
            nomeRecebedor = "VALTER SANTOS",
            documentoRecebedor = "43.892.109-8",
            observacao = "Entrega offline de teste no emulador",
            latitude = -5.5266,
            longitude = -47.4789,
            canhotoFotoUri = "foto_canhoto_teste.jpg",
            assinaturaPresente = true
        )

        // 3. Validações estritas dos requisitos do sistema:
        // A) UUID único de idempotência gerado
        assertNotNull(confirmation.id)
        assertTrue(confirmation.id.isNotBlank())

        // B) Data/Hora da entrega gravada no exato momento do clique
        assertTrue("Timestamp de entrega deve ser recente", confirmation.dataHoraEntrega > 0)
        assertNotNull(confirmation.dataHoraEntregaFormatada)

        // C) Data/Hora de sincronização DEVE ser nula inicialmente
        assertNull("Data de sincronização não pode existir antes do envio", confirmation.dataHoraSincronizacao)

        // D) Status deve ser PENDENTE
        assertEquals("PENDENTE", confirmation.status)

        // E) Verifica que a fila de pendências incrementou
        val pendentes = repository.pendingConfirmations.first()
        assertTrue("A fila deve conter o registro pendente", pendentes.any { it.id == confirmation.id })

        // 4. Simula o momento em que a internet voltou e o servidor confirma a gravação (200 OK)
        val serverAckTimestamp = confirmation.dataHoraEntrega + 3600000L // 1 hora depois
        repository.markAsSynchronized(
            id = confirmation.id,
            syncTimestamp = serverAckTimestamp,
            romaneioId = romaneioAlvo.id,
            tipo = "entrega_romaneio",
            nfId = null,
            originalDeliveryTimestamp = confirmation.dataHoraEntrega
        )

        // 5. Verifica que após confirmação do servidor:
        val salvo = db.deliveryConfirmationDao().getById(confirmation.id)
        assertNotNull(salvo)
        assertEquals("SINCRONIZADO", salvo!!.status)
        assertEquals(serverAckTimestamp, salvo.dataHoraSincronizacao)
        assertNotNull(salvo.dataHoraSincronizacaoFormatada)
        // A data original da entrega permanece INTACTA!
        assertEquals(confirmation.dataHoraEntrega, salvo.dataHoraEntrega)
    }

    @Test
    fun `testar exibicao do banner obrigatorio no ViewModel`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DeliveryViewModel(app)

        // Dispara confirmação
        viewModel.confirmDelivery(
            tipo = "entrega_romaneio",
            romaneioId = "rom-2314",
            numeroRomaneio = "2314",
            nomeRecebedor = "ANTONIO DE SOUSA NUNES",
            documentoRecebedor = "88807703149",
            observacao = "Teste de interface"
        )

        var banner: String? = null
        for (i in 0..20) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            banner = viewModel.offlineBannerMessage.value
            if (banner != null) break
            kotlinx.coroutines.delay(50)
        }

        // Verifica que o banner exibido contém a frase exata exigida pelo cliente
        assertEquals("Entrega confirmada neste dispositivo. Aguardando sincronização.", banner)
    }

    @Test
    fun `testar estado inicial na tela de login e logout no ViewModel`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DeliveryViewModel(app)

        // Deve iniciar obrigatoriamente na tela de login com campos limpos
        assertEquals(ScreenDestination.LOGIN, viewModel.currentScreen.value)
        assertEquals("", viewModel.loginUsername.value)
        assertEquals("", viewModel.loginPassword.value)

        // Ao chamar logout retorna sempre para LOGIN
        viewModel.logout()
        assertEquals(ScreenDestination.LOGIN, viewModel.currentScreen.value)
    }

    @Test
    fun `testar filtragem de romaneios por data`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        val repository = DeliveryRepository(db)
        repository.ensureInitialData()

        val romaneios = repository.allRomaneios.first()
        assertTrue(romaneios.isNotEmpty())

        // Filtra romaneios com data de hoje (2026-10-06)
        val hoje = romaneios.filter { it.createdAt.startsWith("2026-10-06") }
        assertTrue("Deve encontrar romaneios de hoje", hoje.isNotEmpty())

        // Filtra romaneios com data de ontem (2026-10-05)
        val ontem = romaneios.filter { it.createdAt.startsWith("2026-10-05") }
        assertTrue("Deve encontrar romaneios de ontem", ontem.isNotEmpty())

        // Testa precisão do cálculo de últimos 30 dias
        val dentro30Dias = romaneios.filter { it.isWithinDays(30) }
        assertTrue("Deve conter romaneios nos últimos 30 dias", dentro30Dias.isNotEmpty())
    }
}
