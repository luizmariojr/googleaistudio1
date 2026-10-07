package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class SupabaseUserSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val accessToken: String
)

data class RemoteRomaneio(
    val id: String,
    val numeroRomaneio: String,
    val status: String,
    val clientePrincipal: String,
    val cidade: String,
    val uf: String,
    val quantidadeNfs: Int,
    val quantidadeVolumes: Int,
    val pesoTotalKg: Double,
    val motoristaNome: String,
    val createdAt: String,
    val prazoEntrega: String? = null,
    val notasFiscais: List<RemoteNotaFiscal>
)

data class RemoteNotaFiscal(
    val id: String,
    val romaneioId: String,
    val numeroNf: String,
    val destinatario: String,
    val cidade: String,
    val uf: String,
    val volumes: Int,
    val pesoKg: Double,
    val valor: Double,
    val status: String
)

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    const val SUPABASE_URL = "https://hzsacfnbzwexdkhahvni.supabase.co"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh6c2FjZm5iendleGRraGFodm5pIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMjU3NjUsImV4cCI6MjA4NzYwMTc2NX0.YWII9LYwPg8917vbLzroxVTsj2cRUowDV9kQHquPER8"

    /**
     * Converte o usuário (ex: "908") para o e-mail esperado pelo Supabase Auth,
     * exatamente como a aplicação Web oficial do Grupo Bicicletão faz.
     */
    fun formatUsernameToEmail(username: String): String {
        val clean = username.trim()
        if (clean.contains("@")) return clean
        val digitsOnly = clean.replace(Regex("\\D"), "")
        return when {
            digitsOnly.length == 14 -> "$digitsOnly@transportadora.logistica.local"
            clean.all { it.isDigit() } -> "$clean@motorista.logistica.local"
            else -> clean
        }
    }

    suspend fun login(username: String, password: String): Result<SupabaseUserSession> =
        withContext(Dispatchers.IO) {
            try {
                val email = formatUsernameToEmail(username)
                val url = URL("$SUPABASE_URL/auth/v1/token?grant_type=password")
                val conn = url.openConnection() as HttpsURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val payload = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseStr)
                    val token = json.getString("access_token")
                    val userObj = json.getJSONObject("user")
                    val userId = userObj.getString("id")
                    val metadata = userObj.optJSONObject("user_metadata")
                    val fullName = metadata?.optString("full_name") ?: "WILIAN SOUSA DA SILVA"

                    Log.i(TAG, "Login realizado com sucesso para $email ($fullName)")
                    Result.success(
                        SupabaseUserSession(
                            userId = userId,
                            email = email,
                            fullName = fullName,
                            accessToken = token
                        )
                    )
                } else {
                    val errStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Erro HTTP $responseCode"
                    Log.e(TAG, "Erro no login ($responseCode): $errStr")
                    Result.failure(Exception("Credenciais inválidas ou erro no servidor: $errStr"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha na conexão de login", e)
                Result.failure(e)
            }
        }

    suspend fun fetchRealRomaneios(token: String): Result<List<RemoteRomaneio>> =
        withContext(Dispatchers.IO) {
            try {
                val endpoint =
                    "$SUPABASE_URL/rest/v1/romaneios?select=id,numero_romaneio,status,prazo_entrega,created_at,quantidade_volumes,peso_total_kg,transportador_nome,logistica_notas_fiscais(id,numero_nf,destinatario,cidade_cliente,uf_cliente,volumes,peso_bruto,valor,status)&order=created_at.desc&limit=300"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpsURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 10000
                conn.readTimeout = 15000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(responseStr)
                    val list = mutableListOf<RemoteRomaneio>()

                    for (i in 0 until array.length()) {
                        val romJson = array.getJSONObject(i)
                        val id = romJson.getString("id")
                        val numeroRom = romJson.optString("numero_romaneio", "")
                        val status = romJson.optString("status", "Criado")
                        val volumes = romJson.optInt("quantidade_volumes", 1)
                        val peso = romJson.optDouble("peso_total_kg", 0.0)
                        val motoristaNome = romJson.optString("transportador_nome", "WILIAN SOUSA DA SILVA")
                        val createdAt = romJson.optString("created_at", "")
                        val prazoEntrega = romJson.optString("prazo_entrega", "").ifBlank { null }

                        val nfsArray = romJson.optJSONArray("logistica_notas_fiscais") ?: JSONArray()
                        val nfsList = mutableListOf<RemoteNotaFiscal>()
                        var primeiroCliente = "Cliente não identificado"
                        var primeiraCidade = "IMPERATRIZ"
                        var primeiroUf = "MA"

                        for (j in 0 until nfsArray.length()) {
                            val nfJson = nfsArray.getJSONObject(j)
                            val nfId = nfJson.getString("id")
                            val numeroNf = nfJson.optString("numero_nf", "")
                            val dest = nfJson.optString("destinatario", "")
                            val cidade = nfJson.optString("cidade_cliente", "IMPERATRIZ")
                            val uf = nfJson.optString("uf_cliente", "MA")
                            val nfVol = nfJson.optInt("volumes", 1)
                            val nfPeso = nfJson.optDouble("peso_bruto", 1.0)
                            val valor = nfJson.optDouble("valor", 0.0)
                            val nfStatus = nfJson.optString("status", "Pendente")

                            if (j == 0) {
                                primeiroCliente = dest
                                primeiraCidade = cidade
                                primeiroUf = uf
                            }

                            nfsList.add(
                                RemoteNotaFiscal(
                                    id = nfId,
                                    romaneioId = id,
                                    numeroNf = numeroNf,
                                    destinatario = dest,
                                    cidade = cidade,
                                    uf = uf,
                                    volumes = nfVol,
                                    pesoKg = nfPeso,
                                    valor = valor,
                                    status = nfStatus
                                )
                            )
                        }

                        if (nfsList.size > 1) {
                            primeiroCliente = "$primeiroCliente + ${nfsList.size - 1}"
                        }

                        list.add(
                            RemoteRomaneio(
                                id = id,
                                numeroRomaneio = numeroRom,
                                status = status,
                                clientePrincipal = primeiroCliente,
                                cidade = primeiraCidade,
                                uf = primeiroUf,
                                quantidadeNfs = nfsList.size,
                                quantidadeVolumes = volumes,
                                pesoTotalKg = peso,
                                motoristaNome = motoristaNome,
                                createdAt = createdAt,
                                prazoEntrega = prazoEntrega,
                                notasFiscais = nfsList
                            )
                        )
                    }

                    Log.i(TAG, "Carregados ${list.size} romaneios reais do Supabase.")
                    Result.success(list)
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Result.failure(Exception("Erro na busca de romaneios: HTTP ${conn.responseCode} - $err"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao buscar romaneios do Supabase", e)
                Result.failure(e)
            }
        }

    suspend fun callRpcConfirmarEntrega(
        token: String,
        tipo: String,
        romaneioId: String,
        nfId: String?,
        canhotoPath: String?,
        dataHoraEntregaIso: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rpcName = if (tipo == "entrega_nf") "confirmar_entrega_nf_portal" else "confirmar_entrega_portal"
            val url = URL("$SUPABASE_URL/rest/v1/rpc/$rpcName")
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                if (tipo == "entrega_nf") {
                    put("p_nf_id", nfId)
                } else {
                    put("p_romaneio_id", romaneioId)
                }
                put("p_canhoto_path", canhotoPath ?: "canhoto_offline.jpg")
                // Envia a data/hora original da entrega se o RPC aceitar ou como parâmetro adicional
                put("p_data_hora_entrega", dataHoraEntregaIso)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            if (code in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                Result.success(resp)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Erro RPC $rpcName ($code): $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
