package com.bravo.warehouse.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
itemCount: Int,
warehouseCount: Int,
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

var savedMessage by remember { mutableStateOf(false) }

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
            "الإعدادات",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }

    HorizontalDivider()

    Spacer(Modifier.height(20.dp))

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
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        )
    )

    Spacer(Modifier.height(10.dp))

    Button(
        onClick = {
            preferences.edit()
                .putString("factory_name", factoryName.trim())
                .apply()
            savedMessage = true
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = factoryName.isNotBlank()
    ) {
        Text("حفظ بيانات المنشأة")
    }

    if (savedMessage) {
        Text(
            "تم حفظ اسم المنشأة بنجاح",
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(Modifier.height(24.dp))

    Text(
        "معلومات المخزون",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Spacer(Modifier.height(10.dp))

    InfoCard("عدد الأصناف", itemCount.toString())
    InfoCard("عدد المخازن النشطة", warehouseCount.toString())

    Spacer(Modifier.height(24.dp))

    Text(
        "النسخ الاحتياطي",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Spacer(Modifier.height(10.dp))

    OutlinedButton(
        onClick = onBackupClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("إدارة النسخ الاحتياطي والاستعادة")
    }

    Spacer(Modifier.height(24.dp))

    Text(
        "حول التطبيق",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Text(
        "Bravo Warehouse\nنظام إدارة مخازن براڤو برو",
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
Text(value, fontWeight = FontWeight.Bold)
}
}
}
