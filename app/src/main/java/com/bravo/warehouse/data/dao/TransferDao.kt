package com.bravo.warehouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bravo.warehouse.data.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Query(
        "SELECT * FROM stock_transfers ORDER BY transferDate DESC"
    )
    fun getAllTransfers(): Flow<List<TransferEntity>>

    // جلب جميع أصناف مستند تحويل محدد
    @Query(
        """
        SELECT * FROM stock_transfers
        WHERE transferNumber = :number
        ORDER BY id ASC
        """
    )
    suspend fun getTransfersByNumber(
        number: String
    ): List<TransferEntity>

    // متابعة أصناف مستند التحويل مباشرة
    @Query(
        """
        SELECT * FROM stock_transfers
        WHERE transferNumber = :number
        ORDER BY id ASC
        """
    )
    fun observeTransfersByNumber(
        number: String
    ): Flow<List<TransferEntity>>

    @Query(
        "SELECT * FROM stock_transfers WHERE id = :transferId LIMIT 1"
    )
    suspend fun getTransferById(
        transferId: Long
    ): TransferEntity?

    @Insert
    suspend fun insertTransfer(
        transfer: TransferEntity
    ): Long

    @Query(
        "UPDATE stock_transfers SET isApproved = 1 WHERE id = :transferId AND isApproved = 0"
    )
    suspend fun approveTransfer(
        transferId: Long
    ): Int

    @Query(
        "SELECT COUNT(*) FROM stock_transfers WHERE transferNumber = :number"
    )
    suspend fun countByTransferNumber(
        number: String
    ): Int
}
