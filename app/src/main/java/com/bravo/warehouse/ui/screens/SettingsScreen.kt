package com.bravo.warehouse.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bravo.warehouse.data.entity.WarehouseEntity

@Composable
fun SettingsScreen(
itemCount: Int,
warehouseCount: Int,
warehouses: List<WarehouseEntity>,
onBack: () -> Unit,
onBackupClick: () -> Unit
) {
val context = LocalContext.current

val preferences = remember {
    context.getSharedPreferences(
        "bravo_settings",
        Context.MODE_PRIVATE
    )
}

var factoryName by remember {
    mutableStateOf(
        preferences.getString(
            "factory_name",
            "براڤو برو للصناعات البلاستيكية"
        ) ?: "براڤو برو للصناعات البلاستيكية"
    )
}

var selectedWarehouseId by remember {
    mutableLongStateOf(
        preferences.getLong("default_warehouse_id", 0L)
    )
}

var dateFormat by remember {
    mutableStateOf(
        preferences.getString(
            "date_format",
            "dd/MM/yyyy"
        ) ?: "dd/MM/yyyy"
    )
}

var timeFormat by remember {
    mutableStateOf(
        preferences.getString(
            "time_format",
            "24"
        ) ?: "24"
    )
}

var warehouseExpanded by remember { mutableStateOf(false) }
var dateExpanded by remember { mutableStateOf(false) }
var timeExpanded by remember { mutableStateOf(false) }

var savedMessage by remember { mutableStateOf(false) }

val selectedWarehouse = warehouses.firstOrNull {
    it.id == selectedWarehouseId
}

Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("رجوع ←")
        }

        Text(
            text = "إعدادات التطبيق",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }

    HorizontalDivider()

    Spacer(Modifier.height(18.dp))

    Text(
        "بيانات المنشأة",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Spacer(Modifier.height(10.dp))

    OutlinedTextField(
        value = factoryName,
        onValueChange = {
            factoryName = it
            savedMessage = false
        },
        label = { Text("اسم المصنع") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(Modifier.height(22.dp))

    HorizontalDivider()

    Spacer(Modifier.height(18.dp))

    Text(
        "إعدادات المخازن",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Spacer(Modifier.height(10.dp))

    Text(
        "المخزن الافتراضي",
        fontWeight = FontWeight.Medium
    )

    Spacer(Modifier.height(6.dp))

    Box(modifier = Modifier.fillMaxWidth()) {

        OutlinedButton(
            onClick = {
                warehouseExpanded = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                selectedWarehouse?.warehouseName
                    ?: if (selectedWarehouseId == 0L) {
                        "لم يتم تحديد مخزن افتراضي"
                    } else {
                        "المخزن المحفوظ غير متاح"
                    }
            )
        }

        DropdownMenu(
            expanded = warehouseExpanded,
            onDismissRequest = {
                warehouseExpanded = false
            }
        ) {

            DropdownMenuItem(
                text = { Text("بدون مخزن افتراضي") },
                onClick = {
                    selectedWarehouseId = 0L
                    warehouseExpanded = false
                    savedMessage = false
                }
            )

            warehouses.forEach { warehouse ->

                DropdownMenuItem(
                    text = {
                        Text(warehouse.warehouseName)
                    },
                    onClick = {
                        selectedWarehouseId = warehouse.id
                        warehouseExpanded = false
                        savedMessage = false
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(22.dp))

    HorizontalDivider()

    Spacer(Modifier.height(18.dp))

    Text(
        "إعدادات التاريخ والوقت",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Spacer(Modifier.height(10.dp))

    Text("تنسيق التاريخ")

    Box(modifier = Modifier.fillMaxWidth()) {

        OutlinedButton(
            onClick = {
                dateExpanded = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(dateFormat)
        }

        DropdownMenu(
            expanded = dateExpanded,
            onDismissRequest = {
                dateExpanded = false
            }
        ) {

            listOf(
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
            ).forEach { format ->

                DropdownMenuItem(
                    text = { Text(format) },
                    onClick = {
                        dateFormat = format
                        dateExpanded = false
                        savedMessage = false
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Text("تنسيق الوقت")

    Box(modifier = Modifier.fillMaxWidth()) {

        OutlinedButton(
            onClick = {
                timeExpanded = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (timeFormat == "24") {
                    "24 ساعة"
                } else {
                    "12 ساعة"
                }
            )
        }

        DropdownMenu(
            expanded = timeExpanded,
            onDismissRequest = {
                timeExpanded = false
            }
        ) {

            DropdownMenuItem(
                text = { Text("24 ساعة") },
                onClick = {
                    timeFormat = "24"
                    timeExpanded = false
                    savedMessage = false
                }
            )

            DropdownMenuItem(
                text = { Text("12 ساعة") },
                onClick = {
                    timeFormat = "12"
                    timeExpanded = false
                    savedMessage = false
                }
            )
        }
    }

    Spacer(Modifier.height(22.dp))

    Button(
        onClick = {

            preferences.edit()
                .putString("factory_name", factoryName.trim())
                .putLong("default_warehouse_id", selectedWarehouseId)
                .putString("date_format", dateFormat)
                .putString("time_format", timeFormat)
                .apply()

            savedMessage = true
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = factoryName.isNotBlank()
    ) {
        Text("حفظ جميع الإعدادات")
    }

    if (savedMessage) {
        Text(
            text = "تم حفظ الإعدادات بنجاح",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    Spacer(Modifier.height(22.dp))

    HorizontalDivider()

    Spacer(Modifier.height(18.dp))

    Text(
        "معلومات المخزون",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Spacer(Modifier.height(8.dp))

    InfoCard("عدد الأصناف", itemCount.toString())
    InfoCard("عدد المخازن النشطة", warehouseCount.toString())

    Spacer(Modifier.height(18.dp))

    Text(
        "النسخ الاحتياطي",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    OutlinedButton(
        onClick = onBackupClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text("إدارة النسخ الاحتياطي والاستعادة")
    }

    Spacer(Modifier.height(18.dp))

    Text(
        "حول التطبيق",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Text(
        "Bravo Warehouse\nنظام إدارة مخازن براڤو برو\nالإصدار 1.0.2",
        modifier = Modifier.padding(top = 8.dp),
        lineHeight = 25.sp
    )
}

}

@Composable
private fun InfoCard(
title: String,
value: String
) {
Card(
modifier = Modifier
.fillMaxWidth()
.padding(vertical = 4.dp),
shape = RoundedCornerShape(12.dp)
) {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(16.dp),
horizontalArrangement = Arrangement.SpaceBetween,
verticalAlignment = Alignment.CenterVertically
) {
Text(title)
Text(
value,
fontWeight = FontWeight.Bold
)
}
}
}
