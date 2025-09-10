package com.bearmod.license.data

import android.content.Context
import android.content.SharedPreferences

class SellerAccountManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("seller_account", Context.MODE_PRIVATE)

    fun getBalance(): Double = java.lang.Double.longBitsToDouble(
        prefs.getLong(KEY_BALANCE, java.lang.Double.doubleToLongBits(DEFAULT_BALANCE))
    )

    fun setBalance(value: Double) {
        prefs.edit().putLong(KEY_BALANCE, java.lang.Double.doubleToLongBits(value)).apply()
    }

    fun tryDeduct(amount: Double): Boolean {
        val current = getBalance()
        return if (amount <= 0) {
            true
        } else if (current >= amount) {
            setBalance(current - amount)
            true
        } else {
            false
        }
    }

    companion object {
        private const val KEY_BALANCE = "balance"
        private const val DEFAULT_BALANCE = 1000.0

        @Volatile private var instance: SellerAccountManager? = null
        fun get(context: Context): SellerAccountManager =
            instance ?: synchronized(this) {
                instance ?: SellerAccountManager(context.applicationContext).also { instance = it }
            }
    }
}
