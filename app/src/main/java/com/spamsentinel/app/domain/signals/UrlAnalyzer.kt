package com.spamsentinel.app.domain.signals

import java.net.URI

data class UrlAnalysisResult(
    val hasUrl: Boolean,
    val hasRiskyUrl: Boolean,
    val host: String? = null,
    val warning: String? = null
)

object UrlAnalyzer {

    private val urlPattern = Regex("""(https?://[^\s]+|www\.[^\s]+|[a-zA-Z0-9-]+\.(xyz|top|click|icu|live|shop|cfd|link|site|fun|zip|apk)[^\s]*)""", RegexOption.IGNORE_CASE)
    private val shorteners = setOf("bit.ly", "tinyurl.com", "t.co", "rb.gy", "cutt.ly", "is.gd", "ow.ly", "buff.ly", "shorturl.at")
    private val riskyTlds = setOf("xyz", "top", "click", "icu", "live", "shop", "cfd", "link", "site", "fun", "zip")
    private val ipPattern = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")
    private val brandLookAlikes = listOf("hdfc", "sbi", "icici", "axis", "paytm", "phonepe", "gpay", "yono", "kyc", "income-tax", "challan")

    fun analyze(messageBody: String): UrlAnalysisResult {
        val matches = urlPattern.findAll(messageBody).map { it.value }.toList()
        if (matches.isEmpty()) {
            return UrlAnalysisResult(hasUrl = false, hasRiskyUrl = false)
        }

        var highestRiskHost: String? = null
        var highestWarning: String? = null
        var isRisky = false

        for (rawUrl in matches) {
            val formatted = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) "https://$rawUrl" else rawUrl
            val host = try {
                URI(formatted).host?.lowercase() ?: rawUrl.lowercase()
            } catch (e: Exception) {
                rawUrl.lowercase()
            }

            highestRiskHost = host
            val tld = host.substringAfterLast('.', "")

            when {
                shorteners.contains(host) -> {
                    isRisky = true
                    highestWarning = "URL Shortener ($host) hiding destination! Do not tap."
                }
                ipPattern.matches(host) -> {
                    isRisky = true
                    highestWarning = "Raw IP Host ($host) detected! High risk scam link."
                }
                host.startsWith("xn--") -> {
                    isRisky = true
                    highestWarning = "Punycode domain ($host) obfuscation detected!"
                }
                riskyTlds.contains(tld) -> {
                    isRisky = true
                    highestWarning = "High-risk TLD (.$tld) detected on $host!"
                }
                rawUrl.lowercase().contains(".apk") -> {
                    isRisky = true
                    highestWarning = "Direct Android Package (.apk) download link!"
                }
                host.contains("-") && host.split("-").size >= 3 -> {
                    isRisky = true
                    highestWarning = "Suspicious multi-hyphenated domain ($host)!"
                }
                brandLookAlikes.any { brand -> host.contains(brand) && !isOfficialDomain(host, brand) } -> {
                    isRisky = true
                    highestWarning = "Brand impersonation attempt detected on $host!"
                }
                else -> {
                    if (highestWarning == null) {
                        highestWarning = "External link: $host. Verify sender before tapping."
                    }
                }
            }

            if (isRisky) break
        }

        return UrlAnalysisResult(
            hasUrl = true,
            hasRiskyUrl = isRisky,
            host = highestRiskHost,
            warning = highestWarning
        )
    }

    private fun isOfficialDomain(host: String, brand: String): Boolean {
        val officialMap = mapOf(
            "hdfc" to listOf("hdfcbank.com", "hdfc.com"),
            "sbi" to listOf("sbi.co.in", "onlinesbi.sbi"),
            "icici" to listOf("icicibank.com"),
            "axis" to listOf("axisbank.com"),
            "paytm" to listOf("paytm.com"),
            "phonepe" to listOf("phonepe.com"),
            "gpay" to listOf("google.com"),
            "income-tax" to listOf("incometax.gov.in")
        )
        val validDomains = officialMap[brand] ?: return false
        return validDomains.any { host.endsWith(it) }
    }
}
