package com.bravo.warehouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.bravo.warehouse.data.DatabaseProvider
import com.bravo.warehouse.data.AppDatabase
import com.bravo.warehouse.data.repository.ItemRepository
import com.bravo.warehouse.ui.screens.ItemsScreen

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = DatabaseProvider.getDatabase(applicationContext)

        setContent {
            BravoWarehouseApp(database)
        }
    }
}

private val Navy = Color(0xFF17365D)
private val Background = Color(0xFFF4F6FA)
private val Green = Color(0xFF238B65)

@Composable
fun BravoWarehouseApp(database: AppDatabase) {

    val repository = remember {
        ItemRepository(database.itemDao())
    }

    var showItems by remember {
        mutableStateOf(false)
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Navy,
                secondary = Green,
                background = Background
            )
        ) {
            if (showItems) {
                ItemsScreen(
                    repository = repository,
                    onBack = { showItems = false }
                )
            } else {
                DashboardScreen(
                    onItemsClick = { showItems = true }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    onItemsClick: () -> Unit
) {

    val menuItems = listOf(
        "الأصناف" to "📦",
        "المخازن" to "🏭",
        "الوارد" to "📥",
        "المنصرف" to "📤",
        "التحويلات" to "🔄",
        "المرتجعات" to "↩️",
        "الجرد" to "📋",
        "التقارير" to "📊"
    )

    Scaffold(
        containerColor = Background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            Text(
                text = "براڤو برو",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Text(
                text = "نظام إدارة المخازن",
                fontSize = 15.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Navy
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "لوحة التحكم",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        DashboardStat("عدد الأصناف", "0")
                        DashboardStat("عدد المخازن", "0")
                        DashboardStat("تنبيهات الطلب", "0")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "إدارة المخزون",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                items(menuItems) { item ->

                    MenuCard(
                        title = item.first,
                        icon = item.second,
                        onClick = {
                            if (item.first == "الأصناف") {
                                onItemsClick()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardStat(
    title: String,
    value: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = title,
            color
