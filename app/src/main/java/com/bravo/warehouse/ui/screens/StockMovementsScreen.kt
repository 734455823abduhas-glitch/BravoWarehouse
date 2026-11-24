package com.bravo.warehouse.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bravo.warehouse.data.entity.StockMovementEntity
import com.bravo.warehouse.data.repository.ItemRepository
import com.bravo.warehouse.data.repository.WarehouseRepository
import com.bravo.warehouse.data.repository.StockMovementRepository
import com.bravo.warehouse.util.DateTimeUtils
import kotlinx.coroutines.launch

private val MovementNavy = Color(0xFF082F68)
private val MovementRed = Color(0xFFE5242A)
private val MovementBackground = Color(0xFFF3F6FC)
private val MovementText = Color(0xFF102B50)
private val MovementMuted = Color(0xFF526783)
private val MovementBorder = Color(0xFFB7C8DF)
private val MovementGreen = Color(0xFF18834B)

@Composable
fun StockMovementsScreen(
    itemRepository: ItemRepository,
    warehouseRepository: WarehouseRepository,
    movementRepository: StockMovementRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences(
            "bravo_settings",
            Context.MODE_PRIVATE
        )
    }

    val itemList by itemRepository.getAllItems()
        .collectAsState(initial = emptyList())

    val warehouseList by warehouseRepository.getAllWarehouses()
        .collectAsState(initial = emptyList())

    val movementList by movementRepository.getAllMovements()
        .collectAsState(initial = emptyList())

    var movementType by remember { mutableStateOf("IN") }
    var selectedItemId by remember { mutableStateOf<Long?>(null) }

    var selectedWarehouseId by remember {
        mutableStateOf(
            preferences.getLong("default_warehouse_id", 0L)
                .takeIf { it > 0L }
        )
    }

    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var itemMenu by remember { mutableStateOf(false) }
    var warehouseMenu by remember { mutableStateOf(false) }

    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var currentBalance by remember { mutableStateOf(0.0) }

    val selectedItem = itemList.firstOrNull {
        it.id == selectedItemId
    }

    val selectedWarehouse = warehouseList.firstOrNull {
        it.id == selectedWarehouseId
    }

    val movementTitle = when (movementType) {
        "IN" -> "وارد"
        "OUT" -> "منصرف"
        "RETURN_IN" -> "مرتجع للمخزن"
        "RETURN_OUT" -> "مرتجع من المخزن"
        else -> "حركة مخزون"
    }

    LaunchedEffect(warehouseList) {
        if (warehouseList.isNotEmpty()) {
            val defaultId = preferences.getLong(
                "default_warehouse_id",
                0L
            )

            if (
                selectedWarehouseId == null ||
                warehouseList.none { it.id == selectedWarehouseId }
            ) {
                selectedWarehouseId =
                    warehouseList.firstOrNull { it.id == defaultId }?.id
                        ?: warehouseList.firstOrNull()?.id
            }
        }
    }

    LaunchedEffect(selectedItemId, selectedWarehouseId) {
        currentBalance = 0.0

        val itemId = selectedItemId
        val warehouseId = selectedWarehouseId

        if (itemId != null && warehouseId != null) {
            val balance = movementRepository.getCurrentBalance(
                itemId,
                warehouseId
            )

            if (
                selectedItemId == itemId &&
                selectedWarehouseId == warehouseId
            ) {
                currentBalance = balance
            }
        }
    }

    Scaffold(
        containerColor = MovementBackground,
        snackbarHost = {
            SnackbarHost(hostState = remember { SnackbarHostState() })
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MovementNavy
                        )
                    ) {
                        Text("رجوع")
                    }

                    Text(
                        text = "حركة المخزون",
                        color = MovementNavy,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "نوع الحركة",
                            color = MovementNavy,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "IN" to "وارد",
                                "OUT" to "منصرف"
                            ).forEach { (type, title) ->
                                Button(
                                    onClick = {
                                        movementType = type
                                        message = ""
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (movementType == type)
                                            MovementNavy else MovementBackground,
                                        contentColor = if (movementType == type)
                                            Color.White else MovementNavy
                                    )
                                ) {
                                    Text(title)
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "RETURN_IN" to "مرتجع للمخزن",
                                "RETURN_OUT" to "مرتجع من المخزن"
                            ).forEach { (type, title) ->
                                Button(
                                    onClick = {
                                        movementType = type
                                        message = ""
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (movementType == type)
                                            MovementNavy else MovementBackground,
                                        contentColor = if (movementType == type)
                                            Color.White else MovementNavy
                                    )
                                ) {
                                    Text(title)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Box {
                    OutlinedTextField(
                        value = selectedItem?.let {
                            "${it.itemCode} - ${it.itemName}"
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSaving,
                        label = { Text("اختر الصنف") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = movementFieldColors()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = !isSaving) {
                                itemMenu = true
                            }
                    )

                    DropdownMenu(
                        expanded = itemMenu,
                        onDismissRequest = { itemMenu = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        itemList.filter { it.isActive }.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${item.itemCode} - ${item.itemName}",
                                        color = MovementText
                                    )
                                },
                                enabled = !isSaving,
                                onClick = {
                                    selectedItemId = item.id
                                    itemMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Box {
                    OutlinedTextField(
                        value = selectedWarehouse?.warehouseName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSaving,
                        label = { Text("اختر المخزن") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = movementFieldColors()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = !isSaving) {
                                warehouseMenu = true
                            }
                    )

                    DropdownMenu(
                        expanded = warehouseMenu,
                        onDismissRequest = { warehouseMenu = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        warehouseList.forEach { warehouse ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        warehouse.warehouseName,
                                        color = MovementText
                                    )
                                },
                                enabled = !isSaving,
                                onClick = {
                                    selectedWarehouseId = warehouse.id
                                    warehouseMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                if (selectedItem != null && selectedWarehouse != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MovementNavy
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "الرصيد الحالي",
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Text(
                                "$currentBalance ${selectedItem.unit}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    enabled = !isSaving,
                    label = { Text("الكمية") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = movementFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    enabled = !isSaving,
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = movementFieldColors()
                )
            }

            item {
                Button(
                    onClick = {
                        val qty = quantity.trim().toDoubleOrNull()
                        val itemId = selectedItemId
                        val warehouseId = selectedWarehouseId
                        val typeToSave = movementType
                        val titleToSave = movementTitle
                        val notesToSave = notes.trim()

                        if (itemId == null) {
                            message = "يرجى اختيار الصنف"
                            messageIsError = true
                            return@Button
                        }

                        if (warehouseId == null) {
                            message = "يرجى اختيار المخزن"
                            messageIsError = true
                            return@Button
                        }

                        if (qty == null || !qty.isFinite() || qty <= 0) {
                            message = "يرجى إدخال كمية صحيحة أكبر من صفر"
                            messageIsError = true
                            return@Button
                        }

                        if (isSaving) return@Button

                        isSaving = true
                        message = ""

                        scope.launch {
                            try {
                                val movement = StockMovementEntity(
                                    documentNumber = "MOV-${System.currentTimeMillis()}",
                                    movementType = typeToSave,
                                    itemId = itemId,
                                    warehouseId = warehouseId,
                                    quantity = qty,
                                    notes = notesToSave,
                                    isApproved = false
                                )

                                movementRepository.insertAndApproveMovement(
                                    movement
                                )

                                message = "تم حفظ واعتماد $titleToSave بنجاح"
                                messageIsError = false

                                quantity = ""
                                notes = ""

                                currentBalance =
                                    movementRepository.getCurrentBalance(
                                        itemId,
                                        warehouseId
                                    )

                            } catch (e: Exception) {
                                val insufficientStock =
                                    typeToSave == "OUT" ||
                                    typeToSave == "RETURN_OUT"

                                message = if (
                                    insufficientStock &&
                                    e.message?.contains(
                                        "Movement approval failed",
                                        ignoreCase = true
                                    ) == true
                                ) {
                                    "الرصيد غير كافٍ لتنفيذ هذه الحركة"
                                } else {
                                    "فشل حفظ الحركة: ${e.message ?: "خطأ غير معروف"}"
                                }

                                messageIsError = true
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MovementNavy,
                        contentColor = Color.White
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            "حفظ واعتماد $movementTitle",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (message.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (messageIsError)
                                Color(0xFFFFEEEE) else Color(0xFFE8F5EC)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            color = if (messageIsError)
                                MovementRed else MovementGreen
                        )
                    }
                }
            }

            item {
                HorizontalDivider(
                    color = MovementBorder,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "سجل الحركات",
                    color = MovementNavy,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (movementList.isEmpty()) {
                item {
                    Text(
                        "لا توجد حركات مسجلة حتى الآن",
                        color = MovementMuted
                    )
                }
            } else {
                items(
                    items = movementList,
                    key = { it.id }
                ) { movement ->

                    val item = itemList.firstOrNull {
                        it.id == movement.itemId
                    }

                    val warehouse = warehouseList.firstOrNull {
                        it.id == movement.warehouseId
                    }

                    val typeTitle = when (movement.movementType) {
                        "IN" -> "وارد"
                        "OUT" -> "منصرف"
                        "RETURN_IN" -> "مرتجع للمخزن"
                        "RETURN_OUT" -> "مرتجع من المخزن"
                        "ADJUSTMENT_IN" -> "تسوية بالزيادة"
                        "ADJUSTMENT_OUT" -> "تسوية بالنقص"
                        "TRANSFER" -> "تحويل"
                        else -> "حركة أخرى"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                typeTitle,
                                color = MovementNavy,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            HorizontalDivider(color = MovementBorder)

                            Text(
                                "رقم الحركة: ${movement.documentNumber}",
                                color = MovementText
                            )

                            Text(
                                "الصنف: ${item?.itemName ?: movement.itemId}",
                                color = MovementText
                            )

                            Text(
                                "المخزن: ${warehouse?.warehouseName ?: movement.warehouseId}",
                                color = MovementText
                            )

                            Text(
                                "الكمية: ${movement.quantity}",
                                color = MovementText,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "التاريخ: ${
                                    DateTimeUtils.formatDateTime(
                                        context,
                                        movement.movementDate
                                    )
                                }",
                                color = MovementMuted
                            )

                            if (movement.notes.isNotBlank()) {
                                Text(
                                    "ملاحظات: ${movement.notes}",
                                    color = MovementMuted
                                )
                            }

                            Text(
                                text = if (movement.isApproved)
                                    "الحالة: معتمدة"
                                else
                                    "الحالة: غير معتمدة",
                                color = if (movement.isApproved)
                                    MovementGreen else MovementRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun movementFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MovementNavy,
        unfocusedBorderColor = MovementBorder,
        focusedLabelColor = MovementNavy,
        unfocusedLabelColor = MovementMuted,
        focusedTextColor = MovementText,
        unfocusedTextColor = MovementText,
        cursorColor = MovementRed
    )
