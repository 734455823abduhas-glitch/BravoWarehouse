package com.bravo.warehouse.ui.screens

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.bravo.warehouse.data.entity.WarehouseEntity

private val BravoNavy = Color(0xFF082F68)
private val BravoDarkNavy = Color(0xFF062451)
private val BravoRed = Color(0xFFE5242A)
private val BravoBackground = Color(0xFFF3F6FC)
private val BravoWhite = Color.White
private val BravoText = Color(0xFF102B50)
private val BravoMuted = Color(0xFF526783)
private val BravoBorder = Color(0xFFB7C8DF)
private val BravoLightBlue = Color(0xFFE8EFF9)

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

MaterialTheme(
    colorScheme = lightColorScheme(
        primary = BravoNavy,
        onPrimary = BravoWhite,
        secondary = BravoRed,
        onSecondary = BravoWhite,
        background = BravoBackground,
        onBackground = BravoText,
        surface = BravoWhite,
        onSurface = BravoText,
        outline = BravoBorder
    )
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BravoBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // رأس الشاشة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    BravoNavy,
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = BravoWhite
                )
            ) {
                Text(
                    "رجوع ←",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "إعدادات التطبيق",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = BravoWhite
            )
        }

        Spacer(Modifier.height(20.dp))

        // بيانات المنشأة
        SectionTitle("بيانات المنشأة")

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = factoryName,
            onValueChange = {
                factoryName = it
                savedMessage = false
            },
            label = {
                Text("اسم المصنع")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = BravoText,
                fontSize = 17.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BravoText,
                unfocusedTextColor = BravoText,
                focusedBorderColor = BravoNavy,
                unfocusedBorderColor = BravoBorder,
                focusedLabelColor = BravoNavy,
                unfocusedLabelColor = BravoMuted,
                cursorColor = BravoRed,
                focusedContainerColor = BravoWhite,
                unfocusedContainerColor = BravoWhite
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(22.dp))
        SectionDivider()
        Spacer(Modifier.height(18.dp))

        // إعدادات المخازن
        SectionTitle("إعدادات المخازن")

        Spacer(Modifier.height(10.dp))

        FieldLabel("المخزن الافتراضي")

        Spacer(Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {

            OutlinedButton(
                onClick = {
                    warehouseExpanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BravoNavy,
                    containerColor = BravoWhite
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        BravoBorder
                    )
                )
            ) {
                Text(
                    selectedWarehouse?.warehouseName
                        ?: if (selectedWarehouseId == 0L) {
                            "لم يتم تحديد مخزن افتراضي"
                        } else {
                            "المخزن المحفوظ غير متاح"
                        },
                    fontSize = 16.sp,
                    color = BravoText
                )
            }

            DropdownMenu(
                expanded = warehouseExpanded,
                onDismissRequest = {
                    warehouseExpanded = false
                },
                modifier = Modifier.background(BravoWhite)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "بدون مخزن افتراضي",
                            color = BravoText
                        )
                    },
                    onClick = {
                        selectedWarehouseId = 0L
                        warehouseExpanded = false
                        savedMessage = false
                    }
                )

                warehouses.forEach { warehouse ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                warehouse.warehouseName,
                                color = BravoText
                            )
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
        SectionDivider()
        Spacer(Modifier.height(18.dp))

        // إعدادات التاريخ والوقت
        SectionTitle("إعدادات التاريخ والوقت")

        Spacer(Modifier.height(12.dp))

        FieldLabel("تنسيق التاريخ")

        Box(modifier = Modifier.fillMaxWidth()) {

            OutlinedButton(
                onClick = {
                    dateExpanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BravoNavy,
                    containerColor = BravoWhite
                )
            ) {
                Text(
                    dateFormat,
                    fontSize = 17.sp,
                    color = BravoText
                )
            }

            DropdownMenu(
                expanded = dateExpanded,
                onDismissRequest = {
                    dateExpanded = false
                },
                modifier = Modifier.background(BravoWhite)
            ) {
                listOf(
                    "dd/MM/yyyy",
                    "yyyy-MM-dd",
                    "dd-MM-yyyy"
                ).forEach { format ->
                    DropdownMenuItem(
                        text = {
                            Text(format, color = BravoText)
                        },
                        onClick = {
                            dateFormat = format
                            dateExpanded = false
                            savedMessage = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        FieldLabel("تنسيق الوقت")

        Box(modifier = Modifier.fillMaxWidth()) {

            OutlinedButton(
                onClick = {
                    timeExpanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BravoNavy,
                    containerColor = BravoWhite
                )
            ) {
                Text(
                    if (timeFormat == "24") {
                        "24 ساعة"
                    } else {
                        "12 ساعة"
                    },
                    fontSize = 17.sp,
                    color = BravoText
                )
            }

            DropdownMenu(
                expanded = timeExpanded,
                onDismissRequest = {
                    timeExpanded = false
                },
                modifier = Modifier.background(BravoWhite)
            ) {
                DropdownMenuItem(
                    text = {
                        Text("24 ساعة", color = BravoText)
                    },
                    onClick = {
                        timeFormat = "24"
                        timeExpanded = false
                        savedMessage = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("12 ساعة", color = BravoText)
                    },
                    onClick = {
                        timeFormat = "12"
                        timeExpanded = false
                        savedMessage = false
                    }
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // حفظ الإعدادات
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
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = factoryName.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BravoNavy,
                contentColor = BravoWhite,
                disabledContainerColor = BravoMuted
            )
        ) {
            Text(
                "حفظ جميع الإعدادات",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (savedMessage) {
            Text(
                text = "تم حفظ الإعدادات بنجاح",
                color = Color(0xFF168052),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionDivider()
        Spacer(Modifier.height(18.dp))

        // معلومات المخزون
        SectionTitle("معلومات المخزون")

        Spacer(Modifier.height(10.dp))

        InfoCard("عدد الأصناف", itemCount.toString())
        InfoCard("عدد المخازن النشطة", warehouseCount.toString())

        Spacer(Modifier.height(20.dp))

        // النسخ الاحتياطي
        SectionTitle("النسخ الاحتياطي")

        OutlinedButton(
            onClick = onBackupClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BravoNavy,
                containerColor = BravoWhite
            )
        ) {
            Text(
                "إدارة النسخ الاحتياطي والاستعادة",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionTitle("حول التطبيق")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = BravoLightBlue
            )
        ) {
            Text(
                text = "Bravo Warehouse\nنظام إدارة مخازن براڤو برو\nالإصدار 1.0.2",
                modifier = Modifier.padding(16.dp),
                color = BravoText,
                fontSize = 15.sp,
                lineHeight = 25.sp
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

}

@Composable
private fun SectionTitle(title: String) {
Text(
text = title,
fontSize = 19.sp,
fontWeight = FontWeight.Bold,
color = BravoNavy,
modifier = Modifier
.fillMaxWidth()
.background(
BravoLightBlue,
RoundedCornerShape(10.dp)
)
.padding(horizontal = 12.dp, vertical = 10.dp)
)
}

@Composable
private fun FieldLabel(title: String) {
Text(
text = title,
fontSize = 16.sp,
fontWeight = FontWeight.SemiBold,
color = BravoText
)
}

@Composable
private fun SectionDivider() {
HorizontalDivider(
color = BravoBorder,
thickness = 1.dp
)
}

@Composable
private fun InfoCard(
title: String,
value: String
) {
Card(
modifier = Modifier
.fillMaxWidth()
.padding(vertical = 5.dp),
shape = RoundedCornerShape(12.dp),
colors = CardDefaults.cardColors(
containerColor = BravoWhite
),
elevation = CardDefaults.cardElevation(
defaultElevation = 2.dp
)
) {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(16.dp),
horizontalArrangement = Arrangement.SpaceBetween,
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = title,
color = BravoText,
fontSize = 15.sp
)

        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            color = BravoNavy,
            fontSize = 17.sp
        )
    }
}

}
