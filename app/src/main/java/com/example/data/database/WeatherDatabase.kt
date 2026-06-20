package com.example.data.database

import android.content.Context
import androidx.room.*
import com.example.data.model.SavedCity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCityDao {
    @Query("SELECT * FROM saved_cities ORDER BY timestamp DESC")
    fun getAllCities(): Flow<List<SavedCity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: SavedCity)

    @Query("DELETE FROM saved_cities WHERE cityName = :cityName")
    suspend fun deleteCityByName(cityName: String)

    @Query("UPDATE saved_cities SET currentTemp = :temp, conditionMain = :conditionMain, timestamp = :timestamp WHERE cityName = :cityName")
    suspend fun updateCityWeather(cityName: String, temp: Float, conditionMain: String, timestamp: Long = System.currentTimeMillis())
}

@Database(entities = [SavedCity::class], version = 1, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun savedCityDao(): SavedCityDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "spark_weather_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class WeatherRepository(private val dao: SavedCityDao) {
    val allSavedCities: Flow<List<SavedCity>> = dao.getAllCities()

    suspend fun insertCity(city: SavedCity) {
        dao.insertCity(city)
    }

    suspend fun deleteCity(cityName: String) {
        dao.deleteCityByName(cityName)
    }

    suspend fun updateCityWeather(cityName: String, temp: Float, condition: String) {
        dao.updateCityWeather(cityName, temp, condition)
    }
}
