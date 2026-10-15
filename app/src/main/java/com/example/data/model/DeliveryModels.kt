package com.example.data.model

enum class DeliveryStatus(val label: String) {
    PENDING("Pendente"),
    IN_TRANSIT("Em Rota"),
    DELIVERED("Entregue"),
    FAILED("Insucesso")
}

enum class SyncStatus(val label: String) {
    SYNCED("Sincronizado"),
    PENDING_SYNC("Na Fila Offline"),
    SYNCING("Sincronizando..."),
    SYNC_ERROR("Falha no Envio")
}

data class PointDto(
    val x: Float,
    val y: Float,
    val isBreak: Boolean = false
)

data class DeliveryProof(
    val deliveryId: String,
    val receiverName: String,
    val receiverDocument: String,
    val relationship: String,
    val signaturePoints: List<PointDto>,
    val timestampMillis: Long,
    val receiptHash: String,
    val syncStatus: SyncStatus,
    val latitude: Double = -23.550520,
    val longitude: Double = -46.633308,
    val photoSimulated: Boolean = true
)

data class DeliveryItem(
    val id: String,
    val trackingCode: String,
    val recipientName: String,
    val recipientPhone: String,
    val address: String,
    val complement: String = "",
    val neighborhood: String,
    val city: String = "São Paulo, SP",
    val scheduledTime: String,
    val packageDescription: String,
    val packageWeightKg: Double,
    val notes: String = "",
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val proof: DeliveryProof? = null,
    val failureReason: String? = null
)

data class SyncQueueItem(
    val id: String,
    val deliveryId: String,
    val recipientName: String,
    val trackingCode: String,
    val actionType: String,
    val timestampMillis: Long,
    val syncStatus: SyncStatus,
    val attempts: Int = 0,
    val errorMessage: String? = null
)

data class CourierMetrics(
    val totalDeliveries: Int,
    val deliveredCount: Int,
    val pendingCount: Int,
    val failedCount: Int,
    val estimatedDistanceKm: Double,
    val totalVolumeKg: Double,
    val queuePendingCount: Int
)
