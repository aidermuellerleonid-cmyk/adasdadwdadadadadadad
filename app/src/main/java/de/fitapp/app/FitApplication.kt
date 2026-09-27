package de.fitapp.app

import android.app.Application
import de.fitapp.app.data.AppDatabase
import de.fitapp.app.data.food.FoodRepository

class FitApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val foodRepository: FoodRepository by lazy { FoodRepository(this, database.foodDao()) }
}
