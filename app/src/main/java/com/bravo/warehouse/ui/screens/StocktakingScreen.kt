package com.bravo.warehouse.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch

private val StocktakingNavy = Color(0xFF082F68)
private val StocktakingRed = Color(0xFFE5242A)
private val StocktakingBackground = Color(0xFFF3F6FC)
private val StocktakingText = Color(0xFF102B50)
private val StocktakingMuted = Color(0xFF526783)
private val StocktakingBorder = Color(0xFFB7C8DF)
private val StocktakingGreen = Color(0xFF16834A)
private val StocktakingOrange = Color(0xFFB86B00)

@Composable
fun StocktakingScreen(
    itemRepository: ItemRepository,
    warehouseRepository: WarehouseRepository,
    movementRepository: StockMovementRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val preferences = remember(context) {
        context.getSharedPreferences(
            "bravo_settings",
            Context.MODE_PRIVATE
        )
    }

    val defaultWarehouseId = remember {
        preferences.getLong("default_warehouse_id", -1L)
    }

    val scope = rememberCoroutineScope()

    val items by itemRepository.getAllItems()
        .collectAsState(initial = emptyList())

    val warehouses by warehouseRepository.getAllWarehouses()
        .collectAsState(initial = emptyList())

    var selectedItemId by remember { mutableStateOf<Long?>(null) }
    var selectedWarehouseId by remember { mutableStateOf<Long?>(null) }
    var actualQuantity by remember { mutableStateOf("") }
    var systemBalance by remember { mutableStateOf(0.0) }

    var itemMenu by remember { mutableStateOf(false) }
    var warehouseMenu by remember { mutableStateOf(false) }

    var message by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val selectedItem = items.firstOrNull {
        it.id == selectedItemId
    }

    val selectedWarehouse = warehouses.firstOrNull {
        it.id == selectedWarehouseId
    }

    LaunchedEffect(warehouses, defaultWarehouseId) {
        if (warehouses.isNotEmpty()) {
            val currentSelectionExists = warehouses.any {
                it.id == selectedWarehouseId
            }

            if (!currentSelectionExists) {
                selectedWarehouseId = warehouses
                    .firstOrNull { it.id == defaultWarehouseId }
                    ?.id
            }
        }
    }

    val actual = actualQuantity.toDoubleOrNull()

    val difference = if (actual != null && actual.isFinite()) {
        actual - systemBalance
    } else {
        0.0
    }

    LaunchedEffect(selectedItemId, selectedWarehouseId) {
        actualQuantity = ""
        systemBalance = 0.0
        message = ""

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
                systemBalance = balance
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // رأس الشاشة
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = StocktakingNavy
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        "← رجوع",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "الجرد الفعلي",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "مطابقة الكميات الفعلية مع أرصدة النظام وتسوية الفروقات",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }

        // اختيار الصنف والمخزن
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, StocktakingBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "بيانات الجرد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StocktakingNavy
                )

                HorizontalDivider(color = StocktakingBorder)

                Text(
                    text = "الصنف",
                    color = StocktakingText,
                    fontWeight = FontWeight.SemiBold
                )

                Box {
                    OutlinedTextField(
                        value = selectedItem?.let {
                            "${it.itemCode} - ${it.itemName}"
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text("اضغط لاختيار الصنف")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = stocktakingFieldColors()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { itemMenu = true }
                    )

                    DropdownMenu(
                        expanded = itemMenu,
                        onDismissRequest = { itemMenu = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items.filter { it.isActive }.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${item.itemCode} - ${item.itemName}"
                                    )
                                },
                                onClick = {
                                    selectedItemId = item.id
                                    actualQuantity = ""
                                    systemBalance = 0.0
                                    itemMenu = false
                                    message = ""
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "المخزن",
                    color = StocktakingText,
                    fontWeight = FontWeight.SemiBold
                )

                Box {
                    OutlinedTextField(
                        value = selectedWarehouse?.warehouseName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text("اضغط لاختيار المخزن")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = stocktakingFieldColors()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { warehouseMenu = true }
                    )

                    DropdownMenu(
                        expanded = warehouseMenu,
                        onDismissRequest = { warehouseMenu = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        warehouses.forEach { warehouse ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        warehouse.warehouseName +
                                            if (warehouse.id == defaultWarehouseId) {
                                                " (افتراضي)"
                                            } else {
                                                ""
                                            }
                                    )
                                },
                                onClick = {
                                    selectedWarehouseId = warehouse.id
                                    actualQuantity = ""
                                    systemBalance = 0.0
                                    warehouseMenu = false
                                    message = ""
                                }
                            )
                        }
                    }
                }
            }
        }

        // رصيد النظام
        if (selectedItem != null && selectedWarehouse != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StocktakingNavy
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "رصيد النظام الحالي",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "$systemBalance ${selectedItem.unit}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "الصنف: ${selectedItem.itemName}",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // الكمية الفعلية
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, StocktakingBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "العد الفعلي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StocktakingNavy
                )

                OutlinedTextField(
                    value = actualQuantity,
                    onValueChange = {
                        actualQuantity = it
                        message = ""
                    },
                    label = {
                        Text("الكمية الفعلية بعد العد")
                    },
                    placeholder = {
                        Text("أدخل الكمية الموجودة فعلياً")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = stocktakingFieldColors()
                )

                Text(
                    text = "يمكن إدخال صفر إذا كان الصنف غير موجود في المخزن.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StocktakingMuted
                )
            }
        }

        // نتيجة الجرد
        if (
            selectedItem != null &&
            selectedWarehouse != null &&
            actual != null &&
            actual.isFinite() &&
            actual >= 0
        ) {
            val resultColor = when {
                difference > 0 -> StocktakingGreen
                difference < 0 -> StocktakingRed
                else -> StocktakingNavy
            }

            val resultBackground = when {
                difference > 0 -> Color(0xFFE8F5EC)
                difference < 0 -> Color(0xFFFFEEEE)
                else -> Color(0xFFEAF1FA)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = resultBackground
                ),
                border = BorderStroke(1.dp, resultColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "نتيجة الجرد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StocktakingText
                    )

                    HorizontalDivider(
                        color = resultColor.copy(alpha = 0.3f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "الكمية الفعلية",
                            color = StocktakingMuted
                        )
                        Text(
                            "$actual ${selectedItem.unit}",
                            color = StocktakingText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "رصيد النظام",
                            color = StocktakingMuted
                        )
                        Text(
                            "$systemBalance ${selectedItem.unit}",
                            color = StocktakingText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider(
                        color = resultColor.copy(alpha = 0.3f)
                    )

                    Text(
                        text = when {
                            difference > 0 ->
                                "زيادة: $difference ${selectedItem.unit}"

                            difference < 0 ->
                                "عجز: ${-difference} ${selectedItem.unit}"

                            else ->
                                "الجرد مطابق لرصيد النظام"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = resultColor
                    )
                }
            }
        }

        // اعتماد الجرد
        Button(
            onClick = {
                val itemId = selectedItemId
                val warehouseId = selectedWarehouseId

                if (itemId == null) {
                    message = "يرجى اختيار الصنف"
                    return@Button
                }

                if (warehouseId == null) {
                    message = "يرجى اختيار المخزن"
                    return@Button
                }

                if (
                    actual == null ||
                    !actual.isFinite() ||
                    actual < 0
                ) {
                    message = "أدخل كمية فعلية صحيحة، ويمكن أن تكون صفراً"
                    return@Button
                }

                if (isSaving) return@Button

                isSaving = true
                message = ""

                scope.launch {
                    try {
                        val currentSystemBalance =
                            movementRepository.getCurrentBalance(
                                itemId,
                                warehouseId
                            )

                        if (
                            selectedItemId != itemId ||
                            selectedWarehouseId != warehouseId
                        ) {
                            return@launch
                        }

                        val actualDifference =
                            actual - currentSystemBalance

                        if (
                            kotlin.math.abs(actualDifference) < 0.000001
                        ) {
                            systemBalance = currentSystemBalance
                            message =
                                "الجرد مطابق. لا توجد فروقات لتسويتها"
                        } else {
                            val adjustmentType =
                                if (actualDifference > 0) {
                                    "ADJUSTMENT_IN"
                                } else {
                                    "ADJUSTMENT_OUT"
                                }

                            val movement = StockMovementEntity(
                                documentNumber =
                                    "STK-${System.currentTimeMillis()}",
                                movementType = adjustmentType,
                                itemId = itemId,
                                warehouseId = warehouseId,
                                quantity = kotlin.math.abs(actualDifference),
                                notes = "تسوية جرد. " +
                                    "رصيد النظام: $currentSystemBalance، " +
                                    "الكمية الفعلية: $actual، " +
                                    "الفرق: $actualDifference",
                                isApproved = false
                            )

                            movementRepository.insertAndApproveMovement(
                                movement
                            )

                            val newBalance =
                                movementRepository.getCurrentBalance(
                                    itemId,
                                    warehouseId
                                )

                            if (
                                selectedItemId == itemId &&
                                selectedWarehouseId == warehouseId
                            ) {
                                systemBalance = newBalance
                                actualQuantity = ""

                                message =
                                    if (actualDifference > 0) {
                                        "تم اعتماد الجرد وتسوية الزيادة بنجاح"
                                    } else {
                                        "تم اعتماد الجرد وتسوية العجز بنجاح"
                                    }
                            }
                        }
                    } catch (e: Exception) {
                        message = when {
                            e.message?.contains(
                                "Insufficient stock"
                            ) == true ->
                                "الرصيد غير كافٍ لتنفيذ تسوية العجز"

                            else ->
                                "حدث خطأ أثناء تنفيذ الجرد: " +
                                    (e.message ?: "خطأ غير معروف")
                        }
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
                containerColor = StocktakingRed,
                contentColor = Color.White,
                disabledContainerColor = StocktakingRed.copy(alpha = 0.5f)
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("جارٍ اعتماد الجرد...")
            } else {
                Text(
                    text = "اعتماد الجرد وتسوية الفرق",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // رسالة النتيجة
        if (message.isNotEmpty()) {
            val success = message.startsWith("تم") ||
                message.startsWith("الجرد مطابق")

            val messageColor = if (success) {
                StocktakingGreen
            } else {
                StocktakingRed
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (success) {
                        Color(0xFFE8F5EC)
                    } else {
                        Color(0xFFFFEEEE)
                    }
                ),
                border = BorderStroke(1.dp, messageColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        color = messageColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun stocktakingFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = StocktakingNavy,
        unfocusedBorderColor = StocktakingBorder,
        focusedLabelColor = StocktakingNavy,
        cursorColor = StocktakingNavy,
        focusedTextColor = StocktakingText,
        unfocusedTextColor = StocktakingText
    )
