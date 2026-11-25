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
import com.bravo.warehouse.data.entity.TransferEntity
import com.bravo.warehouse.data.entity.WarehouseEntity
import com.bravo.warehouse.data.repository.TransferLine
import com.bravo.warehouse.data.repository.TransferRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TransferNavy = Color(0xFF082F68)
private val TransferRed = Color(0xFFE5242A)
private val TransferText = Color(0xFF102B50)
private val TransferBorder = Color(0xFFB7C8DF)
private val TransferGreen = Color(0xFF16834A)

private data class TransferDraftLine(
    val item: ItemEntity,
    val quantity: Double
)

@Composable
fun TransferScreen(
    items: List<ItemEntity>,
    warehouses: List<WarehouseEntity>,
    transferRepository: TransferRepository
) {
    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences("bravo_settings", Context.MODE_PRIVATE)
    }

    val defaultWarehouseId = remember {
        preferences.getLong("default_warehouse_id", 0L)
            .takeIf { it > 0L }
    }

    var source by remember { mutableStateOf<WarehouseEntity?>(null) }
    var destination by remember { mutableStateOf<WarehouseEntity?>(null) }

    var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var draftLines by remember {
        mutableStateOf(listOf<TransferDraftLine>())
    }

    var itemMenu by remember { mutableStateOf(false) }
    var sourceMenu by remember { mutableStateOf(false) }
    var destinationMenu by remember { mutableStateOf(false) }

    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showApprovalDialog by remember { mutableStateOf(false) }

    var editingIndex by remember { mutableIntStateOf(-1) }
    var editingQuantity by remember { mutableStateOf("") }

    var selectedDocument by remember {
        mutableStateOf<List<TransferEntity>>(emptyList())
    }

    var selectedDocumentNumber by remember {
        mutableStateOf("")
    }

    val scope = rememberCoroutineScope()

    val transfers by transferRepository
        .getAllTransfers()
        .collectAsState(initial = emptyList())

    LaunchedEffect(warehouses, defaultWarehouseId) {
        if (warehouses.none { it.id == source?.id }) {
            source = warehouses.firstOrNull {
                it.id == defaultWarehouseId && it.isActive
            } ?: warehouses.firstOrNull { it.isActive }
        }

        if (
            destination?.id == source?.id ||
            warehouses.none { it.id == destination?.id }
        ) {
            destination = null
        }
    }

    val totalQuantity = draftLines.sumOf { it.quantity }

    val groupedTransfers = remember(transfers) {
        transfers
            .filter { it.isApproved }
            .groupBy { it.transferNumber }
            .values
            .sortedByDescending { group ->
                group.maxOfOrNull { it.transferDate } ?: 0L
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
                    "التحويل بين المخازن",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    "إنشاء وإدارة مستندات التحويل متعددة الأصناف",
                    color = Color.White.copy(alpha = 0.9f)
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.3f)
                )

                Text(
                    "أضف الأصناف والكميات ثم اعتمد التحويل دفعة واحدة.",
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // بيانات المخازن
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
                    "بيانات التحويل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TransferNavy
                )

                Text("مخزن المصدر", color = TransferText)

                Box {
                    OutlinedTextField(
                        value = source?.warehouseName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text("اختر مخزن المصدر")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable { sourceMenu = true }
                    )

                    DropdownMenu(
                        expanded = sourceMenu,
                        onDismissRequest = { sourceMenu = false }
                    ) {
                        warehouses
                            .filter { it.isActive }
                            .forEach { warehouse ->
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

                Text("المخزن المستلم", color = TransferText)

                Box {
                    OutlinedTextField(
                        value = destination?.warehouseName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text("اختر المخزن المستلم")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable { destinationMenu = true }
                    )

                    DropdownMenu(
                        expanded = destinationMenu,
                        onDismissRequest = {
                            destinationMenu = false
                        }
                    ) {
                        warehouses
                            .filter {
                                it.isActive && it.id != source?.id
                            }
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

        // إضافة الأصناف
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "إضافة الأصناف",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TransferNavy
                )

                Box {
                    OutlinedTextField(
                        value = selectedItem?.let {
                            "${it.itemCode} - ${it.itemName}"
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text("اختر الصنف")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable { itemMenu = true }
                    )

                    DropdownMenu(
                        expanded = itemMenu,
                        onDismissRequest = { itemMenu = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items
                            .filter { it.isActive }
                            .forEach { item ->
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

                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        message = ""
                    },
                    label = { Text("الكمية") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedButton(
                    onClick = {
                        val item = selectedItem
                        val amount = quantity.toDoubleOrNull()

                        when {
                            item == null ->
                                message = "يرجى اختيار الصنف"

                            amount == null ||
                                !amount.isFinite() ||
                                amount <= 0.0 ->
                                message = "أدخل كمية صحيحة أكبر من صفر"

                            draftLines.any {
                                it.item.id == item.id
                            } ->
                                message = "الصنف موجود مسبقاً في المستند"

                            else -> {
                                draftLines = draftLines + TransferDraftLine(
                                    item = item,
                                    quantity = amount
                                )

                                selectedItem = null
                                quantity = ""
                                message = "تمت إضافة الصنف إلى المستند"
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("＋ إضافة الصنف إلى المستند")
                }
            }
        }

        // أصناف المستند
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "أصناف مستند التحويل (${draftLines.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TransferNavy
                )

                if (draftLines.isEmpty()) {
                    Text(
                        "لم تتم إضافة أصناف بعد.",
                        color = Color.Gray
                    )
                }

                draftLines.forEachIndexed { index, line ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3F6FC)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${line.item.itemCode} - ${line.item.itemName}",
                                fontWeight = FontWeight.Bold,
                                color = TransferText
                            )

                            if (editingIndex == index) {
                                OutlinedTextField(
                                    value = editingQuantity,
                                    onValueChange = {
                                        editingQuantity = it
                                    },
                                    label = {
                                        Text("الكمية الجديدة")
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            val newAmount =
                                                editingQuantity.toDoubleOrNull()

                                            if (
                                                newAmount != null &&
                                                newAmount.isFinite() &&
                                                newAmount > 0
                                            ) {
                                                draftLines =
                                                    draftLines.toMutableList().also {
                                                        it[index] = line.copy(
                                                            quantity = newAmount
                                                        )
                                                    }

                                                editingIndex = -1
                                                editingQuantity = ""
                                                message = ""
                                            } else {
                                                message =
                                                    "أدخل كمية صحيحة أكبر من صفر"
                                            }
                                        },
                                        enabled = !loading
                                    ) {
                                        Text("حفظ")
                                    }

                                    TextButton(
                                        onClick = {
                                            editingIndex = -1
                                            editingQuantity = ""
                                        }
                                    ) {
                                        Text("إلغاء")
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "الكمية: ${line.quantity}",
                                        modifier = Modifier.weight(1f),
                                        color = TransferNavy
                                    )

                                    TextButton(
                                        onClick = {
                                            editingIndex = index
                                            editingQuantity =
                                                line.quantity.toString()
                                        },
                                        enabled = !loading
                                    ) {
                                        Text("تعديل")
                                    }

                                    TextButton(
                                        onClick = {
                                            draftLines =
                                                draftLines.filterIndexed {
                                                    i, _ -> i != index
                                                }
                                        },
                                        enabled = !loading
                                    ) {
                                        Text(
                                            "حذف",
                                            color = TransferRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = TransferBorder)

                Text(
                    "إجمالي الأصناف: ${draftLines.size}",
                    fontWeight = FontWeight.Bold,
                    color = TransferNavy
                )

                Text(
                    "إجمالي الكميات: $totalQuantity",
                    fontWeight = FontWeight.Bold,
                    color = TransferNavy
                )
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("ملاحظات المستند") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(12.dp),
            enabled = !loading
        )

        // زر الاعتماد
        Button(
            onClick = {
                val from = source
                val to = destination

                when {
                    from == null ->
                        message = "يرجى اختيار مخزن المصدر"

                    to == null ->
                        message = "يرجى اختيار المخزن المستلم"

                    from.id == to.id ->
                        message = "لا يمكن التحويل إلى المخزن نفسه"

                    draftLines.isEmpty() ->
                        message = "أضف صنفاً واحداً على الأقل"

                    editingIndex >= 0 ->
                        message = "احفظ تعديل الكمية أولاً"

                    loading ->
                        Unit

                    else ->
                        showApprovalDialog = true
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TransferRed,
                contentColor = Color.White
            )
        ) {
            Text(
                "اعتماد مستند التحويل",
                fontWeight = FontWeight.Bold
            )
        }

        // سجل مستندات التحويل
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "سجل مستندات التحويل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TransferNavy
                )

                if (groupedTransfers.isEmpty()) {
                    Text(
                        "لا توجد مستندات تحويل معتمدة.",
                        color = Color.Gray
                    )
                }

                groupedTransfers.forEach { group ->
                    val first = group.first()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    selectedDocumentNumber =
                                        first.transferNumber

                                    selectedDocument =
                                        transferRepository.getTransfersByNumber(
                                            first.transferNumber
                                        )
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3F6FC)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                "رقم المستند: ${first.transferNumber}",
                                fontWeight = FontWeight.Bold,
                                color = TransferNavy
                            )

                            Text(
                                "عدد الأصناف: ${group.size}",
                                color = TransferText
                            )

                            Text(
                                "التاريخ: ${
                                    SimpleDateFormat(
                                        "yyyy/MM/dd HH:mm",
                                        Locale.getDefault()
                                    ).format(Date(first.transferDate))
                                }",
                                color = TransferText
                            )

                            Text(
                                "اضغط لعرض التفاصيل",
                                color = TransferGreen
                            )
                        }
                    }
                }
            }
        }

        if (message.isNotEmpty()) {
            val success = message.startsWith("تم")

            Card(
                modifier = Modifier.fillMaxWidth(),
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
                Text(
                    message,
                    modifier = Modifier.padding(14.dp),
                    color = if (success) TransferGreen else TransferRed
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // تأكيد الاعتماد
    if (showApprovalDialog) {
        AlertDialog(
            onDismissRequest = {
                showApprovalDialog = false
            },
            title = {
                Text("تأكيد اعتماد التحويل")
            },
            text = {
                Text(
                    "سيتم ترحيل ${draftLines.size} صنفاً من مخزن المصدر إلى المخزن المستلم. هل تريد المتابعة؟"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApprovalDialog = false

                        val from = source
                        val to = destination

                        if (from != null && to != null) {
                            scope.launch {
                                loading = true
                                message = ""

                                try {
                                    val number =
                                        "TR-${System.currentTimeMillis()}"

                                    transferRepository.createTransfers(
                                        transferNumber = number,
                                        lines = draftLines.map {
                                            TransferLine(
                                                itemId = it.item.id,
                                                quantity = it.quantity
                                            )
                                        },
                                        sourceWarehouseId = from.id,
                                        destinationWarehouseId = to.id,
                                        notes = notes.trim()
                                    )

                                    message =
                                        "تم اعتماد التحويل بنجاح. رقم المستند: $number"

                                    draftLines = emptyList()
                                    selectedItem = null
                                    quantity = ""
                                    notes = ""
                                    destination = null

                                } catch (e: Exception) {
                                    message = when {
                                        e.message?.contains(
                                            "Insufficient stock",
                                            ignoreCase = true
                                        ) == true ->
                                            "الرصيد غير كافٍ لأحد الأصناف"

                                        e.message?.contains(
                                            "Duplicate items",
                                            ignoreCase = true
                                        ) == true ->
                                            "يوجد صنف مكرر في المستند"

                                        else ->
                                            "فشل التحويل: ${
                                                e.message ?: "خطأ غير معروف"
                                            }"
                                    }
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        "نعم، اعتماد",
                        color = TransferGreen
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showApprovalDialog = false
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // تفاصيل مستند سابق
    if (selectedDocument.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                selectedDocument = emptyList()
                selectedDocumentNumber = ""
            },
            title = {
                Text(
                    "تفاصيل مستند التحويل",
                    color = TransferNavy,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "رقم المستند: $selectedDocumentNumber",
                        fontWeight = FontWeight.Bold
                    )

                    val first = selectedDocument.first()

                    Text(
                        "التاريخ: ${
                            SimpleDateFormat(
                                "yyyy/MM/dd HH:mm",
                                Locale.getDefault()
                            ).format(Date(first.transferDate))
                        }"
                    )

                    Text(
                        "من مخزن: ${
                            warehouses.firstOrNull {
                                it.id == first.sourceWarehouseId
                            }?.warehouseName ?: first.sourceWarehouseId
                        }"
                    )

                    Text(
                        "إلى مخزن: ${
                            warehouses.firstOrNull {
                                it.id == first.destinationWarehouseId
                            }?.warehouseName ?: first.destinationWarehouseId
                        }"
                    )

                    HorizontalDivider()

                    selectedDocument.forEach { line ->
                        val item = items.firstOrNull {
                            it.id == line.itemId
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF3F6FC)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Text(
                                    item?.itemName
                                        ?: "صنف رقم ${line.itemId}",
                                    fontWeight = FontWeight.Bold,
                                    color = TransferText
                                )

                                Text(
                                    "الكود: ${item?.itemCode ?: "-"}"
                                )

                                Text(
                                    "الكمية: ${line.quantity}"
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "إجمالي الأصناف: ${selectedDocument.size}",
                        fontWeight = FontWeight.Bold,
                        color = TransferNavy
                    )

                    Text(
                        "إجمالي الكميات: ${
                            selectedDocument.sumOf { it.quantity }
                        }",
                        fontWeight = FontWeight.Bold,
                        color = TransferNavy
                    )

                    Text(
                        "الملاحظات: ${first.notes.ifBlank { "لا توجد" }}"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDocument = emptyList()
                        selectedDocumentNumber = ""
                    }
                ) {
                    Text("إغلاق")
                }
            }
        )
    }
}
