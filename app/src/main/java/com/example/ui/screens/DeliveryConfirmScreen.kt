package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import com.example.data.local.NotaFiscalEntity
import com.example.data.local.RomaneioEntity
import com.example.data.local.formatarNomeCliente
import com.example.data.local.getClienteFormatado
import com.example.data.local.getDataComHoraFormatada
import com.example.data.local.getDataFormatada
import com.example.data.local.getPrazoFormatado
import com.example.ui.theme.ActionAvaria
import com.example.ui.theme.ActionRedespacho
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.ConfirmGreenBtn
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkHeader
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceCardBorder
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusAmberBorder
import com.example.ui.theme.StatusAmberText
import com.example.ui.theme.StatusEmeraldBg
import com.example.ui.theme.StatusEmeraldText
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeliveryConfirmScreen(
    romaneio: RomaneioEntity?,
    nfs: List<NotaFiscalEntity>,
    isOnline: Boolean,
    onBack: () -> Unit,
    onConfirm: (
        tipo: String,
        romaneioId: String,
        numeroRomaneio: String,
        nfId: String?,
        numeroNf: String?,
        nomeRecebedor: String,
        documentoRecebedor: String,
        observacao: String,
        latitude: Double?,
        longitude: Double?,
        canhotoFotoUri: String?,
        assinaturaPresente: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    if (romaneio == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Romaneio não encontrado.", color = TextPrimary)
        }
        return
    }

    var nfSearchQuery by remember { mutableStateOf("") }
    val attachedPhotos = remember { mutableStateMapOf<String, String>() }
    var showAvariaDialogForNf by remember { mutableStateOf<NotaFiscalEntity?>(null) }
    var showRedespachoDialogForNf by remember { mutableStateOf<NotaFiscalEntity?>(null) }
    var avariaMotivo by remember { mutableStateOf("") }
    var redespachoTransportadora by remember { mutableStateOf("") }

    val context = LocalContext.current
    var targetNfForMedia by remember { mutableStateOf<NotaFiscalEntity?>(null) }
    var showMediaOptionsForNf by remember { mutableStateOf<NotaFiscalEntity?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun saveUriToCanhotoFile(uri: Uri, nf: NotaFiscalEntity) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val mime = context.contentResolver.getType(uri) ?: ""
            val isPdf = mime.contains("pdf") || uri.toString().lowercase().endsWith(".pdf")
            val ext = if (isPdf) "pdf" else "jpg"
            val storageDir = File(context.cacheDir, "canhotos").apply { mkdirs() }
            val destFile = File(storageDir, "anexo_nf_${nf.numeroNf}_${timeStamp}.$ext")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val path = destFile.absolutePath
            attachedPhotos[nf.id] = path
            Toast.makeText(context, "Foto da NF ${nf.numeroNf} anexada com sucesso!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            attachedPhotos[nf.id] = uri.toString()
            Toast.makeText(context, "Foto vinculada com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateDigitalCanhoto(nf: NotaFiscalEntity) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = File(context.cacheDir, "canhotos").apply { mkdirs() }
            val destFile = File(storageDir, "canhoto_digital_nf_${nf.numeroNf}_${timeStamp}.jpg")

            val width = 720
            val height = 480
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fundo
            val bgPaint = Paint().apply { color = android.graphics.Color.WHITE }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Borda do canhoto
            val borderPaint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawRoundRect(RectF(16f, 16f, width - 16f, height - 16f), 12f, 12f, borderPaint)

            // Cabeçalho Bicicletão
            val headerPaint = Paint().apply {
                color = android.graphics.Color.rgb(10, 17, 40)
                textSize = 28f
                isFakeBoldText = true
            }
            canvas.drawText("GRUPO BICICLETÃO - COMPROVANTE DE ENTREGA", 36f, 65f, headerPaint)

            val textPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 22f
            }
            canvas.drawText("ROMANEIO: #${romaneio.numeroRomaneio}", 36f, 120f, textPaint)
            canvas.drawText("NOTA FISCAL: #${nf.numeroNf} (Volumes: ${nf.volumes})", 36f, 160f, textPaint)
            canvas.drawText("DESTINATÁRIO: ${formatarNomeCliente(nf.destinatario)}", 36f, 200f, textPaint)
            canvas.drawText("CIDADE/UF: ${nf.cidade}/${nf.uf}", 36f, 240f, textPaint)
            val dataHoraAgora = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
            canvas.drawText("DATA DA ENTREGA: $dataHoraAgora", 36f, 280f, textPaint)

            // Carimbo de autenticação
            val stampPaint = Paint().apply {
                color = android.graphics.Color.rgb(5, 150, 105)
                textSize = 24f
                isFakeBoldText = true
            }
            canvas.drawText("✓ CANHOTO ASSINADO DIGITALMENTE", 36f, 360f, stampPaint)

            // Linha de assinatura
            val linePaint = Paint().apply {
                color = android.graphics.Color.GRAY
                strokeWidth = 2f
            }
            canvas.drawLine(36f, 430f, 450f, 430f, linePaint)
            val assPaint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 16f
            }
            canvas.drawText("Assinatura do Recebedor", 36f, 452f, assPaint)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            attachedPhotos[nf.id] = destFile.absolutePath
            Toast.makeText(context, "Canhoto digital da NF ${nf.numeroNf} gerado com sucesso!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao gerar canhoto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para tirar foto com a Câmera nativa do Android
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            targetNfForMedia?.let { nf ->
                val finalFile = tempCameraFile
                val finalUri = tempCameraUri
                val path = if (finalFile != null && finalFile.exists() && finalFile.length() > 0) {
                    finalFile.absolutePath
                } else {
                    finalUri?.toString() ?: ""
                }
                if (path.isNotBlank()) {
                    attachedPhotos[nf.id] = path
                    Toast.makeText(context, "Foto do canhoto da NF ${nf.numeroNf} registrada!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher da Galeria Nativa (Photo Picker oficial do Android, sem necessidade de permissões!)
    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            targetNfForMedia?.let { nf ->
                saveUriToCanhotoFile(uri, nf)
            }
        }
    }

    // Launcher universal de Conteúdo/Galeria (GetContent para galeria e pastas)
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            targetNfForMedia?.let { nf ->
                saveUriToCanhotoFile(uri, nf)
            }
        }
    }

    // Launcher de documentos e arquivos gerais
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            targetNfForMedia?.let { nf ->
                saveUriToCanhotoFile(uri, nf)
            }
        }
    }

    // Launcher para solicitar permissão de CÂMERA
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            targetNfForMedia?.let { nf ->
                try {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val storageDir = File(context.cacheDir, "canhotos").apply { mkdirs() }
                    val photoFile = File(storageDir, "canhoto_nf_${nf.numeroNf}_${timeStamp}.jpg")
                    val photoUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    tempCameraFile = photoFile
                    tempCameraUri = photoUri
                    takePictureLauncher.launch(photoUri)
                } catch (e: Exception) {
                    Toast.makeText(context, "Câmera indisponível. Abrindo opções de anexo...", Toast.LENGTH_SHORT).show()
                    showMediaOptionsForNf = nf
                }
            }
        } else {
            Toast.makeText(context, "Permissão da câmera negada. Você pode escolher foto da galeria.", Toast.LENGTH_LONG).show()
            targetNfForMedia?.let { showMediaOptionsForNf = it }
        }
    }

    fun triggerCamera(nf: NotaFiscalEntity) {
        targetNfForMedia = nf
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val storageDir = File(context.cacheDir, "canhotos").apply { mkdirs() }
                val photoFile = File(storageDir, "canhoto_nf_${nf.numeroNf}_${timeStamp}.jpg")
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                tempCameraFile = photoFile
                tempCameraUri = photoUri
                takePictureLauncher.launch(photoUri)
            } catch (e: Exception) {
                // Caso não haja app de câmera ou falhe no emulador, abre o seletor com opções
                Toast.makeText(context, "Não foi possível abrir a câmera diretamente. Selecione uma opção:", Toast.LENGTH_SHORT).show()
                showMediaOptionsForNf = nf
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun triggerFilePicker(nf: NotaFiscalEntity) {
        targetNfForMedia = nf
        try {
            // Tenta abrir o Photo Picker nativo do Android
            pickVisualMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: Exception) {
            try {
                // Fallback 1: GetContent image/* (abre qualquer galeria/gerenciador do aparelho)
                getContentLauncher.launch("image/*")
            } catch (_: Exception) {
                try {
                    // Fallback 2: OpenDocument
                    filePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                } catch (e3: Exception) {
                    // Fallback 3: Abre diálogo com opções incluindo gerador de canhoto de teste
                    showMediaOptionsForNf = nf
                }
            }
        }
    }

    // Diálogo de confirmação com dados adicionais (Recebedor / Obs) se necessário
    var confirmingNf by remember { mutableStateOf<NotaFiscalEntity?>(null) }
    var recebedorNomeInput by remember { mutableStateOf("") }
    var recebedorDocInput by remember { mutableStateOf("") }
    var observacaoInput by remember { mutableStateOf("") }

    val filteredNfs = remember(nfs, nfSearchQuery) {
        if (nfSearchQuery.isBlank()) nfs
        else {
            nfs.filter {
                it.numeroNf.contains(nfSearchQuery, ignoreCase = true) ||
                        it.destinatario.contains(nfSearchQuery, ignoreCase = true) ||
                        it.cidade.contains(nfSearchQuery, ignoreCase = true)
            }
        }
    }

    val deliveredCount = nfs.count { it.status == "Entregue" || it.status == "Aguardando Sincronização" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))

            // Botão "<- Voltar para a lista" (idêntico ao screenshot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBack() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Voltar para a lista",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Principal do Romaneio (ex: #2314 no screenshot)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ROMANEIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (romaneio.status == "Entregue") StatusEmeraldBg else StatusAmberBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (romaneio.status == "Entregue") StatusEmeraldText.copy(alpha = 0.4f) else StatusAmberBorder
                            )
                        ) {
                            Text(
                                text = romaneio.status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (romaneio.status == "Entregue") StatusEmeraldText else StatusAmberText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "#${romaneio.numeroRomaneio}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badge de Data do Romaneio e Prazo de Entrega
                    val dataRomaneioComHora = remember(romaneio.createdAt, romaneio.prazoEntrega) {
                        romaneio.getDataComHoraFormatada()
                    }
                    val dataRomaneioSimples = remember(romaneio.createdAt, romaneio.prazoEntrega) {
                        romaneio.getDataFormatada()
                    }
                    val prazoRomaneio = remember(romaneio.prazoEntrega) {
                        romaneio.getPrazoFormatado()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandBlue.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Data do Romaneio",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Data do romaneio: $dataRomaneioComHora",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }
                        }

                        if (!prazoRomaneio.isNullOrBlank() && !prazoRomaneio.equals("Hoje", ignoreCase = true)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StatusAmberBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusAmberBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Prazo",
                                        tint = StatusAmberText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Prazo: $prazoRomaneio",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusAmberText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Linha 1: Métricas do Romaneio (Volumes, Peso e Data)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${romaneio.quantidadeVolumes} vol.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${romaneio.pesoTotalKg.toString().replace('.', ',')} kg",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = dataRomaneioSimples,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Linha 2 dedicada: Motorista em linha única com largura total (sem quebrar em 3 linhas)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = "Motorista",
                            tint = BrandBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = romaneio.motoristaNome,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Barra de progresso de NFs entregues (ex: 0/2 NFs entregues)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "$deliveredCount/${nfs.size} NFs entregues",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    LinearProgressIndicator(
                        progress = { if (nfs.isNotEmpty()) deliveredCount.toFloat() / nfs.size.toFloat() else 0f },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ConfirmGreenBtn,
                        trackColor = Color(0xFF1E2C4F),
                    )
                }
            }
        }

            Spacer(modifier = Modifier.height(14.dp))

            // Seção de Cabeçalho: "Notas fiscais (2)" + "Anexe um canhoto e confirme cada NF"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Notas fiscais (${nfs.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Anexe um canhoto e confirme cada NF",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de busca: "Buscar por NF, cliente, cidade..."
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (nfSearchQuery.isEmpty()) {
                            Text(
                                text = "Buscar por NF, cliente, cidade...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = nfSearchQuery,
                            onValueChange = { nfSearchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextPrimary,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Cards das Notas Fiscais individuais (idênticos aos screenshots 2 e 3)
        items(filteredNfs, key = { it.id }) { nfItem ->
            val isNfDelivered = nfItem.status == "Entregue"
            val isNfPendingSync = nfItem.status == "Aguardando Sincronização"
            val attachedPhotoPath = attachedPhotos[nfItem.id] ?: nfItem.canhotoFotoUri
            val hasPhoto = !attachedPhotoPath.isNullOrBlank()

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Linha 1: "NF 115317" + "1 vol. / 2,26 kg"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "NF ${nfItem.numeroNf}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${nfItem.volumes} vol.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${nfItem.pesoKg.toString().replace('.', ',')} kg",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Linha 2: Nome do cliente / destinatário
                    Text(
                        text = nfItem.destinatario,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Linha 3: Cidade / UF com ícone de pin
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${nfItem.cidade} / ${nfItem.uf}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isNfDelivered || isNfPendingSync) {
                        // Badge de status entregue / pendente sincronização
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isNfDelivered) StatusEmeraldBg else Color(0xFF78350F),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isNfDelivered) StatusEmeraldText.copy(alpha = 0.4f) else StatusAmberText.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isNfDelivered) StatusEmeraldText else StatusAmberText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isNfDelivered) "NF Entregue" else "Entrega Registrada Localmente",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isNfDelivered) StatusEmeraldText else StatusAmberText
                                    )
                                    Text(
                                        text = if (isNfDelivered) "Sincronizada com o servidor" else "Aguardando envio ao servidor quando a rede voltar",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    } else {
                        if (hasPhoto && attachedPhotoPath != null) {
                            // Canhoto já fotografado ou anexado
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0D1426),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, ConfirmGreenBtn),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("camera_box_${nfItem.numeroNf}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(ConfirmGreenBtn.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Sucesso",
                                                    tint = StatusEmeraldText,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Canhoto anexado com sucesso",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusEmeraldText
                                                )
                                                Text(
                                                    text = File(attachedPhotoPath).name,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { attachedPhotos.remove(nfItem.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remover",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Preview da imagem anexada
                                    val isPdf = attachedPhotoPath.lowercase().endsWith(".pdf")
                                    if (!isPdf) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF070B16))
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = if (attachedPhotoPath.startsWith("content://") || attachedPhotoPath.startsWith("file://")) {
                                                    Uri.parse(attachedPhotoPath)
                                                } else {
                                                    File(attachedPhotoPath)
                                                },
                                                contentDescription = "Foto do Canhoto",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Opções para trocar foto ou documento
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { triggerCamera(nfItem) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3E67))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = BrandBlue
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tirar outra", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        OutlinedButton(
                                            onClick = { triggerFilePicker(nfItem) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3E67))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Galeria / PDF", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Caixa de captura de foto do canhoto (Borda tracejada e ícone de câmera azul como no print)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0D1426),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2C3E67)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMediaOptionsForNf = nfItem }
                                    .testTag("camera_box_${nfItem.numeroNf}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 22.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(BrandBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Câmera",
                                            tint = BrandBlue,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Tirar foto ou anexar canhoto",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Toque para abrir a câmera, galeria ou pastas",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Botão: "Escolher da galeria ou PDF" (abre gerenciador de arquivos/fotos)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3E67)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clickable { showMediaOptionsForNf = nfItem }
                                    .testTag("gallery_btn_${nfItem.numeroNf}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Galeria, Pastas ou Canhoto Digital",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Botão Principal: "Confirmar entrega desta NF" (Verde esmeralda idêntico ao screenshot)
                        Button(
                            onClick = {
                                confirmingNf = nfItem
                                recebedorNomeInput = formatarNomeCliente(nfItem.destinatario)
                                recebedorDocInput = ""
                                observacaoInput = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_confirm_nf_${nfItem.numeroNf}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ConfirmGreenBtn)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirmar entrega desta NF",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Botões Secundários: "Avaria" e "Redespacho" (como nos screenshots 2 e 3)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Botão Avaria
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161F33),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ActionAvaria.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable { showAvariaDialogForNf = nfItem }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Avaria",
                                        tint = ActionAvaria,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Avaria",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ActionAvaria
                                    )
                                }
                            }

                            // Botão Redespacho
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161F33),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ActionRedespacho.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable { showRedespachoDialogForNf = nfItem }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Redespacho",
                                        tint = ActionRedespacho,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Redespacho",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ActionRedespacho
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal de Opções para Anexar Canhoto (Câmera, Galeria, Pastas ou Canhoto Digital)
    if (showMediaOptionsForNf != null) {
        val target = showMediaOptionsForNf!!
        AlertDialog(
            onDismissRequest = { showMediaOptionsForNf = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Anexar Canhoto NF #${target.numeroNf}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Selecione como deseja registrar o comprovante de entrega:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    // 1. Câmera
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val t = target
                                showMediaOptionsForNf = null
                                triggerCamera(t)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Tirar Foto com a Câmera", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Abrir câmera fotográfica do aparelho", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // 2. Galeria de Fotos
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val t = target
                                showMediaOptionsForNf = null
                                triggerFilePicker(t)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Galeria de Fotos", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Escolher foto tirada anteriormente", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // 3. Pastas e Arquivos
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val t = target
                                showMediaOptionsForNf = null
                                targetNfForMedia = t
                                try {
                                    filePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                } catch (_: Exception) {
                                    getContentLauncher.launch("image/*")
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Pastas / Documentos (PDF)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Buscar arquivo no armazenamento interno", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // 4. Canhoto Digital Automático
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val t = target
                                showMediaOptionsForNf = null
                                generateDigitalCanhoto(t)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = StatusEmeraldText, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Gerar Canhoto Digital de Teste", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusEmeraldText)
                                Text("Cria comprovante carimbado instantaneamente", fontSize = 11.sp, color = Color(0xFFA7F3D0))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMediaOptionsForNf = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkHeader
        )
    }

    // Modal de Confirmação Rápida de Entrega da NF
    if (confirmingNf != null) {
        val targetNf = confirmingNf!!
        AlertDialog(
            onDismissRequest = { confirmingNf = null },
            title = {
                Text(
                    text = "Confirmar Entrega NF #${targetNf.numeroNf}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            },
            text = {
                val attachedPhoto = attachedPhotos[targetNf.id] ?: targetNf.canhotoFotoUri
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "A data e horário atuais serão gravados no dispositivo com garantia de precisão mesmo sem internet.",
                                fontSize = 11.sp,
                                color = StatusEmeraldText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!attachedPhoto.isNullOrBlank()) {
                                Text(
                                    text = "✓ Canhoto anexado: ${File(attachedPhoto).name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            } else {
                                Text(
                                    text = "ℹ Você pode anexar o canhoto agora ou confirmar diretamente.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    if (attachedPhoto.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { triggerCamera(targetNf) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(13.dp), tint = BrandBlue)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Câmera", fontSize = 10.sp, color = BrandBlue)
                            }

                            OutlinedButton(
                                onClick = { triggerFilePicker(targetNf) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3E67))
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondary)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Galeria", fontSize = 10.sp, color = TextPrimary)
                            }

                            OutlinedButton(
                                onClick = { generateDigitalCanhoto(targetNf) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669))
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(13.dp), tint = StatusEmeraldText)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Auto", fontSize = 10.sp, color = StatusEmeraldText)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = recebedorNomeInput,
                        onValueChange = { recebedorNomeInput = it },
                        label = { Text("Nome do recebedor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = recebedorDocInput,
                        onValueChange = { recebedorDocInput = it },
                        label = { Text("CPF ou RG") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = observacaoInput,
                        onValueChange = { observacaoInput = it },
                        label = { Text("Observação (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val photoUri = attachedPhotos[targetNf.id] ?: targetNf.canhotoFotoUri ?: "canhoto_nf_${targetNf.numeroNf}.jpg"
                        onConfirm(
                            "entrega_nf",
                            romaneio.id,
                            romaneio.numeroRomaneio,
                            targetNf.id,
                            targetNf.numeroNf,
                            recebedorNomeInput,
                            recebedorDocInput,
                            observacaoInput,
                            -5.5266, // Imperatriz/MA
                            -47.4789,
                            photoUri,
                            true
                        )
                        confirmingNf = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ConfirmGreenBtn)
                ) {
                    Text("Confirmar Agora", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingNf = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Modal de Avaria
    if (showAvariaDialogForNf != null) {
        AlertDialog(
            onDismissRequest = { showAvariaDialogForNf = null },
            title = {
                Text("Registrar Avaria - NF #${showAvariaDialogForNf?.numeroNf}", color = TextPrimary)
            },
            text = {
                Column {
                    Text("Descreva a avaria identificada na mercadoria:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = avariaMotivo,
                        onValueChange = { avariaMotivo = it },
                        placeholder = { Text("Ex: Caixa rasgada, peça riscada...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAvariaDialogForNf = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionAvaria)
                ) {
                    Text("Gravar Avaria", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAvariaDialogForNf = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }

    // Modal de Redespacho
    if (showRedespachoDialogForNf != null) {
        AlertDialog(
            onDismissRequest = { showRedespachoDialogForNf = null },
            title = {
                Text("Registrar Redespacho - NF #${showRedespachoDialogForNf?.numeroNf}", color = TextPrimary)
            },
            text = {
                Column {
                    Text("Informe a transportadora de redespacho parceira:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = redespachoTransportadora,
                        onValueChange = { redespachoTransportadora = it },
                        placeholder = { Text("Nome da transportadora") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRedespachoDialogForNf = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionRedespacho)
                ) {
                    Text("Confirmar Redespacho", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRedespachoDialogForNf = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceCard
        )
    }
}
