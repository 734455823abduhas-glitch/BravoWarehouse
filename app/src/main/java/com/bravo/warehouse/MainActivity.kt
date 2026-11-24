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
import com.bravo.warehouse.ui.screens.StocktakingScreen
import com.bravo.warehouse.ui.screens.ReportsScreen

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

            "stocktaking" -> StocktakingScreen(
                itemRepository = itemRepository,
                warehouseRepository = warehouseRepository,
                movementRepository = movementRepository,
                onBack = {
                    currentScreen = "dashboard"
                }
            )

            "reports" -> ReportsScreen(
                itemRepository = itemRepository,
                warehouseRepository = warehouseRepository,
                movementRepository = movementRepository,
                transferRepository = transferRepository,
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

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

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

                        "الجرد" ->
                            currentScreen = "stocktaking"

                        "التقارير" ->
                            currentScreen = "reports"
                    }
                }
            )
        }
    }
}

}
