package com.spamsentinel.app.domain.signals

import com.spamsentinel.app.domain.model.ScamType
import java.util.Locale

object ScamTypeTagger {

    fun tag(text: String): ScamType {
        val t = text.lowercase(Locale.ROOT)
        return when {
            containsAny(t, "kyc", "pan card", "bank account suspend", "verify identity", "blocked account", "update pan", "ebank") -> ScamType.PHISHING_KYC
            containsAny(t, "lottery", "prize", "won", "claim reward", "lucky draw", "jackpot", "car winner", "cash prize") -> ScamType.LOTTERY_PRIZE
            containsAny(t, "loan", "pre-approved", "instant credit", "0% interest", "cibil", "credit limit", "no doc loan") -> ScamType.LOAN_CREDIT
            containsAny(t, "part-time", "job", "earn daily", "work from home", "hr recruiter", "salary per day", "telegram job") -> ScamType.JOB_SCAM
            containsAny(t, "crypto", "bitcoin", "investment", "double money", "trading signal", "binance", "usdt", "return daily") -> ScamType.CRYPTO_INVESTMENT
            containsAny(t, "parcel", "delivery", "courier", "customs fee", "address update", "india post", "fedex", "failed delivery") -> ScamType.DELIVERY_PARCEL
            containsAny(t, "electricity", "power bill", "disconnection", "tonight 9:30", "officer contact", "update bill") -> ScamType.ELECTRICITY_BILL
            containsAny(t, "offer", "discount", "sale", "flat 50%", "cashback", "coupon", "promo") -> ScamType.PROMOTION
            else -> ScamType.NONE
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
