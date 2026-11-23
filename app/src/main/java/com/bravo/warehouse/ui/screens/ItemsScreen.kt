package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.repository.ItemRepository
import kotlinx.coroutines.launch

@Composable
fun ItemsScreen(
    repository: ItemRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val items by repository.getAllItems().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            modifier = Modifier.fillMaxWidth()
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
                items(filteredItems) { item ->
                    ItemCard(item = item)
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                scope.launch {
                    repository.insertItem(item)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun ItemCard(item: ItemEntity) {
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
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onSave(
                            ItemEntity(
                                itemCode = code.trim(),
                                itemName = name.trim(),
                                category = category.trim(),
                                unit = unit.trim(),
                                minimumStock = minimum.toDoubleOrNull() ?: 0.0,
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
