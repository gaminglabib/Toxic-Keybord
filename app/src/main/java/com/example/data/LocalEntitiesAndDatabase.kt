package com.example.data

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_themes")
data class ThemeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isBuiltIn: Boolean,
    val isDefault: Boolean,
    val jsonPayload: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val lastUsedAtEpochMs: Long
)

@Entity(tableName = "personal_dictionary")
data class PersonalWordEntity(
    @PrimaryKey val word: String,
    val languageCode: String, // "en" or "bn"
    val frequency: Int = 1,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_history")
data class ClipboardItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val textContent: String,
    val isPinned: Boolean = false,
    val copiedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "imported_fonts")
data class ImportedFontEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val importedAtMs: Long = System.currentTimeMillis()
)

@Dao
interface StudioDao {
    @Query("SELECT * FROM saved_themes ORDER BY lastUsedAtEpochMs DESC, updatedAtEpochMs DESC")
    fun observeAllThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM saved_themes ORDER BY lastUsedAtEpochMs DESC")
    suspend fun getAllThemesOnce(): List<ThemeEntity>

    @Query("SELECT * FROM saved_themes WHERE id = :id LIMIT 1")
    suspend fun getThemeById(id: String): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTheme(entity: ThemeEntity)

    @Query("DELETE FROM saved_themes WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomTheme(id: String)

    // Personal Dictionary
    @Query("SELECT * FROM personal_dictionary WHERE languageCode = :lang ORDER BY frequency DESC LIMIT 200")
    fun observeWordsByLanguage(lang: String): Flow<List<PersonalWordEntity>>

    @Query("SELECT * FROM personal_dictionary ORDER BY frequency DESC")
    fun observeAllWords(): Flow<List<PersonalWordEntity>>

    @Query("SELECT * FROM personal_dictionary WHERE languageCode = :lang AND word LIKE :prefix || '%' ORDER BY frequency DESC LIMIT 5")
    suspend fun findSuggestions(lang: String, prefix: String): List<PersonalWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWord(word: PersonalWordEntity)

    @Query("DELETE FROM personal_dictionary WHERE word = :word")
    suspend fun deleteWord(word: String)

    @Query("DELETE FROM personal_dictionary")
    suspend fun clearPersonalDictionary()

    // Clipboard History
    @Query("SELECT * FROM clipboard_history ORDER BY isPinned DESC, copiedAtMs DESC LIMIT 25")
    fun observeClipboardItems(): Flow<List<ClipboardItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClipboardItem(item: ClipboardItemEntity)

    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun deleteClipboardItem(id: Long)

    @Query("DELETE FROM clipboard_history WHERE isPinned = 0")
    suspend fun clearUnpinnedClipboard()

    @Query("DELETE FROM clipboard_history")
    suspend fun clearAllClipboard()

    // Imported Fonts
    @Query("SELECT * FROM imported_fonts ORDER BY importedAtMs DESC")
    fun observeImportedFonts(): Flow<List<ImportedFontEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedFont(font: ImportedFontEntity)

    @Query("DELETE FROM imported_fonts WHERE id = :id")
    suspend fun deleteImportedFont(id: String)
}

@Database(
    entities = [
        ThemeEntity::class,
        PersonalWordEntity::class,
        ClipboardItemEntity::class,
        ImportedFontEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "toxic_keyboard_studio.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
