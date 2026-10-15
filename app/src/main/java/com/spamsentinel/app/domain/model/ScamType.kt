package com.spamsentinel.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ScamType(val displayName: String) {
    NONE("General"),
    PHISHING_KYC("Phishing / KYC"),
    LOTTERY_PRIZE("Lottery / Prize"),
    LOAN_CREDIT("Loan / Credit"),
    JOB_SCAM("Job Scam"),
    CRYPTO_INVESTMENT("Crypto / Investment"),
    DELIVERY_PARCEL("Parcel / Delivery"),
    ELECTRICITY_BILL("Electricity / Bill"),
    PROMOTION("Promotion / Marketing")
}
