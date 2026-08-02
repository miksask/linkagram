package io.github.miksask.linkagram.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: HistoryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRedirects(redirects: List<HistoryRedirectEntity>)

    @Query("SELECT COUNT(*) FROM history_entries")
    suspend fun countAll(): Int

    @Query(
        """
        DELETE FROM history_entries
        WHERE id IN (
            SELECT id FROM history_entries
            ORDER BY completed_at_millis ASC, id ASC
            LIMIT :count
        )
        """,
    )
    suspend fun deleteOldest(count: Int): Int

    @Transaction
    suspend fun insertAndPrune(
        entry: HistoryEntryEntity,
        redirects: List<HistoryRedirectEntity>,
        maxEntries: Int,
    ) {
        insertEntry(entry)
        if (redirects.isNotEmpty()) {
            insertRedirects(redirects)
        }
        val overflow = countAll() - maxEntries
        if (overflow > 0) {
            deleteOldest(overflow)
        }
    }

    @Query(
        """
        SELECT * FROM history_entries
        WHERE
            (:likePattern IS NULL OR
                source_url_search LIKE :likePattern ESCAPE '\' OR
                final_url_search LIKE :likePattern ESCAPE '\' OR
                place_name_search LIKE :likePattern ESCAPE '\' OR
                address_search LIKE :likePattern ESCAPE '\')
            AND (:startInclusiveMillis IS NULL OR completed_at_millis >= :startInclusiveMillis)
            AND (:endExclusiveMillis IS NULL OR completed_at_millis < :endExclusiveMillis)
        ORDER BY completed_at_millis DESC, id DESC
        """,
    )
    fun observeMatching(
        likePattern: String?,
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long?,
    ): Flow<List<HistoryEntryEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM history_entries
        WHERE
            (:likePattern IS NULL OR
                source_url_search LIKE :likePattern ESCAPE '\' OR
                final_url_search LIKE :likePattern ESCAPE '\' OR
                place_name_search LIKE :likePattern ESCAPE '\' OR
                address_search LIKE :likePattern ESCAPE '\')
            AND (:startInclusiveMillis IS NULL OR completed_at_millis >= :startInclusiveMillis)
            AND (:endExclusiveMillis IS NULL OR completed_at_millis < :endExclusiveMillis)
        """,
    )
    fun observeMatchingCount(
        likePattern: String?,
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long?,
    ): Flow<Int>

    @Query("SELECT * FROM history_entries WHERE id = :id LIMIT 1")
    suspend fun getEntry(id: String): HistoryEntryEntity?

    @Query(
        """
        SELECT * FROM history_redirects
        WHERE history_entry_id = :historyEntryId
        ORDER BY ordinal ASC
        """,
    )
    suspend fun getRedirects(historyEntryId: String): List<HistoryRedirectEntity>

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM history_entries")
    suspend fun deleteAll(): Int

    @Query(
        """
        DELETE FROM history_entries
        WHERE
            (:likePattern IS NULL OR
                source_url_search LIKE :likePattern ESCAPE '\' OR
                final_url_search LIKE :likePattern ESCAPE '\' OR
                place_name_search LIKE :likePattern ESCAPE '\' OR
                address_search LIKE :likePattern ESCAPE '\')
            AND (:startInclusiveMillis IS NULL OR completed_at_millis >= :startInclusiveMillis)
            AND (:endExclusiveMillis IS NULL OR completed_at_millis < :endExclusiveMillis)
        """,
    )
    suspend fun deleteMatching(
        likePattern: String?,
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long?,
    ): Int

    @Transaction
    suspend fun restoreEntry(
        entry: HistoryEntryEntity,
        redirects: List<HistoryRedirectEntity>,
    ) {
        insertEntry(entry)
        if (redirects.isNotEmpty()) {
            insertRedirects(redirects)
        }
    }
}
