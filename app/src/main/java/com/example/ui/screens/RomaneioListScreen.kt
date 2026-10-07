package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RomaneioEntity
import com.example.data.local.getClienteFormatado
import com.example.data.local.getDataComHoraFormatada
import com.example.data.local.getDataFormatada
import com.example.data.local.isWithinDays
import com.example.ui.theme.BrandBlue
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomaneioListScreen(
    romaneios: List<RomaneioEntity>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onSelectRomaneio: (RomaneioEntity) -> Unit,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("em_aberto") } // "em_aberto" | "todos"
    var dateFilter by remember { mutableStateOf("30_dias") } // "30_dias" | "hoje" | "todas" | "dd/MM/yyyy"
    var showDatePicker by remember { mutableStateOf(false) }

    val todayDmy = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
    }
    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    // Modal de seleção de data do calendário Material 3
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val day = cal.get(Calendar.DAY_OF_MONTH)
                            val month = cal.get(Calendar.MONTH) + 1
                            val year = cal.get(Calendar.YEAR)
                            dateFilter = String.format("%02d/%02d/%04d", day, month, year)
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("date_picker_confirm_btn")
                ) {
                    Text("Selecionar", color = BrandBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            dateFilter = todayDmy
                            showDatePicker = false
                        },
                        modifier = Modifier.testTag("date_picker_today_btn")
                    ) {
                        Text("Hoje", color = BrandBlue)
                    }
                    TextButton(
                        onClick = {
                            dateFilter = "30_dias"
                            showDatePicker = false
                        },
                        modifier = Modifier.testTag("date_picker_clear_btn")
                    ) {
                        Text("Limpar", color = TextSecondary)
                    }
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = DarkHeader
            )
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Filtrar por data",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = DarkHeader,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = BrandBlue,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = BrandBlue,
                    dayContentColor = TextPrimary,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = BrandBlue,
                    todayContentColor = BrandBlue,
                    todayDateBorderColor = BrandBlue
                )
            )
        }
    }

    // Status permitidos na aba Em Aberto conforme especificação estrita: Criado, Embarcado, Em transito, Coletado
    val statusEmAbertoPermitidos = remember {
        setOf("criado", "embarcado", "em transito", "em trânsito", "coletado")
    }

    val totalEmAberto = remember(romaneios) {
        romaneios.count { statusEmAbertoPermitidos.contains(it.status.trim().lowercase()) }
    }

    // Filtragem precisa dos romaneios por Status (aba), Data e Busca
    val displayedRomaneios = remember(romaneios, selectedTab, dateFilter, searchQuery) {
        romaneios.filter { rom ->
            // 1. Filtro da Aba: Somente status em aberto permitidos
            val matchesTab = if (selectedTab == "em_aberto") {
                val normStatus = rom.status.trim().lowercase()
                statusEmAbertoPermitidos.contains(normStatus)
            } else {
                true
            }

            // 2. Filtro de Data Preciso
            val matchesDate = when {
                dateFilter == "30_dias" || dateFilter.isBlank() -> {
                    // Calcula com exatidão matemática se o romaneio está dentro dos últimos 30 dias!
                    rom.isWithinDays(30)
                }
                dateFilter.equals("todas", ignoreCase = true) -> {
                    // Exibe todos os romaneios cadastrados sem restrição de período
                    true
                }
                dateFilter == "hoje" || dateFilter.equals("Hoje", ignoreCase = true) || dateFilter == todayDmy -> {
                    // Filtro de Hoje: compara com a data de hoje
                    if (rom.createdAt.isNotBlank() && rom.createdAt.startsWith(todayIso)) {
                        true
                    } else if (rom.getDataFormatada() == todayDmy) {
                        true
                    } else {
                        rom.prazoEntrega.equals("Hoje", ignoreCase = true) || rom.isWithinDays(1)
                    }
                }
                else -> {
                    // Filtro com data formatada "dd/MM/yyyy"
                    val targetIso = try {
                        val parts = dateFilter.split("/")
                        if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else ""
                    } catch (_: Exception) {
                        ""
                    }

                    val romDmy = rom.getDataFormatada()
                    (targetIso.isNotBlank() && rom.createdAt.isNotBlank() && rom.createdAt.startsWith(targetIso)) ||
                        romDmy == dateFilter ||
                        (rom.createdAt.isNotBlank() && rom.createdAt.contains(dateFilter))
                }
            }

            // 3. Filtro de Busca por texto
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                rom.numeroRomaneio.lowercase().contains(q) ||
                    rom.clientePrincipal.lowercase().contains(q) ||
                    rom.getClienteFormatado().lowercase().contains(q) ||
                    rom.cidade.lowercase().contains(q) ||
                    rom.motoristaNome.lowercase().contains(q)
            }

            matchesTab && matchesDate && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))

            // Cabeçalho: "Meus romaneios" + Subtítulo + Botão de atualizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Meus romaneios",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (romaneios.isNotEmpty()) "Exibindo ${displayedRomaneios.size} de ${romaneios.size} romaneios disponíveis." else "Romaneios vinculados a você como motorista.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DarkSurfaceCardBorder, RoundedCornerShape(12.dp))
                        .background(DarkSurfaceCard)
                        .testTag("refresh_romaneios_btn")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = BrandBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra de Filtro de Data Responsiva e Elegante (Sem amontoar)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Campo de Indicação da Data / Filtro Ativo
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clickable { showDatePicker = true }
                        .testTag("date_picker_input_box"),
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (dateFilter.contains("/") || dateFilter == "hoje") BrandBlue.copy(alpha = 0.5f) else DarkSurfaceCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendário",
                                tint = if (dateFilter.contains("/") || dateFilter == "hoje") BrandBlue else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    dateFilter == "30_dias" || dateFilter.isBlank() -> "Período: Últimos 30 dias"
                                    dateFilter == "hoje" || dateFilter == todayDmy -> "Período: Hoje ($todayDmy)"
                                    dateFilter == "todas" -> "Período: Todas as datas (geral)"
                                    else -> "Data específica: $dateFilter"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (dateFilter.contains("/")) FontWeight.Bold else FontWeight.Medium,
                                color = if (dateFilter.contains("/")) BrandBlue else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (dateFilter != "30_dias" && dateFilter.isNotBlank()) {
                                IconButton(
                                    onClick = { dateFilter = "30_dias" },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("clear_date_filter_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Restaurar últimos 30 dias",
                                        tint = TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Abrir calendário",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Chips de Filtro Rápido (3 opções limpas, sem duplicação de data)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip: Últimos 30 dias
                    val is30Selected = dateFilter == "30_dias" || dateFilter.isBlank()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (is30Selected) BrandBlue.copy(alpha = 0.18f) else DarkSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (is30Selected) BrandBlue.copy(alpha = 0.5f) else DarkSurfaceCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { dateFilter = "30_dias" }
                            .testTag("filter_last_30_days_btn")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Últimos 30 dias",
                                fontSize = 11.sp,
                                fontWeight = if (is30Selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (is30Selected) BrandBlue else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Chip: Hoje
                    val isTodaySelected = dateFilter == "hoje" || dateFilter == todayDmy
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTodaySelected) BrandBlue.copy(alpha = 0.18f) else DarkSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTodaySelected) BrandBlue.copy(alpha = 0.5f) else DarkSurfaceCardBorder
                        ),
                        modifier = Modifier
                            .weight(0.7f)
                            .clickable { dateFilter = "hoje" }
                            .testTag("filter_today_btn")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hoje",
                                fontSize = 11.sp,
                                fontWeight = if (isTodaySelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTodaySelected) BrandBlue else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Chip: Todas as datas
                    val isAllSelected = dateFilter == "todas"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAllSelected) BrandBlue.copy(alpha = 0.18f) else DarkSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAllSelected) BrandBlue.copy(alpha = 0.5f) else DarkSurfaceCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { dateFilter = "todas" }
                            .testTag("filter_all_dates_btn")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Todas as datas",
                                fontSize = 11.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllSelected) BrandBlue else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Abas de Status: "Em aberto" vs "Todos"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Aba "Em aberto"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { selectedTab = "em_aberto" }
                        .testTag("tab_em_aberto"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == "em_aberto") BrandBlue.copy(alpha = 0.15f) else DarkSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedTab == "em_aberto") BrandBlue.copy(alpha = 0.4f) else DarkSurfaceCardBorder
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (totalEmAberto > 0) "Em aberto ($totalEmAberto)" else "Em aberto",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "em_aberto") BrandBlue else TextSecondary
                        )
                    }
                }

                // Aba "Todos"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { selectedTab = "todos" }
                        .testTag("tab_todos"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == "todos") BrandBlue.copy(alpha = 0.15f) else DarkSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedTab == "todos") BrandBlue.copy(alpha = 0.4f) else DarkSurfaceCardBorder
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (romaneios.isNotEmpty()) "Todos (${romaneios.size})" else "Todos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == "todos") BrandBlue else TextSecondary
                        )
                    }
                }
            }
        }

        // Estado vazio caso a filtragem por data não encontre registros
        if (displayedRomaneios.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .testTag("empty_romaneios_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (dateFilter.isNotBlank()) "Nenhum romaneio para $dateFilter" else "Nenhum romaneio encontrado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tente alterar a data ou selecionar 'Últimos 30 dias'.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                dateFilter = ""
                                selectedTab = "todos"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Text("Ver todos os romaneios", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Lista de Cards de Romaneios
            items(displayedRomaneios, key = { it.id }) { item ->
                RomaneioScreenshotCard(
                    romaneio = item,
                    onClick = { onSelectRomaneio(item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RomaneioScreenshotCard(
    romaneio: RomaneioEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDelivered = romaneio.status == "Entregue"
    val isPendingSync = romaneio.status == "Aguardando Sincronização"

    // Formatação da data do romaneio (ex: "06/10/2026" e "06/10/2026 às 10:04")
    val formattedDate = remember(romaneio.createdAt, romaneio.prazoEntrega) {
        romaneio.getDataFormatada()
    }
    val formattedDateWithTime = remember(romaneio.createdAt, romaneio.prazoEntrega) {
        romaneio.getDataComHoraFormatada()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("romaneio_card_${romaneio.numeroRomaneio}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Linha do topo: "ROMANEIO" + Badge de Status + Data do Romaneio + Seta ">"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ROMANEIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    // Badge de Status (Criado em âmbar / Entregue em verde)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isDelivered -> StatusEmeraldBg
                            isPendingSync -> Color(0xFF78350F)
                            else -> StatusAmberBg
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                isDelivered -> StatusEmeraldText.copy(alpha = 0.4f)
                                isPendingSync -> StatusAmberText.copy(alpha = 0.4f)
                                else -> StatusAmberBorder
                            }
                        )
                    ) {
                        Text(
                            text = romaneio.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isDelivered -> StatusEmeraldText
                                isPendingSync -> StatusAmberText
                                else -> StatusAmberText
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (formattedDate.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandBlue.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Data do Romaneio",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formattedDate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Abrir",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Número do romaneio grande e em negrito (ex: #2359)
            Text(
                text = "#${romaneio.numeroRomaneio}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Nome do cliente formatado e organizado
            Text(
                text = romaneio.getClienteFormatado(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Cidade / UF (ex: IMPERATRIZ / MA)
            Text(
                text = "${romaneio.cidade} / ${romaneio.uf}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rodapé do card: Quantidade de NFs, Volumes e Data/Hora do Romaneio
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${romaneio.quantidadeNfs} NF${if (romaneio.quantidadeNfs > 1) "s" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${romaneio.quantidadeVolumes} vol.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }

                if (formattedDateWithTime.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formattedDateWithTime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
