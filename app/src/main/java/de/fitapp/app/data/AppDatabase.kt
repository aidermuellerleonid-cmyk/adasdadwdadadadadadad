package de.fitapp.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import de.fitapp.app.data.dao.*
import de.fitapp.app.data.entity.*

class Converters {
    @TypeConverter fun goalToString(g: Goal) = g.name
    @TypeConverter fun stringToGoal(s: String) = Goal.valueOf(s)

    @TypeConverter fun genderToString(g: Gender) = g.name
    @TypeConverter fun stringToGender(s: String) = Gender.valueOf(s)

    @TypeConverter fun activityToString(a: ActivityLevel) = a.name
    @TypeConverter fun stringToActivity(s: String) = ActivityLevel.valueOf(s)

    @TypeConverter fun expToString(e: TrainingExperience) = e.name
    @TypeConverter fun stringToExp(s: String) = TrainingExperience.valueOf(s)

    @TypeConverter fun sourceToString(s: FoodSource) = s.name
    @TypeConverter fun stringToSource(s: String) = FoodSource.valueOf(s)

    @TypeConverter fun mealToString(m: MealType) = m.name
    @TypeConverter fun stringToMeal(s: String) = MealType.valueOf(s)

    @TypeConverter fun muscleToString(m: MuscleGroup) = m.name
    @TypeConverter fun stringToMuscle(s: String) = MuscleGroup.valueOf(s)
}

@Database(
    entities = [
        UserProfile::class,
        FoodItemEntity::class,
        DiaryEntryEntity::class,
        ExerciseLogEntity::class,
        SleepEntryEntity::class,
        WeightEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sleepDao(): SleepDao
    abstract fun weightDao(): WeightDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitapp.db"
                ).build().also { INSTANCE = it }
            }
    }
}
