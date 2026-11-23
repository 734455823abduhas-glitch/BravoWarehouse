package com.bravo.warehouse.data.repository

import com.bravo.warehouse.data.dao.ItemDao
import com.bravo.warehouse.data.entity.ItemEntity

class ItemRepository(
    private val itemDao: ItemDao
) {

    fun getAllItems() = itemDao.getAllItems()

    fun searchItems(search: String) =
        itemDao.searchItems(search)

    suspend fun insertItem(item: ItemEntity): Long {
        return itemDao.insertItem(item)
    }

    suspend fun updateItem(item: ItemEntity) {
        itemDao.updateItem(item)
    }

    suspend fun deactivateItem(itemId: Long) {
        itemDao.deactivateItem(itemId)
    }
}
