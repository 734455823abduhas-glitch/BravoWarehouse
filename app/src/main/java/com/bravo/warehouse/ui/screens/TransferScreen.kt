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
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.entity.WarehouseEntity
import com.bravo.warehouse.data.repository.TransferRepository
import kotlinx.coroutines.launch

private val TransferNavy = Color(0xFF082F68)
private val TransferRed = Color(0xFFE5242A)
private val TransferBackground = Color(0xFFF3F6FC)
private val TransferText = Color(0xFF102B50)
private val TransferMuted = Color(0xFF526783)
private val TransferBorder = Color(0xFFB7C8DF)
private val TransferGreen = Color(0xFF16834A)

@Composable
fun TransferScreen(
    items: List<ItemEntity>,
    warehouses: List<WarehouseEntity>,
    transferRepository: TransferRepository
) {
    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences(
            "bravo_settings",
            Context.MODE_PRIVATE
        )
    }

    val defaultWarehouseId = remember {
        preferences.getLong("default_warehouse_id", 0L)
            .takeIf { it > 0L }
    }

    var selectedItem by remember {
        mutableStateOf<ItemEntity?>(null)
    }

    var source by remember {
        mutableStateOf<WarehouseEntity?>(null)
    }

    var destination by remember {
        mutableStateOf<WarehouseEntity?>(null)
    }

    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var itemMenu by remember { mutableStateOf(false) }
    var sourceMenu by remember { mutableStateOf(false) }
    var destinationMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(warehouses, defaultWarehouseId) {
        val currentSourceExists = warehouses.any {
            it.id == source?.id
        }

        if (source == null || !currentSourceExists) {
            source = warehouses.firstOrNull {
                it.id == defaultWarehouseId
            }
        }

        if (destination?.id == source?.id) {
            destination = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = TransferNavy
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "التحويل بين المخازن",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "نقل الأصناف والكميات من مخزن إلى آخر",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.25f)
                )

                Text(
                    text = "حدد الصنف ومخزن المصدر والمخزن المستلم ثم أدخل الكمية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, TransferBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "بيانات التحويل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TransferNavy
                )

                HorizontalDivider(color = TransferBorder)

                Text(
                    text = "الصنف",
                    color = TransferText,
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
                        colors = transferFieldColors()
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
                                    selectedItem = item
                                    itemMenu = false
                                    message = ""
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "مخزن المصدر",
                    color = TransferText,
                    fontWeight = FontWeight.SemiBold
                )

                Box {
                    OutlinedTextField(
                        value = source?.warehouseName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text("اختر المخزن الذي سيتم الصرف منه")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = transferFieldColors()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { sourceMenu = true }
                    )

                    DropdownMenu(
                        expanded = sourceMenu,
                        onDismissRequest = { sourceMenu = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        warehouses.forEach { warehouse ->
                            DropdownMenuItem(
                                text = {
                                    Text(warehouse.warehouseName)
                                },
                                onClick = {
                                    source = warehouse

                                    if (destination?.id == warehouse.id) {
                                        destination = null
                                    }

                                    sourceMenu = false
                                    message = ""
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "المخزن المستلم",
                    color = TransferText,
                    fontWeight = FontWeight.SemiBold
                )

                Box {
                    OutlinedTextField(
                        value = destination?.warehouseName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text("اختر المخزن الذي سيستلم الكمية")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = transferFieldColors()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { destinationMenu = true }
                    )

                    DropdownMenu(
                        expanded = destinationMenu,
                        onDismissRequest = { destinationMenu = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        warehouses
                            .filter { it.id != source?.id }
                            .forEach { warehouse ->
                                DropdownMenuItem(
                                    text = {
                                        Text(warehouse.warehouseName)
                                    },
                                    onClick = {
                                        destination = warehouse
                                        destinationMenu = false
                                        message = ""
                                    }
                                )
                            }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, TransferBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "الكمية والملاحظات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TransferNavy
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        message = ""
                    },
                    label = { Text("الكمية") },
                    placeholder = { Text("أدخل الكمية المراد تحويلها") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = transferFieldColors()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    placeholder = {
                        Text("ملاحظات إضافية (اختياري)")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = transferFieldColors()
                )
            }
        }

        Button(
            onClick = {
                val item = selectedItem
                val from = source
                val to = destination
                val amount = quantity.toDoubleOrNull()

                if (item == null) {
                    message = "يرجى اختيار الصنف"
                    return@Button
                }

                if (from == null) {
                    message = "يرجى اختيار مخزن المصدر"
                    return@Button
                }

                if (to == null) {
                    message = "يرجى اختيار المخزن المستلم"
                    return@Button
                }

                if (from.id == to.id) {
                    message = "لا يمكن التحويل إلى المخزن نفسه"
                    return@Button
                }

                if (
                    amount == null ||
                    !amount.isFinite() ||
                    amount <= 0.0
                ) {
                    message = "يرجى إدخال كمية صحيحة أكبر من صفر"
                    return@Button
                }

                if (loading) return@Button

                scope.launch {
                    loading = true
                    message = ""

                    try {
                        val transferNumber =
                            "TR-${System.currentTimeMillis()}"

                        transferRepository.createTransfer(
                            transferNumber = transferNumber,
                            itemId = item.id,
                            sourceWarehouseId = from.id,
                            destinationWarehouseId = to.id,
                            quantity = amount,
                            notes = notes.trim()
                        )

                        message =
                            "تم التحويل بنجاح. رقم المستند: $transferNumber"

                        selectedItem = null
                        destination = null
                        quantity = ""
                        notes = ""

                    } catch (e: Exception) {
                        message = when {
                            e.message?.contains(
                                "Insufficient stock",
                                ignoreCase = true
                            ) == true ->
                                "الرصيد غير كافٍ لتنفيذ التحويل"

                            else ->
                                "فشل تنفيذ التحويل: ${
                                    e.message ?: "خطأ غير معروف"
                                }"
                        }
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TransferRed,
                contentColor = Color.White,
                disabledContainerColor = TransferRed.copy(alpha = 0.5f)
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(23.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("جارٍ تنفيذ التحويل...")
            } else {
                Text(
                    text = "تنفيذ التحويل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (message.isNotEmpty()) {
            val success = message.startsWith("تم")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (success)
                        Color(0xFFE8F5EC)
                    else
                        Color(0xFFFFEEEE)
                ),
                border = BorderStroke(
                    1.dp,
                    if (success) TransferGreen else TransferRed
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        color = if (success)
                            TransferGreen
                        else
                            TransferRed,
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
private fun transferFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TransferNavy,
        unfocusedBorderColor = TransferBorder,
        focusedLabelColor = TransferNavy,
        cursorColor = TransferNavy,
        focusedTextColor = TransferText,
        unfocusedTextColor = TransferText
    )
