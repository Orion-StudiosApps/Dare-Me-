package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "challenge_sessions")
data class ChallengeSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val challengeTitle: String,
    val challengeDescription: String,
    val userResponse: String,
    val score: Int,
    val gradeComment: String,
    val rankTier: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChallengeSessionDao {
    @Query("SELECT * FROM challenge_sessions ORDER BY timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<ChallengeSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChallengeSession)

    @Query("SELECT SUM(score) FROM challenge_sessions")
    suspend fun getTotalScore(): Int?

    @Query("SELECT COUNT(*) FROM challenge_sessions")
    suspend fun getCompletedChallengesCount(): Int

    @Query("DELETE FROM challenge_sessions")
    suspend fun clearAll()
}

@Database(entities = [ChallengeSession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun challengeSessionDao(): ChallengeSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dareme_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
