package com.spendora

import android.app.Application
import com.spendora.data.database.SpendoraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SpendoraApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: SpendoraDatabase by lazy {
        SpendoraDatabase.getInstance(this, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
