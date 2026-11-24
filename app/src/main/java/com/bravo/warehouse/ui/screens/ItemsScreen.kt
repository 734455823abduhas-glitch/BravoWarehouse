package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.repository.ItemRepository
import kotlinx.coroutines.launch

private val BravoNavy = Color(0xFF082F68)
private val BravoRed = Color(0xFFE5242A)
private val BravoBackground = Color(0xFFF3F6FC)
private val BravoText = Color(0xFF102B50)
private val BravoMuted = Color(0xFF526783)
private val BravoBorder = Color(0xFFB7C8DF)
private val BravoLightBlue = Color(0xFFE8EFF9)

@Composable
fun ItemsScreen(
    repository: ItemRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var search by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDeactivate by remember { mutableStateOf<ItemEntity?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val items by repository.getAllItems().collectAsState(initial = emptyList())

    Scaffold(
        containerColor = BravoBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    containerColor = BravoNavy,
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = BravoNavy
                    )
                ) {
                    Text("رجوع")
                }

                Text(
                    text = "إدارة الأصناف",
                    color = BravoNavy,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("بحث بالكود أو اسم الصنف") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BravoNavy,
                    unfocusedBorderColor = BravoBorder,
                    focusedLabelColor = BravoNavy,
                    unfocusedLabelColor = BravoMuted,
                    focusedTextColor = BravoText,
                    unfocusedTextColor = BravoText,
                    cursorColor = BravoRed
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BravoNavy,
                    contentColor = Color.White
                )
            ) {
                Text("＋  إضافة صنف جديد")
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredItems = items.filter {
                it.itemName.contains(search, ignoreCase = true) ||
                    it.itemCode.contains(search, ignoreCase = true)
            }

            Text(
                text = "الأصناف المسجلة (${filteredItems.size})",
                color = BravoNavy,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (search.isBlank())
                            "لا توجد أصناف مسجلة"
                        else
                            "لا توجد نتائج مطابقة للبحث",
                        color = BravoMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onDeactivate = {
                                itemToDeactivate = item
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                scope.launch {
                    try {
                        repository.insertItem(item)
                        showAddDialog = false
                        snackbarHostState.showSnackbar("تمت إضافة الصنف بنجاح")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            "تعذرت إضافة الصنف: ${e.message ?: "خطأ غير معروف"}"
                        )
                    }
                }
            }
        )
    }

    itemToDeactivate?.let { item ->
        AlertDialog(
            onDismissRequest = {
                if (!isLoading) itemToDeactivate = null
            },
            containerColor = Color.White,
            titleContentColor = BravoNavy,
            textContentColor = BravoText,
            title = { Text("تأكيد تعطيل الصنف") },
            text = {
                Text("هل تريد تعطيل الصنف «${item.itemName}»؟")
            },
            confirmButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val success = repository.deactivateItem(item.id)
                                snackbarHostState.showSnackbar(
                                    if (success) {
                                        "تم تعطيل الصنف بنجاح"
                                    } else {
                                        "لا يمكن تعطيل الصنف لأنه يحتوي على رصيد"
                                    }
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "تعذر تعطيل الصنف: ${e.message ?: "خطأ غير معروف"}"
                                )
                            } finally {
                                isLoading = false
                                itemToDeactivate = null
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = BravoRed
                    )
                ) {
                    Text("تعطيل")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = { itemToDeactivate = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = BravoNavy
                    )
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ItemCard(
    item: ItemEntity,
    onDeactivate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = item.itemName,
                color = BravoNavy,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("كود الصنف: ${item.itemCode}", color = BravoText)
            Text("الفئة: ${item.category}", color = BravoMuted)
            Text("الوحدة: ${item.unit}", color = BravoMuted)
            Text("الحد الأدنى: ${item.minimumStock}", color = BravoMuted)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onDeactivate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BravoRed
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    BravoRed
                )
            ) {
                Text("تعطيل الصنف")
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onSave: (ItemEntity) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("مواد خام") }
    var unit by remember { mutableStateOf("كيلوجرام") }
    var minimum by remember { mutableStateOf("0") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = BravoNavy,
        textContentColor = BravoText,
        title = { Text("إضافة صنف جديد") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                @Composable
                fun inputColors() = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BravoNavy,
                    unfocusedBorderColor = BravoBorder,
                    focusedLabelColor = BravoNavy,
                    unfocusedLabelColor = BravoMuted,
                    focusedTextColor = BravoText,
                    unfocusedTextColor = BravoText,
                    cursorColor = BravoRed
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("كود الصنف") },
                    singleLine = true,
                    colors = inputColors()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف") },
                    singleLine = true,
                    colors = inputColors()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("الفئة") },
                    singleLine = true,
                    colors = inputColors()
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("وحدة القياس") },
                    singleLine = true,
                    colors = inputColors()
                )

                OutlinedTextField(
                    value = minimum,
                    onValueChange = { minimum = it },
                    label = { Text("ال
