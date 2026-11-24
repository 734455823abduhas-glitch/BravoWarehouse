package com.bravo.warehouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import com.bravo.warehouse.data.repository.WarehouseRepository
import com.bravo.warehouse.data.repository.StockMovementRepository
import com.bravo.warehouse.data.repository.TransferRepository
import com.bravo.warehouse.ui.screens.ItemsScreen
import com.bravo.warehouse.ui.screens.WarehouseScreen
import com.bravo.warehouse.ui.screens.StockMovementsScreen
import com.bravo.warehouse.ui.screens.TransferScreen

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

    val itemRepository = remember {
        ItemRepository(database.itemDao())
    }

    val warehouseRepository = remember {
        WarehouseRepository(database.warehouseDao())
    }

    val movementRepository = remember {
        StockMovementRepository(database.stockMovementDao())
    }

    val transferRepository = remember {
        TransferRepository(database)
    }

    val items by itemRepository.getAllItems()
        .collectAsState(initial = emptyList())

    val warehouses by warehouseRepository.getAllWarehouses()
        .collectAsState(initial = emptyList())

    var currentScreen by remember {
        mutableStateOf("dashboard")
    }

    var unavailableFeature by remember {
        mutableStateOf<String?>(null)
    }

    BackHandler(enabled = currentScreen != "dashboard") {
        currentScreen = "dashboard"
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

            when (currentScreen) {

                "items" -> ItemsScreen(
                    repository = itemRepository,
                    onBack = {
                        currentScreen = "dashboard"
                    }
                )

                "warehouses" -> WarehouseScreen(
                    repository = warehouseRepository,
                    onBack = {
                        currentScreen = "dashboard"
                    }
                )

                "movements", "returns" -> StockMovementsScreen(
                    itemRepository = itemRepository,
                    warehouseRepository = warehouseRepository,
                    movementRepository = movementRepository,
                    onBack = {
                        currentScreen = "dashboard"
                    }
                )

                "transfers" -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            TextButton(
                                onClick = {
                                    currentScreen = "dashboard"
                                }
                            ) {
                                Text(
                                    text = "رجوع ←",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "التحويلات بين المخازن",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy
                            )
                        }

                        HorizontalDivider()

                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            TransferScreen(
                                items = items,
                                warehouses = warehouses,
                                transferRepository = transferRepository
                            )
                        }
                    }
                }

                else -> DashboardScreen(
                    itemCount = items.size,
                    warehouseCount = warehouses.size,
                    onMenuClick = { title ->

                        when (title) {

                            "الأصناف" ->
                                currentScreen = "items"

                            "المخازن" ->
                                currentScreen = "warehouses"

                            "الوارد", "المنصرف" ->
                                currentScreen = "movements"

                            "التحويلات" ->
                                currentScreen = "transfers"

                            "المرتجعات" ->
                                currentScreen = "returns"

                            "الجرد", "التقارير" ->
                                unavailableFeature = title
                        }
                    }
                )
            }

            if (unavailableFeature != null) {

                AlertDialog(
                    onDismissRequest = {
                        unavailableFeature = null
                    },
                    title = {
                        Text("القسم قيد التطوير")
                    },
                    text = {
                        Text(
                            "قسم ${unavailableFeature} لم يتم تفعيله بعد."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                unavailableFeature = null
                            }
                        ) {
                            Text("حسناً")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    itemCount: Int,
    warehouseCount: Int,
    onMenuClick: (String) -> Unit
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
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        DashboardStat(
                            "عدد الأصناف",
                            itemCount.toString()
                        )

                        DashboardStat(
                            "عدد المخازن",
                            warehouseCount.toString()
                        )
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
                            onMenuClick(item.first)
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
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MenuCard(
    title: String,
    icon: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = icon,
                fontSize = 29.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
        }
    }
}
