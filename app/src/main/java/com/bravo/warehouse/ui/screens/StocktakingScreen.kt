LaunchedEffect(selectedItemId, selectedWarehouseId) {
    actualQuantity = ""
    systemBalance = 0.0
    message = ""

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
            systemBalance = balance
        }
    }
}
