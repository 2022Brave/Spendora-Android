package com.spendora.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.spendora.data.database.CategorySeedData
import com.spendora.data.database.Migrations
import com.spendora.data.database.SpendoraDatabase
import com.spendora.data.entity.AccountEntity
import com.spendora.data.entity.CategoryEntity
import com.spendora.data.entity.SmsAuditEntity
import com.spendora.data.entity.TransactionEntity
import com.spendora.data.model.AccountType
import com.spendora.data.model.CategoryType
import com.spendora.data.model.ReviewStatus
import com.spendora.data.model.TransactionSource
import com.spendora.data.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class SpendoraDatabaseTest {

    private lateinit var db: SpendoraDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpendoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDatabaseOpens() {
        assertNotNull(db)
        assertTrue(db.isOpen)
    }

    @Test
    fun testFreshDatabaseContainsZeroTransactions() = runBlocking {
        assertEquals(0, db.transactionDao().getCount())
        assertEquals(0, db.accountDao().getActiveCount())
        assertEquals(0, db.smsAuditDao().getCount())
    }

    @Test
    fun testDefaultCategoriesSeeded() = runBlocking {
        db.categoryDao().insertAll(CategorySeedData.defaultCategories)
        assertEquals(27, db.categoryDao().getCount())

        val food = db.categoryDao().getByNameAndType("Food", CategoryType.EXPENSE)
        assertNotNull(food)
        assertTrue(food!!.isSystem)

        val salary = db.categoryDao().getByNameAndType("Salary", CategoryType.INCOME)
        assertNotNull(salary)
        assertTrue(salary!!.isSystem)
    }

    @Test
    fun testAccountsPermitSameNameAcrossInstitutions() = runBlocking {
        val acc1 = AccountEntity(
            name = "Salary Account",
            type = AccountType.BANK_ACCOUNT,
            institutionName = "HDFC Bank",
            maskedNumber = "1234"
        )
        val acc2 = AccountEntity(
            name = "Salary Account",
            type = AccountType.BANK_ACCOUNT,
            institutionName = "ICICI Bank",
            maskedNumber = "5678"
        )
        val id1 = db.accountDao().insert(acc1)
        val id2 = db.accountDao().insert(acc2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
        assertNotEquals(id1, id2)
    }

    @Test
    fun testTwoLegitimateIdenticalSmsTransactionsCanCoexist() = runBlocking {
        // First purchase: 10:00 AM (timestamp T1), Rs 50 at Cafe
        val t1 = 1725430000000L
        val fingerprint1 = "LIVE:VM-HDFCBK:$t1:Rs 50 spent on card XX1234 at Cafe"
        val dedupHash1 = "SMS|VM-HDFCBK|$t1|50.0|1234|Cafe"

        val audit1 = SmsAuditEntity(
            sender = "VM-HDFCBK",
            smsTimestamp = t1,
            isFinancial = true,
            fingerprint = fingerprint1
        )
        val auditId1 = db.smsAuditDao().insert(audit1)
        assertTrue(auditId1 > 0)

        val txn1 = TransactionEntity(
            amount = 50.0,
            transactionType = TransactionType.EXPENSE,
            merchant = "Cafe",
            occurredTimestamp = t1,
            source = TransactionSource.SMS_LIVE,
            smsTimestamp = t1,
            dedupHash = dedupHash1
        )
        val txnId1 = db.transactionDao().insert(txn1)
        assertTrue(txnId1 > 0)

        // Second purchase: 2:00 PM (timestamp T2), Rs 50 at same Cafe, same card
        val t2 = 1725444400000L
        val fingerprint2 = "LIVE:VM-HDFCBK:$t2:Rs 50 spent on card XX1234 at Cafe"
        val dedupHash2 = "SMS|VM-HDFCBK|$t2|50.0|1234|Cafe"

        val audit2 = SmsAuditEntity(
            sender = "VM-HDFCBK",
            smsTimestamp = t2,
            isFinancial = true,
            fingerprint = fingerprint2
        )
        val auditId2 = db.smsAuditDao().insert(audit2)
        assertTrue(auditId2 > 0)

        val txn2 = TransactionEntity(
            amount = 50.0,
            transactionType = TransactionType.EXPENSE,
            merchant = "Cafe",
            occurredTimestamp = t2,
            source = TransactionSource.SMS_LIVE,
            smsTimestamp = t2,
            dedupHash = dedupHash2
        )
        val txnId2 = db.transactionDao().insert(txn2)
        assertTrue(txnId2 > 0)

        // Both transactions exist independently
        assertEquals(2, db.transactionDao().getCount())
    }

    @Test
    fun testReferenceNumberNonUniqueSupportsTransfers() = runBlocking {
        val sharedRef = "UPI1234567890"
        val debitLeg = TransactionEntity(
            amount = 1000.0,
            transactionType = TransactionType.TRANSFER,
            merchant = "Self Transfer to ICICI",
            occurredTimestamp = System.currentTimeMillis(),
            source = TransactionSource.SMS_LIVE,
            transactionReferenceNumber = sharedRef,
            dedupHash = "REF|HDFC|1234|$sharedRef"
        )
        val creditLeg = TransactionEntity(
            amount = 1000.0,
            transactionType = TransactionType.TRANSFER,
            merchant = "Self Transfer from HDFC",
            occurredTimestamp = System.currentTimeMillis(),
            source = TransactionSource.SMS_LIVE,
            transactionReferenceNumber = sharedRef,
            dedupHash = "REF|ICICI|5678|$sharedRef"
        )
        val id1 = db.transactionDao().insert(debitLeg)
        val id2 = db.transactionDao().insert(creditLeg)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
    }

    @Test
    fun testRawSmsPrivacyDefaultNull() = runBlocking {
        val txn = TransactionEntity(
            amount = 100.0,
            transactionType = TransactionType.EXPENSE,
            merchant = "Grocery Store",
            occurredTimestamp = System.currentTimeMillis(),
            source = TransactionSource.SMS_LIVE,
            reviewStatus = ReviewStatus.CONFIRMED,
            dedupHash = "hash_privacy_test"
        )
        val id = db.transactionDao().insert(txn)
        val retrieved = db.transactionDao().getById(id)
        assertNotNull(retrieved)
        assertNull("Confirmed transactions must not store raw SMS excerpts", retrieved!!.rawSmsExcerpt)
    }
}
