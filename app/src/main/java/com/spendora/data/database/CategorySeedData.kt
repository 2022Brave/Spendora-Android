package com.spendora.data.database

import com.spendora.data.entity.CategoryEntity
import com.spendora.data.model.CategoryType

object CategorySeedData {
    val defaultCategories: List<CategoryEntity> = listOf(
        // Expenses (21)
        CategoryEntity(name = "Food", type = CategoryType.EXPENSE, icon = "restaurant", colorHex = "#FF5722", isSystem = true, sortOrder = 1),
        CategoryEntity(name = "Groceries", type = CategoryType.EXPENSE, icon = "shopping_cart", colorHex = "#4CAF50", isSystem = true, sortOrder = 2),
        CategoryEntity(name = "Dining", type = CategoryType.EXPENSE, icon = "local_dining", colorHex = "#FF9800", isSystem = true, sortOrder = 3),
        CategoryEntity(name = "Transport", type = CategoryType.EXPENSE, icon = "directions_bus", colorHex = "#00BCD4", isSystem = true, sortOrder = 4),
        CategoryEntity(name = "Fuel", type = CategoryType.EXPENSE, icon = "local_gas_station", colorHex = "#E91E63", isSystem = true, sortOrder = 5),
        CategoryEntity(name = "Shopping", type = CategoryType.EXPENSE, icon = "shopping_bag", colorHex = "#9C27B0", isSystem = true, sortOrder = 6),
        CategoryEntity(name = "Bills", type = CategoryType.EXPENSE, icon = "receipt_long", colorHex = "#607D8B", isSystem = true, sortOrder = 7),
        CategoryEntity(name = "Rent", type = CategoryType.EXPENSE, icon = "home", colorHex = "#795548", isSystem = true, sortOrder = 8),
        CategoryEntity(name = "Utilities", type = CategoryType.EXPENSE, icon = "power", colorHex = "#FFC107", isSystem = true, sortOrder = 9),
        CategoryEntity(name = "Entertainment", type = CategoryType.EXPENSE, icon = "movie", colorHex = "#673AB7", isSystem = true, sortOrder = 10),
        CategoryEntity(name = "Health", type = CategoryType.EXPENSE, icon = "fitness_center", colorHex = "#009688", isSystem = true, sortOrder = 11),
        CategoryEntity(name = "Medicine", type = CategoryType.EXPENSE, icon = "medical_services", colorHex = "#F44336", isSystem = true, sortOrder = 12),
        CategoryEntity(name = "Education", type = CategoryType.EXPENSE, icon = "school", colorHex = "#3F51B5", isSystem = true, sortOrder = 13),
        CategoryEntity(name = "Travel", type = CategoryType.EXPENSE, icon = "flight", colorHex = "#2196F3", isSystem = true, sortOrder = 14),
        CategoryEntity(name = "Subscriptions", type = CategoryType.EXPENSE, icon = "loyalty", colorHex = "#8E24AA", isSystem = true, sortOrder = 15),
        CategoryEntity(name = "Personal Care", type = CategoryType.EXPENSE, icon = "spa", colorHex = "#D81B60", isSystem = true, sortOrder = 16),
        CategoryEntity(name = "Insurance", type = CategoryType.EXPENSE, icon = "shield", colorHex = "#455A64", isSystem = true, sortOrder = 17),
        CategoryEntity(name = "EMI", type = CategoryType.EXPENSE, icon = "account_balance", colorHex = "#C2185B", isSystem = true, sortOrder = 18),
        CategoryEntity(name = "Investment", type = CategoryType.EXPENSE, icon = "trending_up", colorHex = "#2E7D32", isSystem = true, sortOrder = 19),
        CategoryEntity(name = "Cash Withdrawal", type = CategoryType.EXPENSE, icon = "local_atm", colorHex = "#546E7A", isSystem = true, sortOrder = 20),
        CategoryEntity(name = "Other", type = CategoryType.EXPENSE, icon = "more_horiz", colorHex = "#9E9E9E", isSystem = true, sortOrder = 21),

        // Income (6)
        CategoryEntity(name = "Salary", type = CategoryType.INCOME, icon = "payments", colorHex = "#43A047", isSystem = true, sortOrder = 22),
        CategoryEntity(name = "Freelance", type = CategoryType.INCOME, icon = "laptop", colorHex = "#1E88E5", isSystem = true, sortOrder = 23),
        CategoryEntity(name = "Business", type = CategoryType.INCOME, icon = "store", colorHex = "#FB8C00", isSystem = true, sortOrder = 24),
        CategoryEntity(name = "Interest", type = CategoryType.INCOME, icon = "savings", colorHex = "#00897B", isSystem = true, sortOrder = 25),
        CategoryEntity(name = "Refund", type = CategoryType.INCOME, icon = "replay", colorHex = "#8E24AA", isSystem = true, sortOrder = 26),
        CategoryEntity(name = "Other Income", type = CategoryType.INCOME, icon = "attach_money", colorHex = "#7CB342", isSystem = true, sortOrder = 27)
    )
}
