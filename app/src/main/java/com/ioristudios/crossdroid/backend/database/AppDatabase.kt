package com.ioristudios.crossdroid.backend.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "devices")
data class DeviceRecordEntity(
    @PrimaryKey val deviceId: String,
    val alias: String,
    val publicFingerprint: String,
    val trustState: Int // 0=Untrusted, 1=Trusted, 2=Blocked
)

@Entity(tableName = "transfers")
data class TransferRecordEntity(
    @PrimaryKey val transferId: String,
    val fileName: String,
    val deviceId: String,
    val deviceName: String,
    val isIncoming: Boolean,
    val totalBytes: Long,
    var bytesTransferred: Long,
    var status: Int, // 0=Pending, 1=InProgress, 2=Paused, 3=Completed, 4=Cancelled, 5=Error
    var destinationPath: String,
    var completedUtc: Long
)

@Dao
interface DeviceRecordDao {
    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<DeviceRecordEntity>>

    @Query("SELECT * FROM devices WHERE deviceId = :id LIMIT 1")
    suspend fun getDeviceById(id: String): DeviceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceRecordEntity)
}

@Dao
interface TransferRecordDao {
    @Query("SELECT * FROM transfers ORDER BY completedUtc DESC")
    fun getAllTransfers(): Flow<List<TransferRecordEntity>>

    @Query("SELECT * FROM transfers WHERE transferId = :id LIMIT 1")
    suspend fun getTransferById(id: String): TransferRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferRecordEntity)

    @Update
    suspend fun updateTransfer(transfer: TransferRecordEntity)
}

@Database(entities = [DeviceRecordEntity::class, TransferRecordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceRecordDao
    abstract fun transferDao(): TransferRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "crossdroid_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
