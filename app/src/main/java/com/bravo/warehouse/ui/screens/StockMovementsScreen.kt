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
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.entity.StockMovementEntity
import com.bravo.warehouse.data.repository.ItemRepository
import com.bravo.warehouse.data.repository.StockMovementRepository
import com.bravo.warehouse.data.repository.WarehouseRepository
import com.bravo.warehouse.util.DateTimeUtils
import kotlinx.coroutines.launch

private val MovementNavy = Color(0xFF082F68)
private val MovementRed = Color(0xFFE5242A)
private val MovementBackground = Color(0xFFF3F6FC)
private val MovementText = Color(0xFF102B50)
private val MovementMuted = Color(0xFF526783)
private val MovementBorder = Color(0xFFB7C8DF)
private val MovementGreen = Color(0xFF18834B)

private data class MovementDraftLine(
    val item: ItemEntity,
    val quantity: Double
)

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
        context.getSharedPreferences("bravo_settings", Context.MODE_PRIVATE)
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

    var documentNumber by remember {
        mutableStateOf("MOV-${System.currentTimeMillis()}")
    }

    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }

    var draftLines by remember {
        mutableStateOf(listOf<MovementDraftLine>())
    }

    var itemMenu by remember { mutableStateOf(false) }
    var warehouseMenu by remember { mutableStateOf(false) }

    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showApprovalDialog by remember { mutableStateOf(false) }

    var currentBalance by remember { mutableStateOf(0.0) }

    var editingIndex by remember { mutableIntStateOf(-1) }
    var editingQuantity by remember { mutableStateOf("") }

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

    val totalQuantity = draftLines.sumOf { it.quantity }

    LaunchedEffect(warehouseList) {
        if (warehouseList.isNotEmpty()) {
            val defaultId = preferences.getLong(
                "default_warehouse_id",
                0L
            )

            if (
                selectedWarehouseId == null ||
                warehouseList.none {
                    it.id == selectedWarehouseId && it.isActive
                }
            ) {
                selectedWarehouseId =
                    warehouseList.firstOrNull {
                        it.id == defaultId && it.isActive
                    }?.id ?: warehouseList.firstOrNull {
                        it.isActive
                    }?.id
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

    fun addDraftLine() {
        val item = selectedItem
        val amount = quantity.trim().toDoubleOrNull()

        when {
            item == null -> {
                message = "يرجى اختيار الصنف"
                messageIsError = true
            }

            amount == null || !amount.isFinite() || amount <= 0.0 -> {
                message = "أدخل كمية صحيحة أكبر من صفر"
                messageIsError = true
            }

            draftLines.any { it.item.id == item.id } -> {
                message = "الصنف موجود مسبقاً في المستند"
                messageIsError = true
            }

            else -> {
                draftLines = draftLines + MovementDraftLine(
                    item = item,
                    quantity = amount
                )

                selectedItemId = null
                quantity = ""
                message = "تمت إضافة الصنف إلى المستند"
                messageIsError = false
            }
        }
    }

    Scaffold(
        containerColor = MovementBackground
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
                        enabled = !isSaving,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MovementNavy
                        )
                    ) {
                        Text("رجوع")
                    }

                    Text(
                        "حركة المخزون",
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
                            "بيانات المستند",
                            color = MovementNavy,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        OutlinedTextField(
                            value = documentNumber,
                            onValueChange = { documentNumber = it },
                            enabled = !isSaving && draftLines.isEmpty(),
                            label = { Text("رقم المستند") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = movementFieldColors()
                        )

                        Text(
                            "يمكن تعديل رقم المستند قبل إضافة الأصناف.",
                            color = MovementMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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

                        listOf(
                            listOf("IN" to "وارد", "OUT" to "منصرف"),
                            listOf(
                                "RETURN_IN" to "مرتجع للمخزن",
                                "RETURN_OUT" to "مرتجع من المخزن"
                            )
                        ).forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { (type, title) ->
                                    Button(
                                        onClick = {
                                            if (draftLines.isNotEmpty()) {
                                                message =
                                                    "اعتمد أو ألغِ المستند الحالي قبل تغيير نوع الحركة"
                                                messageIsError = true
                                            } else {
                                                movementType = type
                                                message = ""
                                            }
                                        },
                                        enabled = !isSaving,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor =
                                                if (movementType == type)
                                                    MovementNavy
                                                else MovementBackground,
                                            contentColor =
                                                if (movementType == type)
                                                    Color.White
                                                else MovementNavy
                                        )
                                    ) {
                                        Text(title)
                                    }
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
                                onClick = {
                                    selectedItemId = item.id
                                    itemMenu = false
                                    message = ""
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
                        warehouseList.filter { it.isActive }.forEach { warehouse ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        warehouse.warehouseName,
                                        color = MovementText
                                    )
                                },
                                onClick = {
                                    if (draftLines.isNotEmpty()) {
                                        message =
                                            "لا يمكن تغيير المخزن بعد إضافة الأصناف. ألغِ المسودة أولاً."
                                        messageIsError = true
                                    } else {
                                        selectedWarehouseId = warehouse.id
                                        warehouseMenu = false
                                        message = ""
                                    }
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
                OutlinedButton(
                    onClick = { addDraftLine() },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("＋ إضافة الصنف إلى المستند")
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, MovementBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "أصناف المستند (${draftLines.size})",
                            color = MovementNavy,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        if (draftLines.isEmpty()) {
                            Text(
                                "لم تتم إضافة أصناف بعد.",
                                color = MovementMuted
                            )
                        }

                        draftLines.forEachIndexed { index, line ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MovementBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "${line.item.itemCode} - ${line.item.itemName}",
                                        fontWeight = FontWeight.Bold,
                                        color = MovementText
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
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = movementFieldColors()
                                        )

                                        Row {
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
                                                                it[index] =
                                                                    line.copy(
                                                                        quantity = newAmount
                                                                    )
                                                            }

                                                        editingIndex = -1
                                                        editingQuantity = ""
                                                        message = ""
                                                    } else {
                                                        message =
                                                            "أدخل كمية صحيحة أكبر من صفر"
                                                        messageIsError = true
                                                    }
                                                },
                                                enabled = !isSaving
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
                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "الكمية: ${line.quantity} ${line.item.unit}",
                                                modifier = Modifier.weight(1f),
                                                color = MovementNavy
                                            )

                                            TextButton(
                                                onClick = {
                                                    editingIndex = index
                                                    editingQuantity =
                                                        line.quantity.toString()
                                                },
                                                enabled = !isSaving
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
                                                enabled = !isSaving
                                            ) {
                                                Text(
                                                    "حذف",
                                                    color = MovementRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MovementBorder)

                        Text(
                            "إجمالي الأصناف: ${draftLines.size}",
                            fontWeight = FontWeight.Bold,
                            color = MovementNavy
                        )

                        Text(
                            "إجمالي الكميات: $totalQuantity",
                            fontWeight = FontWeight.Bold,
                            color = MovementNavy
                        )
                    }
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
                            "بيانات المستلم",
                            color = MovementNavy,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        OutlinedTextField(
                            value = recipientName,
                            onValueChange = { recipientName = it },
                            enabled = !isSaving,
                            label = {
                                Text(
                                    if (
                                        movementType == "OUT" ||
                                        movementType == "RETURN_OUT"
                                    )
                                        "اسم المستلم (إلزامي)"
                                    else
                                        "اسم المستلم"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = movementFieldColors()
                        )

                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            enabled = !isSaving,
                            label = {
                                Text("الجهة / القسم (اختياري)")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = movementFieldColors()
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    enabled = !isSaving,
                    label = { Text("ملاحظات المستند") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = movementFieldColors()
                )
            }

            item {
                Button(
                    onClick = {
                        when {
                            documentNumber.isBlank() -> {
                                message = "أدخل رقم المستند"
                                messageIsError = true
                            }

                            selectedWarehouseId == null -> {
                                message = "يرجى اختيار المخزن"
                                messageIsError = true
                            }

                            draftLines.isEmpty() -> {
                                message = "أضف صنفاً واحداً على الأقل"
                                messageIsError = true
                            }

                            editingIndex >= 0 -> {
                                message = "احفظ تعديل الكمية أولاً"
                                messageIsError = true
                            }

                            (
                                movementType == "OUT" ||
                                movementType == "RETURN_OUT"
                            ) && recipientName.isBlank() -> {
                                message = "يرجى إدخال اسم المستلم"
                                messageIsError = true
                            }

                            isSaving -> Unit

                            else -> {
                                message = ""
                                showApprovalDialog = true
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
                    Text(
                        "اعتماد مستند $movementTitle",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (!isSaving) {
                            draftLines = emptyList()
                            selectedItemId = null
                            quantity = ""
                            notes = ""
                            recipientName = ""
                            department = ""
                            editingIndex = -1
                            editingQuantity = ""
                            documentNumber =
                                "MOV-${System.currentTimeMillis()}"
                            message = "تم إلغاء المسودة"
                            messageIsError = false
                        }
                    },
                    enabled = !isSaving && draftLines.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إلغاء المسودة")
                }
            }

            if (message.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (messageIsError)
                                Color(0xFFFFEEEE)
                            else
                                Color(0xFFE8F5EC)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            color = if (messageIsError)
                                MovementRed
                            else
                                MovementGreen
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
                    "سجل الحركات",
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
                                "رقم المستند: ${movement.documentNumber}",
                                color = MovementText
                            )

                            Text(
                                "الصنف: ${item?.itemName ?: movement.itemId}",
                                color = MovementText
                            )

                            Text(
                                "المخزن: ${
                                    warehouse?.warehouseName
                                        ?: movement.warehouseId
                                }",
                                color = MovementText
                            )

                            Text(
                                "الكمية: ${movement.quantity}",
                                color = MovementText,
                                fontWeight = FontWeight.Bold
                            )

                            if (movement.recipientName.isNotBlank()) {
                                Text(
                                    "المستلم: ${movement.recipientName}",
                                    color = MovementText
                                )
                            }

                            if (movement.department.isNotBlank()) {
                                Text(
                                    "الجهة / القسم: ${movement.department}",
                                    color = MovementMuted
                                )
                            }

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
                                if (movement.isApproved)
                                    "الحالة: معتمدة"
                                else
                                    "الحالة: غير معتمدة",
                                color = if (movement.isApproved)
                                    MovementGreen
                                else
                                    MovementRed,
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

    if (showApprovalDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) showApprovalDialog = false
            },
            title = {
                Text("تأكيد اعتماد المستند")
            },
            text = {
                Text(
                    "سيتم اعتماد ${draftLines.size} صنفاً في المستند رقم $documentNumber من نوع $movementTitle. هل تريد المتابعة؟"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        val warehouseId = selectedWarehouseId
                        val typeToSave = movementType
                        val numberToSave = documentNumber.trim()
                        val notesToSave = notes.trim()
                        val recipientToSave = recipientName.trim()
                        val departmentToSave = department.trim()
                        val linesToSave = draftLines.toList()

                        val recipientRequired =
                            typeToSave == "OUT" ||
                            typeToSave == "RETURN_OUT"

                        if (
                            warehouseId == null ||
                            numberToSave.isBlank() ||
                            linesToSave.isEmpty() ||
                            (recipientRequired && recipientToSave.isBlank())
                        ) {
                            showApprovalDialog = false
                            message = "تحقق من بيانات المستند واسم المستلم"
                            messageIsError = true
                        } else if (!isSaving) {
                            scope.launch {
                                isSaving = true
                                message = ""

                                try {
                                    val now = System.currentTimeMillis()

                                    val movements = linesToSave.map { line ->
                                        StockMovementEntity(
                                            documentNumber = numberToSave,
                                            movementType = typeToSave,
                                            itemId = line.item.id,
                                            warehouseId = warehouseId,
                                            quantity = line.quantity,
                                            notes = notesToSave,
                                            movementDate = now,
                                            createdAt = now,
                                            isApproved = false,
                                            recipientName = recipientToSave,
                                            department = departmentToSave
                                        )
                                    }

                                    movementRepository
                                        .insertAndApproveMovements(movements)

                                    showApprovalDialog = false
                                    message =
                                        "تم اعتماد المستند بنجاح. رقم المستند: $numberToSave"
                                    messageIsError = false

                                    draftLines = emptyList()
                                    selectedItemId = null
                                    quantity = ""
                                    notes = ""
                                    recipientName = ""
                                    department = ""
                                    editingIndex = -1
                                    editingQuantity = ""
                                    documentNumber =
                                        "MOV-${System.currentTimeMillis()}"

                                } catch (e: Exception) {
                                    val outgoing =
                                        typeToSave == "OUT" ||
                                        typeToSave == "RETURN_OUT"

                                    message = if (
                                        outgoing &&
                                        (
                                            e.message?.contains(
                                                "Insufficient stock",
                                                ignoreCase = true
                                            ) == true ||
                                            e.message?.contains(
                                                "Movement approval failed",
                                                ignoreCase = true
                                            ) == true
                                        )
                                    ) {
                                        "الرصيد غير كافٍ لأحد الأصناف. لم يتم اعتماد أي صنف."
                                    } else {
                                        "فشل اعتماد المستند: ${
                                            e.message ?: "خطأ غير معروف"
                                        }"
                                    }

                                    messageIsError = true
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        "نعم، اعتماد",
                        color = MovementGreen
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        showApprovalDialog = false
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
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
