package com.spendora.data.sms

import com.spendora.data.model.TransactionType
import com.spendora.data.sms.model.ConfidenceLevel
import com.spendora.data.sms.model.ParseStatus
import com.spendora.data.sms.model.RawSmsInput
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class SmsEngineTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val defaultTimestamp = 1725430000000L

    @Test
    fun testNormalBankDebit() {
        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "Dear Customer, INR 1,250.00 debited from A/c XX4321 on 04-SEP-26 to SWIGGY. UPI Ref: 123456789. Bal: INR 15,400.00",
            smsTimestamp = defaultTimestamp
        )
        val res = SmsEngine.parse(input, zone)
        assertEquals(ParseStatus.PARSED_TRANSACTION, res.status)
        assertEquals(TransactionType.EXPENSE, res.transactionType)
        assertEquals(1250.0, res.amount!!, 0.001)
        assertEquals("Swiggy", res.merchant)
        assertEquals("XX4321", res.maskedAccount)
        assertEquals("123456789", res.referenceNumber)
        assertEquals(15400.0, res.balance!!, 0.001)
        assertNull(res.rawSmsExcerpt)
    }

    @Test
    fun testSalaryCredit() {
        val input = RawSmsInput(
            sender = "AX-ICICIB",
            body = "Your A/c XX9876 is credited with INR 85,000.00 on 01-Sep-26 by Salary for August. Bal: INR 1,20,000.00",
            smsTimestamp = defaultTimestamp
        )
        val res = SmsEngine.parse(input, zone)
        assertEquals(ParseStatus.PARSED_TRANSACTION, res.status)
        assertEquals(TransactionType.INCOME, res.transactionType)
        assertEquals(85000.0, res.amount!!, 0.001)
        assertEquals("XX9876", res.maskedAccount)
    }

    @Test
    fun testAtmCashWithdrawal() {
        val input = RawSmsInput(
            sender = "VK-SBIINB",
            body = "Rs 10,000 withdrawn from ATM using card ending 1234 on 04-Sep-26. Available Bal: Rs 45,000.",
            smsTimestamp = defaultTimestamp
        )
        val res = SmsEngine.parse(input, zone)
        assertEquals(ParseStatus.PARSED_TRANSACTION, res.status)
        assertEquals(TransactionType.CASH_WITHDRAWAL, res.transactionType)
        assertEquals(10000.0, res.amount!!, 0.001)
        assertEquals("XX1234", res.maskedAccount)
    }

    @Test
    fun testRefund() {
        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "Refund of Rs 1,499.00 credited to your A/c XX4321 from AMAZON on 04-Sep. Avail Bal: Rs 18,000.",
            smsTimestamp = defaultTimestamp
        )
        val res = SmsEngine.parse(input, zone)
        assertEquals(ParseStatus.PARSED_TRANSACTION, res.status)
        assertEquals(TransactionType.REFUND, res.transactionType)
        assertEquals(1499.0, res.amount!!, 0.001)
        assertEquals("Amazon", res.merchant)
    }

    @Test
    fun testTransferBetweenAccounts() {
        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "INR 10,000.00 transferred from your A/c XX1234 to A/c XX5678 via IMPS. Ref: 987654321.",
            smsTimestamp = defaultTimestamp
        )
        val res = SmsEngine.parse(input, zone)
        assertEquals(ParseStatus.PARSED_TRANSACTION, res.status)
        assertEquals(TransactionType.TRANSFER, res.transactionType)
        assertEquals(10000.0, res.amount!!, 0.001)
    }

    @Test
    fun testOtpRejection() {
        val input = RawSmsInput(
            sender = "VK-SBIINB",
            body = "482913 is your OTP for transaction of INR 5,000.00 at Amazon. Do not share this OTP with anyone.",
            smsTimestamp = defaultTimestamp
        )
        val res = SmsEngine.parse(input, zone)
        assertEquals(ParseStatus.IGNORE, res.status)
        assertEquals("OTP_OR_SECURITY_CODE", res.rejectionReason)
    }

    @Test
    fun testPromotionalRejection() {
        val input = RawSmsInput(
            sender = "AD-LOANIN",
            body = "Congratulations! You are eligible for a Pre-approved Personal Loan of Rs 5,00,000 at zero interest. Apply now!",
            smsTimestamp = defaultTimestamp
        )
        val res = SmsEngine.parse(input, zone)
        assertEquals(ParseStatus.IGNORE, res.status)
        assertEquals("PROMOTIONAL_OR_MARKETING", res.rejectionReason)
    }

    @Test
    fun testPromotionalCashbackRejectionVsCreditedCashback() {
        // Promotional offer -> IGNORE
        val promo = RawSmsInput(
            sender = "AD-SHOPPING",
            body = "Shop now on Myntra and get ₹500 cashback on min spend of ₹2000. Limited period offer.",
            smsTimestamp = defaultTimestamp
        )
        assertEquals(ParseStatus.IGNORE, SmsEngine.parse(promo, zone).status)

        // Real financial credit -> PARSED
        val credited = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "Cashback of ₹50.00 credited to your A/c XX1234 for UPI payment. Avail Bal: ₹5,050.00",
            smsTimestamp = defaultTimestamp
        )
        val credRes = SmsEngine.parse(credited, zone)
        assertNotEquals(ParseStatus.IGNORE, credRes.status)
        assertEquals(50.0, credRes.amount!!, 0.001)
    }

    @Test
    fun testAmbiguousTransactionPendingReview() {
        val input = RawSmsInput(
            sender = "VK-ALERT",
            body = "Your account was debited by Rs 500.00.",
            smsTimestamp = defaultTimestamp
        )
        val res = SmsEngine.parse(input, zone)
        assertEquals(ParseStatus.PENDING_REVIEW, res.status)
        assertEquals(500.0, res.amount!!, 0.001)
        assertNotNull(res.rawSmsExcerpt)
    }

    @Test
    fun testSuppliedTimestampPreserved() {
        val input = RawSmsInput(
            sender = "VM-HDFCBK",
            body = "Rs 200 debited from A/c XX1234 at Starbucks.",
            smsTimestamp = 1600000000000L
        )
        val res = SmsEngine.parse(input, zone)
        assertEquals(1600000000000L, res.occurredTimestamp)
    }
}
