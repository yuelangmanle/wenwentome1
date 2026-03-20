package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.BookAssetEntity
import com.wenwentome.reader.core.model.AssetRole
import kotlinx.coroutines.flow.Flow

@Dao
interface BookAssetDao {
    @Upsert
    suspend fun upsert(entity: BookAssetEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BookAssetEntity>)

    @Query("SELECT * FROM book_assets WHERE bookId = :bookId AND assetRole = 'PRIMARY_TEXT' LIMIT 1")
    suspend fun findPrimaryAsset(bookId: String): BookAssetEntity?

    @Query("SELECT * FROM book_assets")
    suspend fun getAll(): List<BookAssetEntity>

    @Query("SELECT * FROM book_assets WHERE bookId = :bookId")
    fun observeByBookId(bookId: String): Flow<List<BookAssetEntity>>

    @Query("SELECT * FROM book_assets WHERE bookId = :bookId AND assetRole = :assetRole LIMIT 1")
    suspend fun findByRole(bookId: String, assetRole: AssetRole): BookAssetEntity?

    @Query("DELETE FROM book_assets WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: String)

    @Query("DELETE FROM book_assets WHERE bookId = :bookId AND assetRole = :assetRole")
    suspend fun deleteByRole(bookId: String, assetRole: AssetRole)

    @Query("DELETE FROM book_assets")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<BookAssetEntity>) {
        clearAll()
        upsertAll(entities)
    }
}
