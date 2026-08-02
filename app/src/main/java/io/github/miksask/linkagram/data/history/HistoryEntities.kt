package io.github.miksask.linkagram.data.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_entries",
    indices = [
        Index(value = ["completed_at_millis"]),
        Index(value = ["source_url_search"]),
        Index(value = ["final_url_search"]),
        Index(value = ["place_name_search"]),
        Index(value = ["address_search"]),
    ],
)
data class HistoryEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "completed_at_millis")
    val completedAtMillis: Long,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String,
    @ColumnInfo(name = "normalized_url")
    val normalizedUrl: String,
    @ColumnInfo(name = "final_url")
    val finalUrl: String,
    @ColumnInfo(name = "final_status_code")
    val finalStatusCode: Int,
    @ColumnInfo(name = "result_type")
    val resultType: String,
    @ColumnInfo(name = "provider")
    val provider: String?,
    @ColumnInfo(name = "place_name")
    val placeName: String?,
    @ColumnInfo(name = "address")
    val address: String?,
    @ColumnInfo(name = "latitude")
    val latitude: Double?,
    @ColumnInfo(name = "longitude")
    val longitude: Double?,
    @ColumnInfo(name = "redirect_count")
    val redirectCount: Int,
    @ColumnInfo(name = "source_url_search")
    val sourceUrlSearch: String,
    @ColumnInfo(name = "final_url_search")
    val finalUrlSearch: String,
    @ColumnInfo(name = "place_name_search")
    val placeNameSearch: String,
    @ColumnInfo(name = "address_search")
    val addressSearch: String,
    @ColumnInfo(name = "record_version")
    val recordVersion: Int,
)

@Entity(
    tableName = "history_redirects",
    primaryKeys = ["history_entry_id", "ordinal"],
    foreignKeys = [
        ForeignKey(
            entity = HistoryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["history_entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["history_entry_id"]),
    ],
)
data class HistoryRedirectEntity(
    @ColumnInfo(name = "history_entry_id")
    val historyEntryId: String,
    @ColumnInfo(name = "ordinal")
    val ordinal: Int,
    @ColumnInfo(name = "from_url")
    val fromUrl: String,
    @ColumnInfo(name = "to_url")
    val toUrl: String?,
    @ColumnInfo(name = "status_code")
    val statusCode: Int?,
)
