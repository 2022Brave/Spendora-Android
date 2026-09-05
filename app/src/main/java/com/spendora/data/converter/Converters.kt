package com.spendora.data.converter

import androidx.room.TypeConverter
import com.spendora.data.model.AccountType
import com.spendora.data.model.CategoryType
import com.spendora.data.model.ReviewStatus
import com.spendora.data.model.TransactionSource
import com.spendora.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { enumValueOf<TransactionType>(it) }

    @TypeConverter
    fun fromReviewStatus(value: ReviewStatus?): String? = value?.name

    @TypeConverter
    fun toReviewStatus(value: String?): ReviewStatus? =
        value?.let { enumValueOf<ReviewStatus>(it) }

    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? =
        value?.let { enumValueOf<AccountType>(it) }

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource?): String? = value?.name

    @TypeConverter
    fun toTransactionSource(value: String?): TransactionSource? =
        value?.let { enumValueOf<TransactionSource>(it) }

    @TypeConverter
    fun fromCategoryType(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? =
        value?.let { enumValueOf<CategoryType>(it) }
}
