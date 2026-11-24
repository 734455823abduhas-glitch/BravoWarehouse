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
