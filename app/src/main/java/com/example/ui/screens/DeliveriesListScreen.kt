package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DeliveryItem
import com.example.data.model.DeliveryStatus
import com.example.ui.components.DeliveryCard
import com.example.ui.components.NetworkBanner

@Composable
fun DeliveriesListScreen(
    deliveries: List<DeliveryItem>,
    isOnline: Boolean,
    isSyncing: Boolean,
    pendingQueueCount: Int,
    onToggleNetwork: (Boolean) -> Unit,
    onTriggerSync: () -> Unit,
    onStartTransit: (String) -> Unit,
    onConfirmDelivery: (String) -> Unit,
    onViewReceipt: (String) -> Unit,
    onSelectDelivery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<DeliveryStatus?>(null) }

    val filteredDeliveries = remember(deliveries, searchQuery, selectedFilter) {
        deliveries.filter { item ->
            val matchesFilter = selectedFilter == null || item.status == selectedFilter
            val matchesSearch = searchQuery.isBlank() ||
                    item.recipientName.contains(searchQuery, ignoreCase = true) ||
                    item.address.contains(searchQuery, ignoreCase = true) ||
                    item.trackingCode.contains(searchQuery, ignoreCase = true) ||
                    item.neighborhood.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = "Bicicletão Logo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Bicicletão Logística",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rota Urbana Sustentável",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Network Status Banner
        NetworkBanner(
            isOnline = isOnline,
            isSyncing = isSyncing,
            pendingQueueCount = pendingQueueCount,
            onToggleNetwork = onToggleNetwork,
            onTriggerSync = onTriggerSync,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_deliveries_input"),
            placeholder = { Text("Buscar por nome, endereço ou código...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Todas (${deliveries.size})") },
                    modifier = Modifier.testTag("filter_all_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                val count = deliveries.count { it.status == DeliveryStatus.PENDING }
                FilterChip(
                    selected = selectedFilter == DeliveryStatus.PENDING,
                    onClick = {
                        selectedFilter = if (selectedFilter == DeliveryStatus.PENDING) null else DeliveryStatus.PENDING
                    },
                    label = { Text("Pendentes ($count)") },
                    modifier = Modifier.testTag("filter_pending_chip")
                )
            }
            item {
                val count = deliveries.count { it.status == DeliveryStatus.IN_TRANSIT }
                FilterChip(
                    selected = selectedFilter == DeliveryStatus.IN_TRANSIT,
                    onClick = {
                        selectedFilter = if (selectedFilter == DeliveryStatus.IN_TRANSIT) null else DeliveryStatus.IN_TRANSIT
                    },
                    label = { Text("Em Rota ($count)") },
                    modifier = Modifier.testTag("filter_transit_chip")
                )
            }
            item {
                val count = deliveries.count { it.status == DeliveryStatus.DELIVERED }
                FilterChip(
                    selected = selectedFilter == DeliveryStatus.DELIVERED,
                    onClick = {
                        selectedFilter = if (selectedFilter == DeliveryStatus.DELIVERED) null else DeliveryStatus.DELIVERED
                    },
                    label = { Text("Entregues ($count)") },
                    modifier = Modifier.testTag("filter_delivered_chip")
                )
            }
            item {
                val count = deliveries.count { it.status == DeliveryStatus.FAILED }
                FilterChip(
                    selected = selectedFilter == DeliveryStatus.FAILED,
                    onClick = {
                        selectedFilter = if (selectedFilter == DeliveryStatus.FAILED) null else DeliveryStatus.FAILED
                    },
                    label = { Text("Insucesso ($count)") },
                    modifier = Modifier.testTag("filter_failed_chip")
                )
            }
        }

        // Deliveries List
        if (filteredDeliveries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma entrega encontrada",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Verifique os filtros selecionados ou digite outro termo na busca.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("deliveries_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDeliveries, key = { it.id }) { item ->
                    DeliveryCard(
                        delivery = item,
                        onStartTransit = { onStartTransit(item.id) },
                        onConfirmDelivery = { onConfirmDelivery(item.id) },
                        onViewReceipt = { onViewReceipt(item.id) },
                        onCardClick = { onSelectDelivery(item.id) }
                    )
                }
            }
        }
    }
}
