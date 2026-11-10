package com.example.util

import com.example.data.model.Account
import com.example.data.model.DebtRecord
import com.example.data.model.Product

enum class DashboardSearchFilter(val labelAr: String, val labelEn: String) {
    ALL("الكل", "All"),
    ACCOUNTS("الحسابات", "Accounts"),
    DEBTS("الديون والذمم", "Debts"),
    INVENTORY("المخزون والمواد", "Inventory")
}

data class DashboardSearchResults(
    val query: String = "",
    val matchingAccounts: List<Account> = emptyList(),
    val matchingDebts: List<DebtRecord> = emptyList(),
    val matchingProducts: List<Product> = emptyList()
) {
    val totalCount: Int get() = matchingAccounts.size + matchingDebts.size + matchingProducts.size
    val isEmpty: Boolean get() = totalCount == 0
    val hasMatches: Boolean get() = totalCount > 0
}

object DashboardSearchHelper {
    /**
     * Filters accounts, debts, and inventory products by name (case-insensitive and trimmed).
     * Also supports matching account code/type, debt phone, and product barcode/category.
     */
    fun filter(
        query: String,
        accounts: List<Account>,
        debts: List<DebtRecord>,
        products: List<Product>
    ): DashboardSearchResults {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return DashboardSearchResults(query = "")
        }

        val filteredAccounts = accounts.filter { account ->
            account.name.contains(trimmed, ignoreCase = true) ||
            account.code.contains(trimmed, ignoreCase = true) ||
            account.type.contains(trimmed, ignoreCase = true)
        }

        val filteredDebts = debts.filter { debt ->
            debt.debtorName.contains(trimmed, ignoreCase = true) ||
            debt.debtorPhone.contains(trimmed, ignoreCase = true)
        }

        val filteredProducts = products.filter { product ->
            product.name.contains(trimmed, ignoreCase = true) ||
            product.barcode.contains(trimmed, ignoreCase = true) ||
            product.category.contains(trimmed, ignoreCase = true)
        }

        return DashboardSearchResults(
            query = trimmed,
            matchingAccounts = filteredAccounts,
            matchingDebts = filteredDebts,
            matchingProducts = filteredProducts
        )
    }
}
