package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.repository.ItemRepository
import kotlinx.coroutines.launch

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
    snackbarHost = {
        SnackbarHost(hostState = snackbarHostState)
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
            TextButton(onClick = onBack) {
                Text("رجوع")
            }

            Text(
                text = "إدارة الأصناف",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("بحث بالكود أو اسم الصنف") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("إضافة صنف جديد")
        }

        Spacer(modifier = Modifier.height(12.dp))

        val filteredItems = items.filter {
            it.itemName.contains(search, ignoreCase = true) ||
            it.itemCode.contains(search, ignoreCase = true)
        }

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد أصناف مسجلة")
            }
        } else {
            LazyColumn {
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
                }
            ) {
                Text("تعطيل")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { itemToDeactivate = null }
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
.fillMaxWidth()
.padding(vertical = 5.dp)
) {
Column(modifier = Modifier.padding(14.dp)) {
Text(
text = item.itemName,
style = MaterialTheme.typography.titleMedium
)
Text("كود الصنف: ${item.itemCode}")
Text("الفئة: ${item.category}")
Text("الوحدة: ${item.unit}")
Text("الحد الأدنى: ${item.minimumStock}")

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDeactivate,
            modifier = Modifier.fillMaxWidth()
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
    title = { Text("إضافة صنف") },
    text = {
        Column(
            modifier = Modifier.heightIn(max = 450.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("كود الصنف") },
                singleLine = true
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم الصنف") },
                singleLine = true
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("الفئة") },
                singleLine = true
            )
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text("وحدة القياس") },
                singleLine = true
            )
            OutlinedTextField(
                value = minimum,
                onValueChange = { minimum = it },
                label = { Text("الحد الأدنى للمخزون") },
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("الوصف") }
            )
        }
    },
    confirmButton = {
        TextButton(
            onClick = {
                val minimumValue = minimum.toDoubleOrNull()

                if (
                    code.isNotBlank() &&
                    name.isNotBlank() &&
                    minimumValue != null &&
                    minimumValue.isFinite() &&
                    minimumValue >= 0
                ) {
                    onSave(
                        ItemEntity(
                            itemCode = code.trim(),
                            itemName = name.trim(),
                            category = category.trim(),
                            unit = unit.trim(),
                            minimumStock = minimumValue,
                            description = description.trim()
                        )
                    )
                }
            }
        ) {
            Text("حفظ")
        }
    },
    dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("إلغاء")
        }
    }
)

}
