package com.spendora.data.location

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendora.data.backup.DataBackupManager
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.model.TransactionType
import com.spendora.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class Stage10LocationBrandingTest {

    private lateinit var db: SpendoraDatabase
    private lateinit var txnRepo: TransactionRepository
    private lateinit var backupManager: DataBackupManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpendoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        txnRepo = TransactionRepository(
            transactionDao = db.transactionDao(),
            smsAuditDao = db.smsAuditDao(),
            accountDao = db.accountDao()
        )
        backupManager = DataBackupManager(db)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testTransactionCreatedWithoutLocationHasNullCoordinates() = runBlocking {
        val id = txnRepo.createManualTransaction(
            amount = 120.0,
            type = TransactionType.EXPENSE,
            merchant = "Bakery",
            categoryId = null,
            accountId = null,
            occurredTimestamp = 1725470000000L
        )

        val txn = db.transactionDao().getById(id)!!
        assertNull(txn.latitude)
        assertNull(txn.longitude)
        assertNull(txn.locationLabel)
    }

    @Test
    fun testTransactionWithOptionalLocationAttachment() = runBlocking {
        val lat = 29.94570
        val lon = 78.16420
        val id = txnRepo.createManualTransaction(
            amount = 450.0,
            type = TransactionType.EXPENSE,
            merchant = "Cafe Haridwar",
            categoryId = null,
            accountId = null,
            occurredTimestamp = 1725470000000L,
            latitude = lat,
            longitude = lon,
            locationLabel = "Haridwar Ghat"
        )

        val txn = db.transactionDao().getById(id)!!
        assertEquals(lat, txn.latitude!!, 0.0001)
        assertEquals(lon, txn.longitude!!, 0.0001)
        assertEquals("Haridwar Ghat", txn.locationLabel)
    }

    @Test
    fun testRemoveLocationClearsCoordinates() = runBlocking {
        val id = txnRepo.createManualTransaction(
            amount = 300.0,
            type = TransactionType.EXPENSE,
            merchant = "Store",
            categoryId = null,
            accountId = null,
            occurredTimestamp = 1725470000000L,
            latitude = 29.94570,
            longitude = 78.16420
        )

        val success = txnRepo.removeTransactionLocation(id)
        assertTrue(success)

        val txn = db.transactionDao().getById(id)!!
        assertNull(txn.latitude)
        assertNull(txn.longitude)
        assertEquals(300.0, txn.amount, 0.001)
    }

    @Test
    fun testLocationSurvivesBackupAndRestore() = runBlocking {
        val lat = 29.94570
        val lon = 78.16420
        txnRepo.createManualTransaction(
            amount = 999.0,
            type = TransactionType.EXPENSE,
            merchant = "Hotel",
            categoryId = null,
            accountId = null,
            occurredTimestamp = 1725470000000L,
            latitude = lat,
            longitude = lon,
            locationLabel = "Haridwar"
        )

        val jsonBackup = backupManager.exportToJson()
        assertTrue(jsonBackup.contains(""latitude": 29.9457"))
        assertTrue(jsonBackup.contains(""longitude": 78.1642"))

        // Restore
        val restored = backupManager.restoreFromJson(jsonBackup)
        assertTrue(restored)

        val txns = txnRepo.getAllTransactions().first()
        assertEquals(1, txns.size)
        assertEquals(lat, txns[0].latitude!!, 0.0001)
        assertEquals(lon, txns[0].longitude!!, 0.0001)
        assertEquals("Haridwar", txns[0].locationLabel)
    }
}
