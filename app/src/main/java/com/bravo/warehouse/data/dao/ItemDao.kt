package com.bravo.warehouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bravo.warehouse.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE isActive = 1 ORDER BY itemName ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("""
        SELECT * FROM items
        WHERE isActive = 1
        AND (itemName LIKE '%' || :search || '%'
        OR itemCode LIKE '%' || :search || '%')
        ORDER BY itemName ASC
    """)
    fun searchItems(search: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET isActive = 0, updatedAt = :time WHERE id = :itemId")
    suspend fun deactivateItem(
        itemId: Long,
        time: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM items WHERE itemCode = :code")
    suspend fun countByCode(code: String): Int
}
