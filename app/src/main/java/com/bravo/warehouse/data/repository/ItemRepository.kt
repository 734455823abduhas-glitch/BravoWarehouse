package com.bravo.warehouse.data.repository

import com.bravo.warehouse.data.dao.ItemDao
import com.bravo.warehouse.data.entity.ItemEntity

class ItemRepository(
    private val itemDao: ItemDao
) {

    // الأصناف النشطة لشاشات الإدخال
    fun getAllItems() =
        itemDao.getAllItems()

    // جميع الأصناف للتقارير والسجل التاريخي
    fun getAllItemsIncludingInactive() =
        itemDao.getAllItemsIncludingInactive()

    fun searchItems(search: String) =
        itemDao.searchItems(search)

    suspend fun insertItem(item: ItemEntity): Long {
        return itemDao.insertItem(item)
    }

    suspend fun updateItem(item: ItemEntity) {
        itemDao.updateItem(item)
    }

    suspend fun deactivateItem(itemId: Long): Boolean {
        val affectedRows = itemDao.deactivateItem(itemId)
        return affectedRows > 0
    }
}
